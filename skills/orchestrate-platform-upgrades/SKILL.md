---
name: orchestrate-platform-upgrades
description: Run the gs-platform-upgrades dynamic workflow against one Java/Spring service repo. Assesses the repo, plans which upgrade playbooks (CLM fix, Managed Pipelines v2, JDK, Spring Boot 2->3, GS Auth, Spring Boot 3->4) are needed, then executes them one reviewable PR at a time with branch-care for in-flight feature branches and a review gate between stages. Use when asked to "modernize", "bring to platform baseline", or run several upgrade playbooks on a service.
---

# Orchestrate platform upgrades

Use this instead of running the individual playbooks by hand whenever more than one applies.

## Run it
1. Invoke the `dynamic-workflows` skill (required before `run_workflow`).
2. Set configuration as environment variables for the run (all optional, defaults in `workflow.py`):
   `GS_TARGET_REPO`, `GS_BASE_BRANCH`, `GS_TARGET_BOOT` (3|4), `GS_TARGET_JDK`,
   `GS_GATE_POLICY` (`wait` = stop after each PR until it is merged, then resume; `stack` = open the
   next PR on top of the previous branch), `GS_MAX_STAGES` (wave size), `GS_PLUGIN_ROOT`.
3. Settings: `run_workflow` passes no environment, so copy `run.example.json` to `run.json` next to
   `workflow.py` and edit it (keys = the `GS_*` names in the workflow header; env vars still win).
4. `run_workflow` with `workflow_name="gs-platform-upgrades"` and
   `script_path=<plugin>/skills/orchestrate-platform-upgrades/workflow.py`.
4. When the run stops at a review gate (policy `wait`), tell the owner which PR needs review. After it
   is merged, re-run `run_workflow` with the same `run_id`; finished stages replay, the gate re-checks.

## What the workflow does
| Phase | Agents | Mode | Output |
|---|---|---|---|
| assess | 1 | lite | Boot/JDK/pipeline/auth/CLM state + every `feature/*` branch with its changed files |
| plan | 0 (in script) | | Stages needed, in canonical order, with footprint overlap per feature branch |
| upgrade | 1 per stage | normal | `platform/NN-<stage>` branch, validated, one PR |
| branch-care | 1 per stage | lite | `git merge-tree` every feature branch vs the platform branch; `preview/*` branch with resolved conflicts; one comment on the developer's PR |
| review-gate | 1 per stage | lite | PR merged / approved / open / changes requested |

Canonical order and why: CLM fix (pipelines v2 gates PRs on CLM) -> Managed Pipelines v2 (gives JDK 17
build agents) -> JDK 17 (Boot 3 needs it) -> Spring Boot 3.5 (Security 6 needed by GS Auth) -> GS Auth
(resource-server config survives Boot 4; legacy adapter would not) -> Spring Boot 4.0.

## Developer-disruption rules the workflow enforces
- Developer branches are never pushed to. Help arrives as a `preview/<slug>-on-<stage>` branch and a PR comment.
- One PR per stage, each independently reviewable; the next stage starts only after the gate.
- `changes_requested` or a closed PR stops the run; a human resolves it and resumes.
- A red baseline build stops the run before any upgrade starts.

## Cost guide
Roughly 3 child sessions per stage plus 1 for assessment. Use `GS_MAX_STAGES` to run in waves.
