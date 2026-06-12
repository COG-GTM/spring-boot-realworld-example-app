---
name: migrate-angular-app-to-react-with-typescript
description: "Converted from Devin playbook: Migrate Angular App to React with TypeScript"
triggers:
  - user
  - model
---

# Migrate Angular App to React with TypeScript

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Migrate Angular App to React with TypeScript (playbook-f7ad0b1e12ec4d84987bef93ee0aa0ac), macro `!migrate_angular_to_react`

## Procedure

# Migrate Angular App to React with TypeScript

## Overview

Migrate an existing Angular application to React with TypeScript using Vite as the build tool. The new React app is scaffolded alongside the existing Angular source, preserving all functionality: routing, theming, API services, PWA support, and CI/CD. The Angular code is removed only after the React app is fully working.

## What's Needed From User

- Repository URL or name containing the Angular application
- Target React stack preferences (defaults: Vite + React + TypeScript)
- Any specific dependencies to include (e.g., UI libraries, state management)
- Whether PWA support should be preserved (default: yes if the Angular app is a PWA)
- Deployment target details (e.g., Firebase, Vercel, Netlify)

<phase name="Codebase Analysis" id="1">
## Codebase Analysis

Thoroughly explore the existing Angular codebase before writing any code.

1. Read the project README, package.json, and angular.json to understand the app's purpose, dependencies, and build configuration
2. Map the full directory structure and identify:
   - **Models/interfaces** (e.g., `src/app/shared/models/`)
   - **Services** (e.g., API services, settings/state services)
   - **Components** (shared/reusable and feature-specific)
   - **Pages/routes** (route definitions and lazy-loaded modules)
   - **Styling** (SCSS/CSS files, theming system, media queries)
   - **Pipes/directives** that need conversion to utility functions or hooks
   - **Assets** (images, icons, manifest, fonts)
   - **Tests** (unit and e2e)
   - **CI/CD config** (Travis, GitHub Actions, etc.)
   - **PWA config** (service worker, manifest.json)
3. Identify the external API endpoints the app consumes
4. Document any Angular-specific patterns that need React equivalents:
   - Angular Services -> React Context or custom hooks
   - Angular Pipes -> utility functions
   - Angular Directives -> React components or hooks
   - RxJS Observables -> fetch/async-await with AbortController for cancellation
   - NgModules/lazy loading -> React.lazy + Suspense or React Router lazy
   - Angular lifecycle hooks (ngOnInit, ngOnDestroy) -> useEffect
   - `[ngStyle]` -> inline style objects
   - `[attr.target]` -> direct JSX props
5. Note any browser API integrations that need porting (e.g., `window.matchMedia` for system theme detection, `localStorage` for persisted settings, Google Analytics scripts, accessibility features like skip links)
6. Create a migration checklist mapping every Angular file to its React equivalent

<verification>
- All Angular source files have been cataloged
- External API endpoints are identified
- Angular-to-React pattern mapping is documented
- A complete file-by-file migration checklist exists
</verification>
</phase>

<phase name="Scaffold and Port Foundation" id="2">
## Scaffold and Port Foundation

Set up the React project and port the foundational layers (types, services, styles).

1. Create a new git branch for the migration
2. Scaffold a new Vite + React + TypeScript project in a subdirectory (e.g., `react-app/`) alongside the existing Angular source:
   ```bash
   npm create vite@latest react-app -- --template react-ts
   ```
3. Install required dependencies based on the Angular app's needs:
   - `react-router-dom` (v6+) for routing
   - `sass` if the Angular app uses SCSS
   - `vite-plugin-pwa` + `workbox-precaching` if PWA support is needed
   - Any other libraries identified during analysis
4. Set up the project directory structure:
   ```
   react-app/src/
   ├── types/        # TypeScript interfaces
   ├── services/     # API and data services
   ├── hooks/        # Custom React hooks
   ├── context/      # React Context providers
   ├── components/   # Reusable UI components
   ├── pages/        # Route-level page components
   ├── styles/       # Global and theme SCSS/CSS
   ├── utils/        # Utility functions (pipe conversions, formatters)
   ├── App.tsx
   └── main.tsx
   ```
5. Copy static assets (images, icons, fonts) from the Angular project to `react-app/public/`
6. Port all TypeScript model/interface files to `src/types/` — strip Angular decorators, keep only plain TS interfaces and types
7. Port API services to `src/services/` — replace Angular's `HttpClient` and RxJS with `fetch` and async/await. Use `AbortController` where the Angular code had Observable cancellation (e.g., `lazyFetch` wrappers) to prevent memory leaks from unmounted components. Keep the same method signatures where possible
8. Convert Angular services that manage state (e.g., SettingsService) to React Context providers in `src/context/`. Use `localStorage` for persistence where the Angular app did. Preserve any `window.matchMedia` listeners for system preferred color scheme detection
9. Convert Angular Pipes to plain utility functions in `src/utils/`
10. Port SCSS/CSS theming files to `src/styles/` — copy theme variables, mixins, media query helpers, and global styles. Adapt any Angular-specific selectors (e.g., `:host`) to standard CSS

<verification>
- Vite project scaffolds and `npm install` succeeds without errors
- All TypeScript interfaces are ported and compile cleanly
- API service functions are ported and TypeScript types are correct
- Context providers are set up for any stateful services
- Utility functions are ported from pipes
- SCSS/CSS theme files are copied and adapted
</verification>
</phase>

<phase name="Build Components and Pages" id="3">
## Build Components and Pages

Port all Angular components to React functional components with hooks.

1. Port shared/reusable components first (e.g., loaders, error messages, item cards) to `src/components/`. Convert Angular templates to JSX using these mappings:
   - `*ngIf="condition"` -> `{condition && <Component />}` or ternary
   - `*ngFor="let item of items"` -> `{items.map(item => <Component key={item.id} />)}`
   - `[property]="value"` -> `property={value}`
   - `(click)="handler()"` -> `onClick={handler}`
   - `{{interpolation}}` -> `{interpolation}`
   - `[ngStyle]="{...}"` -> `style={{...}}`
   - `[innerHTML]` -> `dangerouslySetInnerHTML` (only where the Angular app used it)
2. Port layout components (header, footer, navigation, settings panel) to `src/components/`. Wire them to the Settings context for theme switching
3. Port page-level components to `src/pages/`. Each Angular route component becomes a React page component that:
   - Fetches data using the ported API service (use `useEffect` + `useState` or a custom hook)
   - Handles loading and error states
   - Renders child components
4. Set up React Router in `App.tsx`:
   - Map every Angular route to a React Router `<Route>`
   - Preserve the same URL structure
   - Use `<Navigate>` for any redirects
   - Wrap routes in a layout component if the Angular app has a shared shell (header/footer)
5. Wire up the app shell in `App.tsx` — include the Settings context provider, router, and global layout
6. Port `index.html` — copy over meta tags, analytics scripts (e.g., Google Analytics), viewport settings, theme color, and accessibility features (e.g., skip navigation links) from the Angular `src/index.html` to `react-app/index.html`

<verification>
- All Angular components have React equivalents
- Routing matches the original Angular route structure
- Theme switching works across all three themes (if applicable)
- The app shell renders correctly with header, content area, and footer
</verification>
</phase>

<phase name="PWA, Testing, CI/CD, and Cleanup" id="4">
## PWA, Testing, CI/CD, and Cleanup

Set up PWA support, testing infrastructure, CI/CD, and remove Angular code.

1. **PWA setup** (if applicable): Configure `vite-plugin-pwa` in `vite.config.ts` with the appropriate manifest, icons, and caching strategy. Port the Angular manifest.json to `public/manifest.json`
2. **Testing**: Install Vitest and React Testing Library (`@testing-library/react`, `@testing-library/jest-dom`). Write at least one test per major component category:
   - A service/utility function test
   - A component render test
   - A page-level integration test
3. **CI/CD**: Update the existing CI config (e.g., `.travis.yml`, `.github/workflows/`) to:
   - Use the Vite build command (`npm run build` from the `react-app/` directory)
   - Run the new test suite
   - Update any deployment commands to point to the new output directory
4. **Cleanup**: Remove Angular-specific files that are no longer needed:
   - `src/app/` (Angular source)
   - `angular.json`, `tsconfig.app.json`, `tsconfig.spec.json` (Angular configs)
   - `karma.conf.js`, `protractor.conf.js` (Angular test configs)
   - `e2e/` directory (Angular e2e tests)
   - Angular-specific dependencies from the root `package.json`
   - Keep any files still referenced by the React app (shared assets, configs)
5. **Update README**: Document the new React stack, how to run the dev server, build, and test
6. **Verify the build**: Run `npm run build` and `npm run lint` (if configured) from the React project directory to confirm everything compiles cleanly
7. **Run the app locally**: Start the Vite dev server (`npm run dev`), open it in the browser, and visually verify:
   - All pages render correctly
   - Navigation works
   - Theme switching works
   - Data loads from the API
8. **Take a screenshot** of the running app as proof
9. **Create a PR** with a clear description of what was migrated

<verification>
- PWA manifest and service worker are configured (if applicable)
- Tests pass with `npm test`
- CI/CD config is updated and the build succeeds
- All Angular-specific files have been removed
- README is updated with React project instructions
- The app builds without errors
- The app runs locally and all pages are functional
- A screenshot of the running app is included
- A PR has been created
</verification>
</phase>

## Specifications

- The React app must preserve all user-facing functionality from the Angular app
- All original routes must work with the same URL structure
- Theming/styling must match the original app's appearance
- The TypeScript build must compile with zero errors
- PWA capabilities must be preserved if the original app was a PWA
- The PR must include a screenshot of the running React app

## Advice and Pointers

- When `npm install` fails with dependency resolution errors, check for React version conflicts. Pin React to a compatible version or use `--legacy-peer-deps` as a last resort
- Convert Angular's RxJS patterns to simple async/await — don't introduce a reactive library unless the user requests it
- For Angular `*ngFor` with `trackBy`, use React `key` props on the mapped elements
- Angular's `[innerHTML]` binding maps to React's `dangerouslySetInnerHTML` — use it sparingly and only where the Angular app already did
- Keep SCSS file names and class names as close to the original as possible to minimize styling drift
- If the Angular app uses lazy-loaded modules, use React Router's lazy loading with `React.lazy()` and `<Suspense>`
- If the Angular app applies themes via a CSS class on the root element (e.g., `[ngClass]="settings.theme"`), replicate this in React by dynamically setting `className` on the top-level component
- Browser API calls like `window.scrollTo(0, 0)` and `window.history.back()` work the same in React — port them directly
- When porting RxJS Observable-based fetch wrappers, use `AbortController` in `useEffect` cleanup to cancel in-flight requests on component unmount

## Forbidden Actions

- Do not delete the Angular source code until the React app is fully building and all pages are verified
- Do not modify the Angular source code — the migration is a parallel rewrite
- Do not change the external API endpoints or data contracts
- Do not introduce new UI libraries (e.g., Material UI, Chakra) unless the user explicitly requests it
- Do not skip the visual verification step — always run the app and take a screenshot

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
