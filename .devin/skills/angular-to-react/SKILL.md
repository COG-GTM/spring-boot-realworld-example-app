---
name: angular-to-react
description: "Converted from Devin playbook: Angular to React"
triggers:
  - user
  - model
---

# Angular to React

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Angular to React (playbook-53a497336a9a42cc9af761b9545ace10), macro `!angular2react`

## Procedure

# Overview

This playbook describes the procedure to convert a component or small module from Angular to React.

## Procedure

### 1. Environment Setup

- Clone the existing Angular repository and create a new migration branch (e.g., `devin/angular-to-react-{filename}-$RANDOM`). Replace `{filename}` with the component or module being migrated.
- Run tests to verify a clean starting state.
- Document the current Angular version and all external dependencies.

---

### 2. Snapshot the Angular Component

- Identify the component to migrate.
- Copy the full Angular component (HTML + TS + CSS) to `~/backup/` for reference.
- Create a placeholder React file (e.g., `ComponentName.tsx`) in the relevant location and commit the empty shell:
  ```tsx
  export const ComponentName = () => {
    return null;
  };
  ```

Commit message: `init: placeholder for React version of ComponentName`

---

### 3. Analyze Angular Component

In `~/notes.md`, start documenting:

- Inputs (i.e., `@Input()` bindings)
- Outputs (i.e., `@Output()` events)
- Services or dependencies
- Lifecycle hooks used
- Template complexity (e.g., \*ngIf, \*ngFor, pipes, etc.)

---

### 4. Port Component Logic

- Translate the component class logic (methods, properties) to a functional React component using hooks.
- Handle `@Input()` via props.
- Handle `@Output()` via `props.onXYZ` callbacks.
- Replace Angular lifecycle hooks with React equivalents:
  - `ngOnInit` → `useEffect(() => { ... }, [])`
  - `ngOnChanges` → `useEffect()` watching specific props
  - `ngOnDestroy` → `return () => {}` inside `useEffect`

Commit message: `feat: port logic from Angular to React`

---

### 5. Convert Template

- Copy over the HTML template.
- Replace Angular directives:
  - `*ngIf` → conditional rendering
  - `*ngFor` → `.map(...)`
  - `(click)` → `onClick`
  - `[value]` or `[(ngModel)]` → `value` and `onChange`
- Inline pipes with equivalent JavaScript/TS logic (e.g., `date | date` → `formatDate(date)`).

Use Tailwind or existing CSS classnames from the Angular styles.

Commit message: `feat: translate template to JSX`

---

### 6. Wire Up Functionality

- Ensure event handlers are correctly bound.
- Port any injected services to useContext/useHooks or props.
- Replace routing with React Router equivalents, if applicable.

Add `TODO:` comments for any unknown or external dependencies. Document these in `~/notes.md`.

Commit message: `feat: wire up inputs, events, and dependencies`

---

### 7. Test Locally

- Render the new component in a test page or dummy route.
- Run a full set of tests to verify application integrity.
- Manually verify core functionality:
  - Does it render?
  - Are key interactions working?
  - Are props passed correctly?

Commit message: `test: verify component renders and interactions work`

---

### 8. Prepare the Pull Request

- Final review: run `git diff $(git merge-base HEAD origin/main)` and confirm no semantic changes were introduced.
- Share the diff in the chat.
- PR title format: `chore(angular-react): Migrate ComponentName to React`
- PR labels: `needs-dev-review`, `frontend-migration`

---

## Advice & Pointers

- Prefer function components with hooks.
- Keep prop types explicit using `interface`.
- Mimic Angular services with context or dedicated hooks.
- Migrate `ng-content` with `props.children`.
- Favor composition over inheritance.
- Use `useEffect` sparingly — avoid unnecessary re-renders.
- Pipe equivalents:
  - `date` → `date-fns`, `Intl.DateTimeFormat`
  - `currency` → `Intl.NumberFormat`
- Do not optimize performance yet — this is a 1:1 migration.

---

## Forbidden Actions

- Do not remove Angular components
- Do not introduce unrelated refactors or style changes
- Do not use `any` types — use `unknown` or gather context
- Do not skip lifecycle logic — explicitly replace it in React
- Do not mix state management paradigms (e.g., Angular services in React context)

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
