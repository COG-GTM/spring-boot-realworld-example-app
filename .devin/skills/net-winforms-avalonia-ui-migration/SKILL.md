---
name: net-winforms-avalonia-ui-migration
description: "Converted from Devin playbook: .NET WinForms → Avalonia UI Migration"
triggers:
  - user
  - model
---

# .NET WinForms → Avalonia UI Migration

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: .NET WinForms → Avalonia UI Migration (playbook-a33665e3ecf344fb90e4f7dcb980f199), macro `!winforms_migration`

## Procedure

# .NET WinForms → Avalonia UI Migration Guide


## Goal
When migrating a WinForms app to Avalonia UI, the result *must* look visually distinct and clearly better than the original — while still looking like real enterprise software, not a designer mockup. After building, test both the old and new apps side-by-side on the desktop to confirm the migration is successful.


## Architecture
- Target .NET 8 + Avalonia UI 11.2
- Use MVVM pattern (separate ViewModels, Views, Models)
- Preserve all existing functionality and data from the source app
- Use the same deterministic random seed / data source so both apps show identical records — this is how you verify the migration worked


## UI Design — Enterprise-Grade, Not a Mockup


The migrated UI must look like software used at a Fortune 500 company (think NetSuite, SAP Fiori, Workday). It should be clearly modern and better than the WinForms original, but still professional. Add data visualization and charts to make the UI looks clearly distinct.


### Palette & Surfaces
- Light mode only (dark mode reads as consumer, not enterprise)
- Cool light grey canvas (`#F4F6F8`), white cards/panels with subtle `1px #E5E7EB` borders
- Corner radius: `2px` maximum everywhere — basically square. Rounded corners look consumer-grade
- Single corporate blue accent (`#2563EB`) for active states and primary actions
- Color-coded status pills (green=Paid, red=Past Due, amber=Pending, blue=Approved)


### Typography
- Segoe UI throughout
- Bold KPI numbers with tight letter-spacing (-0.4px) for confident data callouts
- ALL-CAPS eyebrow labels and column headers with wide letter-spacing (+0.6px) — this is the signature NetSuite/Workday/SAP look
- No decorative or display fonts


### Charts & Sparklines
- Straight-line polylines only — never smooth Bézier/Catmull-Rom curves (smooth curves look fake and decorative)
- No gradient fills under chart lines (gradients look like a Dribbble mockup, not Bloomberg)
- Real Y-axis with labeled ticks, horizontal gridlines, dot markers on data points
- Add reference lines (e.g., period averages) with labels — these make charts read as analytical, not decorative
- All sparklines on a page must tell a coherent data story — they should be logically derived from the same underlying dataset, not random


### Content Must Be Meaningful
- Every number, badge, and count on the page must represent real data — not filler
- Only show count badges on nav items that represent actionable work queues (e.g., "Approvals 18")
- Do not add decorative counts (e.g., "Invoices 1.2k" in a sidebar is meaningless)
- Table footers ("Showing X of Y") must match actual visible row counts
- Don't duplicate the same status info in multiple places


### Layout
- Left sidebar navigation with brand badge, grouped nav sections, and user badge at bottom
- Dashboard with KPI tiles (include sparklines), main chart, and recent activity table
- Tables: dense rows, grey header ribbon, sort indicators, pagination footer
- Dialogs: compact (tight padding ~18px, small spacing ~12px), use real compliance language


### Responsive
- The app must work at both full-screen width AND half-screen width (for side-by-side testing)
- At half-screen: collapse to 2-column KPI tiles, stack chart/activity vertically, narrow sidebar, hide non-essential buttons, reduce table columns
- No text overflow or element overlap at any width


## Side-by-Side Testing Procedure


After building both apps, you MUST test them side-by-side on the desktop:


1. Build both the original WinForms app and the new Avalonia app
2. Launch both apps
3. Tile them side-by-side (each taking exactly half the screen)
4. Verify both apps fit cleanly at half-screen width with no overflow or overlap
5. Click through the same workflows in both apps to confirm feature parity:
   - Navigate to the same vendor/entity in both → confirm identical data
   - Perform the same actions (e.g., approve an invoice) in both → confirm identical behavior
   - Check that all pages/views from the original app exist in the new app
6. Show the user the side-by-side result on the Desktop tab so they can interact with both apps
7. Keep both apps running so the user can click back and forth between them to compare


### Key Testing Notes
- Both apps must use the same data seed — clicking the same entity in both apps should show the same records
- The WinForms app's MinimumSize may need to be reduced to fit at half-screen (640px)
- If the Avalonia app has layout issues at half-screen, fix responsive breakpoints before showing the user
- Always offer the Desktop tab so the user can interact directly

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
