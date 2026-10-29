"""
gs-platform-upgrades :: orchestrate-platform-upgrades

Dynamic workflow that sequences the platform upgrade playbooks in this plugin against one service
repository WITHOUT disrupting developers:

  assess ─► plan (deterministic, in-script) ─► for each stage in order:
              upgrade (one PR)  ─►  branch-care (merge-tree every feature branch, push preview/*
              branches, comment on dev PRs)  ─►  review-gate (PR merged? stacked? blocked?)

Run with the `run_workflow` tool:  script_path=<this file>, workflow_name="gs-platform-upgrades".
Configuration is via environment variables (all optional):

  GS_TARGET_REPO       github.com/<org>/<repo> of the service        (default COG-GTM/spring-boot-realworld-example-app)
  GS_BASE_BRANCH       integration branch                            (default gs/main)
  GS_TARGET_BOOT       highest Spring Boot major to reach (3 or 4)   (default 4)
  GS_TARGET_JDK        JDK to upgrade to                             (default 17)
  GS_GATE_POLICY       what to do when a stage PR is not merged yet:
                         wait   -> stop the run; merge the PR and resume with run_id   (production default)
                         stack  -> open the next PR stacked on the previous branch     (demo / fast-follow)
                       (default wait)
  GS_MAX_STAGES        cap the number of stages executed in this run (wave size)      (default 6)
  GS_FEATURE_GLOB      branch pattern treated as developer work      (default feature/CLT-*)
  Any of these may also be set in a `run.json` next to this file (run_workflow passes no env).
  GS_PLUGIN_ROOT       plugin checkout, used to embed SKILL.md text into child prompts
"""
import asyncio
import fnmatch
import json
import os
from pathlib import Path

# ----------------------------------------------------------------------------- configuration
# `run_workflow` copies this file into a temporary directory before executing it, so `__file__` does not
# point at the plugin checkout. Locate the checkout explicitly (GS_PLUGIN_ROOT, then well-known places) and
# read run.json / the SKILL.md files relative to it.
SKILL_DIR = "skills/orchestrate-platform-upgrades"

def _discover_plugin_root():
    if os.environ.get("GS_PLUGIN_ROOT"):
        return Path(os.environ["GS_PLUGIN_ROOT"])
    candidates = []
    try:
        candidates.append(Path(__file__).resolve().parents[2])
    except (NameError, IndexError):
        pass
    home = Path.home()
    candidates.append(home / "gs-platform-upgrades")
    for pattern in (f"*/{SKILL_DIR}/workflow.py", f"repos/*/{SKILL_DIR}/workflow.py"):
        candidates.extend(p.parents[2] for p in sorted(home.glob(pattern)))
    candidates.extend(p.parents[2] for p in sorted(Path("/opt/.devin/plugins/cache").glob(f"*/*/{SKILL_DIR}/workflow.py")))
    for root in candidates:
        if (root / SKILL_DIR / "SKILL.md").exists():
            return root
    return None

PLUGIN_ROOT = _discover_plugin_root()

def _run_config():
    if PLUGIN_ROOT is None:
        return {}
    try:
        p = PLUGIN_ROOT / SKILL_DIR / "run.json"
        return json.loads(p.read_text()) if p.exists() else {}
    except (OSError, ValueError):
        return {}

_RUN_CFG = _run_config()

def _cfg(key, default):
    return os.environ.get(key, _RUN_CFG.get(key, default))

TARGET_REPO = _cfg("GS_TARGET_REPO", "COG-GTM/spring-boot-realworld-example-app")
BASE_BRANCH = _cfg("GS_BASE_BRANCH", "gs/main")
TARGET_BOOT = int(_cfg("GS_TARGET_BOOT", "4"))
TARGET_JDK = int(_cfg("GS_TARGET_JDK", "17"))
GATE_POLICY = _cfg("GS_GATE_POLICY", "wait")
MAX_STAGES = int(_cfg("GS_MAX_STAGES", "6"))
FEATURE_GLOB = _cfg("GS_FEATURE_GLOB", "feature/CLT-*")

ENV_NOTE = f"""
Environment notes: the repository's default branch is an unrelated project; the service lives on `{BASE_BRANCH}`
and its `feature/*`/`platform/*` branches, so always `git fetch origin` and `git checkout` the branch named above
before doing anything. The service is a Maven build: if `mvn` is missing run `sudo apt-get install -y maven`; if the
JDK named in `.java-version` (or the target JDK) is missing install it with `sudo apt-get install -y openjdk-<N>-jdk`
and point JAVA_HOME at it (`/usr/lib/jvm/java-<N>-openjdk-amd64`). Use `gh` for all GitHub reads/writes."""

# ----------------------------------------------------------------------------- upgrade catalog
# Order is the GS canonical sequence; `depends_on` is enforced, `needed` is evaluated against the
# detected state, `footprint` globs are used to work out which feature branches each stage can hurt.
CATALOG = [
    {
        "id": "clm-fix", "skill": "clm-scan-fix", "branch": "platform/01-clm-fix",
        "title": "CLM remediation", "depends_on": [],
        "needed": lambda s: s["clm_blocking"] > 0,
        "footprint": ["pom.xml", "src/main/java/**/*Formatter*.java", "src/main/java/**/*Loader*.java"],
        "minutes": 30,
    },
    {
        "id": "pipelines-v2", "skill": "managed-pipelines-v2", "branch": "platform/02-managed-pipelines-v2",
        "title": "Managed Pipelines v2", "depends_on": ["clm-fix"],
        "needed": lambda s: s["pipeline_version"] != "v2",
        "footprint": ["Jenkinsfile", ".gs/pipeline.yml", ".github/workflows/*", ".github/CODEOWNERS"],
        "minutes": 20,
    },
    {
        "id": "jdk", "skill": "jdk-upgrade", "branch": f"platform/03-jdk-{TARGET_JDK}",
        "title": f"JDK {TARGET_JDK}", "depends_on": ["pipelines-v2"],
        "needed": lambda s: s["java_version"] < TARGET_JDK,
        "footprint": ["pom.xml", ".java-version", ".gs/pipeline.yml", "Jenkinsfile", ".github/workflows/*"],
        "minutes": 30,
    },
    {
        "id": "boot3", "skill": "springboot-2-to-3", "branch": "platform/04-spring-boot-3",
        "title": "Spring Boot 3.5", "depends_on": ["jdk"],
        "needed": lambda s: s["boot_major"] == 2 and TARGET_BOOT >= 3,
        "footprint": ["pom.xml", "src/main/**", "src/test/**"],
        "minutes": 60,
    },
    {
        "id": "gs-auth", "skill": "gs-auth-migration", "branch": "platform/05-gs-auth",
        "title": "GS Auth", "depends_on": ["boot3"],
        "needed": lambda s: s["auth"] != "gs-auth",
        "footprint": ["pom.xml", "src/main/java/**/security/**", "src/main/java/**/SecurityConfig.java",
                      "src/main/resources/application*.yml", "src/test/**", "scripts/*"],
        "minutes": 45,
    },
    {
        "id": "boot4", "skill": "springboot-3-to-4", "branch": "platform/06-spring-boot-4",
        "title": "Spring Boot 4.0", "depends_on": ["gs-auth"],
        "needed": lambda s: s["boot_major"] <= 3 and TARGET_BOOT >= 4,
        "footprint": ["pom.xml", "src/main/**", "src/test/**"],
        "minutes": 60,
    },
]

GUARDRAILS = """
GUARDRAILS (non-negotiable):
- One upgrade per PR. Do not fold other upgrades in, even if they look easy.
- Never push to, rebase or force-push any feature/*, bugfix/*, hotfix/* branch. Never push to main.
- Validate with `mvn -B -ntp clean verify` using the JDK in .java-version before opening the PR.
- Commit messages: `PLAT-UPG feature|bug: <summary>`. End the PR description with `Devin-Org: engineering`.
- If validation is red for a reason the playbook does not cover, open the PR as a DRAFT and report status "blocked".
"""

def skill_text(skill):
    if PLUGIN_ROOT is None:
        return "(plugin not on disk; invoke the installed skill `%s` from the gs-platform-upgrades plugin)" % skill
    path = PLUGIN_ROOT / "skills" / skill / "SKILL.md"
    try:
        return path.read_text()
    except OSError:
        return "(SKILL.md not found at %s; invoke the installed skill `%s`)" % (path, skill)

# ----------------------------------------------------------------------------- schemas
ASSESS_SCHEMA = {
    "type": "object",
    "properties": {
        "boot_version": {"type": "string"},
        "boot_major": {"type": "integer"},
        "java_version": {"type": "integer"},
        "pipeline_version": {"type": "string", "enum": ["v1", "v2", "none"]},
        "auth": {"type": "string", "enum": ["legacy-desk-token", "gs-auth", "other"]},
        "clm_blocking": {"type": "integer"},
        "build_green": {"type": "boolean"},
        "feature_branches": {
            "type": "array",
            "items": {"type": "object", "properties": {
                "name": {"type": "string"}, "pr_url": {"type": "string"},
                "ahead": {"type": "integer"}, "files": {"type": "array", "items": {"type": "string"}}}},
        },
        "notes": {"type": "string"},
    },
    "required": ["boot_major", "java_version", "pipeline_version", "auth", "clm_blocking", "feature_branches"],
}

UPGRADE_SCHEMA = {
    "type": "object",
    "properties": {
        "status": {"type": "string", "enum": ["done", "blocked", "not-needed"]},
        "branch": {"type": "string"},
        "pr_url": {"type": "string"},
        "summary": {"type": "string"},
        "validation": {"type": "string"},
        "files_changed": {"type": "array", "items": {"type": "string"}},
    },
    "required": ["status", "branch", "pr_url", "summary"],
}

CARE_SCHEMA = {
    "type": "object",
    "properties": {
        "results": {"type": "array", "items": {"type": "object", "properties": {
            "branch": {"type": "string"},
            "conflicts": {"type": "boolean"},
            "conflicting_files": {"type": "array", "items": {"type": "string"}},
            "preview_branch": {"type": "string"},
            "comment_url": {"type": "string"},
            "note": {"type": "string"}}}},
    },
    "required": ["results"],
}

GATE_SCHEMA = {
    "type": "object",
    "properties": {
        "state": {"type": "string", "enum": ["merged", "approved", "open", "changes_requested", "closed", "draft"]},
        "merge_sha": {"type": "string"},
        "ci": {"type": "string"},
        "note": {"type": "string"},
    },
    "required": ["state"],
}

# ----------------------------------------------------------------------------- planning (deterministic)
def plan_stages(state):
    chosen, skipped = [], []
    for stage in CATALOG:
        if not stage["needed"](state):
            skipped.append((stage["id"], "not needed for detected state"))
            continue
        missing = [d for d in stage["depends_on"] if d not in {c["id"] for c in chosen}
                   and any(x["id"] == d for x in CATALOG) and _needed_or_done(d, state)]
        if missing:
            skipped.append((stage["id"], "dependency not satisfied: %s" % ",".join(missing)))
            continue
        chosen.append(stage)
    return chosen, skipped

def _needed_or_done(stage_id, state):
    stage = next(s for s in CATALOG if s["id"] == stage_id)
    return stage["needed"](state)

def branch_overlap(stage, branches):
    hits = {}
    for b in branches:
        files = [f for f in b.get("files", []) if any(fnmatch.fnmatch(f, g) for g in stage["footprint"])]
        if files:
            hits[b["name"]] = files
    return hits

# ----------------------------------------------------------------------------- agents
async def assess():
    return await agent(
        f"""You are assessing the service repository {TARGET_REPO} (branch `{BASE_BRANCH}`) for a sequenced
platform-upgrade programme. READ ONLY: do not modify or push anything.

Determine and report:
- boot_version / boot_major: Spring Boot parent (or BOM) version in pom.xml / build.gradle.
- java_version: integer from .java-version, java.version or maven.compiler.release.
- pipeline_version: "v1" if a Jenkinsfile using @Library('gs-managed-pipelines@v1') exists, "v2" if .gs/pipeline.yml
  exists, "none" otherwise.
- auth: "legacy-desk-token" if a custom shared-secret/desk-token filter is in use, "gs-auth" if the app is an OAuth2
  resource server validating GS SSO JWTs (gs.auth.* properties), else "other".
- clm_blocking: number of violations with action "FAIL" in .gs/clm-report.json (0 if absent).
- build_green: run `mvn -B -ntp -q clean verify` with the JDK from .java-version and report whether it passes.
- feature_branches: every remote branch matching `{FEATURE_GLOB}` that is ahead of `{BASE_BRANCH}`. For each give
  name, pr_url (open PR for that head branch via `gh pr list --head <branch> --json url`, or "" if none),
  ahead (commit count), and files (paths changed vs merge-base: `git diff --name-only $(git merge-base
  origin/{BASE_BRANCH} origin/<branch>) origin/<branch>`).
Report anything unusual (multi-module, Gradle, Kotlin, extra auth mechanisms) in notes.
{ENV_NOTE}""",
        phase="assess", schema=ASSESS_SCHEMA, label="assess-platform-state", repos=[TARGET_REPO],
        mode="lite", soft_time_limit_minutes=15,
    )

async def upgrade(stage, base_branch, pr_base, index, total, overlaps):
    heads_up = ""
    if overlaps:
        heads_up = ("\nDeveloper branches that touch files in this stage's footprint (do NOT modify them; a separate "
                    "branch-care step will help their owners):\n" + json.dumps(overlaps, sort_keys=True, indent=2))
    stacked = "" if pr_base == BASE_BRANCH else (
        f"\nThis PR is STACKED: the previous platform PR is not merged yet, so branch from `{base_branch}` and open "
        f"the PR with base `{pr_base}`. Say so in the first line of the PR description.")
    return await agent(
        f"""Repository: {TARGET_REPO}. Platform upgrade stage {index}/{total}: **{stage['title']}**.

Create branch `{stage['branch']}` from `origin/{base_branch}` and apply the playbook below exactly. Open a pull
request with base `{pr_base}` titled `[platform {index}/{total}] {stage['title']}`.{stacked}{heads_up}
{GUARDRAILS}
If the playbook's preconditions are not met, or the upgrade is already in place, do not improvise: report status
"blocked" or "not-needed" with an explanation in summary (branch/pr_url may be "").
If `origin/{stage['branch']}` ALREADY EXISTS (an earlier run of this workflow created it), do not recreate it or open a
second PR: check it out, confirm it implements the playbook, re-run the validation, and report status "done" with the
URL of its existing open PR (`gh pr view {stage['branch']} --json url`); open a PR only if none exists.

Report: status, branch, pr_url, a 2-3 sentence summary, validation (e.g. "mvn verify: 15 tests, 0 failures; app
booted, /actuator/health 200"), and files_changed.

===== PLAYBOOK: {stage['skill']} =====
{skill_text(stage['skill'])}
{ENV_NOTE}""",
        phase="upgrade", schema=UPGRADE_SCHEMA, label=f"upgrade-{stage['id']}", repos=[TARGET_REPO],
        soft_time_limit_minutes=stage["minutes"],
    )

async def branch_care(stage, upgrade_result, branches, overlaps):
    if not branches:
        return {"results": []}
    return await agent(
        f"""Repository: {TARGET_REPO}. The platform branch `{upgrade_result['branch']}` ({stage['title']}, PR
{upgrade_result['pr_url']}) has just been opened. Your job is BRANCH CARE for developers' in-flight work. You must
NOT modify, rebase or push to any of their branches.

Developer branches (name, PR, files they change):
{json.dumps(branches, sort_keys=True, indent=2)}

Branches whose files overlap this stage's footprint (highest risk):
{json.dumps(overlaps, sort_keys=True, indent=2)}

For EVERY developer branch:
1. Fetch both branches. Run `git merge-tree --write-tree origin/{upgrade_result['branch']} origin/<feature>`
   (git >= 2.38) to detect conflicts without touching any working tree. Record conflicting files.
2. If there are conflicts: create `preview/<feature-slug>-on-{stage['id']}` from the developer's branch, merge
   `origin/{upgrade_result['branch']}` into it, resolve the conflicts preserving the developer's intent AND the
   platform change, run `mvn -B -ntp -q clean verify` with the JDK in .java-version on the preview branch, and push
   the preview branch. (`<feature-slug>` = branch name without the `feature/` prefix.)
3. If there are no conflicts: still verify the combination builds — create the same preview branch, merge, run the
   build — but only push it if the build FAILS (so the developer can see the breakage), otherwise delete it locally.
4. If the developer branch has an open PR (pr_url non-empty), post ONE comment on it:
   - no conflicts, build green: "Heads-up: platform PR <url> (<title>) merges cleanly with this branch and the
     combined build is green. No action needed."
   - otherwise: list the conflicting files / build failure, name the preview branch, and say the developer can
     `git merge origin/<preview-branch>` or cherry-pick the resolution when ready; no deadline is imposed by the
     platform team.
   If there is no PR, skip the comment and just report.
Never force-push; never push to feature/* or main. Report one result per developer branch (comment_url and
preview_branch may be "").
{ENV_NOTE}""",
        phase="branch-care", schema=CARE_SCHEMA, label=f"branch-care-{stage['id']}", repos=[TARGET_REPO],
        mode="lite", soft_time_limit_minutes=25,
    )

async def review_gate(stage, upgrade_result):
    return await agent(
        f"""Repository: {TARGET_REPO}. Check the review state of pull request {upgrade_result['pr_url']}
(platform stage "{stage['title']}", branch `{upgrade_result['branch']}`). READ ONLY — do not approve, merge, comment
or push.

Use `gh pr view <url> --json state,isDraft,mergedAt,mergeCommit,reviewDecision,statusCheckRollup`.
Report state as: "merged" (include merge_sha), "approved" (open, reviewDecision APPROVED), "changes_requested",
"draft", "closed" (closed without merge), otherwise "open". Summarise CI status in `ci` (e.g. "all checks passed",
"ci pending", "ci failed: <job>").""",
        phase="review-gate", schema=GATE_SCHEMA, label=f"gate-{stage['id']}", repos=[TARGET_REPO],
        mode="lite", soft_time_limit_minutes=5,
    )

# ----------------------------------------------------------------------------- main
async def main():
    await register_workflow({
        "name": "gs-platform-upgrades",
        "description": f"Sequenced platform upgrades for {TARGET_REPO}: assess -> plan -> one reviewable PR per "
                       f"stage with branch-care for developer branches and a review gate between stages "
                       f"(gate policy: {GATE_POLICY}).",
        "product": TARGET_REPO,
        "phases": [
            {"title": "assess", "detail": "Detect platform state and in-flight feature branches", "count": 1},
            {"title": "upgrade", "detail": "Apply one playbook, validate, open one PR",
             "labels": [f"upgrade-{s['id']}" for s in CATALOG]},
            {"title": "branch-care", "detail": "merge-tree each feature branch, push preview/* branches, comment on dev PRs",
             "labels": [f"branch-care-{s['id']}" for s in CATALOG]},
            {"title": "review-gate", "detail": "Wait for / inspect PR review before the next stage",
             "labels": [f"gate-{s['id']}" for s in CATALOG]},
        ],
    })

    log(f"plugin root: {PLUGIN_ROOT}; gate policy: {GATE_POLICY}; target boot {TARGET_BOOT} / jdk {TARGET_JDK}")
    log(f"assessing {TARGET_REPO}@{BASE_BRANCH}")
    state = await assess()
    log("detected state: " + json.dumps({k: v for k, v in state.items() if k != "feature_branches"}, sort_keys=True))
    branches = sorted(state["feature_branches"], key=lambda b: b["name"])
    log(f"{len(branches)} developer branch(es) in flight: " + ", ".join(b["name"] for b in branches))
    if state.get("build_green") is False:
        raise RuntimeError("Baseline build is red on %s; fix the baseline before starting platform upgrades." % BASE_BRANCH)

    stages, skipped = plan_stages(state)
    for sid, why in skipped:
        log(f"skip {sid}: {why}")
    if not stages:
        log("nothing to do: service already at target platform")
        return
    wave = stages[:MAX_STAGES]
    deferred = stages[MAX_STAGES:]
    log("plan (this wave): " + " -> ".join(s["title"] for s in wave)
        + (("  | deferred to next wave: " + ", ".join(s["title"] for s in deferred)) if deferred else ""))
    for s in wave:
        ov = branch_overlap(s, branches)
        log(f"  {s['id']}: footprint overlaps {len(ov)} branch(es)" + (f" -> {sorted(ov)}" if ov else ""))

    total = len(stages)
    base_branch, pr_base = BASE_BRANCH, BASE_BRANCH
    report = []
    for i, stage in enumerate(wave, start=1):
        overlaps = branch_overlap(stage, branches)
        log(f"[{i}/{total}] {stage['title']}: branching from {base_branch}, PR base {pr_base}")

        result = None
        for attempt in (1, 2):
            try:
                result = await upgrade(stage, base_branch, pr_base, i, total, overlaps)
                break
            except WorkflowAgentError as e:
                log(f"[{i}/{total}] upgrade agent failed (attempt {attempt}): {e}")
        if result is None:
            raise RuntimeError(f"stage {stage['id']} failed twice; fix and resume with run_id")
        if result["status"] == "not-needed":
            log(f"[{i}/{total}] {stage['title']} reported not-needed: {result['summary']}")
            report.append((stage, result, None, None))
            continue
        if result["status"] == "blocked":
            raise RuntimeError(f"stage {stage['id']} blocked: {result['summary']} (PR: {result.get('pr_url') or 'none'})")
        log(f"[{i}/{total}] PR opened: {result['pr_url']} — {result.get('validation', '')}")

        try:
            care = await branch_care(stage, result, branches, overlaps)
        except WorkflowAgentError as e:
            log(f"[{i}/{total}] branch-care failed (non-blocking): {e}")
            care = {"results": [], "error": str(e)}
        for r in care["results"]:
            log(f"    {r['branch']}: " + ("CONFLICTS in %s -> %s" % (r.get("conflicting_files"), r.get("preview_branch"))
                                       if r.get("conflicts") else "clean") + (f" ({r['comment_url']})" if r.get("comment_url") else ""))

        is_last = (i == len(wave))
        gate = await review_gate(stage, result)
        log(f"[{i}/{total}] review gate: {gate['state']} {gate.get('ci', '')}")
        report.append((stage, result, care, gate))

        if gate["state"] == "merged":
            base_branch, pr_base = BASE_BRANCH, BASE_BRANCH
        elif gate["state"] in ("changes_requested", "closed"):
            raise RuntimeError(f"stage {stage['id']} PR {result['pr_url']} is {gate['state']}; resolve the review, then resume with run_id")
        elif is_last:
            pass
        elif GATE_POLICY == "stack":
            log(f"[{i}/{total}] PR not merged; stacking the next stage on {result['branch']}")
            base_branch, pr_base = result["branch"], result["branch"]
        else:
            raise RuntimeError(
                f"review gate: {result['pr_url']} ({stage['title']}) is {gate['state']}. Merge it, then resume this run with "
                f"its run_id to continue with {wave[i]['title']}. (Set GS_GATE_POLICY=stack to stack PRs instead.)")

    log("==== wave summary ====")
    for stage, result, care, gate in report:
        line = f"{stage['title']}: {result['status']} {result.get('pr_url', '')}"
        if gate:
            line += f" | gate={gate['state']}"
        if care:
            conflicted = [r['branch'] for r in care['results'] if r.get('conflicts')]
            line += f" | branch-care: {len(care['results'])} checked, {len(conflicted)} conflicted {conflicted}"
        log(line)
    if deferred:
        log("deferred to next wave: " + ", ".join(s["title"] for s in deferred) + " (re-run after this wave is merged)")

asyncio.run(main())
