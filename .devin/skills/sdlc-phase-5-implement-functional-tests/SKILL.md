---
name: sdlc-phase-5-implement-functional-tests
description: "Converted from Devin playbook: SDLC Phase 5: Implement Functional Tests"
triggers:
  - user
  - model
---

# SDLC Phase 5: Implement Functional Tests

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: SDLC Phase 5: Implement Functional Tests (playbook-1687da5bd3794315a7a13346e857a7c0), macro `!sdlc_phase5`

## Procedure

# SDLC Phase 5: Implement Functional Tests

## Overview
Picks up test case tickets from Jira (created in Phase 4), implements all unit tests, component tests, and E2E/functional tests in the target repository. Creates a PR with the complete test suite. This is the final phase of the SDLC pipeline — produces a completion report linking all artifacts.

## What's Needed From User (passed from Phase 4)
- Jira PRD Epic Key
- Jira Test Plan Ticket Key
- Jira Project: `COG-GTM`
- Target repository (e.g., `COG-GTM/my-app`)
- Tech stack
- Implementation PR URL and branch from Phase 3
- Figma file key
- Test ticket keys from Phase 4
- Atlassian Cloud ID: `e395c468-f9ea-4f8f-adae-0ea6d2eb6970`

## MCP Tools Reference

### Atlassian MCP (server: `atlassian`)
- `getJiraIssue` — read PRD, design spec, test plan, and individual test tickets
- `searchJiraIssuesUsingJql` — fetch test tickets
- `addCommentToJiraIssue` — update tickets with test results
- `editJiraIssue` — update ticket descriptions with implementation notes
- `transitionJiraIssue` — move tickets to "Done" after tests pass
- `getTransitionsForJiraIssue` — get available transitions for a ticket

### Figma MCP (server: `figma`)
- `figma_get_file_nodes` — verify expected UI structure for component tests
- `figma_get_images` — render reference images for visual regression tests

### Devin MCP (tool: `devin_mcp`)
- `devin_session_create` — optional: spawn parallel child sessions for large test suites

<phase name="Test Environment Setup" id="1">
## Test Environment Setup

1. Clone the target repository and check out the implementation branch from Phase 3
2. Install all dependencies
3. Read the test plan ticket from Jira using `getJiraIssue` (cloudId: `e395c468-f9ea-4f8f-adae-0ea6d2eb6970`, responseContentFormat: `markdown`)
4. Fetch all test tickets using `searchJiraIssuesUsingJql`:
   - Query: `project = "COG-GTM" AND labels = "sdlc-automated" AND labels in ("unit-test", "component-test", "e2e-test") AND parent = <EPIC_KEY> ORDER BY priority DESC, created ASC`
5. Read each test ticket's full details using `getJiraIssue` to extract:
   - Test category (unit, component, integration, E2E)
   - Target file/function/component
   - Test scenarios with inputs and expected outputs
   - Acceptance criteria
6. Analyze the existing test setup:
   - Testing framework (Jest, Vitest, JUnit, pytest, Cypress, Playwright, etc.)
   - Test utilities and helpers already in the project
   - Mock/fixture patterns used
   - Test configuration files
   - Coverage reporting setup
7. Create a test implementation plan grouped by category:
   - Unit tests first (fastest, most isolated)
   - Component tests second
   - Integration tests third
   - E2E tests last (most complex)

<verification>
- Repository is cloned and on the correct branch with dependencies installed
- Test plan ticket has been read from Jira
- All test tickets have been fetched and their scenarios extracted
- Existing test framework and patterns are understood
- A test implementation plan exists ordered by category
</verification>
</phase>

<phase name="Unit & Component Test Implementation" id="2">
## Unit & Component Test Implementation

For each unit test ticket:

1. **Create the test file** following existing naming conventions (e.g., `*.test.ts`, `*.spec.ts`, `*Test.java`)
2. **Implement each test scenario** from the ticket:
   - Use the existing test framework's assertion style
   - Follow Arrange-Act-Assert (AAA) pattern
   - Mock external dependencies (API calls, database, etc.) using existing patterns
   - Cover edge cases specified in the ticket
3. **Run the tests** to verify they pass

For each component test ticket:

1. **Create the component test file**
2. **Implement rendering tests**: verify the component renders with expected structure
3. **Implement interaction tests**: simulate user events (click, type, submit)
4. **Implement state tests**: verify state changes after interactions
5. **Implement prop validation tests**: verify behavior with different props
6. **Use Figma as reference**: Use `figma_get_file_nodes` to verify expected element hierarchy matches the test assertions
7. **Run the tests** to verify they pass

After implementing each group, update the Jira tickets:
- Use `addCommentToJiraIssue` to post: test file path, pass/fail status, coverage delta

<verification>
- All unit test tickets have corresponding test files
- All component test tickets have corresponding test files
- Every test scenario from the Jira tickets has been implemented
- All unit and component tests pass locally
- Jira tickets have been updated with test results
</verification>
</phase>

<phase name="E2E & Integration Test Implementation" id="3">
## E2E & Integration Test Implementation

For each integration test ticket:

1. **Create integration test files** following project conventions
2. **Implement API integration tests**: test data flow between services
3. **Implement state management tests**: verify store/context interactions
4. **Mock external services** at the boundary (HTTP level, not implementation level)

For each E2E/functional test ticket:

1. **Create E2E test files** using the project's E2E framework (Cypress, Playwright, etc.)
2. **Implement the full user flow** step by step as described in the ticket:
   - Navigate to the page
   - Interact with UI elements (fill forms, click buttons)
   - Assert on visible outcomes (text, elements, navigation)
   - Verify error states and edge cases
3. **Use Figma for visual reference**: Use `figma_get_images` to render expected screenshots
4. **Run the E2E tests** to verify they pass

After implementing each test:
- Use `addCommentToJiraIssue` to post results
- If all scenarios pass, transition the ticket to "Done" using `transitionJiraIssue` (get transition IDs via `getTransitionsForJiraIssue` first)

<verification>
- All integration test tickets have corresponding test files
- All E2E test tickets have corresponding test files
- Every test scenario from the Jira tickets has been implemented
- All tests pass locally (unit + component + integration + E2E)
- Passing test tickets have been transitioned to "Done" in Jira
</verification>
</phase>

<phase name="PR & Final Report" id="4">
## PR Creation & Final SDLC Report

1. **Create the test PR**:
   - Branch: `devin/<timestamp>-sdlc-test-implementation`
   - If the implementation branch from Phase 3 is still open, base off that branch
   - Otherwise, base off `main` and include both implementation and tests
   - Commit with clear messages referencing Jira test ticket keys
   - PR description includes:
     - Link to test plan ticket in Jira
     - Summary of test coverage: unit, component, integration, E2E counts
     - Links to all Jira test tickets
     - Test run results

2. **Run full test suite** and include results in the PR description

3. **Run lint and type checks** to ensure no issues

4. **Update Jira**:
   - Comment on the PRD Epic with the test PR link using `addCommentToJiraIssue`
   - Comment on the Test Plan ticket with final test results summary

5. **Generate SDLC Completion Report** — message the user with:
   ```
   SDLC Pipeline Complete!

   Phase 1 — PRD & Tickets:
   - Jira PRD Epic: [EPIC_URL]

   Phase 2 — Design Spec:
   - Jira Design Spec: [DESIGN_SPEC_URL]
   - Figma File: [FIGMA_URL]

   Phase 3 — Implementation:
   - PR: [PR_URL]

   Phase 4 — Test Plan & Tickets:
   - Jira Test Plan: [TEST_PLAN_URL]
   - Test Tickets: [COUNT] tickets created

   Phase 5 — Test Implementation:
   - Test PR: [TEST_PR_URL]
   - Unit Tests: X passed
   - Component Tests: X passed
   - Integration Tests: X passed
   - E2E Tests: X passed
   ```

<verification>
- Test PR has been created with all test files
- Full test suite passes (all categories)
- Lint and type checks pass
- Jira tickets have been updated with PR links and test results
- SDLC completion summary has been sent to the user with all artifact links
</verification>
</phase>

## Specifications
- Tests must use the existing framework and patterns — do not introduce new test dependencies
- Follow AAA pattern (Arrange-Act-Assert) for all tests
- Test files must follow the existing naming convention in the repo
- Every test scenario from a Jira ticket must be implemented — no skipping
- Commit messages must reference Jira ticket keys
- All artifacts saved as Jira tickets — no Confluence pages

## Advice and Pointers
- Read existing tests in the repo first to understand patterns before writing new ones
- For component tests, prefer testing behavior (what the user sees/does) over implementation details
- For E2E tests, use stable selectors (data-testid, aria labels) over CSS selectors
- If the test suite is large (20+ tickets), consider using `devin_session_create` to spawn parallel child sessions
- Run tests incrementally as you implement them, not all at the end

## Forbidden Actions
- Do not modify the implementation code to make tests pass
- Do not skip test scenarios defined in Jira tickets
- Do not use snapshot tests as a substitute for behavioral tests
- Do not introduce new testing libraries without checking if alternatives exist in the project
- Do not push directly to main
- Do not save artifacts to Confluence — everything goes into Jira tickets

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
