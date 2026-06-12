---
name: modernization-planning
description: "Converted from Devin playbook: Modernization Planning"
triggers:
  - user
  - model
---

# Modernization Planning

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Modernization Planning (playbook-9e1ed47f8135425ea86b06c9c6d4352c), macro `!plan_modernization`

## Procedure

Plan and execute a comprehensive modernization of this repository's legacy components. Follow this structured approach:

## Phase 1: Assessment & Planning
1. Analyze the codebase to identify:
   - Legacy dependencies with known vulnerabilities or deprecated versions
   - Outdated framework versions (e.g., React, Spring Boot, Node.js)
   - Deprecated code patterns and APIs
   - Build tool configurations that need updating
2. Create a detailed modernization plan with:
   - Specific upgrade targets (e.g., "React 16 → 18", "Spring Boot 2.7 → 3.2")
   - Risk assessment for each change
   - Dependencies between changes (what must be done first)
   - Rollback strategy for each major change

## Phase 2: Parallel Execution Strategy
Identify components that can be modernized in parallel and launch child Devin sessions for:
- Independent dependency upgrades (packages that don't depend on each other)
- Separate modules/services that don't share code
- Isolated configuration updates
- Non-interdependent code pattern migrations

For each child session, provide:
- Clear scope and acceptance criteria
- Specific files/components to modify
- Expected outcome and validation steps

## Phase 3: Sequential Execution
Execute changes that must be done sequentially:
- Framework upgrades that affect multiple components
- Breaking changes that require coordinated updates
- Database schema migrations if applicable
- Build system reconfigurations

## Phase 4: Comprehensive Frontend Testing
After modernization, conduct thorough frontend testing:
- Run existing test suites and report results
- Test critical user flows (login, navigation, form submissions, API calls)
- Check responsive design across different screen sizes
- Validate accessibility (WCAG compliance)
- Test browser compatibility (Chrome, Firefox, Safari, Edge)
- Performance testing (load times, bundle sizes)
- Visual regression testing for UI components

## Phase 5: Documentation & Reporting
Create a comprehensive modernization report including:
- Detailed list of all changes made
- Before/after comparisons (dependency versions, bundle sizes, performance metrics)
- Test results with screenshots/video evidence
- Known issues or limitations
- Recommendations for future modernization
- Rollback instructions if needed

Start by providing an assessment and plan before executing any changes

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
