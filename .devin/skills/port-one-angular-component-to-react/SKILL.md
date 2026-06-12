---
name: port-one-angular-component-to-react
description: "Converted from Devin playbook: Port One Angular Component to React"
triggers:
  - user
  - model
---

# Port One Angular Component to React

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Port One Angular Component to React (playbook-3a438110378c46acb76cb7125a8c145f), macro `!port_angular_component`

## Procedure

Playbook: Port One Angular Component to React

## Overview
Port a single Angular component (template + TypeScript class + SCSS) from a source Angular repo into an equivalent idiomatic React + TypeScript component in a target React repo. The target repo already contains the scaffolding (models, hooks, context, shared components, theme). Your job is to add ONE component plus its CSS Module so the build and lint pass cleanly, then commit and push.

## What's Needed From User
- **Source repo** (read-only Angular reference): `owner/repo` (e.g. `ankehao-demo/angular2-hn`).
- **Target repo** (where commits go): `owner/repo` (e.g. `COG-GTM/react-hn`).
- **Component name**: the Angular class/selector being ported (e.g. `FeedComponent`, `UserComponent`, `ItemDetailsComponent`).
- **Target branch**: the integration branch to commit onto (default: the target repo's default branch, or one specified by the user, e.g. `devin-develop-test-hn-react`).
- Optional: a parallel-batch scope note listing files you MUST NOT touch (when multiple sessions are porting siblings concurrently).

## Procedure
1. Clone/pull both repos. In the target repo: `git fetch origin && git checkout <target-branch> && git pull --rebase origin <target-branch>`. Run `npm install` (a fresh install — stale `node_modules` is the most common cause of spurious `tsc`/eslint failures).
2. Map the target file paths. Mirror the Angular folder under `src/components/<area>/<ComponentName>.tsx` + `<ComponentName>.module.scss`. Confirm the file does not already exist on the branch; if it does, stop and ask the user.
3. Read the Angular source end-to-end: the component's `.ts`, `.html` template, `.scss`, any sub-templates, and the route entry that uses it. List every `@Input`, `@Output`, injected service, pipe, router param, lifecycle hook, and `@HostListener`.
4. Inventory what's already ported on the target branch so you reuse it instead of re-implementing. Check at minimum:
   - `src/models/` for typed domain models
   - `src/hooks/` (e.g. `useSettings`, `useHackerNewsAPI`) — these replace Angular services
   - `src/context/` for cross-tree state (e.g. `SettingsContext`)
   - `src/utils/` for pipe replacements (formatters, time-ago, etc.)
   - `src/components/shared/` and peer components for `Loader`, `ErrorMessage`, `Comment`, etc.
   - `src/styles/` for theme variables and mixins
5. Translate the component using this idiom map:
   - `@Input foo` → props (`interface Props { foo: T }`)
   - `@Output EventEmitter` → callback prop (`onFoo: (x: T) => void`)
   - Constructor-injected service → corresponding hook (`useX()`) or context consumer
   - `ngOnInit` / `ngOnDestroy` → `useEffect(() => { ...; return cleanup; }, [deps])`
   - `*ngIf` / `*ngFor` → conditional rendering / `array.map`
   - `[class.x]`, `[style.x]` → `className={styles.x}` via CSS Modules, inline `style={{}}`
   - Angular pipes → utility functions from `src/utils/` or inline
   - `Router` / `ActivatedRoute` → `react-router-dom`: `useParams`, `useNavigate`, `useSearchParams`, `<Link>`
   - Observables / subscriptions → data hooks that return `{ data, loading, error }`; never subscribe manually in the component
   - `@HostListener('window:...')` → `useEffect` that adds/removes the listener
   - Template-driven forms / `[(ngModel)]` → controlled inputs with `useState`
6. Translate the SCSS 1:1 into a CSS Module (`<ComponentName>.module.scss`), keeping theme imports (`@use '../../styles/...' as *;`) and replacing `:host` with the component's root class. Keep selectors flat where possible so CSS Modules hashing works.
7. Do NOT wire the component into `App.tsx` or routes unless the user explicitly asks — routing integration is a separate batch step.
8. Run the gate: `npm run build` and `npm run lint`. Both must be clean. If either fails with config errors (e.g. unknown `tsconfig` option, ESLint flat-config `recommended` undefined), first `rm -rf node_modules && npm install` before touching config.
9. Commit with a focused message (`Port <ComponentName> to React <File>.tsx`) and push to `<target-branch>`. Do NOT force-push. Do NOT touch unrelated files or other sessions' components.
10. Report back: target-branch name, commit SHA, exact files created, build/lint status, and any behavior gaps or assumptions (e.g. features deferred, routes not wired).

## Specifications
- Deliverables: exactly two new files — `src/components/<area>/<ComponentName>.tsx` and `src/components/<area>/<ComponentName>.module.scss` — plus a single commit on `<target-branch>`.
- The new component uses TypeScript with explicit prop and state types; no `any`, no `@ts-ignore`.
- All data fetching goes through existing hooks in `src/hooks/`; no raw `fetch`/`axios` inside the component.
- Styling uses the CSS Module and existing theme tokens; no inline colors that bypass the theme.
- No changes to `App.tsx`, routes, `package.json`, or shared files unless the user asks.
- Validation: `npm run build` (tsc + vite/next/etc.) exits 0 AND `npm run lint` exits 0 on the exact commit you pushed. Paste the tail of both outputs in the final message along with the commit SHA.

## Advice and Pointers
- Spend the first few minutes reading peer components that were already ported — they encode the project's conventions (file layout, import order, how hooks replace services, how SCSS is imported). Mimic them.
- Prefer replacing Angular services with existing hooks even if the hook's shape differs slightly; adapt the component, not the hook.
- Angular's change detection masks subtle bugs; in React, memoize derived values with `useMemo` and stable callbacks with `useCallback` when they're passed to children.
- Router params are strings in React Router — parse numeric IDs explicitly.
- For lists that Angular keyed with `trackBy`, use a stable `key` prop (usually the item id).
- If the Angular component relied on `ChangeDetectorRef` or zone hacks, model the equivalent state transitions explicitly with `useState`/`useReducer` instead of porting the workaround.

## Forbidden Actions
- Do NOT modify files outside `src/components/<area>/<ComponentName>.{tsx,module.scss}`.
- Do NOT wire the component into routes or `App.tsx` unless explicitly asked.
- Do NOT force-push, amend, or rebase shared branches beyond a clean `pull --rebase`.
- Do NOT add new dependencies to `package.json` to work around a missing feature — ask the user first.
- Do NOT paper over build/lint failures with `eslint-disable` or `@ts-ignore`; fix the root cause.
- Do NOT commit if either `npm run build` or `npm run lint` fails.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
