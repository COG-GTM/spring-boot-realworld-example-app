---
name: linear-implement-fast
description: "Converted from Devin playbook: Linear: Implement Fast"
triggers:
  - user
  - model
---

# Linear: Implement Fast

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Linear: Implement Fast (playbook-31c9ae5d97e54717a4818901ec1da682), macro `!implement_fast`

## Procedure

# Implement Fast

## Overview

Abbreviated version of the `!implement` playbook optimized for speed and live demos. Skips deep disambiguation research, extensive test writing, and prolonged CI waiting. Gets from ticket to PR as fast as possible while maintaining code quality.

## What's Needed From User

- Linear ticket URL or ticket ID (e.g., `ENG-123` or `https://linear.app/team/issue/ENG-123/...`)
- Repository access for the codebase where changes will be made

<phase name="Quick Scope" id="1">
## Quick Scope Phase

Rapidly understand the ticket and get moving.

1. Fetch the ticket details using the Linear MCP `get_issue` tool with the ticket ID
2. Read any comments on the ticket for additional context
3. If the ticket references specific files, modules, or systems — search the codebase directly to understand the affected area. Skip wiki research and cross-repo analysis.
4. If the ticket is clear enough to act on: proceed immediately to Implementation
5. Only block on the user if the ticket is fundamentally ambiguous (e.g., missing which repo, contradictory requirements)

<verification>
- Do you know what repo and what area of code to change?
- Do you understand the desired end-state?
- If yes to both: move on. Don't over-research.
</verification>
</phase>

<phase name="Implementation" id="2">
## Implementation Phase

1. Use `list_issue_statuses` with the team from the issue to discover available status names, then update the ticket status to "In Progress" (or equivalent) using `update_issue`
2. Search the codebase to identify the files that need changes — focus on the directly affected code, not exhaustive tracing
3. Create a feature branch following the repository's branching conventions
4. Implement the changes
5. Run lint checks and fix any issues
6. Commit changes with a clear message referencing the ticket ID
7. Push the branch and create a pull request with a concise description linking to the ticket
8. Update the ticket using `update_issue` to add the PR link via the `links` parameter and change status to "For Review" (or equivalent)
9. Send a brief message with the PR link

<verification>
- Code compiles / lint passes
- PR has been created with description linking to the ticket
- Ticket status updated and PR link added
</verification>
</phase>

<phase name="Quick Review" id="3">
## Quick Review Phase

1. Do a fast self-review of the diff — check for obvious issues (typos, missing imports, leftover debug code)
2. Check CI status once using `git(action="pr_checks", wait_mode="none")` — report current status but do not wait for full CI completion
3. If there are immediate lint or build failures visible, fix them quickly and push
4. Send a final message confirming the PR is ready for human review

<verification>
- Self-review completed (no obvious issues)
- CI status checked and reported
- Final message sent with PR link
</verification>
</phase>

## Specifications

- The PR must address the requirements specified in the ticket
- Code must pass lint checks
- The ticket must be updated with the PR link
- All ticket status transitions must be reflected in Linear
- Do NOT wait for full CI completion — check once and report status
- Do NOT write new tests unless the ticket specifically asks for them
- Do NOT do exhaustive codebase research — understand enough to implement correctly

## TODO list guidance
Only create the todo list for the current phase. Once you fully move to the next phase, create the todo list for the next phase.

## MCP Tool Reference

### Linear MCP
- `get_issue`: Fetch issue details. Parameter: `id` (the issue identifier like "ENG-123")
- `list_issue_statuses`: List available statuses. Parameter: `team` (team name or ID, not `teamId`)
- `update_issue`: Update an issue. Parameter: `id` for the issue, plus `state`, `links`, etc.
  - When adding resource links, first read the issue to collect existing links and include them in the update so you do not overwrite prior links.

### Devin MCP
Use sparingly — only if you need quick context on unfamiliar code:
- `ask_question`: Ask about a repo. Parameters: `repoName` and `question`

IMPORTANT: Do NOT use the deepwiki MCP. Only use the Devin MCP for private repos.

## Advice and Pointers

- **Speed is the priority** — get to a PR as fast as possible
- **Keep all ticket comments extremely brief** — write like a human, not an AI
- **Skip extensive research** — read the ticket, understand the code, implement, ship
- **Don't gold-plate** — implement what's asked, nothing more

## Forbidden Actions

- Do not block on the user unless fundamentally stuck
- Do not push directly to the main branch
- Do not mark the ticket as complete/done — leave that for human verification
- Do not wait for full CI to pass before reporting completion
- Do not spend time on exhaustive codebase wiki research
- Do not write tests unless explicitly asked
- Removing or overriding attributes of the existing tickets without explicit instructions

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
