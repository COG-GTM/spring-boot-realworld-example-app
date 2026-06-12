---
name: jenkins-to-github-actions-migration
description: "Converted from Devin playbook: Jenkins to GitHub Actions Migration"
triggers:
  - user
  - model
---

# Jenkins to GitHub Actions Migration

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Jenkins to GitHub Actions Migration (playbook-69c0efd79a1048a6915e40c01cbecf1c), macro `!jenkins`

## Procedure

# Jenkins to GitHub Actions Migration Playbook

## Overview
Migrate existing Jenkins CI/CD pipelines to GitHub Actions workflows. This playbook analyzes Jenkins pipelines (Jenkinsfile, shared libraries, plugins) and creates equivalent GitHub Actions workflow files that maintain the same functionality.

## What's Needed From User
- Repository URL containing Jenkins pipeline(s) to migrate
- Branch name where Jenkins pipelines are located (if not main/master)
- Any specific requirements for the GitHub Actions workflows (e.g., self-hosted runners, specific secrets naming conventions)

## Procedure

1. Clone the repository and identify all Jenkins pipeline files
   - Look for `Jenkinsfile` in root directory
   - Check for additional Jenkinsfiles in subdirectories (e.g., `GitOps/Jenkinsfile`, `ci/Jenkinsfile`)
   - Note any `vars/` directory indicating Jenkins Shared Library usage

2. Analyze each Jenkins pipeline to understand its structure
   - Identify stages and their purposes (build, test, scan, deploy, etc.)
   - Document environment variables and parameters used
   - List credentials/secrets referenced (note credential types: username/password, SSH key, file)
   - Note any shared library function calls (e.g., `@Library('Shared') _`)
   - Identify post-build actions (notifications, artifact archiving, downstream triggers)
   - Note agent/node requirements (`agent { label '...' }`, `docker { image ... }`)
   - Identify conditional stages (`when { branch ... }`, `when { expression ... }`)
   - Note any `post { always|success|failure }` blocks
   - Identify quality gates that should fail the build on failure

3. Create the `.github/workflows/` directory if it doesn't exist

4. Handle shared library functions
   - For each `@Library('X') _` reference, identify what functions are called
   - Infer functionality from function names and parameters (e.g., `docker_build("app","tag","registry")` builds and tags a Docker image)
   - Convert to equivalent GitHub Actions steps or shell commands
   - For each function, document in the PR: function name, inferred behavior, and GitHub Actions equivalent
   - Flag any functions with unclear behavior for reviewer confirmation

5. For each Jenkins pipeline, create a corresponding GitHub Actions workflow file
   - Name workflows descriptively (e.g., `ci.yml`, `cd.yml`, `build.yml`)
   - Map Jenkins triggers to GitHub Actions triggers (see Mapping Reference below)
   - Convert each Jenkins stage to GitHub Actions steps
   - Map `post { always|success|failure }` blocks to steps with `if: always()`, `if: success()`, `if: failure()`
   - Convert `when` conditions to `if:` expressions on jobs or steps
   - Configure secrets references using `${{ secrets.SECRET_NAME }}` syntax
   - Preserve quality gate behavior (fail the workflow if gates fail)

6. Handle CI/CD pipeline chaining if multiple pipelines exist
   - Preferred: use `workflow_call` for reusable workflows (works with default GITHUB_TOKEN permissions)
   - Alternative: use `repository_dispatch` events if workflows must be in separate repos
   - Pass parameters via `client_payload` in dispatch or `inputs` in workflow_call
   - If CD workflow pushes to the repo, verify branch protection allows Actions to push (may need a bot branch + PR pattern instead)

7. Verify workflow syntax is valid
   - Check YAML syntax and GitHub Actions-specific issues
   - Ensure all secrets are referenced (not hardcoded)
   - Verify trigger conditions match original Jenkins behavior

8. Create a PR with the new workflow files
   - Include a summary of what each workflow does
   - Document any secrets that need to be configured in repository settings
   - Note any placeholders that need to be updated (e.g., server URLs)
   - List any assumptions made about shared library functions

9. Document the migration in the PR description
   - List the mapping between Jenkins stages and GitHub Actions steps
   - Specify required secrets and their purposes
   - Include testing instructions: trigger workflow via `workflow_dispatch` and verify outputs

## Specifications

**Deliverables:**
- GitHub Actions workflow files in `.github/workflows/` directory
- PR with clear documentation of the migration
- List of required secrets to configure

**Success Criteria:**
- All Jenkins pipeline functionality is preserved in GitHub Actions
- Workflow files pass YAML syntax validation
- PR description includes clear documentation of required secrets and any manual configuration needed

**Validation:**
- Verify all Jenkins stages have corresponding GitHub Actions steps
- Confirm trigger conditions match original Jenkins pipeline behavior
- Ensure secrets are referenced correctly (not hardcoded)
- Verify quality gates preserve fail-on-failure behavior
- Confirm downstream workflow triggering is properly configured
- Check that artifacts are uploaded and notifications are configured

## Jenkins to GitHub Actions Mapping Reference

### Triggers
| Jenkins | GitHub Actions |
|---------|----------------|
| `triggers { pollSCM('H/5 * * * *') }` | `on: schedule: - cron: '*/5 * * * *'` |
| `triggers { upstream('job-name') }` | `on: repository_dispatch: types: [trigger-name]` |
| Push to branch | `on: push: branches: [branch-name]` |
| Manual trigger | `on: workflow_dispatch:` |
| PR trigger | `on: pull_request: branches: [branch-name]` |

### Common Stage Mappings
| Jenkins Stage | GitHub Actions Equivalent |
|---------------|---------------------------|
| `cleanWs()` | Not needed (fresh runner each time) |
| `checkout scm` | `actions/checkout@v4` |
| `tool 'Maven'` | `actions/setup-java@v4` with Maven |
| `tool 'NodeJS'` | `actions/setup-node@v4` |
| `withCredentials([...])` | `${{ secrets.SECRET_NAME }}` |
| `archiveArtifacts` | `actions/upload-artifact@v4` |
| `build job: 'downstream'` | `repository_dispatch` event via curl |
| `emailext` | `dawidd6/action-send-mail@v3` |
| `agent { label 'X' }` | `runs-on: X` or self-hosted runner |
| `agent { docker { image 'X' } }` | `container: image: X` in job |
| `when { branch 'X' }` | `if: github.ref == 'refs/heads/X'` |
| `parallel { ... }` | Matrix strategy or multiple jobs with `needs:` |
| `stash/unstash` | `upload-artifact` / `download-artifact` |
| `timeout(time: X)` | `timeout-minutes: X` on job |
| `retry(N)` | Custom retry logic or `continue-on-error` |
| `post { always }` | Step with `if: always()` |
| `post { success }` | Step with `if: success()` |
| `post { failure }` | Step with `if: failure()` |
| `junit '**/test-results.xml'` | `actions/upload-artifact@v4` for test reports |

### Security Scanning Tools
| Jenkins | GitHub Actions |
|---------|----------------|
| Trivy scan (shared lib) | Install trivy and run `trivy fs .` |
| OWASP Dependency Check | Download and run dependency-check CLI |
| SonarQube analysis | Run `mvn sonar:sonar` with SONAR_TOKEN |

### Docker Operations
| Jenkins | GitHub Actions |
|---------|----------------|
| `docker.build()` | `docker/build-push-action@v5` |
| `docker.withRegistry()` | `docker/login-action@v3` |
| Docker push | `docker/build-push-action@v5` with `push: true` |

### Parameter Passing Between Pipelines
| Jenkins | GitHub Actions |
|---------|----------------|
| `parameters { string(name: 'PARAM') }` | `inputs:` in `workflow_dispatch` or `client_payload` in `repository_dispatch` |
| `${params.PARAM}` | `${{ github.event.inputs.param }}` or `${{ github.event.client_payload.param }}` |

## Advice and Pointers

- **Shared Libraries**: When encountering `@Library('X') _`, infer function behavior from names and parameters. Common patterns:
  - `code_checkout(url, branch)` → `actions/checkout@v4` with ref
  - `docker_build(name, tag, registry)` → `docker/build-push-action@v5`
  - `docker_push(name, tag, registry)` → same action with `push: true`
  - `trivy_scan()` → install trivy and run `trivy fs .`
  - `sonarqube_analysis(tool, key, name)` → `mvn sonar:sonar` with parameters
  - Document any assumptions in the PR description

- **Credentials**: Never hardcode secrets. Always use `${{ secrets.SECRET_NAME }}` syntax. Document all required secrets in the PR description with their purpose.

- **Docker Image Tags**: If Jenkins uses a manual `DOCKER_TAG` parameter, decide whether to preserve it (use `workflow_dispatch` inputs) or replace with `${{ github.sha }}` for automatic tags. Document this decision in the PR.

- **Workspace**: GitHub Actions runners start fresh each time, so `cleanWs()` is unnecessary.

- **Quality Gates**: If Jenkins has quality gate stages (e.g., SonarQube quality gates), ensure the GitHub Actions workflow fails if the gate fails. Don't just run the scan—enforce the gate.

- **Pipeline Chaining**: When using `repository_dispatch` to chain CI→CD workflows, the curl command needs `${{ secrets.GITHUB_TOKEN }}` with appropriate permissions. If dispatch fails due to permissions, consider `workflow_call` as an alternative.

- **Self-hosted Runners**: If the Jenkins pipeline uses specific agent labels or requires tools not available on GitHub-hosted runners, note this in the PR and suggest self-hosted runners.

## Forbidden Actions

- Do not hardcode any secrets, passwords, or API keys in workflow files
- Do not remove existing Jenkins pipeline files without explicit user approval
- Do not change the fundamental behavior or functionality of the pipeline
- Do not skip security scanning steps that exist in the original Jenkins pipeline

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
