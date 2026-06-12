---
name: template-enterprise-usage-report
description: "Converted from Devin playbook: Template Enterprise Usage Report"
triggers:
  - user
  - model
---

# Template Enterprise Usage Report

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Template Enterprise Usage Report (playbook-59be06cf524b49e093aa27a121013b6c), macro `!template_enterprise_usage_report`

## Procedure

# Enterprise Usage Report

## Overview

Fetches Devin session data for any enterprise from Redshift and generates an interactive HTML dashboard showing Agent Compute Unit (ACU) consumption, use case breakdown, business unit analysis, and power user insights. The report supports per-month filtering via a dropdown toggle.

**Output:** Single HTML file with embedded data, interactive charts (Chart.js), month toggle, and four tabs: Overview, Sub-Use Cases, Business Units, Power Users.

## Required Inputs

- **Enterprise ID** (e.g., `enterprise-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`). The user must provide this.
- Redshift credentials via environment variables (see Running the Script section)
- `MONTH_START`: Start date (default: `2025-10-01`)
- `MONTH_END`: End date (default: today)

The script queries `analytics.dim_sessions`, `analytics.dim_orgs`, `analytics.dim_users`, and `analytics.consumption_by_org` tables via direct Redshift connection.

## Procedure

Create and run `enterprise_report.py` following this structure:

### 1. Configuration

```python
import os, json, re, psycopg2
from collections import defaultdict, Counter
from datetime import datetime, timezone, timedelta
from decimal import Decimal

MONTH_START = "2025-10-01"
MONTH_END = datetime.now().strftime("%Y-%m-%d")
ENTERPRISE_ID = '<USER_PROVIDED_ENTERPRISE_ID>'  # User must supply this

# Optionally resolve enterprise display name from dim_orgs after connecting:
# ENTERPRISE_NAME is derived at runtime from the org data or the enterprise_id

MONTH_LABELS = {
    "01": "Jan", "02": "Feb", "03": "Mar", "04": "Apr",
    "05": "May", "06": "Jun", "07": "Jul", "08": "Aug",
    "09": "Sep", "10": "Oct", "11": "Nov", "12": "Dec"
}

STOP_WORDS = {
    "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
    "of", "with", "by", "from", "as", "is", "was", "are", "were", "been",
    "be", "have", "has", "had", "do", "does", "did", "will", "would",
    "could", "should", "may", "might", "must", "shall", "can", "need",
    "it", "its", "this", "that", "these", "those", "i", "you", "he",
    "she", "we", "they", "what", "which", "who", "when", "where", "why",
    "how", "all", "each", "every", "both", "few", "more", "most", "other",
    "some", "such", "no", "nor", "not", "only", "own", "same", "so",
    "than", "too", "very", "just", "also", "now", "new", "use", "using",
    "used", "update", "add", "create", "file", "files", "code", "please",
    "help", "want", "like", "get", "make", "into"
}

REMOVED_METRICS = []
```

### 2. Use Case Classification (Keyword-Based Top-Level Categories)

Define broad use case categories with keyword matching. These serve as the top-level buckets:

```python
USE_CASE_KEYWORDS = {
    "Framework Migrations": ["migrate", "migration", "convert", "angular", "react", "pyspark", ".net", "java"],
    "Code Quality & Security": ["security", "vulnerability", "cve", "sonar", "checkmarx", "remediation", "test coverage"],
    "Version Upgrades": ["upgrade", "spring boot", "dependency update"],
    "Unit Test Generation": ["unit test", "jest", "pytest", "junit", "test case", "coverage"],
    "Refactoring & Optimization": ["refactor", "optimize", "performance", "cleanup", "tech debt", "consolidate"],
    "Feature Development": ["feature", "implement", "add", "create", "build", "new endpoint"],
    "Documentation": ["doc", "readme", "documentation", "swagger", "runbook"],
    "CI/CD & DevOps": ["ci/cd", "pipeline", "jenkins", "docker", "kubernetes", "terraform", "deploy"],
    "Bug Fixing": ["bug", "fix", "issue", "error", "debug", "hotfix", "patch"],
    "Code Analysis & Exploration": ["analyze", "analysis", "explain", "investigate", "examine", "understand", "explore", "review code"],
    "Repo Setup & Verification": ["repo access", "verify repo", "verify access", "load_repo", "clone repo", "check repo"],
    "Data Engineering": ["data migrat", "etl", "sql", "query", "database", "snowflake", "data process", "data model"],
    "Scheduling & Job Automation": ["jil", "autosys", "scheduler", "cron", "batch job", "scheduling"],
}

def classify_use_case(title):
    title_lower = (title or "").lower()
    for category, keywords in USE_CASE_KEYWORDS.items():
        if any(kw in title_lower for kw in keywords):
            return category
    return "Other"
```

### 3. Dynamic Sub-Use Case Bucketing

Instead of hardcoded sub-use case patterns, dynamically extract sub-categories from session titles within each top-level use case. This makes the report work across any enterprise without manual keyword tuning.

```python
def extract_meaningful_tokens(title):
    """Extract meaningful bigrams and unigrams from a title."""
    if not title:
        return []
    title_lower = re.sub(r'[^a-z0-9\s\.\-/]', ' ', title.lower())
    words = [w for w in title_lower.split() if w not in STOP_WORDS and len(w) > 2]
    tokens = list(words)  # unigrams
    for i in range(len(words) - 1):
        tokens.append(f"{words[i]} {words[i+1]}")  # bigrams
    return tokens

def build_dynamic_sub_use_cases(sessions_in_category, min_cluster_size=3, max_sub_categories=12):
    """
    Analyze session titles to find recurring term clusters.
    Returns a dict mapping sub-category label -> list of keyword patterns.
    """
    token_counter = Counter()
    token_sessions = defaultdict(set)

    for i, s in enumerate(sessions_in_category):
        title = s.get('title', '')
        for token in extract_meaningful_tokens(title):
            token_counter[token] += 1
            token_sessions[token].add(i)

    # Filter to tokens appearing in at least min_cluster_size sessions
    frequent = {t: c for t, c in token_counter.items()
                if c >= min_cluster_size and ' ' in t}  # prefer bigrams
    # Add high-frequency unigrams not already covered by bigrams
    for t, c in token_counter.items():
        if c >= min_cluster_size * 2 and ' ' not in t and t not in STOP_WORDS:
            if not any(t in bigram for bigram in frequent):
                frequent[t] = c

    # Sort by frequency descending, pick top clusters
    sorted_tokens = sorted(frequent.items(), key=lambda x: -x[1])

    sub_categories = {}
    assigned_sessions = set()

    for token, count in sorted_tokens:
        if len(sub_categories) >= max_sub_categories:
            break
        # Skip if this token's sessions are mostly already assigned
        token_sess = token_sessions[token]
        new_sessions = token_sess - assigned_sessions
        if len(new_sessions) < min_cluster_size:
            continue

        label = token.title()
        sub_categories[label] = [token]
        assigned_sessions.update(token_sess)

    return sub_categories

def classify_sub_use_case_dynamic(title, sub_category_map):
    """Classify a session into a dynamic sub-use case."""
    title_lower = (title or "").lower()
    for label, keywords in sub_category_map.items():
        if any(kw in title_lower for kw in keywords):
            return label
    return "General"
```

### 4. Business Unit Extraction

Extract business unit from org names. Use the org name prefix before " - " if present, otherwise use the full org name:

```python
def extract_bu(org_name):
    if not org_name:
        return "OTHER"
    if " - " in org_name:
        return org_name.split(" - ")[0]
    return org_name
```

Do NOT hardcode BU_PREFIXES. Let the org names speak for themselves. The report will group by whatever prefixes or names exist.

### 5. Database Connection and Data Fetching

```python
def get_connection():
    return psycopg2.connect(
        host=os.environ['REDSHIFT_MCP_REDSHIFT_HOST_PROD'],
        port=int(os.environ['REDSHIFT_MCP_REDSHIFT_PORT_PROD']),
        dbname='redshift',
        user=os.environ['REDSHIFT_MCP_REDSHIFT_USERNAME_PROD'],
        password=os.environ['REDSHIFT_MCP_REDSHIFT_PASSWORD_PROD']
    )

def run_query(conn, sql):
    cur = conn.cursor()
    cur.execute(sql)
    columns = [desc[0] for desc in cur.description]
    rows = cur.fetchall()
    cur.close()
    return [dict(zip(columns, row)) for row in rows]
```

Fetch organizations, sessions, users, and consumption data. Filter by `enterprise_id = ENTERPRISE_ID` and date range `MONTH_START` to `MONTH_END`:

- **Organizations**: `analytics.dim_orgs WHERE enterprise_id = ENTERPRISE_ID`
- **Sessions**: `analytics.dim_sessions s JOIN analytics.dim_orgs o ON s.org_id = o.org_id WHERE o.enterprise_id = ENTERPRISE_ID AND s.created_at >= MONTH_START AND s.created_at < MONTH_END + 1 day`
- **Users**: `analytics.dim_users WHERE user_id IN (...)` for all user_ids from sessions
- **Consumption**: Use `analytics.consumption_by_org.total_acu_delta` as the canonical ACU source (not `dim_sessions.consumed_acus`). This table may contain duplicate org_ids, so always aggregate first:

```python
def fetch_consumption(conn, org_ids):
    placeholders = ','.join([f"'{oid}'" for oid in org_ids])
    return run_query(conn, f"""
        SELECT org_id, SUM(total_acu_delta) AS total_acus
        FROM analytics.consumption_by_org
        WHERE org_id IN ({placeholders})
          AND total_acu_delta > 0
        GROUP BY org_id
    """)
```

Use this consumption data for all ACU totals in the report. Fall back to `dim_sessions.consumed_acus` only for session-level breakdowns (e.g., per-use-case ACU splits) where org-level consumption cannot be attributed.

### 6. Data Processing — Per-Month Slices

Build a data slice for each month plus a "total" slice. For each slice:

1. Classify each session into a top-level use case via `classify_use_case(title)`
2. Group sessions by use case, then run `build_dynamic_sub_use_cases()` on each group to discover sub-categories automatically
3. Classify each session into its dynamic sub-use case
4. Compute summary metrics: total sessions, total ACUs (from `consumption_by_org.total_acu_delta` for totals; `consumed_acus` for per-session breakdowns, treating NULL as 0), unique users, PRs created (`gh_pr IS NOT NULL`), PRs merged (`pr_merged_at IS NOT NULL`)
5. Build business unit breakdown from org names, including per-BU: use case table with nested sub-use case drill-down (top 3 sub-use cases per use case), top 10 users, and top 10 organizations
6. Compute global top users (`top_users_global`) and global top organizations (`top_orgs_global`) ranked by ACUs for the Overview tab and "All Business Units" aggregate view
7. Build power users list (top 10 by ACUs) with per-user: monthly charts, use case pie, org breakdown, PRs created count, and sub-use case breakdown per use case (top 5 sub-use cases per use case)
8. For "General" sub-use cases, extract a keyword cloud from unmatched session titles to show common terms (top 20, font size scaled by frequency)

**ACU Scaling:** Compute an ACU scale factor as `total_canonical_acus / total_session_acus` where canonical comes from `consumption_by_org.total_acu_delta` and session comes from `dim_sessions.consumed_acus`. Apply this factor to all per-session ACU values so breakdowns sum to the canonical total.

**Sub-use case maps:** Build sub-use case maps once from the full "total" session set (not per-month) and reuse them across all monthly slices for consistent categorization.

The `build_all_slices()` function should produce a dict keyed by month string (e.g., `"2025-10"`) plus `"total"`, each containing: `summary`, `use_cases` (with nested `sub_use_cases`), `business_units` (with nested `use_cases`, `top_users`, `top_orgs`), `top_users_global`, `top_orgs_global`, `power_users`, `monthly` chart data.

### 7. HTML Template

Generate a single self-contained HTML file using Tailwind CSS (via CDN) and Chart.js. The template structure:

**Header Section:**
- Blue gradient month toggle bar at the very top (above the header) with calendar icon and "Time Period" label
- Dark gradient header (`from-slate-800 to-slate-900`): enterprise name as `<h1>`, enterprise ID and current slice label, data source line
- Summary cards row (4 columns, `bg-white/10` on dark background): Sessions, Users, PRs Created, PRs Merged (with asterisk)
- Small footnote below cards: "* PR merge data depends on GitHub integration configuration and may be incomplete."
- Do NOT include a large "Total ACUs" number display in the header
- Do NOT include an "ACU Progress" bar or any ACU target/spend tracking
- ACU values still appear normally in charts, tables, and per-user breakdowns — only the header aggregate display and progress bar are removed

**Tab Navigation:**
Four tabs: Overview, Sub-Use Cases, Business Units, Power Users

**Overview Tab:**
- Monthly ACU consumption bar chart (highlight selected month in orange)
- Top Use Cases table with ACU and session counts
- Top 5 Organizations by ACUs
- Top 5 Users by ACUs

**Sub-Use Cases Tab:**
- Dropdown to select a use case category, sorted by total ACU activity descending ("Other" always last)
- Two-column layout: horizontal bar chart (top 8 sub-categories) on the left, table on the right
- Table of sub-use cases within that category: name, ACUs, percentage. "General" row styled in muted italic (`bg-slate-50 text-slate-500 italic`)
- For the "General" bucket, show keyword pills in a `bg-slate-50` rounded section below the table, with font size scaled by frequency

**Business Units Tab:**
- Prominent styled dropdown (`.selector-prominent`) defaulting to "{Enterprise Name} (All Business Units)". Each option shows BU name with ACU and user counts
- When "All Business Units" is selected, show enterprise-wide aggregate data (uses global top users/orgs and overall use case breakdown)
- Summary cards row: Total ACUs, Users, PRs Merged (with asterisk footnote about PR merge data)
- Monthly ACU consumption bar chart (selected month highlighted in orange)
- Two-column layout: Use case distribution pie chart (with percentage labels rendered inside slices for values ≥8%) on the left, use case table on the right
- Use case table includes sub-use case drill-down: each use case row followed by up to 3 indented sub-use case rows (prefixed with `->`, styled `bg-slate-50 text-xs`)
- Two-column layout below: Top 10 Users table (rank, name, ACUs, PRs) and Top 10 Organizations table (rank, name, ACUs, users)

**Power Users Tab:**
- Description text: "Top 10 users ranked by ACU consumption. Select a user to see their detailed breakdown."
- Prominent styled dropdown (`.selector-prominent`) listing top 10 users, each option showing rank, name, ACU count, and session count
- Per-user summary cards row: Total ACUs, Sessions, PRs Created, PRs Merged (with asterisk footnote)
- Two-column layout: Monthly consumption bar chart on the left, use case distribution pie chart (top 8 use cases) on the right
- Two-column layout below: Use cases table on the left with sub-use case drill-down (each use case row followed by up to 5 indented sub-use case rows prefixed with `->`, styled `bg-slate-50 text-xs`, showing ACUs, sessions, percentage), Organizations table on the right (name, ACUs, sessions)

**Styling:**
- Dark gradient header: `bg-gradient-to-r from-slate-800 to-slate-900` with white text
- Month toggle bar: blue gradient (`linear-gradient(135deg, #1e40af 0%, #3b82f6 100%)`) with semi-transparent white select styling, placed above the header
- `.selector-prominent` class for BU and Power User dropdowns: `padding: 10px 16px`, `border: 2px solid #cbd5e1`, blue focus ring
- `.chart-box` class: `position: relative; height: 300px` (inner charts at `height: 220px` for BU/Power User sections)
- Color palette constant: `COLORS = ['#3b82f6','#10b981','#f59e0b','#ef4444','#8b5cf6','#ec4899','#06b6d4','#84cc16','#f97316','#6366f1','#14b8a6','#e11d48']`
- PR merge disclaimer footnote (`text-xs text-slate-500`) in Business Units and Power Users sections
- Report footer: centered generation timestamp, data source, and PR merge caveat note

**JavaScript:**
- Month dropdown change destroys all Chart.js instances and rerenders all tabs
- `D()` function returns the current month's data slice
- Chart instances tracked in a `charts` object and destroyed before recreation via `destroyAllCharts()`
- All data embedded as JSON in the HTML via template replacement
- Pie chart plugin for BU use case chart renders percentage labels inside slices (only for values ≥8%)

### 8. Main Execution

```python
def main():
    conn = get_connection()
    orgs = fetch_organizations(conn)
    sessions = fetch_sessions(conn)
    user_ids = [s['user_id'] for s in sessions if s.get('user_id')]
    user_lookup = fetch_users(conn, user_ids)
    conn.close()

    # Metric availability checks — log and note any NULL fields
    # Check consumed_acus, title, pr_state, plan_type

    slices, month_options = build_all_slices(sessions, orgs, user_lookup)
    metadata = {
        "enterprise": ENTERPRISE_ID,
        "period": f"{MONTH_START} to {MONTH_END}",
        "generated": datetime.now().strftime("%Y-%m-%d %H:%M UTC"),
        "source": "Redshift (analytics schema)"
    }

    html = HTML_TEMPLATE
    html = html.replace("__REPORT_DATA__", json.dumps(slices, default=json_serial))
    html = html.replace("__MONTH_OPTIONS__", json.dumps(month_options))
    html = html.replace("__METADATA__", json.dumps(metadata))

    os.makedirs("./reports", exist_ok=True)
    html_path = f"./reports/enterprise_report_{MONTH_START}_to_{MONTH_END}.html"
    with open(html_path, "w") as f:
        f.write(html)
    print(f"Saved: {html_path}")
```

## Running the Script

```bash
pip install psycopg2-binary
python enterprise_report.py
```

Redshift credentials are expected via environment variables:

| Variable | Description |
|----------|-------------|
| `REDSHIFT_MCP_REDSHIFT_HOST_PROD` | Redshift cluster hostname |
| `REDSHIFT_MCP_REDSHIFT_PORT_PROD` | Redshift port (typically 5439) |
| `REDSHIFT_MCP_REDSHIFT_USERNAME_PROD` | Redshift username |
| `REDSHIFT_MCP_REDSHIFT_PASSWORD_PROD` | Redshift password |

## Output

- `./reports/enterprise_report_YYYY-MM-DD_to_YYYY-MM-DD.html` — Interactive dashboard
- Console output with metric availability notes and per-month summary

## Verification

Open the HTML file in a browser and verify:
- Month toggle dropdown allows switching between individual months and "Total (All Months)"
- Switching months updates all summary cards, charts, and tables across all tabs
- Header shows enterprise name and summary cards (Sessions, Users, PRs) but NO "Total ACUs" large display and NO "ACU Progress" bar
- Overview tab shows Monthly ACU chart, Top Use Cases, Top 5 Orgs, Top 5 Users
- User names display properly (not opaque user IDs) in all user lists
- Sub-Use Cases tab shows dropdown with categories sorted by ACU activity (highest first), "Other" last
- Sub-use case buckets are dynamically generated from session title terms (not hardcoded)
- "General" sub-categories show keyword pills derived from unmatched titles
- Business Units tab derives BU names from org name prefixes (no hardcoded list)
- Power Users tab shows top 10 users ranked by ACU consumption
- Power Users: selecting a user shows summary cards, monthly chart, use case pie, sub-use case drill-down in use case table, and orgs
- Currently selected month is highlighted in orange on monthly bar charts
- All charts render correctly and update when month changes

## Advice and Pointers

- The top-level `USE_CASE_KEYWORDS` dict provides a reasonable default set of categories. If the enterprise has unusual use cases not captured by these keywords, add entries to this dict.
- Dynamic sub-use case bucketing uses bigram frequency analysis with a minimum cluster size of 3 sessions. For smaller enterprises (under 500 sessions), lower `min_cluster_size` to 2. For very large enterprises (10k+ sessions), raise it to 5 to reduce noise. Adjust `max_sub_categories` similarly.
- PR merge data depends on GitHub integration being properly configured. Low merge rates (<1%) with significant PR creation rates (>30%) almost certainly indicate a missing integration, not actual low merge rates. Always note this caveat in the report footer.
- Use `consumption_by_org.total_acu_delta` as the canonical ACU source for totals. Do not rely solely on `dim_sessions.consumed_acus` for aggregate consumption numbers — it can produce vastly different (incorrect) totals.
- `plan_type` and `subscription_status` may be NULL for enterprise orgs — check and omit from the report if so.
- When adding the HTML template, follow the layout and styling described in the HTML Template section above, use the enterprise name from metadata for all display text, do not include ACU target/progress bar elements, and ensure the summary cards row contains only: Sessions, Users, PRs Created, PRs Merged.
- The `metadata` dict must include `enterprise_name` (resolved from `dim_orgs.enterprise_name` at runtime) in addition to `enterprise` (the ID). The HTML template references `METADATA.enterprise_name` for display.

## Forbidden Actions

- Do not include any ACU target numbers, ACU progress bars, or ACU total spend displays in the HTML output
- Do not hardcode business unit prefix lists — derive them from org names
- Do not hardcode sub-use case patterns — use dynamic bucketing from session title analysis
- Do not use `dim_sessions.consumed_acus` as the sole source for aggregate ACU totals — always use `consumption_by_org.total_acu_delta`

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
