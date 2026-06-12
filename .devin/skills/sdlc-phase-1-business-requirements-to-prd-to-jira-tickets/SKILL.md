---
name: sdlc-phase-1-business-requirements-to-prd-to-jira-tickets
description: "Converted from Devin playbook: SDLC Phase 1: Business Requirements to PRD to Jira Tickets"
triggers:
  - user
  - model
---

# SDLC Phase 1: Business Requirements to PRD to Jira Tickets

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: SDLC Phase 1: Business Requirements to PRD to Jira Tickets (playbook-9ee1f72c523242a6a1756beaa9ae291d), macro `!sdlc_phase1`

## Procedure

# SDLC Phase 1: Business Requirements to PRD to Jira Tickets

## Overview
Takes business requirements and transforms them into a structured PRD (saved as a Jira Epic description), then breaks the PRD into actionable Jira tickets (Stories and Tasks). On completion, automatically chains to **SDLC Phase 2** (Figma Design) by spawning a child Devin session with the next playbook.

## What's Needed From User
- Business requirements (text, document, or link)
- Target Jira project key (default: `COG-GTM`)
- Target repository (e.g., `COG-GTM/my-app`) for the implementation
- Figma file key (if an existing design file should be used in Phase 2)
- Optional: specific tech stack preferences (React, Angular, Spring Boot, etc.)

## MCP Tools Reference

### Atlassian MCP (server: `atlassian`)
- `searchJiraIssuesUsingJql` — check for existing tickets
- `getJiraProjectIssueTypesMetadata` — get issue types for the project
- `createJiraIssue` — create epics/stories/tasks
- `editJiraIssue` — update ticket descriptions
- `createIssueLink` — link related tickets (Blocks, Relates)
- `addCommentToJiraIssue` — add context/details to tickets

### Devin MCP (tool: `devin_mcp`)
- `devin_playbook_manage` — look up the Phase 2 playbook ID
- `devin_session_create` — spawn the next phase

### Jira Defaults
- Cloud ID: `e395c468-f9ea-4f8f-adae-0ea6d2eb6970`
- Content format: always use `contentFormat: "markdown"`
- Include repo org/name in ticket titles per org convention

<phase name="Requirements Gathering" id="1">
## Requirements Gathering

1. Collect or read the business requirements from the user's prompt
2. Confirm the target Jira project key (default `COG-GTM`). Use `getJiraProjectIssueTypesMetadata` to discover available issue types (Epic, Story, Task, etc.)
3. Search for existing related tickets using `searchJiraIssuesUsingJql` to avoid duplicates
4. Confirm the target repository name and tech stack with the user
5. Compile a structured summary of all requirements, constraints, dependencies, and the target repo

<verification>
- All business requirements have been collected and understood
- The target Jira project and available issue types have been identified
- Existing related tickets have been checked for duplicates
- The target repository and tech stack are confirmed
</verification>
</phase>

<phase name="PRD Generation & Epic Creation" id="2">
## PRD Generation & Epic Creation

Generate a comprehensive PRD and save it as the description of a Jira Epic:

1. **Create the PRD Epic** using `createJiraIssue` (server: `atlassian`):
   - `cloudId`: `e395c468-f9ea-4f8f-adae-0ea6d2eb6970`
   - `projectKey`: `COG-GTM` (or user-specified)
   - `issueTypeName`: `Epic`
   - `summary`: "PRD: [Feature Name] in [REPO_NAME]"
   - `contentFormat`: `markdown`
   - `additional_fields`: `{"labels": ["sdlc-automated", "prd"]}`
   - `description`: The full PRD in markdown with this structure:

2. **PRD Structure** (goes in the Epic description):
   1. **Executive Summary** — One-paragraph overview
   2. **Problem Statement** — What problem and for whom
   3. **Goals & Success Metrics** — KPIs, SLAs
   4. **User Personas & Use Cases** — Who uses this and how
   5. **Functional Requirements** — Numbered features with acceptance criteria
   6. **Non-Functional Requirements** — Performance, security, scalability
   7. **Technical Considerations** — Architecture, integrations, target repo, tech stack
   8. **UI/UX Requirements** — Screen descriptions, user flows, interactions (this feeds Figma design in Phase 2)
   9. **Dependencies & Risks** — External dependencies, risks, mitigations
   10. **Timeline & Milestones** — Suggested phases
   11. **Out of Scope** — Excluded items

3. Present the PRD Epic to the user for review before proceeding to Story/Task creation.

<verification>
- A Jira Epic has been created with the full PRD as its description
- PRD contains all 11 sections listed above
- Section 8 (UI/UX Requirements) has enough detail to drive Figma design work
- Each functional requirement has clear acceptance criteria
- The Epic key and URL have been shared with the user
- User has reviewed and approved the PRD
</verification>
</phase>

<phase name="Story & Task Creation" id="3">
## Story & Task Creation

Break the PRD into child tickets under the Epic:

1. **Create Stories** — For each functional requirement, create a Story under the Epic:
   - `cloudId`: `e395c468-f9ea-4f8f-adae-0ea6d2eb6970`
   - `projectKey`: `COG-GTM`
   - `issueTypeName`: `Story` (or `Task` if Story is unavailable)
   - `parent`: the PRD Epic key
   - `summary`: Include repo name (e.g., "As a user, I want [action] so that [benefit] in COG-GTM/my-app")
   - `description` (markdown): acceptance criteria, technical notes
   - `contentFormat`: `markdown`
   - `additional_fields`: `{"priority": {"name": "High|Medium|Low"}, "labels": ["sdlc-automated"]}`

2. **Create Design Tasks** — For each UI/UX requirement from PRD Section 8:
   - `issueTypeName`: `Task`
   - `parent`: the PRD Epic key
   - `summary`: "Design: [screen/component name] in [REPO_NAME]"
   - `additional_fields`: `{"labels": ["sdlc-automated", "design"]}`

3. **Link tickets** using `createIssueLink` for dependencies between Stories

4. Present a summary table of all created tickets to the user.

<verification>
- Every functional requirement has a corresponding Story/Task
- All Stories have acceptance criteria in their descriptions
- Design tasks exist for UI/UX requirements
- Tickets are linked with dependencies
- Epic key and all child ticket keys are recorded for handoff
- Summary of all tickets presented to the user
</verification>
</phase>

<phase name="Chain to Phase 2" id="4">
## Chain to Phase 2: Figma Design

Spawn a child Devin session to run SDLC Phase 2 (Figma Design from PRD):

1. Use `devin_playbook_manage` (action: `list`) via `devin_mcp` to find the playbook titled **"SDLC Phase 2: Figma Design from PRD"** and get its `playbook_id`
2. Construct the handoff prompt with all context:
   ```
   Continue SDLC pipeline from Phase 1.

   Context from Phase 1:
   - Jira PRD Epic Key: [EPIC_KEY]
   - Jira Project: COG-GTM
   - Target Repository: [REPO_NAME]
   - Tech Stack: [TECH_STACK]
   - Figma File Key: [FIGMA_KEY or "to be created"]
   - Atlassian Cloud ID: e395c468-f9ea-4f8f-adae-0ea6d2eb6970
   - Child Ticket Keys: [LIST_OF_STORY_KEYS]

   Please proceed with Phase 2: Design the UI based on the PRD requirements.
   ```
3. Use `devin_session_create` via `devin_mcp` to spawn the child session with the playbook_id and prompt
4. Share the child session link with the user

<verification>
- The Phase 2 playbook ID has been found
- A child session has been spawned with the correct playbook and full context
- The child session link has been shared with the user
</verification>
</phase>

## Specifications
- All Jira tickets use markdown content format
- Stories follow "As a [persona], I want [action] so that [benefit]" format
- Include repo org/name in all ticket titles
- Use `COG-GTM` as the default Jira project
- Label all tickets with `sdlc-automated` for traceability
- The PRD lives as the Epic description — no Confluence pages

## Advice and Pointers
- Check for existing epics before creating new ones
- Keep Story descriptions concise — the PRD in the Epic has the full context
- The UI/UX Requirements section of the PRD is critical — it drives Phase 2

## Forbidden Actions
- Do not create tickets without acceptance criteria
- Do not skip the PRD user review step
- Do not proceed to Phase 2 without confirming the Jira tickets with the user
- Do not save artifacts to Confluence — everything goes into Jira tickets

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
