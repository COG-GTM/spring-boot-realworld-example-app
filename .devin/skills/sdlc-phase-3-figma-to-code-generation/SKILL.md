---
name: sdlc-phase-3-figma-to-code-generation
description: "Converted from Devin playbook: SDLC Phase 3: Figma to Code Generation"
triggers:
  - user
  - model
---

# SDLC Phase 3: Figma to Code Generation

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: SDLC Phase 3: Figma to Code Generation (playbook-0c81ec07b91f4ada82955993c0f92b1a), macro `!sdlc_phase3`

## Procedure

# SDLC Phase 3: Figma to Code Generation

## Overview
Reads Figma designs and the design specification from the Jira ticket (Phase 2), then generates production-ready code for each screen/component. The generated UI should match the **BBVA Mexico (https://www.bbva.mx/)** design language — dark navy/turquoise palette, card-based layouts, clean typography, and modern banking UX patterns. Creates a PR in the target repository. On completion, chains to **SDLC Phase 4** (Write Test Tickets).

## What's Needed From User (passed from Phase 2)
- Jira PRD Epic Key
- Jira Design Spec Ticket Key
- Jira Project: `COG-GTM`
- Target repository (e.g., `COG-GTM/my-app`)
- Tech stack (React, Angular, Vue, Spring Boot, etc.)
- Figma file key
- Figma component IDs and frame-to-screen mapping
- Design tokens (colors, typography, spacing)
- Atlassian Cloud ID: `e395c468-f9ea-4f8f-adae-0ea6d2eb6970`

## MCP Tools Reference

### Figma MCP (server: `figma`)
- `figma_get_file` — get file structure
- `figma_get_file_nodes` — get specific frame/component details (layout, children, styles)
- `figma_get_images` — export assets as PNG/SVG
- `figma_get_file_components` — list components for code mapping
- `figma_get_file_styles` — get style definitions
- `figma_get_image_fills` — get image asset URLs
- `figma_post_comment` — post implementation status comments on Figma

### Atlassian MCP (server: `atlassian`)
- `getJiraIssue` — read PRD Epic and design spec ticket
- `searchJiraIssuesUsingJql` — fetch implementation tickets
- `addCommentToJiraIssue` — update tickets with PR links
- `editJiraIssue` — update ticket descriptions
- `transitionJiraIssue` — move tickets to "In Progress"

### Devin MCP (tool: `devin_mcp`)
- `devin_playbook_manage` — look up Phase 4 playbook ID
- `devin_session_create` — spawn Phase 4

<phase name="Context & Codebase Analysis" id="1">
## Context & Codebase Analysis

1. Read the PRD from Jira using `getJiraIssue` (cloudId: `e395c468-f9ea-4f8f-adae-0ea6d2eb6970`, responseContentFormat: `markdown`)
2. Read the Design Spec ticket using `getJiraIssue`
3. Fetch all child Story/Task tickets under the Epic using `searchJiraIssuesUsingJql`
4. Clone the target repository and analyze:
   - Project structure (src layout, component organization)
   - Package manager and dependencies (package.json, pom.xml, etc.)
   - Existing component library and shared utilities
   - Routing setup
   - State management pattern
   - CSS/styling approach (CSS modules, styled-components, Tailwind, etc.)
   - Existing test setup
5. Create a mapping plan: which Figma frames map to which code files/components

<verification>
- PRD and design spec have been read from Jira
- Jira tickets have been fetched
- Target repo has been cloned and analyzed
- Project structure, dependencies, and patterns are understood
- A Figma-to-code mapping plan exists
</verification>
</phase>

<phase name="Design Token & Asset Extraction" id="2">
## Design Token & Asset Extraction

1. **Extract design tokens from Figma**:
   - Use `figma_get_file_styles` to get all style definitions
   - For each color style: extract hex/rgba values
   - For each text style: extract font family, size, weight, line height, letter spacing
   - For effect styles: extract shadow values (x, y, blur, spread, color)

2. **Create design token files** in the repo (using BBVA-style defaults if Figma tokens are incomplete):
   - For CSS: create a `tokens.css` or update CSS custom properties (`:root { --primary-navy: #072146; --primary-teal: #028484; --accent-teal: #04A59B; --bg-light: #F4F4F4; --white: #FFFFFF; }`)
   - For JS/TS: create a `theme.ts` or update the existing theme config
   - For Tailwind: extend `tailwind.config.js` with the Figma/BBVA color/typography values
   - Match the existing styling approach in the codebase

3. **Export image assets**:
   - Use `figma_get_images` (format: `svg` for icons, `png` for photos/illustrations, scale: 2)
   - Use `figma_get_image_fills` for background images
   - Save to the appropriate assets directory in the repo

4. **Extract component specifications** from Figma:
   - For each key component, use `figma_get_file_nodes` to get:
     - Layout details (Auto Layout = flexbox direction, padding, gap)
     - Size constraints (min/max width/height)
     - Children hierarchy (maps to JSX/HTML structure)
     - Text content (maps to labels, placeholders)
     - Interaction states (from component set variants)

<verification>
- Design tokens file has been created matching the repo's styling approach
- Image assets have been exported and saved to the repo
- Component specifications (layout, sizing, children) have been extracted from Figma
- All extracted values match the Figma design
</verification>
</phase>

<phase name="Code Generation & PR" id="3">
## Code Generation & PR

For each screen/page identified in the design spec:

1. **Generate component code**:
   - Create components matching the Figma frame hierarchy
   - Use extracted design tokens for all styling (no hardcoded values)
   - Follow existing codebase patterns (naming, file structure, imports)
   - Implement responsive layouts matching Figma breakpoints
   - Add proper TypeScript types/interfaces if the project uses TS
   - Wire up routing for new pages

2. **Implement component composition**:
   - Reuse existing shared components where possible
   - Create new shared components for Figma components used across multiple screens
   - Follow atomic design principles (atoms -> molecules -> organisms -> pages)

3. **Add data models and API stubs**:
   - Create TypeScript interfaces/types for data shapes visible in the design
   - Add placeholder API service functions based on PRD technical requirements
   - Wire up state management following existing patterns

4. **Create the PR**:
   - Create a feature branch: `devin/<timestamp>-sdlc-figma-implementation`
   - Commit with clear messages referencing Jira ticket keys
   - Create PR with description linking to the PRD Epic, design spec ticket, and Figma file
   - Run lint and type checks before pushing

5. **Update Jira tickets**:
   - Use `addCommentToJiraIssue` to add the PR URL to each implemented ticket
   - Transition tickets to "In Review" if possible using `transitionJiraIssue`

6. **Post Figma implementation comment**:
   - Use `figma_post_comment` noting which frames have been implemented and linking the PR

<verification>
- Code has been generated for all screens in the design spec
- Design tokens are used consistently (no hardcoded colors/fonts/spacing)
- Code follows existing codebase patterns and conventions
- Lint and type checks pass
- A PR has been created with proper description and Jira references
- Jira tickets have been updated with the PR link
- Figma file has been commented with implementation status
</verification>
</phase>

<phase name="Chain to Phase 4" id="4">
## Chain to Phase 4: Write Test Tickets

Spawn a child Devin session to run SDLC Phase 4 (Write Test Tickets in Jira):

1. Use `devin_playbook_manage` (action: `list`) via `devin_mcp` to find the playbook titled **"SDLC Phase 4: Write Test Tickets in Jira"** and get its `playbook_id`
2. Construct the handoff prompt:
   ```
   Continue SDLC pipeline from Phase 3.

   Context from Phases 1-3:
   - Jira PRD Epic Key: [EPIC_KEY]
   - Jira Design Spec Ticket Key: [DESIGN_SPEC_KEY]
   - Jira Project: COG-GTM
   - Target Repository: [REPO_NAME]
   - Tech Stack: [TECH_STACK]
   - PR URL: [PR_URL]
   - PR Branch: [BRANCH_NAME]
   - Figma File Key: [FIGMA_KEY]
   - Implemented Screens: [LIST_OF_SCREENS]
   - Atlassian Cloud ID: e395c468-f9ea-4f8f-adae-0ea6d2eb6970

   Please proceed with Phase 4: Create test case tickets in Jira for all implemented features.
   ```
3. Use `devin_session_create` via `devin_mcp` to spawn the child session
4. Share the child session link with the user

<verification>
- The Phase 4 playbook ID has been found
- A child session has been spawned with the correct playbook and full context
- The child session link has been shared with the user
</verification>
</phase>

## Specifications
- Generated code must use extracted design tokens, never hardcoded values
- Follow existing codebase conventions for file naming, structure, and patterns
- All components must be properly typed (if using TypeScript)
- PR description must link to PRD Epic, design spec ticket, and Figma file
- All artifacts saved as Jira tickets — no Confluence pages

## Advice and Pointers
- Use `figma_get_file_nodes` with specific IDs rather than fetching the whole file for large projects
- Auto Layout in Figma maps to CSS flexbox: `layoutMode: "HORIZONTAL"` = `flex-direction: row`
- `itemSpacing` in Figma Auto Layout maps to CSS `gap`
- `paddingLeft/Right/Top/Bottom` map directly to CSS `padding`
- Component variants in Figma (e.g., State=Hover) map to CSS pseudo-classes or conditional classes
- Reference https://www.bbva.mx/ for visual patterns: card shadows, button radius (~8px), gradient hero sections, icon-driven nav
- BBVA-style banking components: account balance cards with colored left border, transaction rows with amount/date/description, rounded CTA buttons in teal

## Forbidden Actions
- Do not hardcode design values — always use tokens
- Do not modify existing tests
- Do not push directly to main — always use a feature branch
- Do not skip lint/type checks before creating the PR
- Do not save artifacts to Confluence — everything goes into Jira tickets

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
