---
name: ado-pipeline-to-github-actions-migration
description: "Converted from Devin playbook: ADO Pipeline to GitHub Actions Migration"
triggers:
  - user
  - model
---

# ADO Pipeline to GitHub Actions Migration

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: ADO Pipeline to GitHub Actions Migration (playbook-aa492a6733f94206bb1a2b8ba318d359), macro `!ado_to_gha`

## Procedure

# ADO Pipeline to GitHub Actions Migration

## Overview

Migrate a single Azure DevOps (ADO) pipeline from `parkerduff/azure-pipelines` to an equivalent GitHub Actions workflow. This playbook handles pipelines that consume central shared templates, alt-templates, or inline YAML by resolving all template references, translating every ADO task to its GHA equivalent, and producing a verified, production-ready workflow file with a detailed mapping document.

## What's Needed From User

- **Pipeline name or ADO ID** to migrate (e.g., `portfolio-api-ci` or `102`)
- Any specific requirements (e.g., self-hosted runner labels, secret naming conventions, target environment names)
- Whether to update shared helper scripts for GHA backward compatibility (default: yes)

## Reference Material

### Repository Structure
```
services/<name>/azure-pipelines.yml   — Per-service ADO pipeline definitions
templates/build/                       — Central shared build templates (.NET, Java, Python, Node, Go, Rust)
templates/test/                        — Central shared test templates
templates/release/                     — Central shared release templates (standard, hotfix, preprod, prod)
templates/language/                    — Language-specific setup templates
templates/legacy/                      — Older template versions still in use
alt-templates/                         — Team-maintained alternative templates
build-tools/scripts/                   — Shared helper scripts (Python)
build-tools/yaml/                      — Shared YAML fragments
docs/pipeline-inventory-report.md      — Full inventory with classifications, categories, and risk flags
docs/migration/                        — Per-pipeline migration mapping documents
.github/workflows/                     — Generated GHA workflow files
```

### Pipeline Categories (from inventory report)
| Category | Description | Example |
|---|---|---|
| 1: Central Template Consumers | Use `templates/build/` + `templates/release/` from shared branch | pricing-engine-ci, portfolio-api-ci |
| 2: Alt-Template Consumers | Use `alt-templates/` from team branches | frontend-workbench-ci |
| 3: Hybrid | Mix team-override templates with inline steps | regulatory-reporting-ci |
| 4: Custom Inline | Fully inline YAML, no shared templates | market-sim-ci, ops-control-plane-ci |
| 5: Non-Build Workloads | Scheduled compute/data/compliance jobs on CI infra | notebook-executor, scenario-runner |
| 6: Ad-Hoc / Deprecated | Manual trigger or stale — skip dead pipelines | bulk-reprocess-trades |

### ADO-to-GHA Task Mapping Reference
| ADO Task | GHA Equivalent | Notes |
|---|---|---|
| `UseDotNet@2` | `actions/setup-dotnet@v4` | |
| `NuGetToolInstaller@1` + `NuGetCommand@2` | `dotnet restore` | NuGet bundled with .NET SDK |
| `DotNetCoreCLI@2` (build) | `dotnet build` | |
| `DotNetCoreCLI@2` (test) | `dotnet test` | |
| `DotNetCoreCLI@2` (publish, `publishWebProjects: true`) | `grep` for `Microsoft.NET.Sdk.Web` + per-project `dotnet publish` | Must replicate web-project filtering |
| `PublishBuildArtifacts@1` | `actions/upload-artifact@v4` | |
| `PublishTestResults@2` | `actions/upload-artifact@v4` | No native GHA test tab; consider `dorny/test-reporter@v1` |
| `Maven@4` / `Maven@3` | `mvn` CLI | |
| `JavaToolInstaller@0` | `actions/setup-java@v4` | |
| `UsePythonVersion@0` | `actions/setup-python@v5` | |
| `NodeTool@0` | `actions/setup-node@v4` | |
| `GoTool@0` | `actions/setup-go@v5` | |
| `deployment` job + `environment:` | GHA `environment:` with protection rules | |
| `**` glob patterns (DotNetCoreCLI task) | `find` commands | GHA bash runs without globstar by default |
| ADO env vars (`BUILD_SOURCEBRANCH`, etc.) | Shimmed via `env:` blocks | So helper scripts run unmodified |
| Template repo `resources:` | Eliminated — templates inlined | Templates are in the same repo |

### ADO → GHA Variable Shim Reference
| ADO Variable | GHA Equivalent |
|---|---|
| `$(Build.SourceBranch)` | `${{ github.ref }}` |
| `$(Build.SourceVersion)` | `${{ github.sha }}` |
| `$(Build.BuildId)` | `${{ github.run_id }}` |
| `$(Build.ArtifactStagingDirectory)` | `${{ runner.temp }}/staging` |
| `$(Build.SourcesDirectory)` | `${{ github.workspace }}` |
| `$(Agent.Name)` | `${{ runner.name }}` |
| `$(Agent.OS)` | `${{ runner.os }}` |
| `$(Build.RequestedFor)` | `${{ github.actor }}` |
| `$(Build.DefinitionName)` | `${{ github.workflow }}` |
| `$(System.TeamFoundationCollectionUri)` | `${{ github.server_url }}/` |
| `$(System.TeamProject)` | `${{ github.repository }}` |
| Pipeline URL (ADO format) | `${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}` |

## Azure DevOps MCP Tools

Use the `azure-devops-mcp` MCP server (org: `shawn0864`) to verify pipeline configuration:
- `pipeline_list_pipelines` — list all pipelines in a project
- `pipeline_get_pipeline` — get pipeline details by ID
- `pipeline_list_runs` — list recent runs for a pipeline
- `pipeline_get_run` — get details of a specific run
- `pipeline_preview_pipeline_yaml` — preview the fully expanded YAML (resolves templates)

Note: The ADO MCP org (`shawn0864`) may not contain all referenced pipeline templates (e.g., `contoso-financial/shared-ci-platform`). If `previewRun` fails, fall back to manually expanding templates by reading the YAML files from the repo.

<phase name="Pipeline Discovery & Analysis" id="1">
## Phase 1: Pipeline Discovery & Analysis

1. Read `docs/pipeline-inventory-report.md` to find the target pipeline's entry — note the **ADO ID**, **YAML path**, **category**, **template branch**, **pool**, **owner**, and any **risk flags**.

2. Read the pipeline's ADO YAML file (e.g., `services/<name>/azure-pipelines.yml`). Extract:
   - **Trigger configuration**: branches, paths, PR triggers
   - **Pool / agent**: hosted vs self-hosted, vmImage
   - **Variables**: inline variables and variable group references
   - **Resources**: template repository references (repo name, branch/ref)
   - **Stages**: list each stage with its display name, dependencies, and conditions
   - **Template references**: for each stage/job/step, note the `template:` path and `parameters:` passed

3. Classify the pipeline using the category table above. The category determines the migration approach:
   - **Categories 1-3** (template consumers): You must resolve all template files and inline their logic
   - **Category 4** (custom inline): Direct translation of inline steps, no template resolution needed
   - **Category 5** (non-build workloads): Flag to user — these should NOT migrate to GHA; recommend dedicated compute (K8s CronJob, AWS Batch, etc.)
   - **Category 6** (ad-hoc/deprecated): Check if dead (0 runs + disabled). If dead, recommend skipping. If active ad-hoc, migrate with `workflow_dispatch` trigger.

4. If the pipeline is Category 5 or a dead Category 6 pipeline, message the user with your recommendation and stop. Do not proceed with migration.

<verification>
- The target pipeline's ADO YAML has been read and all stages/templates are documented
- The pipeline category has been identified
- All template references (paths + branch refs) have been listed
- All parameters passed to each template have been recorded
- Any risk flags from the inventory report have been noted
</verification>
</phase>

<phase name="Template Resolution & Task Analysis" id="2">
## Phase 2: Template Resolution & Task Analysis

1. For each template referenced by the pipeline, read the template YAML file **from the correct branch**. The pipeline's `resources.repositories.ref` tells you which branch to use. Use `git show <branch>:<path>` if the template differs across branches.

2. For each template, document every ADO task/step:
   - Task name and version (e.g., `DotNetCoreCLI@2`)
   - Task inputs and their values (resolve `${{ parameters.* }}` with the actual parameter values from the pipeline)
   - Conditions (e.g., `${{ if eq(parameters.runTests, true) }}`)
   - Script steps and what helper scripts they call

3. Read any helper scripts referenced by the templates (typically in `build-tools/scripts/`). Understand:
   - What env vars the script reads (especially ADO-specific ones like `BUILD_SOURCEBRANCH`)
   - What the script does (artifact registration, notifications, attestation generation, etc.)
   - Whether the script constructs URLs using ADO-format patterns

4. Try to verify the pipeline configuration via the Azure DevOps MCP:
   ```
   Use azure-devops-mcp pipeline_get_pipeline with the ADO pipeline ID
   Use azure-devops-mcp pipeline_list_runs to see recent run history
   Use azure-devops-mcp pipeline_preview_pipeline_yaml to get the fully expanded YAML
   ```
   If the MCP org doesn't contain this pipeline's templates, note this and proceed with manual template expansion (which you've already done in steps 1-2).

5. Create a complete expanded view: write out every step that will execute, in order, with all template parameters resolved. This is your "source of truth" for generating the GHA workflow.

<verification>
- Every referenced template has been read from the correct branch
- All ADO tasks have been identified with their full input configurations
- All helper scripts have been read and their env var dependencies documented
- A complete expanded step-by-step execution view has been created
- ADO MCP verification has been attempted (and results or fallback noted)
</verification>
</phase>

<phase name="GHA Workflow Generation" id="3">
## Phase 3: GHA Workflow Generation

1. Create the GHA workflow file at `.github/workflows/<pipeline-name>.yml`. Start with a header comment block:
   ```yaml
   # <pipeline-name> CI workflow
   # Migrated from: <ado-yaml-path> (ADO pipeline ID <id>)
   # Stack: <language/framework>
   # Original template source: <template-branch>
   #
   # ADO templates inlined:
   #   - <template-path-1>
   #   - <template-path-2>
   ```

2. Map triggers:
   - ADO `trigger.branches.include` → GHA `on.push.branches`
   - ADO `trigger.paths.include` → GHA `on.push.paths`
   - Add `on.pull_request` for the main branch (ADO pipelines typically lack this — it provides earlier CI feedback)
   - For branch globs: ADO `release/*` (single-level) can be broadened to GHA `release/**` (recursive) — document this intentional change
   - For scheduled pipelines: ADO `schedules` → GHA `on.schedule` with cron syntax

3. Map environment variables:
   - ADO `variables:` → GHA top-level `env:` block
   - Do NOT use `**` glob patterns in env vars — GHA bash lacks globstar. Use `find` commands in run steps instead.

4. Map each ADO stage to a GHA job:
   - ADO `stages[].stage` → GHA `jobs.<job-name>`
   - ADO `dependsOn` → GHA `needs`
   - ADO `condition` → GHA `if:` (translate ADO expression syntax to GHA expression syntax)
   - ADO `deployment` jobs with `environment:` → GHA `environment:` with protection rules
   - Each job gets `runs-on: ubuntu-latest` (or appropriate runner for self-hosted pools)

5. Map each ADO task to GHA steps using the mapping reference table above. For each step:
   - Add a `# --- mapped from: <ADO-task> ---` comment for traceability
   - Use the correct GHA action version (always `@v4` or latest stable)
   - Translate task inputs to action `with:` parameters or `run:` commands

6. Apply these critical translation rules:
   - **`publishWebProjects: true`**: Replace `dotnet publish $SOLUTION` with a `grep -rl '<Project Sdk="Microsoft.NET.Sdk.Web"'` loop that publishes only web projects
   - **`**` glob patterns**: Replace with `find` commands (e.g., `find services/<name> -name '*.sln'`)
   - **Artifactory registration**: Guard with `if: github.event_name == 'push'` to prevent PR artifact registration
   - **Helper script env vars**: Add `env:` blocks that shim ADO variables to GHA equivalents (see variable shim reference)
   - **Pipeline URL**: Add `PIPELINE_URL` env var with the correct GHA format instead of relying on ADO-style URL construction
   - **Staging directories**: Add `mkdir -p` before any step that writes to `${{ runner.temp }}/staging` on a fresh runner (especially deploy jobs)
   - **`if: always()`**: Add to test result upload steps so results are captured even on test failure

7. Review the complete workflow for correctness:
   - Verify job dependency chain matches ADO stage dependencies
   - Verify deploy conditions match ADO conditions (e.g., main branch only, push only)
   - Verify all secrets are referenced via `${{ secrets.* }}`, never hardcoded
   - Verify `actions/checkout@v4` is the first step in every job

<verification>
- GHA workflow file has been created at `.github/workflows/<pipeline-name>.yml`
- Every ADO stage maps to a GHA job with correct `needs:` dependencies
- Every ADO task maps to a GHA step with a `# --- mapped from:` comment
- No `**` glob patterns are used in shell commands (replaced with `find`)
- `publishWebProjects: true` behavior is replicated where applicable
- Artifactory registration is guarded to push-only events
- Helper script env vars are shimmed correctly
- Pipeline URL uses GHA format (not ADO `_build/results` format)
- Deploy jobs have `mkdir -p` for staging directories
- Workflow YAML is syntactically valid
</verification>
</phase>

<phase name="Mapping Documentation & Script Updates" id="4">
## Phase 4: Mapping Documentation & Script Updates

1. Create a mapping document at `docs/migration/<pipeline-name>-ado-to-gha-mapping.md` with:
   - **Trigger mapping**: ADO trigger config → GHA trigger config, noting any intentional changes
   - **Stage/job mapping table**: Each ADO stage → GHA job, with step-by-step correspondence
   - **Task mapping table**: Each ADO task → GHA action/step, with input translations
   - **Variable mapping**: ADO variables → GHA env vars
   - **Condition mapping**: ADO conditions → GHA `if:` expressions
   - **Integration points**: Artifactory, D2 notifications, compliance attestation — how each is handled
   - **Known gaps**: Any behavioral differences (e.g., test result format mismatches, branch glob scope changes)
   - **Secrets required**: List all secrets the GHA workflow needs configured in repository settings

2. Check if any helper scripts in `build-tools/scripts/` need updates for GHA compatibility:
   - Scripts that construct URLs using ADO env vars (e.g., `SYSTEM_TEAMFOUNDATIONCOLLECTIONURI + SYSTEM_TEAMPROJECT + "/_build/results"`) → add `PIPELINE_URL` env var support with fallback
   - Scripts that read ADO-specific env vars → verify the env shims in the workflow cover them
   - Make all script changes **backward compatible** — they must still work when called from ADO pipelines

3. Present the mapping document and any script changes to the user in the session as a summary before creating the PR.

<verification>
- Mapping document has been created at `docs/migration/<pipeline-name>-ado-to-gha-mapping.md`
- All trigger, stage, task, variable, and condition mappings are documented
- Known gaps and behavioral differences are explicitly called out
- Required secrets are listed
- Helper scripts have been checked and updated if needed (backward compatible)
- The user has been shown a summary of deliverables in the session
</verification>
</phase>

<phase name="PR Creation & Verification" id="5">
## Phase 5: PR Creation & Verification

1. Create a new branch: `devin/<timestamp>-<pipeline-name>-gha-migration`

2. Commit all files:
   - `.github/workflows/<pipeline-name>.yml` — the GHA workflow
   - `docs/migration/<pipeline-name>-ado-to-gha-mapping.md` — the mapping document
   - Any updated helper scripts in `build-tools/scripts/`

3. Create a PR with a description that includes:
   - **Summary**: Which pipeline was migrated, its ADO ID, and category
   - **ADO pipeline analysed**: Source YAML path and templates consumed
   - **What this PR adds**: List of new/modified files
   - **Key translation decisions**: Table of ADO → GHA concept mappings specific to this pipeline
   - **Review & Testing Checklist**: Items for the human reviewer to verify
   - **Notes**: Any caveats, known gaps, or pre-existing issues preserved from ADO

4. Wait for CI to pass. If CI fails, investigate and fix.

5. Address any Devin Review findings that appear on the PR. Common issues to watch for:
   - Missing `mkdir -p` for staging directories on fresh runners
   - ADO-format URLs in helper scripts
   - Missing `publishWebProjects` filtering
   - Shell glob patterns that depend on `globstar`
   - Artifactory registration running on PR builds

6. After addressing review findings, update the PR description with the fixes applied.

<verification>
- PR has been created with all deliverables
- PR description follows the template above
- CI passes
- Devin Review findings have been addressed (if any)
- PR description has been updated with any fixes applied
- The user has been notified with the PR link
</verification>
</phase>

## Specifications

- The GHA workflow must be **functionally equivalent** to the ADO pipeline — same stages, same tasks, same conditions, same artifacts
- Every GHA step must have a `# --- mapped from: <ADO-task> ---` comment for traceability
- Helper script changes must be **backward compatible** with ADO callers
- The workflow file header must document the source ADO pipeline path, ID, and inlined templates
- Do not use `**` glob patterns in shell `run:` commands — use `find` instead
- Do not hardcode secrets — use `${{ secrets.* }}` references
- Guard Artifactory / artifact registration steps to push-only events
- Add `on.pull_request` triggers even if the ADO pipeline lacked them

## Advice and Pointers

- **Template branch matters**: ADO pipelines reference templates from specific branches (main, master, staging/preprod, etc.). The same template can differ significantly across branches. Always read the template from the branch specified in `resources.repositories.ref`.
- **`publishWebProjects: true` is subtle**: This ADO-specific flag filters to projects using `Microsoft.NET.Sdk.Web`. In GHA, replicate with `grep -rl '<Project Sdk="Microsoft.NET.Sdk.Web"'`.
- **GHA bash lacks globstar**: The `**` pattern in GHA bash `run:` steps only matches one directory level (globstar is off by default). Always use `find` commands for recursive file matching.
- **Env var shims keep scripts portable**: Rather than rewriting helper scripts, add `env:` blocks in the workflow that map GHA context vars to the ADO env var names the scripts expect.
- **Pipeline URL format**: ADO constructs `CollectionUri + Project + /_build/results?buildId=X`. In GHA the correct format is `server_url/repository/actions/runs/run_id`. Add a `PIPELINE_URL` env var and update scripts to prefer it.
- **Fresh runners in deploy jobs**: Each GHA job runs on a fresh runner. Directories created in the build job don't exist in deploy jobs. Add `mkdir -p` for any directory a script writes to.
- **Test result format gap**: ADO `PublishTestResults@2` has a native test results viewer. GHA does not. Upload results as artifacts and optionally add `dorny/test-reporter@v1` for richer reporting.
- **Pre-existing ADO issues**: If the ADO pipeline has existing gaps (e.g., TRX/JUnit format mismatch in test normalization), preserve the same behavior in GHA and document it as a pre-existing gap — don't try to fix it during migration.
- **Category 5 pipelines (non-build workloads)** should NOT be migrated to GHA. Recommend dedicated compute instead.
- Use the `docs/pipeline-inventory-report.md` as the authoritative reference for pipeline metadata, categories, risk flags, and ownership.

## Forbidden Actions

- Do not migrate Category 5 (non-build workload) pipelines to GitHub Actions — flag them for the user
- Do not migrate dead pipelines (0 runs + disabled/paused) — recommend skipping
- Do not hardcode secrets or credentials in workflow files
- Do not use `**` glob patterns in GHA bash `run:` steps
- Do not break backward compatibility of helper scripts — ADO pipelines still call them
- Do not modify the ADO pipeline YAML files — this is a migration, not a replacement
- Do not skip creating the mapping document — it is a required deliverable
- Do not guess at template contents — always read the actual template file from the correct branch

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
