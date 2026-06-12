---
name: angular-to-react-component-migration-playbook
description: "Converted from Devin playbook: Angular-to-React Component Migration Playbook"
triggers:
  - user
  - model
---

# Angular-to-React Component Migration Playbook

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Angular-to-React Component Migration Playbook (playbook-0487cc8d4a544adcac11fabc07323c66)

## Procedure

# Angular-to-React Component Migration Playbook

## Required Input

**Component name**: `<ComponentName>` — the Angular component being migrated (e.g. `FooterComponent`, `HeaderComponent`, `FeedComponent`). If this has not been provided, STOP and ask the user for the exact Angular component name before proceeding.

In the rest of this playbook, `<Name>` refers to the PascalCase component name **without** the `Component` suffix (e.g. `Footer` for `FooterComponent`). `<kebab-name>` refers to the kebab-case filename prefix used in the Angular source (e.g. `footer` for `footer.component.ts`).

## Repos & Branch

- **Target (write)**: `COG-GTM/react-hn`
- **Source (read-only reference)**: `ankehao-demo/angular2-hn`
- **Branch**: `devin-develop-test-hn-react` — work on this existing branch in the target repo. `git pull` before starting. Do NOT open a PR — commit and push directly.

## Key Angular → React mapping reference

| Angular | React |
|---|---|
| `@Component` class | Functional component (`export default function <Name>(...)`) |
| `@Input() foo: T` | Prop `foo: T` on a `<Name>Props` interface |
| `@Output() bar = new EventEmitter<T>()` | Callback prop `onBar: (arg: T) => void` |
| Constructor DI (`SettingsService`) | `useSettings()` hook from `src/contexts/SettingsContext` |
| Constructor DI (`HackerNewsAPIService`) | Plain function calls from `src/services/hackerNewsApi.ts`, invoked inside `useEffect` |
| `ActivatedRoute` / route params | `useParams()` from `react-router-dom` |
| `*ngIf="x"` | `{x && <X/>}` (or ternary for else) |
| `*ngFor="let i of items"` | `{items.map((i) => <X key={...}/>)}` |
| `[property]="expr"` | `property={expr}` |
| `(event)="handler($event)"` | `onEvent={(e) => handler(e)}` |
| `{{ interpolation }}` | `{expression}` |
| `[innerHTML]="html"` | `dangerouslySetInnerHTML={{ __html: html }}` |
| `[routerLink]="['/x', id]"` | `<Link to={` + "`/x/${id}`" + `}>...</Link>` from `react-router-dom` |
| `routerLinkActive="active"` | `<NavLink className={({isActive}) => isActive ? 'active' : ''}>` |
| `{{ x \| somePipe }}` | Utility function: `{someUtil(x)}` |
| `class=` | `className=` |
| Self-closing tags (`<br>`, `<img>`) | Must be self-closed in JSX (`<br />`, `<img />`) |

## Step-by-step checklist

1. **Confirm the component name input.** If the user did not provide `<ComponentName>`, stop and ask. Derive `<Name>` (PascalCase without `Component`) and `<kebab-name>` (kebab-case filename prefix).

2. **Identify the Angular source files** in `ankehao-demo/angular2-hn` under the relevant module directory. Check these common locations in order:
   - `src/app/core/<kebab-name>/<kebab-name>.component.{ts,html,scss}`
   - `src/app/feeds/<kebab-name>/<kebab-name>.component.{ts,html,scss}`
   - `src/app/shared/components/<kebab-name>/<kebab-name>.component.{ts,html,scss}`
   - `src/app/item-details/<kebab-name>/<kebab-name>.component.{ts,html,scss}`
   - `src/app/user/<kebab-name>/<kebab-name>.component.{ts,html,scss}`

   Read all three files (`.ts`, `.html`, `.scss`). Note any `@Input()`, `@Output()`, injected services, `ngOnInit`/lifecycle hooks, and template directives.

3. **Pull the target repo.** In `COG-GTM/react-hn`, run `git checkout devin-develop-test-hn-react && git pull` to pick up any changes from previous batches.

4. **Create the React component directory** at `src/components/<Name>/` with two files:
   - `src/components/<Name>/<Name>.tsx`
   - `src/components/<Name>/<Name>.module.scss`

5. **Define the `<Name>Props` interface.** Convert each `@Input()` property to a typed prop. Convert each `@Output() EventEmitter<T>` to a callback prop typed as `(arg: T) => void` (naming convention: `onXxx`). If the component has no inputs/outputs, omit the props interface. Destructure props in the function signature.

6. **Replace Angular DI with React hooks** inside the function body:
   - Settings: `const { settings, updateSettings } = useSettings();` from `src/contexts/SettingsContext`
   - API calls: import plain functions from `src/services/hackerNewsApi.ts` and invoke them inside `useEffect`, storing results in `useState`
   - Route params: `const { id } = useParams<{ id: string }>();` from `react-router-dom`
   - Location/navigation: `const navigate = useNavigate();` from `react-router-dom`

7. **Convert the Angular template (HTML) to JSX** using the mapping reference table above. Remember:
   - `class=` → `className=`
   - Self-close void elements (`<br />`, `<img />`, `<input />`)
   - `for` attribute → `htmlFor`
   - `tabindex` → `tabIndex`, `readonly` → `readOnly`, etc.
   - Preserve all visible text, links (`href`, `target`, `rel`), and accessibility attributes verbatim.

8. **Convert the SCSS file** to a SCSS module at `<Name>.module.scss`:
   - Copy selectors and rules from the Angular `.scss` file.
   - Replace Angular imports of shared partials (e.g. `@import "../../shared/scss/media"`) with either inline media-query strings (e.g. `@media only screen and (max-width: 768px) { ... }`) or an import from `src/styles/` if a matching partial exists there.
   - Convert id-based selectors (`#footer`) to class-based selectors (`.footer`) so they can be referenced via CSS modules.
   - Import in the component with `import styles from './<Name>.module.scss'` and reference classes via `className={styles.foo}`.

9. **Export** the component as the default export: `export default function <Name>(props: <Name>Props) { ... }`.

10. **Build & lint** in the target repo root:
    - `npm run build` — must succeed (no TypeScript errors, no Vite build failures).
    - `npm run lint` — must not introduce any **new** errors beyond the pre-existing `src/hooks/use-toast.ts` warning from Batch 0. Pre-existing warnings are acceptable.

11. **Commit & push** to `devin-develop-test-hn-react`:
    - Stage only the new `src/components/<Name>/` files (do not `git add .`).
    - Commit message: `feat: migrate <Name>Component to React`.
    - Push to the same branch. Do **not** open a PR.

12. **Report** a concise summary including:
    - The file paths you created.
    - Whether `npm run build` and `npm run lint` succeeded.
    - The commit SHA that was pushed.

## Scope reminders

- Do **not** modify unrelated files. Scope changes tightly to the single component being migrated.
- Do **not** open a PR — just commit and push to `devin-develop-test-hn-react`.
- Do **not** add new dependencies unless the Angular component genuinely requires one that isn't already in `react-hn`'s `package.json`.
- This is one of a sequence of batches — other Angular components will be migrated by later sessions using this same playbook.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
