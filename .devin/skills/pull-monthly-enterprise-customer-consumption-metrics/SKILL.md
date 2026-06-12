---
name: pull-monthly-enterprise-customer-consumption-metrics
description: "Converted from Devin playbook: Pull Monthly Enterprise Customer Consumption Metrics"
triggers:
  - user
  - model
---

# Pull Monthly Enterprise Customer Consumption Metrics

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Pull Monthly Enterprise Customer Consumption Metrics (playbook-f5e831d911ca408a9e599bedb1a1fa29)

## Procedure

# Playbook: Pull Enterprise Customer Consumption Metrics

## Overview

Pull a standardized set of consumption metrics for a specific enterprise customer, aggregated by billing cycle. The output includes a SQL query, Metabase playground link, and PowerPoint presentation. This playbook is specifically for billing-cycle views; do not use it for calendar-month reporting.

## What's Needed From User

- **Enterprise customer name** (e.g., "litera", "hg-access") - used to filter by `enterprise_name` in the database
- **Start date** (optional, defaults to 2025-06-01) - earliest billing cycle to include

## Procedure

1. **Validate the enterprise filter** by running this query to ensure you're matching the correct enterprise:
   ```sql
   SELECT DISTINCT enterprise_id, enterprise_name, COUNT(DISTINCT org_id) AS org_count
   FROM analytics.enterprise_consumption_cycles
   WHERE LOWER(enterprise_name) LIKE '%{customer_name}%'
   GROUP BY 1, 2
   ORDER BY org_count DESC
   ```
   - If this returns multiple unrelated enterprises, refine your filter (use a more specific name or filter by `enterprise_id`)
   - If this returns 0 rows, the enterprise may not have billing cycles configured yet - inform the user

2. **Copy the SQL template from Specifications**, replace `{customer}` with the lowercase customer name (e.g., `litera`) and `{start_date}` with `2025-06-01` (or user-specified date)

3. **Execute the query** using the Redshift MCP:
   ```bash
   mcp-cli tool call execute_sql -s redshift --input '{"query": "<FULL_QUERY>"}'
   ```
   - If all rows show `total_sessions = 0` or `total_acus = 0`, flag this to the user before proceeding

4. **Generate a Metabase playground link** using the Redshift MCP (required for all final answers):
   ```bash
   mcp-cli tool call get_metabase_playground_link -s redshift --input '{"query": "<FULL_QUERY>"}'
   ```

5. **Create a PowerPoint presentation** with the results:
   - Install if needed: `pip install python-pptx`
   - Title slide: "{Enterprise Name} - Consumption Metrics by Billing Cycle"
   - Table slide with all columns, sorted by `cycle` ascending
   - Round numeric columns appropriately
   - Add a note that the last cycle may be partial (incomplete data)
   - Add a key insights bullet summarizing growth trends (e.g., "Active users grew X% from first to last cycle")

6. **Share results with user** including:
   - Formatted markdown table with the results
   - The full SQL query
   - Metabase playground link (formatted as clickable link)
   - PowerPoint file attachment
   - Note if the last cycle is partial
   - If PR merge rate is very low (<1%) but PR creation is high, mention the GitHub integration caveat

## Specifications

**Output deliverables:**
- Markdown table with consumption metrics by billing cycle
- Complete SQL query that produced the results
- Metabase playground link for interactive exploration
- PowerPoint presentation file (.pptx) with the data

**Query template:**
```sql
WITH {customer}_cycles AS (
  SELECT DISTINCT
    org_id,
    cycle_start,
    cycle_end,
    TO_CHAR(cycle_start, 'YYYY-MM') AS cycle_name
  FROM analytics.enterprise_consumption_cycles
  WHERE LOWER(enterprise_name) LIKE '%{customer}%'
    AND cycle_start >= '{start_date}'
),
acu_consumption AS (
  SELECT
    TO_CHAR(cycle_start, 'YYYY-MM') AS cycle_name,
    SUM(acus) AS total_acus,
    MAX(monthly_acu_capacity) AS monthly_allocation,
    ROUND(100.0 * SUM(acus) / NULLIF(MAX(monthly_acu_capacity), 0), 1) AS pct_allocation
  FROM analytics.enterprise_consumption_cycles
  WHERE LOWER(enterprise_name) LIKE '%{customer}%'
    AND cycle_start >= '{start_date}'
  GROUP BY 1
),
repo_indexed AS (
  SELECT
    c.cycle_name,
    COUNT(*) AS total_repos_indexed
  FROM devin.repo_indexing_jobs r
  JOIN {customer}_cycles c
    ON r.org_id = c.org_id
   AND r.created_at >= c.cycle_start
   AND r.created_at < c.cycle_end
  GROUP BY 1
),
searches AS (
  SELECT
    c.cycle_name,
    COUNT(*) AS total_searches
  FROM analytics.ada_queries q
  JOIN {customer}_cycles c
    ON q.org_id = c.org_id
   AND q.created_at >= c.cycle_start
   AND q.created_at < c.cycle_end
  GROUP BY 1
),
sessions AS (
  SELECT
    c.cycle_name,
    COUNT(DISTINCT s.devin_id) AS total_sessions,
    COUNT(DISTINCT CASE WHEN s.uses_playbook THEN s.devin_id END) AS playbook_sessions,
    COUNT(DISTINCT s.user_id) AS active_users
  FROM analytics.dim_sessions s
  JOIN {customer}_cycles c
    ON s.org_id = c.org_id
   AND s.created_at >= c.cycle_start
   AND s.created_at < c.cycle_end
  GROUP BY 1
),
prs AS (
  SELECT
    c.cycle_name,
    COUNT(*) AS total_prs_created,
    SUM(CASE WHEN p.state = 'merged' THEN 1 ELSE 0 END) AS total_prs_merged
  FROM devin.devin_prs p
  JOIN analytics.dim_sessions s
    ON p.devin_id = s.devin_id
  JOIN {customer}_cycles c
    ON s.org_id = c.org_id
   AND p.created_at >= c.cycle_start
   AND p.created_at < c.cycle_end
  GROUP BY 1
),
cycles AS (
  SELECT DISTINCT cycle_name
  FROM {customer}_cycles
)
SELECT
  cy.cycle_name AS cycle,
  COALESCE(s.active_users, 0) AS active_users,
  COALESCE(r.total_repos_indexed, 0) AS total_repos_indexed,
  COALESCE(se.total_searches, 0) AS total_searches,
  COALESCE(s.total_sessions, 0) AS total_sessions,
  COALESCE(s.playbook_sessions, 0) AS playbook_sessions,
  COALESCE(p.total_prs_created, 0) AS total_prs_created,
  COALESCE(p.total_prs_merged, 0) AS total_prs_merged,
  CASE WHEN COALESCE(p.total_prs_created, 0) > 0
       THEN ROUND(100.0 * p.total_prs_merged / p.total_prs_created, 1)
       ELSE 0
  END AS pr_merge_pct,
  ROUND(COALESCE(a.total_acus, 0)) AS total_acus,
  COALESCE(a.pct_allocation, 0) AS pct_allocation,
  ROUND(COALESCE(a.total_acus, 0) / NULLIF(s.total_sessions, 0), 1) AS acus_per_session,
  ROUND(COALESCE(a.total_acus, 0) / NULLIF(p.total_prs_created, 0), 1) AS acus_per_pr_created,
  ROUND(COALESCE(a.total_acus, 0) / NULLIF(p.total_prs_merged, 0), 1) AS acus_per_pr_merged
FROM cycles cy
LEFT JOIN repo_indexed r ON cy.cycle_name = r.cycle_name
LEFT JOIN searches se ON cy.cycle_name = se.cycle_name
LEFT JOIN sessions s ON cy.cycle_name = s.cycle_name
LEFT JOIN prs p ON cy.cycle_name = p.cycle_name
LEFT JOIN acu_consumption a ON cy.cycle_name = a.cycle_name
ORDER BY cy.cycle_name
```

**Note on billing cycles:** Enterprise billing cycles typically run from the 15th of one month to the 15th of the next. The cycle_name reflects the start month (e.g., "2025-06" = June 15 to July 15).

**Note on PR merge data:** PR merge tracking depends on GitHub integration being properly configured. If merge rates appear unusually low (<1% with significant PR creation), do not interpret this as "customer isn't merging PRs" - instead mention the integration caveat in your output.

**Note on ACU source:** This playbook uses `analytics.enterprise_consumption_cycles` for ACU data because it provides monthly allocation and cycle boundaries. For other billing analyses, use `analytics.consumption_by_org.total_acu_delta` as the canonical ACU metric.

## Advice and Pointers

- Use case-insensitive matching (`LOWER(enterprise_name) LIKE '%{customer}%'`) to catch variations in enterprise naming
- The `devin.devin_prs` table doesn't have `org_id` directly - join through `analytics.dim_sessions` using `devin_id`
- ACU stands for "Agent Compute Unit" - Devin's billing metric for compute resource consumption
- Partial months (current billing cycle) will show incomplete data - always note this in the output
- The Metabase playground link is mandatory in all final answers

## Forbidden Actions

- Do not use `merged_at IS NOT NULL` to check for merged PRs - use `state = 'merged'` instead
- Do not query `devin.devin_prs.org_id` directly - it may not exist; always join through `dim_sessions`
- Do not run any queries other than SELECT (read-only access)
- Do not use this playbook for calendar-month reporting - it is designed for billing cycles only

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
