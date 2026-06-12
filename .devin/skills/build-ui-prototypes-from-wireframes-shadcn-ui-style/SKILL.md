---
name: build-ui-prototypes-from-wireframes-shadcn-ui-style
description: "Converted from Devin playbook: Build UI Prototypes from Wireframes (shadcn/ui Style)"
triggers:
  - user
  - model
---

# Build UI Prototypes from Wireframes (shadcn/ui Style)

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Build UI Prototypes from Wireframes (shadcn/ui Style) (playbook-d3da168c65174dc3b7c4bc283a76a99a), macro `!build_ui`

## Procedure

## Overview

Build a standalone HTML prototype page from a wireframe or screenshot, styled to match the design language of [shadcn/ui](https://ui.shadcn.com/docs/components). The output is a single self-contained HTML file with embedded CSS and JavaScript — no framework dependencies, no build step. Deliver the HTML file, a screenshot, and a recorded video demo of the page.

## What's Needed From User

- **Wireframe or screenshot** of the page to build (attached image or URL)
- **Which shadcn/ui components** to reference (e.g., Card, Input, Select, Sidebar, Dialog) — or let Devin infer from the wireframe
- **Color scheme preference** (optional): specific hex values, an existing project's design tokens, or "use your judgment"
- **Interactivity requirements** (optional): form validation, dropdowns, tag inputs, navigation behavior, etc.

## Procedure

1. **Examine the wireframe** — Identify the layout structure (sidebar, header, content area, cards, forms), the component types needed (inputs, selects, buttons, tags, tables), and any interactive behaviors visible in the design
2. **Browse shadcn/ui for reference** — Visit https://ui.shadcn.com/docs/components and study the specific components identified in the wireframe. Pay attention to:
   - CSS variable naming conventions (e.g., `--primary`, `--border`, `--muted-foreground`)
   - Border radius, spacing, and shadow patterns
   - Focus ring styles (`box-shadow: 0 0 0 2px`)
   - Typography using Inter font family
   - Color semantics (primary, secondary, muted, destructive, accent)
3. **Gather color tokens** — If the user specifies an existing project's colors, read its token files (e.g., `tokens.css`, `tokens.spec.json`, or equivalent). If no preference is given, use shadcn/ui's default neutral palette
4. **Build the HTML file** — Write a single self-contained `.html` file with all CSS in a `<style>` block and JS in a `<script>` block:
   - Define all colors and spacing as CSS custom properties in `:root`
   - Use Inter from Google Fonts as the base typeface
   - Structure the layout with semantic HTML and CSS (flexbox/grid)
   - Implement each UI component following shadcn/ui conventions: subtle borders (`1px solid var(--border)`), rounded corners (`var(--radius)`), smooth transitions, focus-visible rings
   - Add interactive behaviors: form validation highlighting, dropdown selects, tag add/remove, sidebar nav state, button hover/active states
5. **Serve and test locally** — Run `python3 -m http.server 8080` in the directory containing the HTML file and open it in the browser at `http://localhost:8080/filename.html`. Verify:
   - Layout matches the wireframe structure
   - All interactive elements work (clicks, hovers, focus states)
   - Colors and spacing look correct and consistent
6. **Take a screenshot** — Use Puppeteer to capture a clean screenshot without browser chrome: `take-screenshot http://localhost:8080/filename.html screenshot.png`
7. **Record a video demo** — Record yourself interacting with the page: navigate the sidebar, fill out form fields, trigger validation, use dropdowns, add/remove tags, and submit the form
8. **Deliver to the user** — Send the HTML file, screenshot, and video as attachments with a summary of what was built and the key interactive features

## Specifications

- Output is a **single HTML file** — no external CSS/JS files, no npm dependencies, no build tools
- All styling must use **CSS custom properties** following shadcn/ui naming: `--primary`, `--primary-foreground`, `--border`, `--input`, `--ring`, `--muted`, `--muted-foreground`, `--accent`, `--destructive`, `--radius`, etc.
- Typography: **Inter** font via Google Fonts, with weights 400/500/600/700
- Interactive elements must have proper **hover**, **focus-visible**, and **active** states
- Form validation should highlight required empty fields with a destructive border color on submit
- The page must be responsive enough to not break at common screen widths
- Deliverables: HTML file, Puppeteer screenshot, and a screen-recorded video demo showing all interactions
- Validation: open the page in the browser and visually confirm it matches the wireframe layout and components before delivering

## Advice and Pointers

- shadcn/ui's signature look comes from: subtle 1px borders, generous padding, soft rounded corners (8px default), muted label colors, and consistent focus rings. Prioritize these details.
- Use `box-shadow: 0 0 0 2px rgba(primary, 0.15)` for focus rings — this is the shadcn/ui pattern, not browser-default outlines.
- For sidebar navigation, use a simple active-state class with the primary color background and primary-foreground text.
- For multi-select/tag inputs, implement a container div with inline tags and a text input — pressing Enter adds a tag, clicking × removes it.
- Keep JavaScript minimal and vanilla — just enough for interactivity demos. No frameworks needed.
- If the user provides feedback mid-session (e.g., "change the colors"), update the CSS variables in `:root` and re-test rather than rewriting the page.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
