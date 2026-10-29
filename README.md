# gs-platform-upgrades

Devin plugin: platform upgrade playbooks for Java/Spring services plus the dynamic workflow that sequences them.

```
.devin-plugin/plugin.json
AGENTS.md                              guardrails every skill follows (one PR per upgrade, never touch feature/*)
skills/
  clm-scan-fix/                        1. remediate FAIL violations in .gs/clm-report.json
  managed-pipelines-v2/                2. Jenkinsfile (v1 shared lib) -> .gs/pipeline.yml (v2)
  jdk-upgrade/                         3. JDK 11 -> 17
  springboot-2-to-3/                   4. Boot 2.7 -> 3.5 (jakarta, SecurityFilterChain, springdoc 2)
  gs-auth-migration/                   5. desk tokens -> GS SSO JWT resource server, kill-switch kept
  springboot-3-to-4/                   6. Boot 3.5 -> 4.0 (starter split, Jackson 3, MockitoBean)
  orchestrate-platform-upgrades/       the dynamic workflow (workflow.py) + how to run it
```

## Why a workflow and not one big PR
Goldman's constraint is sequencing. Every stage has a hard dependency on the previous one, every stage
needs its own review, and developers with open feature branches must not wake up to a rebase storm.
The workflow:

1. **Assesses** the repo and lists in-flight `feature/*` branches with the files they touch.
2. **Plans** only the stages that are actually needed, in canonical order, and computes which feature
   branches each stage's footprint will collide with.
3. For each stage: **one PR**, then **branch-care** (merge-tree every feature branch against the platform
   branch, push a `preview/*` branch with conflicts resolved, comment on the developer's PR), then a
   **review gate**. With `GS_GATE_POLICY=wait` the run pauses until the PR is merged and is resumed with
   its `run_id`; with `stack` the next PR is stacked on the previous branch.

## Install
Upload this repo as a plugin (org scope) so child sessions can invoke the skills, or point
`GS_PLUGIN_ROOT` at a checkout: `workflow.py` embeds each `SKILL.md` into the child prompt, so the run also
works where the plugin is not installed.

## Demo target
`gs-collateral-service` (branch `gs/main` of `COG-GTM/spring-boot-realworld-example-app`): Spring Boot 2.7.18 / JDK 11 collateral management service with a v1
Jenkinsfile, legacy `X-Desk-Token` auth, a failing CLM report and three open feature branches:

| branch | footprint | expected branch-care result |
|---|---|---|
| `feature/CLT-4821-corp-bond-eligibility` | `EligibilityService` + test | clean through every stage |
| `feature/CLT-4902-margin-call-dispute` | `SecurityConfig`, controller, service, `javax.validation` | conflicts at Boot 3 (jakarta, SecurityFilterChain) and GS Auth |
| `feature/CLT-4955-notice-csv-export` | `pom.xml` dependency, `javax.servlet` | conflicts at CLM fix (pom) and Boot 3 |
