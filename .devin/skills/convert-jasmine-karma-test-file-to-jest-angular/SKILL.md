---
name: convert-jasmine-karma-test-file-to-jest-angular
description: "Converted from Devin playbook: Convert Jasmine/Karma Test File to Jest (Angular)"
triggers:
  - user
  - model
---

# Convert Jasmine/Karma Test File to Jest (Angular)

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Convert Jasmine/Karma Test File to Jest (Angular) (playbook-e763df016f834e96a496debb9b510c4a), macro `!convert_to_jest`

## Procedure

**📋 Description:**
This playbook converts a single Angular test file written in Jasmine (run with Karma) to use Jest. It replaces Jasmine-specific syntax and prepares the file to run under Jest using modern best practices.

---

### 🧾 Input

* A single Angular test file (typically ending in `.spec.ts`) written with Jasmine/Karma.

---

### 🎯 Goal

* The test file should be fully compatible with Jest.
* Jasmine syntax (e.g. `it`, `spyOn`, `expect`) should be replaced with Jest equivalents.
* The test file should run successfully under Jest (`npx jest` or `npm test`).
* Karma-specific logic or dependencies should be removed from the file.

---

### 🛠️ Procedure

1. **Replace Jasmine Syntax:**

   * Convert `describe`, `it`, `beforeEach`, `afterEach` to Jest equivalents (same API, different environment).
   * Replace `spyOn(obj, 'method')` with `jest.spyOn(obj, 'method')`.
   * Replace `expect(value).toBeTruthy()` or `.toEqual()` as-is (Jest is compatible).

2. **Adjust Imports or Globals (if any):**

   * Remove Karma-specific setup if present in the file (e.g., `inject`, `async` helpers).
   * Add any Jest-specific setup imports if needed (e.g., mock setup).

3. **Optional - Timer or Async Fixes:**

   * Replace `jasmine.clock()` or `fakeAsync` with `jest.useFakeTimers()` and `jest.runAllTimers()` if needed.

4. **Validate Compatibility:**

   * Ensure the test can be executed via Jest (assumes `jest.config.js` exists).
   * If syntax or behavior needs adjustment (e.g., mocking), apply the minimum fix to match Jest's behavior.

---

### ✅ Output

* A Jest-compatible version of the original test file.
* Any important notes about adjustments made (e.g., replaced spies, mocked timers).

---

### 📋 Specifications

* No usage of Jasmine APIs (`spyOn`, `jasmine.createSpy`, etc.) remains.
* The file runs and passes when using Jest.
* The logic and intent of the original test are preserved.

---

### ❌ Forbidden Actions

* Do not modify or migrate other files outside the one provided.
* Do not introduce new dependencies unless required for Jest compatibility.
* Do not keep Jasmine syntax in the output file.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
