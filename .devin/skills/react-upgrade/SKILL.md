---
name: react-upgrade
description: "Converted from Devin playbook: React Upgrade"
triggers:
  - user
  - model
---

# React Upgrade

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: React Upgrade (playbook-b3246f5f61d44ff1b226747df1890934)

## Procedure

## Overview

This playbook outlines a general approach for upgrading React applications across major versions. It follows best practices to ensure compatibility, minimize regressions, and maintain application stability. The process assumes incremental upgrades when necessary and emphasizes safe iteration, testing, and validation.

## Upgrade Procedure

### 1. Audit Current Version and Dependencies

* Inspect `package.json` to determine the current version of `react` and `react-dom`.
* Run:

  ```bash
  npm list react react-dom
  ```
* Identify any peer dependencies (e.g., `react-router`, `redux`, `enzyme`, etc.) that may require updates.

### 2. Create Upgrade Branch

* Create a clean branch to isolate upgrade changes:

  ```bash
  git checkout -b upgrade/react-version
  ```

### 3. Plan Version Path

* Check if a direct upgrade is supported. If not, plan incremental upgrades (e.g., 16 → 17 → 18).
* Use official guides for intermediate releases:

  * [React 17 Upgrade Guide](https://reactjs.org/blog/2020/10/20/react-v17.html)
  * [React 18 Upgrade Guide](https://reactjs.org/blog/2022/03/29/react-v18.html)

### 4. Upgrade React and React-DOM

Update core dependencies to the target version:

```bash
npm install react@latest react-dom@latest
```

* For yarn:

```bash
yarn add react@latest react-dom@latest
```

### 5. Update Ecosystem Packages

* Upgrade libraries that depend on React internals or major APIs:

  * `react-router`, `redux`, `formik`, `react-scripts`, etc.
* Use tools like:

  ```bash
  npm outdated
  npx npm-check-updates
  ```

### 6. Update Testing Tools

* Upgrade `@testing-library/react`, `enzyme`, and other testing frameworks to ensure compatibility.
* Check for breaking changes:

  * [React Testing Library Docs](https://testing-library.com/docs/react-testing-library/intro/)
  * [Enzyme Compatibility Notes](https://enzymejs.github.io/enzyme/)

### 7. Resolve Breaking Changes

* Refer to the official changelogs and migration guides.
* Common changes:

  * Removal of legacy lifecycle methods
  * New root API in React 18:

    ```tsx
    import { createRoot } from 'react-dom/client';
    const root = createRoot(document.getElementById('root'));
    root.render(<App />);
    ```

### 8. Run Full Test Suite

* Run all tests:

  ```bash
  npm test
  ```
* Fix failures related to deprecated APIs or changed behaviors.

### 9. Perform Manual Testing

* Validate core flows in dev/staging:

  * Routing
  * State updates
  * Component mounting/unmounting
  * Third-party integrations (auth, analytics, etc.)

### 10. Check Runtime Warnings

* Open the app in development mode and observe console warnings/errors.
* Address any use of deprecated APIs or compatibility issues.

### 11. Clean Up

* Remove legacy code paths or polyfills no longer needed.
* Ensure no remaining usage of deprecated features (e.g., `ReactDOM.render`, unsafe lifecycle methods).

### 12. Run Security Audit

* Run:

  ```bash
  npm audit fix
  ```
* Manually fix critical vulnerabilities if needed.

### 13. Push and Open PR

* Push the branch and open a pull request.
* Title:

  ```
  Upgrade React from X to Y
  ```
* Description:

  * Version path taken
  * Major changes or incompatibilities resolved
  * Test coverage and results

## Best Practices

* Upgrade one major version at a time for clarity and rollback.
* Use `react-codemod` utilities for automating large changes:

  ```bash
  npx react-codemod <transform>
  ```
* Use a bundler analyzer (e.g., `webpack-bundle-analyzer`) to detect regressions in bundle size.
* Confirm CI pipeline runs successfully on the new version.

## Forbidden Actions

* Do not upgrade core libraries without checking compatibility of peers.
* Avoid large refactors during the upgrade—keep changes scoped.
* Do not skip manual testing or release validation.
* Never ignore console warnings in development—they often signal serious issues in production.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
