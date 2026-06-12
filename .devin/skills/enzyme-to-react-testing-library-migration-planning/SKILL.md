---
name: enzyme-to-react-testing-library-migration-planning
description: "Converted from Devin playbook: Enzyme to React Testing Library Migration Planning"
triggers:
  - user
  - model
---

# Enzyme to React Testing Library Migration Planning

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Enzyme to React Testing Library Migration Planning (playbook-83c62e46c620401e980a9d5597107fe3), macro `!enzyme_to_rtl_planning`

## Procedure

# Enzyme to React Testing Library Migration Planning Playbook

## Objective

Analyze a React project using Enzyme and generate a migration plan for converting tests to React Testing Library (RTL). The output should be a structured set of migration tasks or sprints, identification of blockers, and references to guide future Devin sessions.

> This playbook does not perform the migration. It only generates a migration strategy.

---

## Procedure

### 1. Audit the Test Environment

* **Determine Test Framework Setup:**

  * Identify test runner: `jest`, `mocha`, etc.
  * Confirm presence of Enzyme and adapter setup files

    * e.g., `setupTests.js`, `enzymeAdapterReactXX`, or `configure({ adapter: new Adapter() })`

* **Detect RTL Presence:**

  * Check for installed packages:

    * `@testing-library/react`
    * `@testing-library/jest-dom`
    * `@testing-library/user-event`

* **Log Output:**

  * Testing framework
  * Enzyme version and adapter
  * Any existing RTL usage

---

### 2. Inventory Enzyme Usage

* **File Scan:**

  * Search for files using:

    * `import { shallow, mount, render } from 'enzyme'`
    * `wrapper.find()`, `.simulate()`, `.setProps()`, `.state()`, etc.

* **Generate Usage Report:**
  For each test file:

  ```
  - File: src/components/Button.test.js
    - Uses: mount, simulate, find, setProps
    - Component: Button
  ```

* **Summarize Usage Patterns:**

  * Count of files using Enzyme
  * Count using shallow vs. mount vs. render
  * Common patterns (simulate, state access, ref usage)

---

### 3. Identify Compatibility Gaps

* **Document Unsupported Patterns in RTL:**

  | Enzyme Usage              | RTL Equivalent / Migration Note                                      |
  | ------------------------- | -------------------------------------------------------------------- |
  | `shallow()`               | Not supported; RTL encourages full DOM rendering                     |
  | `.state()`, `.setState()` | RTL does not allow state access—tests must verify DOM output instead |
  | `.simulate('click')`      | Use `userEvent.click()` from RTL                                     |
  | `.find(Component)`        | Use `screen.getByRole()` or `within()` from RTL                      |
  | `mount()`                 | Often replaceable with RTL render + async assertions                 |

* **Highlight Problem Areas:**

  * Tests relying on Enzyme's internal state
  * Tests using `setProps()` for rerenders
  * Custom wrappers or helpers tightly coupled to Enzyme APIs

---

### 4. Plan the Migration Strategy

Recommend an approach:

| Strategy              | Description                                                         |
| --------------------- | ------------------------------------------------------------------- |
| Gradual (recommended) | Convert one test file/module at a time to RTL                       |
| Parallel Testing      | Run both Enzyme and RTL during transition                           |
| Big Bang              | Convert all tests at once and remove Enzyme immediately (high risk) |

Create module groups to migrate together:

* Group 1: Utility functions or atomic components
* Group 2: Medium-complexity presentational components
* Group 3: Stateful or legacy class components
* Group 4: Integration tests or containers

---

### 5. Migration Plan by Sprint

#### Sprint 1: Setup and Tooling

* Install RTL packages if missing:

  * `@testing-library/react`
  * `@testing-library/jest-dom`
  * `@testing-library/user-event`
* Add global RTL setup (e.g., `setupTests.js`)
* Configure ESLint rules for RTL (optional)
* Update test runners to support RTL features
* Add `jest-dom` matchers to Jest config

#### Sprint 2: Inventory and Analysis

* Generate file-level Enzyme usage report
* Tag tests by complexity and replaceability
* Create tracking checklist by file

#### Sprint 3: Migrate Utility and Low-Risk Component Tests

* Migrate files that:

  * Use `shallow()` with minimal assertions
  * Do not rely on lifecycle/state access

#### Sprint 4: Migrate Stateful and Lifecycle-Heavy Tests

* Convert tests that:

  * Use `mount()` or `simulate()`
  * Interact with `setState()`, `ref`, or async behavior

#### Sprint 5: Migrate Containers, Complex Integrations

* Convert tests that:

  * Mock external APIs or Redux stores
  * Include navigation, modals, or portals

#### Sprint 6: Final Cleanup

* Remove all Enzyme-related dependencies
* Delete `enzymeAdapter`, `configure()`, and globals
* Replace remaining helper functions built around Enzyme

---

### 6. Generate Final Plan

Devin should output:

* ✅ Testing framework and Enzyme version
* ✅ Count of Enzyme test files and components
* ✅ List of problematic patterns and files
* ✅ Recommended migration approach
* ✅ Sprint breakdown with file/module grouping
* ✅ Tooling adjustments required
* ✅ Migration readiness score or blocker summary

---

## Resources

* [RTL Migration Guide](https://testing-library.com/docs/react-testing-library/migration/)
* [Enzyme to RTL Cheatsheet](https://kentcdodds.com/blog/migrating-from-enzyme-to-react-testing-library)
* [jest-dom matchers](https://github.com/testing-library/jest-dom)
* [user-event API docs](https://testing-library.com/docs/user-event/intro/)

---

## Forbidden Actions

* 🚫 Do not modify or delete existing Enzyme tests
* 🚫 Do not install or use deprecated test libraries
* 🚫 Do not attempt to polyfill shallow rendering in RTL
* 🚫 Do not begin migration without a usage audit

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
