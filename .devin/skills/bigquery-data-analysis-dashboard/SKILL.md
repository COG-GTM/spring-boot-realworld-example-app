---
name: bigquery-data-analysis-dashboard
description: "Converted from Devin playbook: BigQuery Data Analysis & Dashboard"
triggers:
  - user
  - model
---

# BigQuery Data Analysis & Dashboard

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: BigQuery Data Analysis & Dashboard (playbook-1a1c6024ae094cedbd33f4ae50999045), macro `!revenue_anomaly`

## Procedure

# BigQuery Data Analysis & Dashboard

## Overview

Execute a user-specified analytical query against a BigQuery dataset, then present the results as an interactive HTML dashboard with supporting artifacts (CSV export, markdown report, Puppeteer screenshot). This playbook handles the full pipeline: connecting to BigQuery, exploring the schema, executing the analysis, and delivering polished, consistent results.

## What's Needed From User

- **Analysis request**: A description of the question to answer or analysis to perform (e.g. "detect revenue anomalies", "compare weekly conversion rates", "segment users by LTV").
- **BigQuery dataset**: The fully qualified dataset (e.g. `bigquery-public-data.google_analytics_sample`). If not provided, ask the user.
- **Parameters** (optional): Any thresholds, date ranges, filters, or grouping dimensions relevant to the analysis. Use sensible defaults when not specified.

Only ever create the TODO list for the current phase.

<phase name="Data Connection & Schema Discovery" id="1">
## Data Connection & Schema Discovery

1. Connect to BigQuery using the `cognition-bigquery` MCP server. If MCP is unavailable (e.g. credential errors, timeout), fall back to the `google-cloud-bigquery` Python client:
   - `pip install google-cloud-bigquery`
   - Write the `GCP_SA_JSON` environment variable to a temporary credentials file.
   - Initialize the client with `google.oauth2.service_account.Credentials.from_service_account_info()`.
2. List tables in the target dataset. Identify the table structure:
   - Sharded daily tables (e.g. `ga_sessions_*`) queried via `_TABLE_SUFFIX`
   - Date-partitioned tables queried via a partition column
   - Standard tables
3. Query `INFORMATION_SCHEMA.COLUMNS` (or equivalent) to inspect column names and data types. Identify which columns are relevant to the user's analysis request.
4. Determine the date range by querying for `MIN` and `MAX` of the date/partition column.
5. Summarize the schema findings and confirm the approach with the user if the dataset is unfamiliar or column mappings are ambiguous.

<verification>
- BigQuery connection is established and queries execute successfully
- Table structure and partitioning pattern are identified
- Columns relevant to the analysis are identified with their data types
- The full date range of the dataset is known
</verification>
</phase>

<phase name="Query Design & Execution" id="2">
## Query Design & Execution

1. Based on the user's analysis request and the discovered schema, design SQL query(s) that answer the question. Apply any user-specified parameters (date ranges, thresholds, filters, grouping dimensions).
2. Handle data type nuances discovered in Phase 1 (e.g. values stored in micros need division by 1,000,000; nested/repeated fields need `UNNEST`; timestamp columns need casting).
3. Execute the query. If it fails, inspect the error, adjust the SQL, and retry.
4. Process the results into structured data:
   - A **full dataset** (all rows, for charting) saved to a JSON file on disk.
   - A **summary/filtered dataset** (key findings, anomalies, top-N, etc.) saved to a separate JSON file on disk.
5. Validate the results: check row counts, null rates, and value ranges for sanity.

<verification>
- The SQL query executed successfully and returned results
- Results are sensible (no unexpected nulls, row counts match expectations)
- Full dataset and summary dataset are saved as JSON files on disk
</verification>
</phase>

<phase name="Visualization & Delivery" id="3">
## Visualization & Delivery

1. **Build an interactive HTML dashboard** using Apache ECharts (loaded via CDN: `https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js`). The dashboard must be a single self-contained HTML file with data embedded as inline JSON. Include:
   - **KPI cards** at the top summarizing key metrics from the analysis.
   - **Primary chart(s)** that directly answer the user's question (choose chart type to match the data — line/area for time series, bar for categorical, scatter for correlations, heatmap for two-dimensional distributions, etc.).
   - **Supporting chart(s)** that provide context or drill-down (e.g. breakdowns, distributions, comparisons).
   - **Data table** showing the detailed summary/filtered results.
   - Follow the standardized color scheme:
     - Page background: `#0F172A`
     - Card/panel background: `#1E293B`
     - Primary accent: `#22D3EE` (cyan)
     - Secondary: `#60A5FA` (blue)
     - Tertiary: `#818CF8` (indigo)
     - Quaternary: `#A78BFA` (purple)
     - Negative/alert: `#F87171` (red)
     - Positive/success: `#4ADE80` (green)
     - Text: `#F1F5F9` (light), `#94A3B8` (muted)
     - Font: Inter (Google Fonts)
   - All charts must have tooltips, axis labels, and legends. Use `echarts.init(container, 'dark')` for dark theme, then override colors to match the scheme above.

2. **Generate a CSV file** of the summary/filtered results for downstream use.

3. **Write an analysis report** (markdown) including:
   - Executive summary of findings.
   - Data table with key results.
   - Interpretation and commentary on notable patterns.
   - Methodology section documenting the query, parameters, and any assumptions.

4. **Screenshot the dashboard**:
   - Start a local HTTP server (`python3 -m http.server 8080`) to serve the HTML file.
   - Use Puppeteer (headless Chrome) via the global `take-screenshot` command to capture a full-page screenshot without browser chrome, scrollbars, or cursor.
   - Open the dashboard in the Devin browser (`google-chrome http://localhost:8080/<filename>.html`) for interactive exploration.

5. **Deliver all artifacts** to the user via `message_user` with attachments:
   - The screenshot image
   - The interactive HTML dashboard file
   - The CSV file
   - The analysis report markdown
   - Set `suggest_open_desktop=true` so the user can explore the interactive dashboard in the Devin browser.
   - Tell the user: "The interactive visualization is loaded in the Devin browser — you can hover, zoom, and explore it directly there."

<verification>
- The HTML dashboard renders correctly with KPI cards, charts, and data table
- The screenshot has been taken via Puppeteer and shows the full dashboard without browser chrome
- The CSV file contains the summary/filtered data with correct values
- The analysis report includes summary, table, interpretation, and methodology
- All artifacts (screenshot, HTML, CSV, report) have been sent to the user as attachments
- Key metrics are consistent across dashboard, CSV, and report
</verification>
</phase>

## Specifications

- The dashboard HTML must be a single self-contained file with no external dependencies except CDN-loaded ECharts and Google Fonts.
- All charts must use the standardized enterprise color scheme listed above.
- The screenshot must be taken via Puppeteer (headless Chrome) to produce a clean image without browser chrome, scrollbars, or cursor.
- Key metrics must be consistent across the dashboard KPI cards, the data table, the CSV, and the report.
- The CSV must use standard formatting (comma-separated, UTF-8, header row).

## Advice and Pointers

- GA360 data (`ga_sessions_*`) uses sharded daily tables queried with `_TABLE_SUFFIX`. GA4 data (`events_*`) uses the same sharding pattern but a different schema.
- If the BigQuery MCP server fails, fall back to the Python `google-cloud-bigquery` client using `GCP_SA_JSON`.
- For large datasets, add `LIMIT` clauses during development and remove for the final run.
- Choose chart types that match the data: line/area for time series, bar for categorical comparisons, scatter for correlations, heatmap for two-dimensional distributions.
- When the user specifies a custom or unfamiliar dataset, always complete schema discovery before writing queries.
- The ECharts `dark` theme provides a good base but override colors to match the standardized scheme.
- Use `SAFE_DIVIDE` in BigQuery to avoid division-by-zero errors.

## Forbidden Actions

- Do not use matplotlib, seaborn, or any static image library for visualizations — always use interactive HTML + ECharts.
- Do not hardcode column names without first confirming them via schema exploration in Phase 1.
- Do not skip the Puppeteer screenshot step — the user must receive a rendered image alongside the interactive HTML.
- Do not send raw JSON to the user as a deliverable — always produce formatted artifacts (dashboard, CSV, report).

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
