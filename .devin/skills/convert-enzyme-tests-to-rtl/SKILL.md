---
name: convert-enzyme-tests-to-rtl
description: "Converted from Devin playbook: Convert Enzyme Tests to RTL"
triggers:
  - user
  - model
---

# Convert Enzyme Tests to RTL

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Convert Enzyme Tests to RTL (playbook-35f890bd30f741bc96a19bad0d22d6b6), macro `!enzyme_to_rtl`

## Procedure

## Objective
Migrating the test suite from Enzyme to React Testing Library (RTL) to ensure compatibility with future React versions and follow current testing best practices.

## Instructions
1. Replace all Enzyme-specific imports (`shallow`, `mount`, etc.) with RTL equivalents from `@testing-library/react`.
2. Refactor each test case to use RTL queries such as `getByText`, `queryByRole`, `getByLabelText`, etc.
3. Remove or replace Enzyme lifecycle methods (`.instance()`, `.setProps()`, etc.) with testing-library-compatible logic.
4. Use `@testing-library/user-event` to simulate user interactions where applicable.
5. Ensure the updated tests pass locally and that behavior remains consistent.
6. Output a diff summary of changes, including removed Enzyme usage and added RTL functions.

## Notes
- Ensure test readability and maintainability are preserved or improved.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
