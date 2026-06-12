---
name: microservices-migration-orchestrator
description: "Converted from Devin playbook: Microservices Migration Orchestrator"
triggers:
  - user
  - model
---

# Microservices Migration Orchestrator

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Microservices Migration Orchestrator (playbook-70312e71ef2b424cb6837c760000b357), macro `!microservice_migration`

## Procedure

# Microservices Migration Orchestrator

## Overview

This playbook orchestrates a phased monolith-to-microservices migration by reading task definitions from a Jira board, building a dependency graph, and autonomously executing work via parallel Devin batch sessions. It manages a single migration branch, squashing parallel session outputs into it after each batch with automated conflict resolution, and validates the cumulative result before proceeding.

## What's Needed From User

- **Jira project key and board URL** for the migration (e.g., `EM` board at `https://cog-gtm.atlassian.net/jira/software/projects/EM/boards/761`)
- **Target repository** (e.g., `COG-GTM/ftgo-monolith`)
- **Migration branch name** (e.g., `feat/microservices-migration`)
- **Atlassian MCP** configured for Jira access
- **GitHub MCP** configured for repository access
- **Devin API key** — use the environment variable `$MASON_DEVIN_API_KEY` if the user explicitly authorizes API-based session creation as an alternative to `propose_sessions`

## Advice and Pointers

- Tasks within a batch are independent and touch non-overlapping files. If two tasks in the same batch could edit the same file, move one to a later batch.
- Each squash commit on the migration branch represents exactly one Jira task. This keeps the history linear and auditable — one commit per task.
- If a child session's PR fails CI, do not squash it. Flag it in the execution log and re-queue it in the next batch after diagnosing the failure.
- Keep the execution log as a markdown table updated after every batch so the user has a living record of progress.
- When resolving merge conflicts autonomously, prefer the migration branch version for shared config files (e.g., `settings.gradle`, `build.gradle` root) and the incoming PR version for service-specific files. For files both sides meaningfully changed, combine the changes logically (e.g., append new entries to lists, merge configuration blocks).

## Forbidden Actions

- Do not merge any PR that has failing CI checks onto the migration branch.
- Do not reorder phases from the Jira board — phase numbering reflects architectural dependencies.
- Do not modify files outside the scope of the assigned Jira task in any child session prompt.
- Do not delete or force-push the migration branch.
- Do not leave the migration branch in a broken state — if a conflict resolution breaks the build, revert the squash commit immediately before proceeding.
- Do not use the Devin API (`https://api.devin.ai/v1/sessions`) to create sessions unless the user has explicitly instructed you to do so. The `propose_sessions` tool is the only approved method for launching batch sessions — it exists specifically so the user can review and approve sessions before they start. Never interpret a lack of immediate approval as a reason to fall back to the API.
- Do not proceed past a `propose_sessions` call until the user has approved the proposed sessions. Waiting is expected — do not treat a delay in approval as a failure or reason to use an alternative method.

<phase name="Discovery and Dependency Analysis" id="1">
## Phase 1: Discovery and Dependency Analysis

Fetch every issue from the Jira migration project and build a dependency graph that determines execution order.

1. Use the Atlassian MCP `searchJiraIssuesUsingJql` tool to fetch all issues in the project, retrieving fields: `summary`, `issuetype`, `status`, `parent`, `description`.
   - JQL: `project = <PROJECT_KEY> ORDER BY rank ASC`
   - Page through results if more than 50 issues exist.

2. Parse the results and classify each issue:
   - **Epics** = Phases (e.g., "Phase 1: Project Structure & Shared Libraries")
   - **Tasks/Stories** = Executable work items, each parented to a Phase epic

3. For each task, extract its dependencies from the `## Dependencies` section of its Jira description. Build a directed acyclic graph (DAG) where edges point from dependency to dependent.

4. Topologically sort the DAG and group tasks into **execution batches**:
   - **Batch N** contains all tasks whose dependencies are fully satisfied by batches 1 through N-1.
   - Tasks within the same batch have zero dependencies on each other and can run in parallel.
   - Cross-phase parallelism is allowed: a Phase 3 task can appear in the same batch as a Phase 1 task **if and only if** it has zero dependencies on any incomplete task. The constraint is the dependency DAG, not the phase number.

5. For each batch, build a **file overlap matrix**: list the directories/files each task is expected to touch (from Jira descriptions). If two tasks in the same batch share any files, move the lower-priority task to the next batch and note the reason in the execution plan.

6. Present the execution plan to the user as a numbered list of batches, each showing:
   - Batch number
   - Jira keys and summaries
   - Which phase each task belongs to
   - Estimated parallelism (how many sessions in this batch)
   - Any tasks deferred due to file overlap (with explanation)

7. Ask the user to confirm or adjust the execution plan before proceeding.

<verification>
- All Jira issues in the project have been fetched and classified as Epics or Tasks
- A dependency DAG has been constructed with no cycles
- File overlap analysis has been performed for each batch
- Tasks are grouped into execution batches respecting both dependency ordering and file isolation
- The execution plan has been presented to and confirmed by the user
</verification>
</phase>

<phase name="Migration Branch Setup" id="2">
## Phase 2: Migration Branch Setup

Create the migration branch and establish the baseline for squashing work onto it.

1. Clone the target repository if not already available locally.

2. Create the migration branch from the default branch (e.g., `main`):
   ```bash
   git checkout main && git pull origin main
   git checkout -b <MIGRATION_BRANCH>
   git push origin <MIGRATION_BRANCH>
   ```

3. Record the initial commit SHA as `BASE_SHA` — this is the baseline for all future validation.

4. Initialize an **Execution Log** as a markdown file (`migration_execution_log.md`) in the repo root with columns:
   | Batch | Jira Key | Summary | Phase | Session Status | PR Link | Squash Status | Conflicts Resolved |
   Commit and push this file to the migration branch.

5. Confirm the migration branch is visible on the remote and CI runs green on it.

<verification>
- Migration branch exists on the remote repository
- BASE_SHA is recorded
- Execution log file is committed to the migration branch
- CI passes on the migration branch in its initial state
</verification>
</phase>

<phase name="Batch Execution and Squash Loop" id="3">
## Phase 3: Batch Execution and Squash Loop

Iterate through each execution batch: launch parallel sessions, monitor them to completion, squash results onto the migration branch with conflict resolution, and validate.

**Repeat the following steps for each batch in the execution plan:**

### 3a. Launch Parallel Sessions

1. For each task in the current batch, compose a session prompt that includes:
   - The Jira task key, summary, and full description (copy from the Jira issue)
   - Instruction to branch from `<MIGRATION_BRANCH>` (not `main`)
   - Instruction to create a PR targeting `<MIGRATION_BRANCH>` (e.g., `-- repo COG-GTM/<REPO_NAME>`)
   - The repository URL
   - The build and test commands for the project (e.g., `./gradlew clean build test`)
   - A list of directories/files the task is scoped to (derived from the Jira description's technical context), so the session avoids touching unrelated code
   - Any relevant context from previously completed batches (e.g., "Batch 1 established the repo structure under `/services/*`; build on that layout")
   - Acceptance criteria from the Jira task as the definition of done

2. **Launch the batch using the `propose_sessions` tool.** This is the **only** approved method for launching batch sessions. Structure the sessions array with one entry per task, each containing a descriptive `title` (e.g., `"EM-30: Define Microservices Repository Structure"`) and the full prompt as the `message`. If the batch contains many tasks with a repetitive prompt structure, use CSV mode: write a CSV file with per-task fields (jira_key, summary, description, scoped_files, etc.) and provide a single template session with `{column_name}` placeholders.

   **Important `propose_sessions` considerations:**
   - Sessions in a single `propose_sessions` call run in parallel — only include tasks from the same batch (no cross-batch dependencies).
   - The user will be prompted to review and approve the proposed sessions before they start.
   - After the user approves, the sessions will start automatically.

3. **Immediately after calling `propose_sessions`, you MUST stop and wait for user approval.** Call `message_user` with `block_on_user=true` to notify the user that Batch N has been proposed and is awaiting their approval. Include a summary of the tasks in the batch. **Do not continue any work until the user responds or approves the sessions.** Do not poll, do not prepare future batches, do not use any alternative method to launch sessions. Simply wait.

### 3b. Monitor and Collect Results

4. **Only after the user has approved the batch and sessions have started**, begin monitoring. Poll each child session using `get_session` every 3 minutes until all sessions in the batch reach a terminal state (completed or failed). Do not message the user repeatedly while waiting — send a single status update after all sessions finish.

5. Once all sessions are terminal, collect each session's PR link and CI status using `git_view_pr`.

6. Update the execution log with each session's outcome (PR link, pass/fail).

7. If any session failed or produced a PR with failing CI:
   - Log the failure reason in the execution log.
   - Automatically re-queue the failed task into the next batch. Recalculate remaining batches — any task that depends on the re-queued task must also be pushed to a later batch.
   - Notify the user of re-queued tasks and the updated plan. Continue without waiting for approval unless more than 30% of the batch failed, in which case pause and ask the user whether to continue or investigate.

### 3c. Squash onto Migration Branch with Conflict Resolution

8. For each successful PR in the batch, squash-merge it onto the migration branch one at a time:
   ```bash
   git checkout <MIGRATION_BRANCH> && git pull origin <MIGRATION_BRANCH>
   git fetch origin <PR_BRANCH>
   git merge --squash origin/<PR_BRANCH>
   ```

9. **If the squash merge applies cleanly**, commit it:
   ```bash
   git commit -m "squash(Phase <N>): <JIRA_KEY> - <summary>"
   ```

10. **If the squash merge produces conflicts**, resolve them autonomously using these rules:
    - **Shared build config files** (e.g., `settings.gradle`, root `build.gradle`, `pom.xml`): Accept both sides — combine entries from the migration branch and the incoming PR. For include/dependency lists, append new entries. For version properties, keep the higher version.
    - **Shared documentation files** (e.g., `README.md`, `CONTRIBUTING.md`): Merge both sets of changes, preserving sections from both sides.
    - **Service-specific files** (files under a single service's directory): Accept the incoming PR version, since the PR owns that service's scope.
    - **Cross-cutting config** (e.g., Docker Compose root, CI workflow files): Combine logically — append new service entries, merge environment blocks, union job definitions.
    - After resolving, stage all files and commit:
      ```bash
      git add -A
      git commit -m "squash(Phase <N>): <JIRA_KEY> - <summary> [conflicts resolved]"
      ```
    - Record which files had conflicts and how they were resolved in the execution log.

11. **Post-squash integrity check** — after each individual squash commit, immediately run:
    ```bash
    ./gradlew clean build test
    ```
    - If the build passes, proceed to the next PR in the batch.
    - If the build fails, inspect the error. If the failure is in a file that had conflicts, revert the squash commit (`git revert HEAD --no-edit`), log it as a conflict resolution failure, and re-queue the task to the next batch.
    - If the failure is unrelated to conflicts, attempt a targeted fix (e.g., missing import, dependency ordering). If the fix succeeds, amend the commit. If not, revert and re-queue.

12. After all PRs in the batch are squashed, push the migration branch:
    ```bash
    git push origin <MIGRATION_BRANCH>
    ```

### 3d. Validate the Batch

13. Run a **diff audit** to verify no unintended changes crept in:
    ```bash
    git diff <LAST_BATCH_SHA>..HEAD --stat
    ```
    Review the changed files list. Flag any files that were not in the expected scope of any task in this batch.

14. Update the execution log with squash status (success/conflict-resolved/reverted) and any conflict details for each task.

15. Commit and push the updated execution log.

16. Notify the user that Batch N is complete with a summary: tasks squashed, conflicts resolved (if any), build status, and any re-queued tasks. **Then return to step 3a to propose the next batch using `propose_sessions` and wait for user approval before proceeding.** Every batch requires user approval via `propose_sessions` — do not skip this for any batch.

**Repeat steps 3a-3d until all batches are complete.**

<verification>
- Every batch in the execution plan has been processed
- All successful PRs have been squash-merged onto the migration branch
- All merge conflicts were resolved autonomously and documented in the execution log
- The migration branch builds and tests pass after each individual squash commit and after each batch
- The diff audit shows no out-of-scope file changes
- The execution log is fully updated with status, conflict details, and disposition for every task
- Any failed tasks are documented with disposition (re-queued, abandoned, or fixed)
</verification>
</phase>

<phase name="Final Validation and Completion" id="4">
## Phase 4: Final Validation and Completion

Validate the complete migration and report results.

1. Pull the latest migration branch and run the full validation suite:
   ```bash
   git checkout <MIGRATION_BRANCH> && git pull origin <MIGRATION_BRANCH>
   ./gradlew clean build test
   ```

2. Run a **cumulative diff audit** against the original baseline:
   ```bash
   git diff <BASE_SHA>..HEAD --stat
   ```
   Verify every changed file maps to a completed Jira task. Flag any orphaned changes.

3. Cross-reference the execution log against the Jira board:
   - Fetch all tasks again via Atlassian MCP.
   - Verify every task has a corresponding "success" entry in the execution log.
   - List any tasks that were abandoned or remain incomplete.

4. For each completed task, update its Jira status to "Done" using the Atlassian MCP `transitionJiraIssue` tool (if the user approves).

5. Generate a **Migration Summary Report** covering:
   - Total batches executed
   - Tasks completed vs. failed vs. skipped
   - Conflicts encountered and how each was resolved
   - Final migration branch SHA
   - Build and test status
   - Cumulative diff summary (files changed, insertions, deletions)
   - Any open items requiring manual follow-up

6. Present the summary report to the user and ask whether to open a PR from the migration branch to `main`.

<verification>
- Full build and test suite passes on the migration branch
- Cumulative diff audit shows no orphaned or out-of-scope changes
- Every Jira task has been accounted for in the execution log
- Migration summary report has been generated and presented to the user
- Jira statuses have been updated (if approved by user)
</verification>
</phase>

## Specifications

- The migration branch must build and pass all tests after every individual squash commit — never leave it in a broken state.
- The execution log (`migration_execution_log.md`) must be kept current and committed to the migration branch after every batch.
- All child session PRs must target the migration branch, not `main`.
- The playbook must work for any Jira-managed phased migration, not just the ftgo-monolith project.
- Final deliverable is a green migration branch with all tasks squashed and an execution log documenting the full history including any conflict resolutions.
- **User approval is required for every batch.** The orchestrator must use `propose_sessions` for each batch and then call `message_user` with `block_on_user=true` to wait for the user to approve the proposed sessions. Do not proceed with monitoring or launching sessions until approval is received. User input is required for: initial plan approval (Phase 1), **every batch's session approval** via `propose_sessions` (Phase 3), Jira status transitions (Phase 4), and failures exceeding the 30% batch threshold.
- **`propose_sessions` is the only approved method for launching batch sessions.** Do not use the Devin API (`https://api.devin.ai/v1/sessions`) unless the user explicitly instructs you to do so. The API is not a fallback — it is an alternative that requires explicit user authorization.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
