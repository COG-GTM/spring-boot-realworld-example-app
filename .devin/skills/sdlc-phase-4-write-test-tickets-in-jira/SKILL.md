---
name: sdlc-phase-4-write-test-tickets-in-jira
description: "Converted from Devin playbook: SDLC Phase 4: Write Test Tickets in Jira"
triggers:
  - user
  - model
---

# SDLC Phase 4: Write Test Tickets in Jira

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: SDLC Phase 4: Write Test Tickets in Jira (playbook-20ac2d25e3574a41bd31a79b948a21d4), macro `!sdlc_phase4`

## Procedure

# SDLC Phase 4: Write Test Tickets in Jira

## Overview
Analyzes the implemented code from Phase 3, the PRD, and the design specification to create comprehensive test case tickets in Jira. Covers unit tests, component tests, integration tests, and E2E/functional tests. Each ticket includes detailed test steps, expected results, and acceptance criteria. On completion, chains to **SDLC Phase 5** (Implement Tests).

## What's Needed From User (passed from Phase 3)
- Jira PRD Epic Key
- Jira Design Spec Ticket Key
- Jira Project: `COG-GTM`
- Target repository (e.g., `COG-GTM/my-app`)
- Tech stack
- PR URL and branch from Phase 3
- Figma file key
- List of implemented screens/components
- Atlassian Cloud ID: `e395c468-f9ea-4f8f-adae-0ea6d2eb6970`

## MCP Tools Reference

### Atlassian MCP (server: `atlassian`)
- `getJiraIssue` — read PRD Epic and design spec ticket
- `searchJiraIssuesUsingJql` — fetch existing tickets, check for duplicates
- `createJiraIssue` — create test case tickets (Tasks/Subtasks)
- `createIssueLink` — link test tickets to feature tickets
- `addCommentToJiraIssue` — add test plan references to epic
- `editJiraIssue` — update tickets with test details

### Figma MCP (server: `figma`)
- `figma_get_file_nodes` — verify UI behavior expectations against designs
- `figma_get_images` — render reference screenshots for visual test specs

### Devin MCP (tool: `devin_mcp`)
- `devin_playbook_manage` — look up Phase 5 playbook ID
- `devin_session_create` — spawn Phase 5

<phase name="Analysis & Test Planning" id="1">
## Analysis & Test Planning

1. Read the PRD from Jira using `getJiraIssue` (cloudId: `e395c468-f9ea-4f8f-adae-0ea6d2eb6970`, responseContentFormat: `markdown`)
2. Read the Design Spec ticket using `getJiraIssue`
3. Fetch all child tickets under the Epic using `searchJiraIssuesUsingJql`:
   - Query: `project = "COG-GTM" AND parent = <EPIC_KEY> ORDER BY created ASC`
4. Clone the target repository and check out the PR branch
5. Analyze the implemented code:
   - Identify all new/modified components, services, and utilities
   - Map each component to its PRD requirement and Figma frame
   - Identify existing test setup (testing framework, test utilities, mocks)
   - Note the test file naming convention and directory structure
6. For UI-specific tests, use `figma_get_file_nodes` to verify expected layouts and `figma_get_images` to render reference screenshots
7. Create a test plan matrix:
   - **Unit tests**: individual functions, utilities, hooks, services
   - **Component tests**: rendering, props, user interactions, state changes
   - **Integration tests**: API interactions, data flow between components
   - **E2E/Functional tests**: full user workflows, acceptance criteria validation

<verification>
- PRD and design spec have been read from Jira
- All Jira feature tickets have been fetched
- The implemented code has been analyzed (components, services, test setup identified)
- A test plan matrix exists covering unit, component, integration, and E2E tests
- The existing test framework and conventions have been identified
</verification>
</phase>

<phase name="Test Plan Ticket" id="2">
## Test Plan Ticket

Create a comprehensive Test Plan ticket in Jira:

1. Use `createJiraIssue` (server: `atlassian`):
   - `cloudId`: `e395c468-f9ea-4f8f-adae-0ea6d2eb6970`
   - `projectKey`: `COG-GTM`
   - `issueTypeName`: `Task`
   - `parent`: the PRD Epic key
   - `summary`: "Test Plan: [Project Name] in [REPO_NAME]"
   - `contentFormat`: `markdown`
   - `additional_fields`: `{"labels": ["sdlc-automated", "test-plan"]}`

2. `description` includes:
   - **Test Scope** — what is being tested and what is excluded
   - **Test Environment** — framework, tools, configuration
   - **Test Categories** — unit, component, integration, E2E with counts
   - **Test Case Summary Table** — ID, name, category, priority, linked requirement
   - **Entry/Exit Criteria** — when to start/stop testing
   - **Risks** — areas with low testability, external dependencies

3. Add a comment to the PRD Epic linking to the test plan ticket using `addCommentToJiraIssue`

<verification>
- Test plan ticket has been created under the PRD Epic
- Test plan covers all 4 test categories
- Test case summary table lists all planned tests
- PRD Epic has been commented with the test plan ticket key
</verification>
</phase>

<phase name="Create Test Case Tickets" id="3">
## Create Test Case Tickets in Jira

For each test case in the test plan, create a Jira ticket:

1. **Create Unit Test tickets** (as Tasks under the PRD Epic):
   - `cloudId`: `e395c468-f9ea-4f8f-adae-0ea6d2eb6970`
   - `projectKey`: `COG-GTM`
   - `issueTypeName`: `Task`
   - `parent`: PRD Epic key
   - `summary`: "Unit Test: [function/method name] in [REPO_NAME]"
   - `contentFormat`: `markdown`
   - `description`:
     ```
     ## Test Case
     **Category**: Unit Test
     **Component/File**: [path to source file]
     **Function**: [function name]
     **Linked Requirement**: [PRD requirement #]

     ## Test Scenarios
     1. **[Scenario name]**
        - Input: [test input]
        - Expected: [expected output]
        - Priority: High/Medium/Low

     2. **[Scenario name]**
        - Input: [test input]
        - Expected: [expected output]
        - Priority: High/Medium/Low

     ## Acceptance Criteria
     - [ ] All scenarios pass
     - [ ] Edge cases covered (null, empty, boundary values)
     - [ ] No mocking of internal implementation details
     ```
   - `additional_fields`: `{"labels": ["sdlc-automated", "unit-test"]}`

2. **Create Component Test tickets** (same format):
   - `summary`: "Component Test: [ComponentName] in [REPO_NAME]"
   - Description includes: render scenarios, user interaction scenarios, prop validation, state change verification
   - `additional_fields`: `{"labels": ["sdlc-automated", "component-test"]}`

3. **Create E2E/Functional Test tickets**:
   - `summary`: "E2E Test: [User Flow Name] in [REPO_NAME]"
   - Description includes: step-by-step user flow, page navigation, form submissions, expected UI states
   - `additional_fields`: `{"labels": ["sdlc-automated", "e2e-test"]}`

4. **Link test tickets to feature tickets** using `createIssueLink`:
   - Type: `Relates` (link each test ticket to the Story it validates)

5. Present a summary table of all created test tickets to the user.

<verification>
- Unit test tickets exist for all key functions/services
- Component test tickets exist for all new UI components
- E2E test tickets exist for all user workflows from the PRD
- Every test ticket has detailed test scenarios and acceptance criteria
- Test tickets are linked to their corresponding feature tickets
- Summary table of all test tickets has been presented
</verification>
</phase>

<phase name="Chain to Phase 5" id="4">
## Chain to Phase 5: Implement Tests

Spawn a child Devin session to run SDLC Phase 5 (Implement Functional Tests):

1. Use `devin_playbook_manage` (action: `list`) via `devin_mcp` to find the playbook titled **"SDLC Phase 5: Implement Functional Tests"** and get its `playbook_id`
2. Construct the handoff prompt:
   ```
   Continue SDLC pipeline from Phase 4.

   Context from Phases 1-4:
   - Jira PRD Epic Key: [EPIC_KEY]
   - Jira Test Plan Ticket Key: [TEST_PLAN_KEY]
   - Jira Project: COG-GTM
   - Target Repository: [REPO_NAME]
   - Tech Stack: [TECH_STACK]
   - Implementation PR URL: [PR_URL]
   - Implementation Branch: [BRANCH_NAME]
   - Figma File Key: [FIGMA_KEY]
   - Test Ticket Keys: [LIST_OF_TEST_TICKET_KEYS]
   - Atlassian Cloud ID: e395c468-f9ea-4f8f-adae-0ea6d2eb6970

   Please proceed with Phase 5: Implement all test cases from the test tickets.
   ```
3. Use `devin_session_create` via `devin_mcp` to spawn the child session
4. Share the child session link with the user

<verification>
- The Phase 5 playbook ID has been found
- A child session has been spawned with the correct playbook and all context
- The child session link has been shared with the user
</verification>
</phase>

## Specifications
- Every test ticket must include specific test scenarios with inputs and expected outputs
- Test tickets must reference the PRD requirement they validate
- Use the repo org/name convention in all ticket titles
- Label all tickets with `sdlc-automated` for traceability
- All artifacts saved as Jira tickets — no Confluence pages

## Advice and Pointers
- Prioritize tests for critical user flows and acceptance criteria from the PRD
- Include edge cases: null inputs, empty states, error states, boundary values
- For E2E tests, write step-by-step instructions that could be followed manually
- Reference Figma frame IDs in component test descriptions for visual verification

## Forbidden Actions
- Do not create test tickets without detailed test scenarios
- Do not skip E2E tests — every acceptance criterion must have an E2E test
- Do not create duplicate test tickets — check existing tickets first
- Do not save artifacts to Confluence — everything goes into Jira tickets

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
