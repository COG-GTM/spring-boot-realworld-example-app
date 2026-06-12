---
name: angular-component-to-react-typescript-migration
description: "Converted from Devin playbook: Angular Component to React + TypeScript Migration"
triggers:
  - user
  - model
---

# Angular Component to React + TypeScript Migration

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Angular Component to React + TypeScript Migration (playbook-05b9de27f54146bb9400a62675586811), macro `!migrate_from_angular_to_react`

## Procedure

# Angular Component to React + TypeScript Migration

## Overview

This playbook migrates individual Angular component modules to React functional components with TypeScript, React Router v6, and React hooks. It produces new `.tsx` and `.module.scss` files alongside the original Angular files so both implementations can be compared side-by-side.

Derived from the migration of `item-details` and `comment` components in the angular2-hn PWA.

## What's Needed From User

- Repository name and branch to work from
- Which Angular component(s) to migrate (file paths)
- Whether to preserve original Angular files alongside the new React files (default: yes)

<phase name="Analyze Angular Source" id="1">
## Analyze Angular Source

1. Clone the repo and create a migration branch: `devin/<timestamp>-migrate-<component-name>`
2. Identify all files belonging to the Angular component module:
   - `.component.ts` (class logic, lifecycle hooks, DI)
   - `.component.html` (template)
   - `.component.scss` (styles)
   - `.module.ts` (module declaration)
   - Any child components in subdirectories
3. Document the following in a TODO list:
   - `@Input()` bindings → will become `Props` interface fields
   - `@Output()` events → will become callback props
   - Injected services (`constructor` DI) → will become hooks, context, or plain `fetch`
   - Lifecycle hooks used (`ngOnInit`, `ngOnDestroy`, etc.)
   - Template directives (`*ngIf`, `*ngFor`, `[innerHTML]`, `[ngStyle]`, `[routerLink]`, `(click)`, `[class.X]`)
   - Pipes used → will become utility functions
   - `Subscription` objects → will become `AbortController` cleanup
4. Check for shared models (e.g., `Story`, `Comment`) that the component imports — these will be reused as-is or converted to TS interfaces

<verification>
- All Angular source files for the component have been read and catalogued
- Every @Input, @Output, service injection, lifecycle hook, and template directive has been documented
- Child/nested components have been identified
- Shared models and pipes have been listed
</verification>
</phase>

<phase name="Create React Component Files" id="2">
## Create React Component Files

Apply these Angular → React conversion rules:

### Component Structure
| Angular | React |
|---------|-------|
| `@Component` class + `.html` template | Functional component with inline JSX in `.tsx` |
| `@Input()` | Typed `Props` interface |
| `@Output()` EventEmitter | Callback props (`onXyz: () => void`) |
| `constructor` DI services | Hooks / Context / plain `fetch` |
| `ngOnInit` + `route.params.subscribe()` | `useEffect` + `useParams()` (react-router-dom v6) |
| `ngOnDestroy` / `Subscription` | `useEffect` return cleanup with `AbortController` |
| `_location.back()` | `useNavigate()` → `navigate(-1)` |
| Component state properties | `useState<T>()` |

### Template Directives
| Angular | React |
|---------|-------|
| `*ngIf="condition"` | `{condition && <Element />}` |
| `*ngFor="let item of items"` | `{items.map(item => <Element key={item.id} />)}` |
| `[innerHTML]="html"` | `dangerouslySetInnerHTML={{ __html: html }}` |
| `[ngStyle]="{width: x}"` | `style={{ width: x }}` |
| `[class.active]="cond"` | Template literal class: `` `${styles.base}${cond ? ` ${styles.active}` : ''}` `` |
| `[routerLink]="['/path', id]"` | `<Link to={\`/path/${id}\`}>` |
| `(click)="handler()"` | `onClick={handler}` |
| `[hidden]="cond"` | `hidden={cond}` or conditional render |

### Services
| Angular | React |
|---------|-------|
| `HackerNewsAPIService` (HTTP DI) | Plain `fetch` with `useEffect` + `useState` + `AbortController` |
| `SettingsService` (DI) | `useSettings()` hook (React Context) |
| Angular pipes (e.g., `comment`) | Standalone utility function (e.g., `formatCommentCount.ts`) |

### Styles
| Angular | React |
|---------|-------|
| `styleUrls: ['./x.component.scss']` | CSS Modules: `import styles from './X.module.scss'` |
| SCSS class names (kebab-case) | camelCase class names in CSS Modules |

### File Naming Convention
| Angular | React |
|---------|-------|
| `item-details.component.ts` | `ItemDetails.tsx` |
| `item-details.component.scss` | `ItemDetails.module.scss` |
| `comment.component.ts` | `Comment.tsx` |
| `comment` pipe | `formatCommentCount.ts` in `shared/utils/` |

### Implementation Steps

1. Create the main component `.tsx` file:
   - Define the functional component with typed props
   - Convert lifecycle hooks to `useEffect` with proper dependency arrays
   - Replace DI service calls with `fetch` + `useState` + `AbortController`
   - Always clean up subscriptions/fetches in the `useEffect` return
   - Handle `AbortError` separately: `if (err.name !== 'AbortError') { setError(...) }`

2. Create the `.module.scss` file:
   - Copy styles from Angular `.component.scss`
   - Convert class names to camelCase
   - Keep existing `@import` paths (adjust relative paths as needed)
   - Preserve all media queries and responsive breakpoints

3. For each Angular pipe used, create a utility function in `shared/utils/`:
   - Export a pure function with typed parameters and return type
   - Example: `export function formatCommentCount(count: number): string`

4. For child components (e.g., recursive comment tree):
   - Create a separate `.tsx` + `.module.scss` in the child directory
   - Define a `Props` interface with the required data model
   - Use the same component name for recursive rendering

5. Handle special patterns:
   - **Recursive components**: Import the component and render itself for nested data
   - **Poll/async data**: Chain `Promise.all` with proper `.catch()` handlers
   - **Scroll-to-top on mount**: `window.scrollTo(0, 0)` in `useEffect`
   - **Loading/error states**: Conditional render with `Loader` and `ErrorMessage` components
   - **Conditional link targets**: Derive `target` and `rel` from settings

<verification>
- All .tsx component files have been created with typed Props interfaces
- All .module.scss files have been created with camelCase class names
- Any pipes have been converted to utility functions in shared/utils/
- useEffect hooks have proper dependency arrays and AbortController cleanup
- AbortError is handled separately from real errors in all catch blocks
- Template directives have been correctly converted (ngIf→&&, ngFor→map, etc.)
- Recursive components render correctly (e.g., nested comment trees)
- Loading and error states are handled
</verification>
</phase>

<phase name="Validate and Create PR" id="3">
## Validate and Create PR

1. Run lint checks to ensure code quality:
   - Check for TypeScript errors
   - Verify CSS Modules class names are camelCase
   - Ensure no `any` types are used

2. Verify the file structure matches the expected output:
   - New React files sit alongside original Angular files
   - Shared utilities are in the correct `shared/utils/` directory
   - Import paths are correct and consistent

3. Review the conversion checklist:
   - [ ] All `@Input()` bindings converted to typed Props
   - [ ] All lifecycle hooks converted to useEffect with correct deps
   - [ ] All service injections replaced with hooks/fetch/context
   - [ ] All template directives converted to JSX equivalents
   - [ ] All pipes converted to utility functions
   - [ ] All subscriptions replaced with AbortController cleanup
   - [ ] Styles use CSS Modules with camelCase class names
   - [ ] No Angular dependencies remain in the new React files

4. Commit all changes with message: `feat: migrate <ComponentName> from Angular to React + TypeScript`

5. Push the branch and create a PR with:
   - Summary table of files created and their purpose
   - Full list of Angular → React conversions applied
   - Note that original Angular files are preserved
   - Testing checklist for the reviewer
   - Note any shared components referenced but not yet created (e.g., `useSettings`, `Loader`, `ErrorMessage`)

6. Wait for CI to pass

<verification>
- All lint checks pass
- No TypeScript errors
- PR has been created with a comprehensive description
- CI checks pass
- Original Angular files are preserved (not deleted)
</verification>
</phase>

## Specifications

- Output files use React 18+ functional components with hooks
- TypeScript strict mode — no `any` types
- React Router v6 (`useParams`, `useNavigate`, `Link`)
- CSS Modules (`.module.scss`) with camelCase class names
- All fetch calls use `AbortController` for cleanup
- Original Angular files are NOT deleted

## Advice and Pointers

- Always check the Angular component's constructor for injected services — each one needs a React equivalent
- For `HackerNewsAPIService` or similar HTTP services, trace the actual API call to get the URL and use plain `fetch` instead
- When converting `Subscription` patterns, the `AbortController` should be created at the top of the `useEffect` and `.abort()` called in the cleanup function
- For recursive components (like comment trees), use the same component name and import pattern
- Poll data fetching requires chaining `Promise.all` — always add `.catch()` with `AbortError` guard
- CSS Modules require importing as `styles` object — access classes as `styles.className`
- When concatenating conditional classes, use template literals: `` `${styles.base}${condition ? ` ${styles.active}` : ''}` ``
- Reference existing React components in the repo for patterns and conventions
- Keep `dangerouslySetInnerHTML` usage minimal but necessary for HTML content from APIs

## Forbidden Actions

- Do not delete the original Angular component files
- Do not use `any` types — use `unknown` or investigate the actual type
- Do not introduce unrelated refactors or style changes
- Do not skip lifecycle hook conversion — explicitly replace each one
- Do not leave unhandled promise rejections — always add `.catch()` handlers
- Do not use class components — always use functional components with hooks
- Do not skip `AbortController` cleanup in fetch-based `useEffect` hooks
- Do not use inline styles when CSS Modules are available

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
