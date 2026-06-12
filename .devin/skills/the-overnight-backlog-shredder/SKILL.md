---
name: the-overnight-backlog-shredder
description: "Converted from Devin playbook: The Overnight Backlog Shredder"
triggers:
  - user
  - model
---

# The Overnight Backlog Shredder

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: The Overnight Backlog Shredder (playbook-b8a1f5355de145289c77b5d51ba7828e)

## Procedure

# The Overnight Backlog Shredder

## Overview
Automatically score a Linear backlog for Devin suitability, group tickets into confidence tiers, and spin up child Devin sessions for the highest-confidence issues — turning an idle overnight window into shipped PRs.

## What's Needed From User
- Linear team name (e.g., "Cog GTM") or specific project name
- Number of top tickets to execute (default: 5)
- Target repositories for implementation (optional — auto-detected from project context)

<phase name="Backlog Discovery" id="1">
## Backlog Discovery

Pull the full backlog from Linear and build a working dataset.

1. Use the Linear MCP `list_issues` tool to fetch all issues for the specified team with `statusType` in ("backlog", "unstarted")
2. Paginate if needed (use `cursor` param) — fetch up to 250 issues per page
3. For each issue, capture: id, title, description, priority, estimate, labels, project, assignee, status, URL
4. Filter out canceled, completed, or already-in-progress issues
5. Save the raw dataset and print a summary: total issues, breakdown by project, priority distribution

<verification>
- All backlog and unstarted issues have been fetched from Linear
- Issue count and project breakdown have been printed
- Raw dataset is saved for scoring phase
</verification>
</phase>

<phase name="Confidence Scoring" id="2">
## Confidence Scoring

Score every issue on a 0–100 scale using these weighted signals:

### Scoring Criteria

**Description Quality (up to +25 pts)**
- Rich description (>500 chars): +10
- Has acceptance criteria: +15
- Technical detail (endpoints, file names, components): +8 to +15
- Code references in backticks: +5
- Missing/short description: -15

**Task Type (up to +10 pts)**
- High-confidence types (bug fix, test generation, refactor, static page, CRUD feature, API endpoint, CI/CD, dependency update, documentation): +10
- Low-confidence types (regulatory/legal, external service integration, architecture decision, design work): -10

**Size (+12 to -5 pts)**
- Small (≤2 pts or labeled S/XS): +10 to +12
- Medium (3 pts or labeled M): +5 to +8
- Large (5 pts or labeled L): +3 or 0
- Very large (>5 pts or labeled XL): -5

**Priority (up to +5 pts)**
- Urgent: +5, High: +4, Medium: +2

**Context (up to +10 pts)**
- Has project association: +5
- Has good labels (bug, feature, etc.): +3
- No dependencies: +5
- Has blocking dependencies: -5 to -8

**Repository Available (up to +10 pts)**
- Project maps to a known Devin-accessible repo: +10

### Tier Assignment
- **Score ≥ 70**: 🟢 High-Confidence Autonomous PRs
- **Score 45–69**: 🟡 Needs Human Clarification
- **Score < 45**: 🔴 Not Suitable for Devin

### Output
1. Score all issues and sort by confidence score (descending)
2. Generate a markdown report with:
   - Executive summary table (count per tier, % of backlog)
   - Full ranked table for each tier
   - Top 5 detail cards with signals and risks
   - Recommendations section
3. Present the report to the user in the session

<verification>
- All issues have been scored with confidence values 0-100
- Issues are grouped into three tiers
- Markdown report has been generated and presented
- Top 5 high-confidence issues have been identified with detailed signal breakdowns
</verification>
</phase>

<phase name="Implementation Planning" id="3">
## Implementation Planning

For each of the top N high-confidence issues (default 5):

1. Fetch the full issue details from Linear (description, comments, attachments)
2. Identify the target repository from the project context
3. Generate an implementation plan:
   - **Objective**: One-line summary of what the PR will accomplish
   - **Files to modify**: List expected files based on description and codebase analysis
   - **Approach**: Step-by-step implementation strategy
   - **Testing strategy**: What tests to write or run
   - **Risks**: Any potential issues or edge cases
4. Post the implementation plan as a comment on the Linear issue
5. Update the issue status to "In Progress" and assign to Devin

<verification>
- Implementation plans have been generated for the top N issues
- Plans have been posted as comments on each Linear issue
- Issues have been updated to "In Progress" status
</verification>
</phase>

<phase name="Session Orchestration" id="4">
## Session Orchestration

Spin up child Devin sessions to execute the top tickets in parallel.

1. For each of the top N issues, create a child Devin session using `devin_session_create` with:
   - **prompt**: Include the issue ID, title, full description, implementation plan, and target repo
   - **title**: "[Backlog Shredder] {issue_id}: {title}"
   - **repos**: Target repository for the issue
   - **tags**: ["backlog-shredder", "overnight-batch"]
2. Each child session prompt should instruct Devin to:
   - Clone the repo and understand the codebase
   - Implement the changes per the plan
   - Run lint, typecheck, and tests
   - Create a PR with a clear description linking back to the Linear issue
   - Update the Linear issue with the PR link
3. Track all spawned session IDs
4. Post a summary comment on each Linear issue with the Devin session link

<verification>
- Child Devin sessions have been created for the top N issues
- Each session has proper prompts, repos, and tags
- Linear issues have been updated with session links
- Session IDs are tracked for monitoring
</verification>
</phase>

<phase name="Monitoring & Reporting" id="5">
## Monitoring & Reporting

Monitor child sessions and compile a final status report.

1. Poll each child session using `devin_session_interact(action="get")` to check status
2. For completed sessions, gather:
   - PR URL (if created)
   - Test results (pass/fail)
   - Any blockers encountered
3. Generate a final execution report:
   - Summary: X of N sessions completed, Y PRs created, Z tests passing
   - Per-ticket status card with PR link, session link, and outcome
   - Recommendations for failed or blocked tickets
4. Present the final report to the user
5. Optionally update Linear issues with final status and PR links

<verification>
- All child sessions have been checked for status
- Final execution report has been generated
- PR links and outcomes are documented
- Linear issues are updated with results
</verification>
</phase>

## Specifications
- All reports and plans must be presented directly in the Devin session (not just saved to filesystem)
- Child sessions must be tagged for easy filtering
- Linear issues must be updated at each stage (scoring, planning, execution)
- The scoring algorithm must be deterministic and explainable

## Advice and Pointers
- Start with the scoring phase even if the user wants to jump straight to execution — visibility into the full backlog builds trust
- When generating implementation plans, use the Devin wiki/codebase search to understand repo structure before planning
- If a child session fails, capture the error and include it in the report rather than retrying immediately
- Batch child session creation for efficiency — create all sessions in one `devin_session_create` call
- Only create a TODO list for the current phase

## Forbidden Actions
- Do not modify Linear issues without scoring them first
- Do not start child sessions without generating implementation plans
- Do not skip the monitoring phase — always report back on session outcomes
- Do not delete or close Linear issues
- Do not push directly to main/master branches

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
