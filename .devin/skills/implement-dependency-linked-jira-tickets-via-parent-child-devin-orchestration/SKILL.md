---
name: implement-dependency-linked-jira-tickets-via-parent-child-devin-orchestration
description: "Converted from Devin playbook: Implement Dependency-Linked Jira Tickets via Parent/Child Devin Orchestration"
triggers:
  - user
  - model
---

# Implement Dependency-Linked Jira Tickets via Parent/Child Devin Orchestration

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Implement Dependency-Linked Jira Tickets via Parent/Child Devin Orchestration (playbook-a56af22f57c64239b5f83a1ce4bb9fd0), macro `!orchestrate_tickets`

## Procedure

# Implement Dependency-Linked Jira Tickets via Parent/Child Devin Orchestration

## Overview
Implement a set of related Jira tickets (work packages) by acting as a **parent** Devin
that orchestrates one **child** managed Devin session per ticket. The parent resolves the
dependency order between tickets, runs independent tickets first, merges/validates each
result, then runs blocked tickets stacked on their prerequisite's branch. Each child opens
its own PR with green CI, and the final deliverable is validated against golden reference
data via a test harness. Use this whenever a user hands you 2+ tickets and asks to
implement them with managed/child Devins respecting their dependencies.

## What's Needed From User
- The Jira ticket keys to implement (e.g. `AN-3`, `AN-4`) — or a Jira board/epic link
- The target GitHub repo and base branch (default `main`)
- Confirmation of the desired scope (which tickets, and how general the run should be)
- Any repo conventions: branch naming, commit-message keywords, fork-vs-upstream PR target
- The validation source of truth (e.g. golden CSV + a test harness location) if results must be verified

<phase name="Ticket & Dependency Analysis" id="1">
## Phase 1 — Ticket & Dependency Analysis

1. Fetch each ticket's full details with the Atlassian (Jira) MCP (`getJiraIssue`): goal, acceptance criteria, and linked issues.
2. Read the "blocks / is blocked by" links to build the dependency graph. Treat "is blocked by X" as "X must merge first".
3. Inspect the target repo to confirm the relevant source files, target directories, CI workflow triggers, and conventions.
4. Produce an explicit execution plan: which tickets run independently vs. which are stacked, and in what order.
5. Share the plan with the user before spawning any children.

<verification>
- Every ticket has goal + acceptance criteria captured
- The dependency order is explicit and justified by Jira links (not guessed)
- Repo source/target paths and the CI trigger (which base branch fires CI) are confirmed
- The plan states which children are independent vs. stacked
</verification>
</phase>

<phase name="Run Independent Ticket(s)" id="2">
## Phase 2 — Run Independent Ticket(s)

1. For each ticket with no unmet dependency, create a child session with `devin_session_create`.
2. Write a self-contained child prompt that includes: repo + base branch, a feature branch name, the ticket goal, the acceptance criteria, required file conversions/changes, the testing requirement, and all repo conventions (fork PR target, commit keyword).
3. Use `devin_session_gather` to wait for the child to settle. Use `devin_session_interact` to nudge it if it stalls or asks a question.
4. Verify the child's PR meets all acceptance criteria and that CI is green (distinguish a genuine failure from a preexisting/environmental one on the base branch — confirm with evidence).
5. Merge the prerequisite PR to the base branch once verified (or note explicitly that it will be merged so dependents can stack).

<verification>
- One child session per independent ticket was created with a self-contained prompt
- Each child produced a PR satisfying its acceptance criteria
- CI status is green, or any red check is proven preexisting on the base branch
- Prerequisite branch is merged (or its branch name is recorded for stacking)
</verification>
</phase>

<phase name="Run Dependent (Stacked) Ticket(s)" id="3">
## Phase 3 — Run Dependent (Stacked) Ticket(s)

1. For each blocked ticket, create a child session that branches off the prerequisite's branch if it is not yet merged (`git checkout -b <new> origin/<prereq-branch>`).
2. Instruct the child to open its PR against the base branch that triggers CI (e.g. `main`), and to NOT delete/modify the prerequisite's files to "clean up" the temporarily larger stacked diff — it resolves once the prerequisite merges.
3. Include the dependency precondition in the prompt (which composite types/functions/contracts the prerequisite provides and how this ticket consumes them).
4. Gather and verify the dependent child's PR and CI as in Phase 2.

<verification>
- Dependent child branched off the correct prerequisite branch (deps present)
- Dependent PR targets the CI-triggering base branch and CI ran
- The stacked diff is left intact (no deletion of prerequisite files)
- Dependent PR satisfies its acceptance criteria
</verification>
</phase>

<phase name="Validate & Report" id="4">
## Phase 4 — Validate & Report

1. Run (or confirm the child ran) the test harness that validates results against the golden reference data (e.g. diff computed balances vs. `expected_balances.csv`).
2. Confirm every ticket's acceptance criteria are satisfied and each PR's CI is green.
3. Report a concise summary to the user: per-ticket PR links, CI status, validation result, and the child session links.

<verification>
- The test harness ran and results match the golden data (or discrepancies are reported)
- All acceptance criteria across tickets are met
- A summary with PR links, CI status, validation outcome, and child session links was delivered
</verification>
</phase>

## Specifications
- One child session per ticket; tickets executed in dependency order (independent first, blocked stacked).
- Each ticket results in its own PR with passing CI on the CI-triggering base branch.
- Final results are validated against golden reference data via the test harness.
- Deliverable: a summary message containing every PR link, CI status, validation outcome, and child session links.
- Validation method: re-run the harness and confirm zero unexplained diffs against the golden data.

## Advice and Pointers
- Independent tickets may run in parallel; blocked tickets must wait for (or stack on) their prerequisite.
- Prefer stacking on the prerequisite branch over waiting for a human merge, so the dependent work can start immediately — but still open the dependent PR against the CI-triggering branch.
- COG-GTM repos are forks: always target the fork (`COG-GTM/<repo>`) when creating PRs. If a PR create fails with "Resource not accessible by integration", retry targeting the fork explicitly.
- Make child prompts fully self-contained — children do not share the parent's filesystem, env, or context.
- When checking CI, verify whether a red check is preexisting on the base branch before calling it unrelated.

## Forbidden Actions
- Do not run a blocked ticket before its prerequisite's work exists (merged or stacked).
- Do not delete or modify a prerequisite's files in a stacked PR to shrink the diff.
- Do not open PRs against an upstream repo — always target the COG-GTM fork.
- Do not mark a ticket complete on a red CI check without proving the failure is preexisting/environmental.
- Do not skip the golden-data validation step.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
