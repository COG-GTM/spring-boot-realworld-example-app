---
name: increase-test-coverage
description: "Converted from Devin playbook: Increase Test Coverage"
triggers:
  - user
  - model
---

# Increase Test Coverage

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Increase Test Coverage (playbook-0b9a4c3a808743bf842213f516aa9bed), macro `!increase_test_coverage`

## Procedure

This playbook outlines the process for increasing automated test coverage across any codebase, regardless of language or framework. The goal is to identify untested areas, write meaningful tests, and track progress using the project’s coverage tooling.

---

### Procedure

#### 1. Identify Untested or Low-Coverage Code

* Audit the existing codebase for modules, components, classes, or utilities that lack corresponding test files or have low test coverage.
* Use your coverage tool (e.g., `jest --coverage`, `pytest --cov`, `nyc`, etc.) to generate a baseline coverage report.
* Focus first on core logic, utilities, or critical paths that are currently untested or under-tested.

#### 2. Create Missing Tests

* For each untested module:

  * Add a test file in the appropriate directory (e.g., `tests/`, `spec/`, or adjacent to the source).
  * Follow the project's conventions for naming (e.g., `module.test.ts`, `test_module.py`, `ModuleSpec.java`).
  * Use the testing framework already adopted by the project (e.g., Jest, Pytest, JUnit, etc.).
  * Stub or mock external dependencies, API calls, or services where applicable.

#### 3. Strengthen Existing Tests

* For modules with existing tests but low coverage:

  * Add scenarios to cover edge cases, failure paths, and exception handling.
  * Ensure conditional logic (e.g., if/else, switches, guards) is fully tested.
  * Review test assertions for depth—ensure they verify output, side effects, and key behaviors.

#### 4. Validate with a Coverage Report

* Re-run the test suite with coverage enabled to validate improvements.

  * Example: `yarn test --coverage`, `pytest --cov`, `npm run coverage`, `go test -cover`
* Compare the coverage delta before and after your changes.
* Identify any remaining coverage gaps and iterate as needed.

#### 5. Finalize Changes and Submit

* Document the initial and final coverage rates in a comment or PR description.
* Push changes to a new branch and open a pull request.
* Include:

  * Summary of files/modules covered
  * Notable edge cases or logic paths added to tests
  * Coverage improvement (e.g., “Increased line coverage from 72% to 88%”)

---

### Tips & Best Practices

* Start with logic-heavy files (e.g., business logic, core utilities) before UI or glue code.
* Mock external services to keep tests deterministic and fast.
* Use CI to enforce minimum coverage thresholds if not already configured.
* Keep test files in sync with source files as part of future development.

---

### Forbidden Actions

* Do not delete existing tests unless they are demonstrably redundant or broken.
* Do not rely solely on snapshot or golden tests for behavioral coverage.
* Do not reduce test coverage or bypass coverage tools.
* Do not introduce large test files without meaningful assertions.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
