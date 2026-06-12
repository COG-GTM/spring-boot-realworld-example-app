---
name: convert-angular-testbed-test-file-to-angular-testing-library
description: "Converted from Devin playbook: Convert Angular TestBed Test File to Angular Testing Library"
triggers:
  - user
  - model
---

# Convert Angular TestBed Test File to Angular Testing Library

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Convert Angular TestBed Test File to Angular Testing Library (playbook-fdff2e76face4e6d824990f465c82d15), macro `!convert_testbed_to_atl`

## Procedure

**📋 Description:**
This playbook converts a single Angular test file written with `TestBed`-based logic to use [Angular Testing Library (ATL)](https://testing-library.com/docs/angular-testing-library/intro). The goal is to shift from implementation-focused tests to user-focused tests that query and interact with the DOM.

---

### 🧾 Input

* A single Angular test file (typically ending in `.spec.ts`) that uses `TestBed` and interacts with `fixture.debugElement`, `componentInstance`, or manual lifecycle methods.

---

### 🎯 Goal

* The test should be rewritten using Angular Testing Library's `render()` and DOM query APIs (`screen`, `getByText`, etc.).
* Replace internal interactions (e.g., calling component methods) with simulated user actions (`userEvent.click`, etc.).
* The new test should focus on visible DOM behavior, not internal state.

---

### 🛠️ Procedure

1. **Replace Test Setup:**

   * Remove `TestBed.createComponent` and related boilerplate.
   * Use `render()` from Angular Testing Library to render the component.

2. **Replace Queries and Assertions:**

   * Replace `fixture.debugElement.query()` or `nativeElement.querySelector()` with `screen.getBy...` or `screen.queryBy...`.
   * Replace `componentInstance` method calls with DOM interactions (e.g., click buttons or type in inputs).

3. **Use `userEvent` for Interaction:**

   * Simulate user input and events using `userEvent.click`, `userEvent.type`, etc., instead of directly modifying component state.

4. **Remove Direct Access to Component Internals:**

   * Do not use `fixture.componentInstance` or test internal method outputs.
   * Validate outcomes through the DOM (e.g., visible text or component behavior).

5. **Ensure the Test Passes:**

   * Validate the rewritten test using `npm test` or `jest`.
   * Match assertions to what the user would actually see or do.

---

### ✅ Output

* A rewritten test file using Angular Testing Library.
* All internal accesses to `componentInstance`, `debugElement`, etc., are removed.
* DOM is queried with `screen` APIs, and user actions are simulated with `userEvent`.

---

### 📋 Specifications

* Uses `render()` from `@testing-library/angular`.
* Only DOM-based queries are used (`getByText`, `getByRole`, etc.).
* Component logic is triggered via DOM events, not internal method calls.
* The test passes and reflects actual user behavior.

---

### ❌ Forbidden Actions

* Do not access `fixture.componentInstance` or `debugElement`.
* Do not simulate logic by calling component methods directly.
* Do not test implementation details or private state.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
