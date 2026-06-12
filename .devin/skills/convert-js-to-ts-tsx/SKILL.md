---
name: convert-js-to-ts-tsx
description: "Converted from Devin playbook: Convert JS to TS/TSX"
triggers:
  - user
  - model
---

# Convert JS to TS/TSX

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Convert JS to TS/TSX (playbook-f0461c127ab043bfb0c218af0cf3c993), macro `!js_to_ts`

## Procedure

### Procedure:

1. **Ensure the environment is set up correctly**
   Run `yarn tsc`, and confirm there are no errors. If errors appear, report them to the user before continuing.

2. **Create a working branch**
   Switch to a new branch called `convert-ts-{{filename}}-{{random_number_3_digits}}`, where `filename` is the name of the file being converted.

3. **Convert the target file to TypeScript**

   * Rename the file from `.js` to `.ts` or `.tsx` (use `.tsx` if the file contains a React component).

4. **Analyze types**

   * Identify all types that can be immediately inferred from the file.
   * List all types that require additional investigation or context.
   * Write both sets into `/tmp/notes.md`, which will serve as a TODO tracker.

5. **First-pass conversion**

   * Implement type changes for all immediately known types.
   * For unknown types, insert `// DEVIN_TODO` and do **not** guess or use `any`.
   * Notify the user of types you inferred and any judgement calls made.

6. **Run type checker**

   * Run `yarn tsc` and document new errors in `notes.md`.

7. **Resolve remaining issues (loop through `notes.md`)**
   7.1. Determine whether the fix can be deduced from the file alone or requires exploration.
   7.2. Use tools like `go_to_definition`, `go_to_references`, `find_filecontent`, or `find_filename` to investigate.
   7.3. If a type cannot be found after reasonable investigation:

   * Add the problem to the third section of `notes.md`
   * Apply the best-possible edit to suppress the error using `@ts-expect-error`
   * Notify the user and move on
     7.4. If the correct type is found, apply the fix. If a fix involves modifying other files and it’s a non-obvious call, document it, notify the user, and suppress the error as in 7.3.
     7.5. Repeat until all entries in `notes.md` are resolved.

8. **Final type checks**

   * Re-run `yarn tsc` and `yarn lint` and confirm both pass without errors (warnings are fine).
   * Ensure no `DEVIN_TODO` comments remain.
   * Remove all `propTypes` from the converted file.

9. **Commit the changes**

   * Do not include `notes.md` in the commit.
   * Ensure the original `.js` file is removed and replaced with the `.ts` or `.tsx` version.

10. **Create a Pull Request**

    * Title: `chore(js-ts): Convert {{target_file_path}} to TypeScript`
    * Labels: `needs-dev-review`, `team-mobile-platform`, `No QA Needed`
    * Submit the PR (not as a draft).

---

### Style & Typing Guidelines:

* Use `interface` instead of `type` for component props.

* Never install new `@types` libraries yourself. Instead:

  * Note the recommendation in `notes.md`
  * Declare the module in `app/declarations/index.d.ts`

* **DO NOT** use or rely on:

  * `Function` → use arrow function syntax
  * `Array` → use `[]` syntax
  * `any` → avoid unless unavoidable
  * `object` → avoid unless specific shape can’t be inferred
  * Redundant explicit annotations when types are inferable

* **PropTypes Caveats**:

  * Not all props in `propTypes` are required by default
  * Treat most props as optional unless confident otherwise
  * For `PropTypes.func` or `PropTypes.object`, investigate rather than assume defaults

* `@ts-expect-error` is acceptable for test files (e.g., `*.test.ts`) when needed

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
