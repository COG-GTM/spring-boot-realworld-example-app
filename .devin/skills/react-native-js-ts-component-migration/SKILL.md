---
name: react-native-js-ts-component-migration
description: "Converted from Devin playbook: React Native JS→TS Component Migration"
triggers:
  - user
  - model
---

# React Native JS→TS Component Migration

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: React Native JS→TS Component Migration (playbook-497dc24cd961470ab34bd583fb9aaa05), macro `!mm_js_to_ts`

## Procedure

# React Native JS→TS Component Migration

## Overview

Migrate a single React Native component file from JavaScript to TypeScript in the target repository (`{REPO_OWNER}/{REPO_NAME}`). This playbook converts `.js`/`.jsx` files to `.ts`/`.tsx`, replacing `PropTypes` with TypeScript interfaces, converting class components to functional components with hooks (where appropriate), and ensuring all existing tests and snapshots remain valid.

## What's Needed From User

- **Target file path** — the `.js` or `.jsx` file to migrate (e.g., `app/components/UI/FoxScreen/index.js`)
- **Reference component** (optional) — an already-migrated TS component in the same area of the codebase to use as a pattern guide. If not provided, Devin will locate one automatically.
- **Repository access** — push access to the target repository

Only ever create the todo list for the current phase.

<phase name="Context Gathering" id="1">
## Context Gathering

1. Check out a new branch: `git checkout -b devin/$(date +%s)-<component-name>-js-to-ts`
2. Read the **target file** to understand:
   - Is it a class component (`Component`, `PureComponent`) or a functional component?
   - Does it use `PropTypes`? What props does it accept?
   - Does it use `ThemeContext`/`mockTheme` or `connect()` (Redux)?
   - What imports does it use (React Native, navigation, theme, selectors)?
3. Read the **test file** (`.test.js` or `.test.tsx`) and **snapshot file** (`__snapshots__/*.snap`) if they exist
4. Find and read a **reference component** — a nearby already-migrated `.tsx` file that follows the modern pattern. Good candidates:
   - Same directory or sibling directory
   - Similar complexity (class vs functional, connected vs standalone)
   - Check `app/component-library/components/` for canonical patterns
5. Read the **theme utility** (`app/util/theme/index.ts`) and **theme models** (`app/util/theme/models.ts`) to understand `useTheme()`, `Colors`, and `Theme` types
6. Document findings: component type, props shape, Redux connection, theme usage, test coverage

<verification>
- The target file has been read and its structure is understood (class vs functional, props, context usage)
- The test file and snapshot have been read (or confirmed not to exist)
- A reference TS component has been identified and read
- Theme types (`Colors`, `Theme`, `useTheme`) are understood
- A branch has been created
</verification>
</phase>

<phase name="Migration" id="2">
## Migration

1. **Rename the file** using `git mv` (preserves git history):
   ```bash
   git mv <path>/index.js <path>/index.tsx
   ```
   Use `.tsx` for components (JSX), `.ts` for pure logic files.

2. **Convert the component** based on what was found in Phase 1:

   **If class component → functional component** (preferred modern pattern):
   - Replace `class Foo extends PureComponent/Component` with `const Foo = () => { ... }`
   - Replace `this.context` / `ThemeContext` with `const { colors } = useTheme()`
   - Replace `this.props` destructuring with function parameters
   - Replace `this.state` with `useState` hooks
   - Remove `ThemeContext` and `mockTheme` imports; add `import { useTheme } from '../../../util/theme'`

   **If already a functional component:**
   - Just add TypeScript types to props, state, and function signatures

   **If Redux-connected component** (uses `connect()`):
   - Keep `connect()` — do NOT convert to hooks during migration
   - Define `OwnProps`, `StateProps`, `DispatchProps` interfaces
   - Type `mapStateToProps` with `(state: RootState): StateProps`
   - Type `mapDispatchToProps` similarly

   **If component uses BOTH `connect()` AND `ThemeContext`:**
   - Keep `connect()` as-is (do not convert to hooks)
   - Replace `ThemeContext`/`this.context` with `useTheme()` hook (this requires converting the class component to a functional component)
   - Define all three prop interfaces (`OwnProps`, `StateProps`, `DispatchProps`) and combine them
   - The inner functional component uses `useTheme()`, while `connect()` wraps it from outside

3. **Add TypeScript types:**
   - Define a `Props` interface (use `interface`, not `type`) for component props:
     - Map `PropTypes.string` → `string`, `PropTypes.number` → `number`, etc.
     - `PropTypes.func` → specific function signature (e.g., `() => void`, `(value: string) => void`)
     - `PropTypes.node` → `React.ReactNode`, `PropTypes.element` → `React.ReactElement`
     - `.isRequired` → non-optional; otherwise add `?`
   - Type the `createStyles` function parameter with `Colors` from `../../../util/theme/models`
   - Type refs: `useRef<View>(null)`, `useRef<TextInput>(null)`, etc.
   - Use `ReactNode` for children prop types, `StyleProp<ViewStyle>` for style props

4. **Clean up:**
   - Remove `import PropTypes from 'prop-types'` and all `Foo.propTypes = { ... }` blocks
   - Remove `Foo.contextType = ThemeContext` if present
   - Ensure imports are at the top of the file
   - Place the `import React` statement first

5. **Rename the test file** if it's `.test.js`:
   ```bash
   git mv <path>/index.test.js <path>/index.test.tsx
   ```

<verification>
- The file has been renamed from `.js` to `.tsx` (or `.ts`) using `git mv`
- Class component has been converted to functional component with hooks (if applicable)
- `PropTypes` have been replaced with TypeScript interfaces
- `ThemeContext`/`mockTheme` replaced with `useTheme()` hook (if applicable)
- No `any` types used — all types are properly specified
- No `PropTypes` imports or assignments remain
- Test file renamed if needed
</verification>
</phase>

<phase name="Validation & PR" id="3">
## Validation & PR

1. **Run lint check:**
   ```bash
   source ~/.nvm/nvm.sh && nvm use 20 && yarn lint
   ```
   Fix any lint errors in the migrated file.

2. **Run type check:**
   ```bash
   source ~/.nvm/nvm.sh && nvm use 20 && yarn tsc
   ```
   Fix any type errors. If a type cannot be resolved after reasonable investigation, use `@ts-expect-error` with an explanatory comment and document in the PR description.

3. **Run related tests:**
   ```bash
   source ~/.nvm/nvm.sh && nvm use 20 && yarn test --findRelatedTests <migrated-file-path>
   ```
   - If snapshot tests fail due to the class→functional conversion, update snapshots: `yarn test --findRelatedTests <file> --updateSnapshot`
   - If tests fail for other reasons, fix the issue before proceeding

4. **Commit changes:**
   ```bash
   git add <migrated-files>
   git commit -m "chore(js-ts): Convert <ComponentName> to TypeScript functional component"
   ```
   Do NOT use `git add .` — only add the specific migrated files.

5. **Push and create PR:**
   ```bash
   git push origin <branch-name>
   ```
   Create the PR with:
   - **Title**: `chore(js-ts): Convert <ComponentName> to TypeScript`
   - **Body**: Summary of changes (rename, class→functional conversion, type additions), review checklist, and notes about any `@ts-expect-error` suppressions

6. **Monitor CI:** Wait for CI checks. If failures occur, investigate whether they are caused by the migration or are pre-existing. Report findings to the user.

<verification>
- `yarn lint` passes (or only has pre-existing warnings)
- `yarn tsc` passes (or only has pre-existing errors unrelated to the migrated file)
- Related tests pass (snapshots updated if needed)
- Changes are committed with the conventional commit message format
- PR has been created and pushed
- CI results have been checked and reported to the user
</verification>
</phase>

## Specifications

- The migration must be **behavior-preserving** — the rendered output should be identical (snapshot parity)
- Use `interface` (not `type`) for prop definitions
- Use `useTheme()` hook instead of `ThemeContext` consumer pattern
- Keep `connect()` HOCs — do not convert to hooks during migration
- Preserve `///: BEGIN:ONLY_INCLUDE_IF(...)` preprocessor directives exactly as-is
- PR title format: `chore(js-ts): Convert <ComponentName> to TypeScript`
- All existing tests must pass after migration

## Advice and Pointers

- **Node version**: MetaMask Mobile requires Node 20. Always run `nvm use 20` before any yarn commands.
- **Reference patterns**: `app/component-library/components/` contains canonical TS component patterns. `app/components/UI/SettingsNotification/index.js` is a good reference for `useTheme()` usage.
- **Theme types**: The `Colors` type is in `app/util/theme/models.ts`. The `useTheme()` hook returns a `Theme` object with a `colors` property.
- **Zero-prop components**: If the component takes no props, you can omit the `Props` interface entirely or add an empty one for consistency.
- **`PureComponent` → functional**: When converting `PureComponent`, note that `PureComponent` provides shallow prop comparison. For zero-prop components this has no impact. For components with props, consider wrapping with `React.memo()` if performance is a concern.
- **Snapshot changes**: Converting from class to functional component may cause minor snapshot differences (e.g., component display name). Update snapshots if the rendered output is structurally identical.
- **Pre-existing CI failures**: The MetaMask Mobile CI can have infrastructure-related failures (e.g., dependency resolution issues). Distinguish these from migration-caused failures when reporting results.

## Forbidden Actions

- Do NOT use `any` type without justification — investigate and find the correct type. If truly unavoidable, add a `// TODO: Replace "any" with type` comment (this is the established codebase convention) and document it in the PR description
- Do NOT install new `@types` packages — declare modules in `app/declarations/index.d.ts` instead
- Do NOT refactor logic during migration — only add types and convert class→functional
- Do NOT convert `connect()` to hooks — this is a separate refactoring effort
- Do NOT modify test assertions to make tests pass — fix the source code instead
- Do NOT use `git add .` — only stage the specific files you changed
- Do NOT skip reading reference components — they establish the patterns to follow
- Do NOT commit temporary files (`notes.md`, etc.) to the repository

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
