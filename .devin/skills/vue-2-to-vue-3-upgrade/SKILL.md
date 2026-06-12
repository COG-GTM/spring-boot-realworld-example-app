---
name: vue-2-to-vue-3-upgrade
description: "Converted from Devin playbook: Vue 2 to Vue 3 Upgrade"
triggers:
  - user
  - model
---

# Vue 2 to Vue 3 Upgrade

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Vue 2 to Vue 3 Upgrade (playbook-cae4861e2c674144bb7a0b88c0fb916f)

## Procedure

## Overview

This playbook outlines a structured and phased approach to upgrading a Vue.js application from version 2 to version 3. The goal is to preserve stability, ensure compatibility, and simplify the migration of critical features, libraries, and build tooling.

## Upgrade Process

### 1. Clone the repository and create an upgrade branch

```bash
git clone https://your.repo.url
cd your-project
git checkout -b upgrade/vue3
```

### 2. Assess current state

* Identify the current Vue version in `package.json`.
* Document usage of Vue 2-specific APIs (e.g., `filters`, `sync`, `event bus`, `$on/$off`).
* Review external dependencies that rely on Vue 2 (e.g., Vuetify 2, Vue Router 3).

### 3. Install the Vue 2 compatibility build

To ease the transition:

```bash
npm install vue@3 vue-compat@latest
```

Then enable compat mode in `main.js`:

```js
import { createApp, configureCompat } from 'vue'
configureCompat({ MODE: 3 })
```

> This allows you to run Vue 3 while temporarily using Vue 2 APIs.

### 4. Incrementally update incompatible features

Systematically refactor your codebase by priority:

* Replace `$on/$off/$once` with `emits` and `v-model`.
* Remove deprecated options (e.g., `filters`, `inline-template`).
* Replace `this.$listeners` and `this.$attrs` with `defineEmits()` and `defineProps()`.
* Convert `Vue.extend()` to composition API or defineComponent.

Use the [Vue 2 to 3 migration guide](https://v3-migration.vuejs.org/) to identify and fix each breaking change.

### 5. Upgrade core packages and tooling

```bash
npm install vue@3 vue-router@4 vuex@4
```

Other common packages:

* `vue-test-utils`: replace with `@vue/test-utils@next`
* `vue-loader`: upgrade to ^17+
* `eslint-plugin-vue`: upgrade and align with Vue 3 rules

### 6. Adjust build tooling and configuration

* Update `webpack.config.js` or `vite.config.js` for Vue 3.
* Replace `vue-template-compiler` with `@vue/compiler-sfc`.
* Ensure `babel.config.js` is compatible.

Example:

```bash
npm uninstall vue-template-compiler
npm install @vue/compiler-sfc --save-dev
```

### 7. Migrate to the Composition API (optional but recommended)

Start by migrating new components using Composition API via:

```js
import { ref, computed, watch, onMounted } from 'vue'
```

Use the [Vue Composition API migration guide](https://vuejs.org/guide/extras/composition-api-faq.html).

### 8. Update test infrastructure

* Update test libraries for Vue 3 compatibility.
* Ensure mocks and component stubs follow Vue 3 conventions.
* Run all tests and fix breakages.

### 9. Run full test suite and fix issues

```bash
npm run test
```

* Fix test failures, UI regressions, and behavior inconsistencies.
* Perform visual and E2E checks if using Cypress or Playwright.

### 10. Audit for vulnerabilities

```bash
npm audit fix
npm audit
```

* Resolve all known critical/high vulnerabilities.
* Ensure no security regressions in the upgrade.

### 11. Finalize upgrade and remove compat build

Once stable and tested:

```bash
// In main.js
remove configureCompat() and any deprecated API usage
```

Switch to the official Vue 3 build:

```bash
npm uninstall vue-compat
```

### 12. Push changes and open PR

```bash
git add -A
git commit -m "Upgrade Vue 2 app to Vue 3"
git push origin upgrade/vue3
```

Open a PR summarizing:

* Version path and key changes
* Any breaking changes addressed
* Compatibility build use/removal
* Test suite and audit results

## Specifications

* Upgrade is incremental and well-documented.
* All tests must pass before merging.
* No deprecated APIs should remain by the end.
* Application must compile, build, and run successfully on Vue 3.

## Best Practices

* Use the official [Vue Migration Build](https://v3-migration.vuejs.org/migration-build.html) for smoother transitions.
* Avoid upgrading large apps in one go — tackle one feature/module at a time.
* Use `defineComponent()` and script setup to future-proof components.
* Annotate all major changes clearly in the codebase.

## Forbidden Actions

🚫 Do not skip the test suite or audit step.
🚫 Do not force upgrade third-party libraries unless a compatible version exists.
🚫 Do not mix Composition and Options APIs in a confusing way — be consistent.
🚫 Do not commit deprecated API usage post-upgrade.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
