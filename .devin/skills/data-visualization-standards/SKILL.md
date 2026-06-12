---
name: data-visualization-standards
description: "Converted from Devin playbook: Data Visualization Standards"
triggers:
  - user
  - model
---

# Data Visualization Standards

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Data Visualization Standards (playbook-5d410d8841f04afd920f99e46cf458bb), macro `!mcp_data_visualization`

## Procedure

# Playbook: Data Visualization Standards

## Overview

This playbook defines the standard approach for creating data visualizations in Devin sessions. By default, use an interactive HTML page with Apache ECharts as the visualization surface. If the user explicitly requests a different format (e.g., "PNG only" or "no HTML"), follow the user's instructions instead of the default HTML+ECharts path.

## What's Needed From User

- Data source or query requirements (e.g., database, API, CSV file)
- Analysis objectives and metrics to visualize
- Output format constraints, if any (e.g., "PNG only", "no HTML")
- Any specific chart types or layout preferences (optional)

## Procedure

1. Check the user's requested output format:
   - If the user explicitly requests a different format (e.g., "PNG only", "no HTML"), follow their instructions and apply only the relevant parts of this playbook (e.g., color scheme, screenshot cleanliness)
   - Otherwise, proceed with the default HTML + ECharts approach below
2. Query or prepare the data from the specified source
3. Create a single `index.html` file with:
   - Apache ECharts loaded via CDN (default charting library)
   - Query results hardcoded as a JavaScript data structure
   - Dark theme styling per the color scheme specifications below
   - No build tools or frameworks required
4. Start a local HTTP server in the HTML directory:
   ```bash
   cd /path/to/html/directory && python3 -m http.server 8080 &
   ```
5. Take a screenshot using Puppeteer (captures clean output without browser chrome):
   ```bash
   take-screenshot http://localhost:8080/index.html screenshot.png
   ```
   - **Fallback if Puppeteer fails:** Fullscreen the browser with F11, move the mouse out of the viewport, capture the screenshot, and crop to include only the webpage body
6. Load the page in the Devin browser using the computer tool to navigate to `http://localhost:8080/index.html`
   - This is separate from the Puppeteer screenshot: Puppeteer runs headless and does not make the page visible to the user
   - Loading in the Devin browser lets the user hover, zoom, and interact with the visualization
   - Wait for the page to fully load
   - Leave this tab active when blocking on the user
7. Prepare a CSV export of the underlying data
8. Send final message to user with:
   - Key findings summary
   - Data table in markdown format
   - SQL query or data preparation code used
   - Attachments: screenshot, CSV, and HTML file
   - Note: "The interactive visualization is loaded in the Devin browser - you can hover, zoom, and explore it directly there."

## Specifications

### Output Requirements
- Screenshot must show only webpage content (no browser chrome, scrollbars, or cursor)
- HTML file must be self-contained and portable
- CSV must contain the full dataset used in visualization
- Final message must include the SQL/code used for validation

### Color Scheme (Dark Theme)

**Backgrounds:**
- Page background: `#0F172A` (dark navy)
- Card/component background: `#1E293B`
- Table rows alternating: `#1E293B` and `#243449`

**Text:**
- Primary: `#FFFFFF`
- Secondary: `#CBD5E1`
- Muted labels: `#94A3B8`

**Chart Accent Colors (use in order):**
| Purpose | Color | Notes |
|---------|-------|-------|
| Primary accent | `#22D3EE` | Teal cyan |
| Secondary | `#60A5FA` | Blue |
| Tertiary | `#818CF8` | Indigo |
| Quaternary | `#A78BFA` | Purple |
| Negative/critical | `#F87171` | Red |
| Positive/success | `#4ADE80` | Green |

**Semantic Color Assignments:**
- Funnel steps: View Item `#22D3EE` → Add to Cart `#60A5FA` → Begin Checkout `#818CF8` → Purchase `#4ADE80`
- Device categories: Desktop `#22D3EE`, Mobile `#60A5FA`, Tablet `#818CF8`
- Financial metrics: Revenue `#22D3EE`, Positive change `#4ADE80`, Negative change `#F87171`

### Chart Styling
- Transparent chart backgrounds (cards show through)
- Gridlines: `rgba(255,255,255,0.05)`
- Axis labels: `#CBD5E1`
- Line thickness: 2.5px, solid only
- Bar corners: slightly rounded
- Tooltips: card background with white text

### Layout
- Font: Inter, Roboto, or system sans-serif
- Outer padding: 24px
- Internal padding: 16px
- Border radius: 12px on cards

## Advice and Pointers

- Always use localhost URLs (not file://) to ensure JavaScript and CDN resources load correctly
- Puppeteer runs in a separate headless Chrome instance - it does NOT load the page in the Devin browser that users see, so you must also open the URL in the Devin browser
- For charts with more than four categories, cycle through the accent palette but keep related categories grouped by hue
- Maintain high contrast for accessibility - avoid color-only distinctions for critical information

## Forbidden Actions

- Do not generate standalone static images using matplotlib, seaborn, or similar Python libraries unless the user explicitly requests that format
- Do not create both a static image and a webpage - the HTML page plus its screenshots is the single visualization path
- Do not include browser chrome, scrollbars, or mouse cursor in screenshots
- Do not use neon colors or highly saturated tones
- Do not use dotted or dashed line patterns in charts
- Do not navigate away from the visualization tab before blocking on the user

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
