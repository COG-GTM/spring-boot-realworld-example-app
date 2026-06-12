---
name: javascript-to-typescript-migration-planning
description: "Converted from Devin playbook: JavaScript to TypeScript Migration Planning"
triggers:
  - user
  - model
---

# JavaScript to TypeScript Migration Planning

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: JavaScript to TypeScript Migration Planning (playbook-15de99e0caf347629c8968279b31bfcb), macro `!js_to_ts_planning`

## Procedure

# JavaScript to TypeScript Migration Planning Playbook

## Objective

Analyze a JavaScript project and produce a structured migration plan for converting it to TypeScript. The output should be a prioritized set of tasks or sprints, compatibility research, and potential blockers to guide future Devin sessions.

> This playbook is for planning only. No code changes should be made.

---

## Procedure

### 1. Audit the Codebase

* **Detect Language and Framework:**

  * Count `.js`, `.jsx`, `.ts`, `.tsx` files to assess current state
  * Identify frameworks used (React, Node.js, Express, Vue, etc.)
  * Check for any existing TypeScript config files (`tsconfig.json`)

* **Catalog Project Structure:**

  * Identify key folders: source, tests, components, utilities, etc.
  * Determine if the repo is monolithic or includes packages (e.g., Lerna, Turborepo, Nx)

* **Check Build System:**

  * Record tooling: Babel, Webpack, Vite, Rollup, etc.
  * Record testing tools: Jest, Mocha, Vitest, etc.

* **Log Output:**

  * Number of JS files
  * Build/test stack
  * Frameworks detected

---

### 2. Identify Compatibility and Migration Readiness

* **TypeScript Dependency Check:**

  * List any existing TypeScript or DefinitelyTyped dependencies (`@types/*`)
  * Identify any already-migrated files (`*.ts`, `*.tsx`)
  * Check for TS build tools or configurations (e.g., Babel preset-typescript)

* **Third-Party Library Coverage:**

  * Generate a list of all dependencies (via `package.json`)
  * For each:

    * Does it have TypeScript types (bundled or via `@types`)?
    * Are types actively maintained?

* **Tooling Compatibility:**

  * Identify which tools must be reconfigured for TS support:

    * Linters (`eslint`, `prettier`)
    * Bundlers (Babel config, Webpack loaders)
    * Test frameworks (type-aware configs)

---

### 3. Identify High-Risk Areas

* **Dynamic Code:**

  * Locate use of `eval`, dynamic imports, prototype modification
* **Complex Typing:**

  * Flag files with large untyped object structures, deeply nested logic, or lots of implicit coercion
* **Loose APIs:**

  * Flag usage of `any`, `arguments`, or duck-typed patterns
* **Interop Challenges:**

  * Identify files importing CommonJS or legacy modules that may be harder to type

Output a list of high-risk files or patterns.

---

### 4. Define Migration Strategy

Recommend a migration mode:

| Mode                  | Description                                                    |
| --------------------- | -------------------------------------------------------------- |
| Gradual (Recommended) | Convert one file/module at a time; use `allowJs` and `checkJs` |
| All-at-once           | Convert the entire project in one pass (high risk)             |

Propose a structure for incrementally migrating the codebase:

* Convert leaf modules first (utils, constants)
* Then core business logic
* Finally, UI/components and integration points

---

### 5. Create Migration Plan

Break down the migration into logical **sprints or phases**:

#### Sprint 1: Prep Environment

* Add TypeScript as a dev dependency
* Create a `tsconfig.json` with `allowJs` and `checkJs` enabled
* Update build tools (e.g., Babel, Webpack) to support `.ts`, `.tsx` files
* Configure linters and formatters for TS support

#### Sprint 2: Infrastructure Compatibility

* Migrate configuration and tooling files (e.g., `webpack.config.js`, `.eslintrc.js`) to TypeScript-compatible formats if needed
* Run linters and formatters on JS files to standardize structure before conversion
* Enable `checkJs` and fix type-check errors in existing JS files (optional pre-step)

#### Sprint 3: Migrate Core Utilities

* Convert utility/helper functions (`utils/`, `lib/`, etc.) to `.ts`
* Add basic type annotations and interfaces

#### Sprint 4: Migrate Components and Business Logic

* Convert core components and modules
* Add interfaces and type aliases as needed
* Use `@types/*` for third-party modules

#### Sprint 5: Migrate Entry Points and Framework Code

* Convert root modules, index files, routing code, server entry points, etc.
* Ensure types propagate correctly across module boundaries

#### Sprint 6: Enforce Type Safety

* Update `tsconfig.json` to disallow JS files (`allowJs: false`)
* Enable stricter compiler options (`strict`, `noImplicitAny`, etc.)
* Replace `any` with proper types

#### Sprint 7: Final Cleanup

* Remove remaining JS files
* Add documentation and onboarding notes for TypeScript usage
* Confirm all scripts, builds, and tests are TS-compatible

---

### 6. Generate Output Plan

The final deliverable should include:

* ✅ Language and tooling audit
* ✅ Dependency compatibility report
* ✅ High-risk file list
* ✅ Recommended migration mode
* ✅ Migration plan broken into sprints or phases
* ✅ `tsconfig.json` recommendations
* ✅ Linter and test configuration tasks
* ✅ Links to official TS migration guides (see below)

---

## Resources

* [TypeScript Handbook](https://www.typescriptlang.org/docs/)
* [Migrating from JavaScript](https://www.typescriptlang.org/docs/handbook/migrating-from-javascript.html)
* [React + TypeScript Cheatsheets](https://react-typescript-cheatsheet.netlify.app/)
* [DefinitelyTyped](https://definitelytyped.org/)
* [eslint-config-typescript](https://typescript-eslint.io/)

---

## Forbidden Actions

* 🚫 Do not rename files to `.ts` or `.tsx`
* 🚫 Do not auto-infer or guess types
* 🚫 Do not remove existing JS support unless confirmed ready
* 🚫 Do not run TS compiler in strict mode until enough files are migrated
* 🚫 Do not skip toolchain review—build/test tools often need config updates

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
