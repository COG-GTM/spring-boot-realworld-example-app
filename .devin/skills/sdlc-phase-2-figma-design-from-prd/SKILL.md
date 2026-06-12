---
name: sdlc-phase-2-figma-design-from-prd
description: "Converted from Devin playbook: SDLC Phase 2: Figma Design from PRD"
triggers:
  - user
  - model
---

# SDLC Phase 2: Figma Design from PRD

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: SDLC Phase 2: Figma Design from PRD (playbook-46a50d5310f440689b3f047f020d49e6), macro `!sdlc_phase2`

## Procedure

# SDLC Phase 2: Figma Design from PRD

## Overview
Reads the PRD from the Jira Epic (created in Phase 1) and analyzes the Figma design file to extract the design system (components, styles, colors, typography). The design should follow the visual language of **BBVA Mexico (https://www.bbva.mx/)** — clean, modern banking UI with their signature dark navy/turquoise palette, card-based layouts, generous whitespace, and professional typography. Produces a detailed design specification saved as a Jira ticket, and posts design review comments on the Figma file. On completion, chains to **SDLC Phase 3** (Code Generation from Figma).

## Design Reference: BBVA Mexico (https://www.bbva.mx/)
Use BBVA.mx as the primary design inspiration for all banking UI work:
- **Color palette**: Dark navy (#072146), BBVA turquoise/teal (#028484, #04A59B), white backgrounds, light grays (#F4F4F4)
- **Typography**: Clean sans-serif (BentonSans or similar), clear hierarchy with bold headings
- **Layout**: Card-based dashboard widgets, generous padding, responsive grid
- **Components**: Rounded buttons, subtle shadows on cards, icon-driven navigation, hero banners with gradients
- **Patterns**: Account summary cards, transaction lists with date grouping, quick action tiles, product carousels
- If no Figma file exists yet, create design specs that describe screens in the BBVA style

## What's Needed From User (passed from Phase 1)
- Jira PRD Epic Key (contains the PRD in its description)
- Jira Project: `COG-GTM`
- Target repository (e.g., `COG-GTM/my-app`)
- Tech stack (React, Angular, etc.)
- Figma file key (from the Figma file URL: `figma.com/file/<FILE_KEY>/...`)
- Atlassian Cloud ID: `e395c468-f9ea-4f8f-adae-0ea6d2eb6970`

## MCP Tools Reference

### Figma MCP (server: `figma`)
- `figma_get_file` — get the full Figma file structure (use `depth` to control traversal)
- `figma_get_file_nodes` — get specific nodes by ID
- `figma_get_images` — render nodes as PNG/SVG images for reference
- `figma_get_file_components` — list all components in the file
- `figma_get_file_component_sets` — list component sets (variants)
- `figma_get_file_styles` — get all styles (colors, text, effects)
- `figma_get_comments` — read existing design comments
- `figma_post_comment` — post design review/specification comments
- `figma_get_image_fills` — get image fill URLs

### Atlassian MCP (server: `atlassian`)
- `getJiraIssue` — read the PRD Epic description
- `searchJiraIssuesUsingJql` — fetch child tickets from Phase 1
- `createJiraIssue` — create the design spec ticket
- `addCommentToJiraIssue` — update tickets with design references
- `editJiraIssue` — update ticket descriptions with design details

### Devin MCP (tool: `devin_mcp`)
- `devin_playbook_manage` — look up Phase 3 playbook ID
- `devin_session_create` — spawn Phase 3

<phase name="Context Loading" id="1">
## Context Loading

1. Read the PRD from Jira using `getJiraIssue` (server: `atlassian`, cloudId: `e395c468-f9ea-4f8f-adae-0ea6d2eb6970`, responseContentFormat: `markdown`)
2. Extract the UI/UX Requirements section (Section 8) — this drives all design work
3. Fetch all child tickets under the Epic using `searchJiraIssuesUsingJql`:
   - Query: `project = "COG-GTM" AND parent = <EPIC_KEY> ORDER BY created ASC`
   - Specifically look for tickets labeled `design`
4. Build a requirements matrix: each screen/component mapped to its functional requirements and acceptance criteria

<verification>
- PRD has been read from the Jira Epic and the UI/UX requirements section extracted
- All child Jira tickets under the Epic have been fetched
- A requirements matrix mapping screens to requirements exists
</verification>
</phase>

<phase name="Figma Design System Analysis" id="2">
## Figma Design System Analysis

Analyze the existing Figma file to understand the design system:

1. **Get file structure** — Use `figma_get_file` (server: `figma`, fileKey: `<FIGMA_FILE_KEY>`, depth: 2) to get the page and frame hierarchy
2. **Extract components** — Use `figma_get_file_components` to list all reusable components (buttons, inputs, cards, nav items, etc.)
3. **Extract component sets** — Use `figma_get_file_component_sets` to get component variants (primary/secondary buttons, active/inactive states, etc.)
4. **Extract styles** — Use `figma_get_file_styles` to get:
   - Color styles (primary, secondary, background, text colors)
   - Text styles (headings, body, captions — font family, size, weight, line height)
   - Effect styles (shadows, blurs)
5. **Get existing comments** — Use `figma_get_comments` (as_md: true) to read any designer notes
6. **Render key frames as images** — Use `figma_get_images` (format: `png`, scale: 2) to render existing screens for reference
7. Document the complete design system inventory:
   - Component library (name, description, variants)
   - Color palette with hex values
   - Typography scale
   - Spacing and layout patterns
   - Icon set

<verification>
- The Figma file structure has been mapped (pages, frames, layers)
- All components and component sets have been cataloged
- All styles (color, text, effect) have been extracted
- Key frames have been rendered as reference images
- A complete design system inventory document exists
</verification>
</phase>

<phase name="Design Specification" id="3">
## Design Specification

Create a detailed design specification and save it as a Jira ticket:

1. **Create the Design Spec ticket** using `createJiraIssue`:
   - `cloudId`: `e395c468-f9ea-4f8f-adae-0ea6d2eb6970`
   - `projectKey`: `COG-GTM`
   - `issueTypeName`: `Task`
   - `parent`: the PRD Epic key
   - `summary`: "Design Specification: [Project Name] in [REPO_NAME]"
   - `contentFormat`: `markdown`
   - `additional_fields`: `{"labels": ["sdlc-automated", "design-spec"]}`
   - `description`: Full design spec in markdown covering:

2. For each screen/page identified in the PRD UI/UX requirements:
   - Map to existing Figma frames (by name/ID match)
   - List the components used (from the component inventory)
   - Document the layout structure (flexbox/grid patterns, spacing)
   - Note responsive behavior if visible in Figma
   - Extract specific design tokens: colors, typography, spacing values
   - Capture interaction patterns (hover states, transitions from component variants)

3. For screens in the PRD but NOT in Figma:
   - Document them as "design gaps" in the ticket description
   - Post a comment on the Figma file using `figma_post_comment` flagging the gap

4. **Update Jira design tickets** — For each design task ticket:
   - Use `addCommentToJiraIssue` to add Figma node IDs and component references
   - Use `editJiraIssue` to add design spec references to the description

5. **Post summary comment on Figma** — Use `figma_post_comment` with an overview of which screens map to which PRD requirements

<verification>
- A Design Spec Jira ticket has been created under the PRD Epic
- Every PRD screen has been mapped to Figma frames (or flagged as a design gap)
- Design tokens (colors, typography, spacing) have been extracted for each screen
- Jira design tickets have been updated with Figma references
- Design gaps have been flagged via Figma comments
</verification>
</phase>

<phase name="Chain to Phase 3" id="4">
## Chain to Phase 3: Code Generation from Figma

Spawn a child Devin session to run SDLC Phase 3 (Figma to Code):

1. Use `devin_playbook_manage` (action: `list`) via `devin_mcp` to find the playbook titled **"SDLC Phase 3: Figma to Code Generation"** and get its `playbook_id`
2. Construct the handoff prompt:
   ```
   Continue SDLC pipeline from Phase 2.

   Context from Phase 1 & 2:
   - Jira PRD Epic Key: [EPIC_KEY]
   - Jira Design Spec Ticket Key: [DESIGN_SPEC_KEY]
   - Jira Project: COG-GTM
   - Target Repository: [REPO_NAME]
   - Tech Stack: [TECH_STACK]
   - Figma File Key: [FIGMA_KEY]
   - Figma Component IDs: [KEY_COMPONENT_IDS]
   - Figma Frame IDs per screen: [FRAME_MAPPING]
   - Design Tokens: [COLOR_PALETTE, TYPOGRAPHY, SPACING summary]
   - Atlassian Cloud ID: e395c468-f9ea-4f8f-adae-0ea6d2eb6970

   Please proceed with Phase 3: Generate code from the Figma designs.
   ```
3. Use `devin_session_create` via `devin_mcp` to spawn the child session with the playbook_id and prompt
4. Share the child session link with the user

<verification>
- The Phase 3 playbook ID has been found
- A child session has been spawned with the correct playbook and full context
- The child session link has been shared with the user
</verification>
</phase>

## Specifications
- Always extract design tokens as concrete values (hex colors, px sizes) not just references
- Render images at 2x scale for clarity
- Post Figma comments referencing PRD section numbers for traceability
- All artifacts saved as Jira tickets — no Confluence pages

## Advice and Pointers
- Start with `figma_get_file` at depth 2 to understand the page structure, then drill into specific frames
- Component names in Figma often match CSS class names — note these for code generation
- Look for Auto Layout properties in frames — they map directly to flexbox/grid in CSS
- Reference https://www.bbva.mx/ for design decisions when the Figma file is incomplete or absent
- BBVA's key screens to emulate: account overview dashboard, transaction history, transfer flow, product cards, mobile-responsive navigation

## Forbidden Actions
- Do not modify or delete Figma designs — this playbook is read-only on designs
- Do not delete existing Figma comments
- Do not skip the design gap analysis
- Do not save artifacts to Confluence — everything goes into Jira tickets

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
