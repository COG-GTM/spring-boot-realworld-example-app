---
name: scaffold-react-typescript-project-vite
description: "Converted from Devin playbook: Scaffold React + TypeScript Project (Vite)"
triggers:
  - user
  - model
---

# Scaffold React + TypeScript Project (Vite)

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Scaffold React + TypeScript Project (Vite) (playbook-9d1f76a1068d4cd291893bfbaa2c52a0)

## Procedure

# Scaffold a React + TypeScript Project Using Vite

This playbook scaffolds a new React + TypeScript project using Vite inside a target repository. It creates a clean starting point for migration or greenfield development.

## Prerequisites
- A target repository cloned locally
- Node.js installed

## Steps

### 1. Create the Vite project
From the repo root, run:
```bash
npm create vite@latest react-app -- --template react-ts
```
When prompted to install dependencies and start the dev server, decline — we'll install manually after adding extra dependencies.

### 2. Install core dependencies
```bash
cd react-app
npm install react-router-dom sass
```

### 3. Install DOMPurify for HTML sanitization (optional)
If the project will render user-generated HTML content:
```bash
npm install dompurify
npm install -D @types/dompurify
```

### 4. Add prettier config
Copy the prettier config from the root `package.json` (if it exists) into `react-app/package.json`. Example:
```json
"prettier": {
  "trailingComma": "es5",
  "tabWidth": 4,
  "singleQuote": true,
  "printWidth": 120
}
```

### 5. Verify the scaffold builds and runs
```bash
cd react-app
npm run build
npm run dev
```
Confirm the Vite dev server starts (default port 5173) and the page loads in the browser.

### 6. Clean out Vite boilerplate
Remove default Vite/React content from `react-app/src/`:
- Delete `App.css`, `index.css`
- Delete logo/image files from `src/assets/` (e.g., `react.svg`, `vite.svg`, `hero.png`)
- Delete unnecessary public assets (e.g., `public/icons.svg`, `public/favicon.svg`)
- Replace `App.tsx` with a blank component:
  ```tsx
  function App() {
      return <></>;
  }
  export default App;
  ```
- Update `main.tsx` to remove CSS imports:
  ```tsx
  import { StrictMode } from 'react';
  import { createRoot } from 'react-dom/client';
  import App from './App.tsx';

  createRoot(document.getElementById('root')!).render(
      <StrictMode>
          <App />
      </StrictMode>
  );
  ```

### 7. Create the directory structure
Create the following directories inside `react-app/src/` with `.gitkeep` files so they are tracked by git:
```bash
mkdir -p src/api src/components src/context src/models src/styles src/utils
touch src/api/.gitkeep src/components/.gitkeep src/context/.gitkeep src/models/.gitkeep src/styles/.gitkeep src/utils/.gitkeep
```

Directory purposes:
- `api/` — API client functions
- `components/` — React components
- `context/` — React context providers
- `models/` — TypeScript interfaces/types
- `styles/` — Global SCSS and theme files
- `utils/` — Helper/utility functions

### 8. Commit and create PR
Commit all changes and create a PR. Verify build passes before submitting:
```bash
cd react-app && npm run build
```

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
