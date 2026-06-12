---
name: measuring-a-devin-pilot-final-summary
description: "Converted from Devin playbook: Measuring A Devin Pilot - final summary"
triggers:
  - user
  - model
---

# Measuring A Devin Pilot - final summary

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Measuring A Devin Pilot - final summary (playbook-c5d21257c1db43399982cd4aeba09caa), macro `!pilot_end`

## Procedure

# Playbook: Measuring Devin Pilot Success for Enterprise Customers

## Overview
This playbook provides a repeatable, data-driven approach to measuring the success of Devin pilots for enterprise customers. It focuses on productivity metrics that limit subjectivity and provide actionable insights for executive decision-making. The goal is to establish a baseline for the organization's ability to adopt and utilize AI agents effectively, rather than simply evaluating whether Devin "works."

**Key Principle:** Pilot success is measured by the team's baseline productivity in utilizing agentic systems, with the goal of improving that baseline in production through training, better context provision, and optimized workflows.

**Target Audience:** Go-To-Market Engineers, Sales Engineers, Customer Success teams conducting pilot evaluations with enterprise customers.

## What's Needed From User

### Required Inputs
- **Enterprise ID** (e.g., enterprise-00b8e02a6ab343d68369755c2baa7d11)
- **Pilot Start Date** (e.g., 2025-09-23) - Will be auto-detected and confirmed
- **Pilot End Date** (e.g., 2025-11-02) - Will be auto-detected and confirmed
- **Timezone:** Default to UTC (clearly label in all outputs)

### Optional Parameters (with defaults)
- **ACU Price:** Cost per Agent Compute Unit for ROI calculations (default: $3.33/ACU)
- **Engineer Hourly Cost:** Fully-loaded cost per engineer hour for ROI calculations (default: $50, $100, $150/hr - show all three)
- **Minimum Data Threshold:** Minimum sessions/PRs to show metrics (default: 10 sessions)
- **Ask Devin Tag Patterns:** Patterns to identify Ask Devin sessions (default: '%Devin Search prompt%', '%from-devin:%')

### Access Requirements
- Access to Redshift MCP server (redshift)
- Ability to generate Metabase playground links for customer validation

## Procedure

### Step 0: Auto-Detect and Confirm Pilot Window
Run an initial query to detect the pilot date range and require explicit confirmation before proceeding.

**Query 0.1: Auto-Detect Pilot Window**

```sql
SELECT 
    MIN(created_at) as suggested_pilot_start,
    MAX(created_at) as suggested_pilot_end,
    EXTRACT(DAY FROM (MAX(created_at) - MIN(created_at))) + 1 as pilot_days,
    COUNT(*) as total_sessions,
    COUNT(DISTINCT org_id) as active_sub_orgs,
    COUNT(DISTINCT user_id) as active_users
FROM analytics.dim_sessions
WHERE enterprise_id = '{ENTERPRISE_ID}'
```

**Action:**
- Present suggested dates to the operator
- Require explicit confirmation: "Pilot runs from {suggested_pilot_start} to {suggested_pilot_end} ({pilot_days} days). Confirm? (Y/N)"
- If confirmed, set :pilot_start and :pilot_end parameters for all subsequent queries
- If not confirmed, prompt for correct dates
- Document the confirmed pilot window in all outputs

### Step 1: Validate Data Availability and Coverage
Run data availability checks to understand pilot scope and validate sufficient data exists.

**Query 1.1: Check Session Data Availability**

```sql
SELECT 
    COUNT(*) as total_sessions,
    COUNT(CASE WHEN is_cognition_session_in_customer_org = false THEN 1 END) as customer_sessions,
    COUNT(CASE WHEN is_cognition_session_in_customer_org = true THEN 1 END) as cognition_run_sessions,
    MIN(created_at) as earliest_session,
    MAX(created_at) as latest_session,
    EXTRACT(DAY FROM (MAX(created_at) - MIN(created_at))) + 1 as pilot_duration_days,
    COUNT(DISTINCT org_id) as active_sub_orgs,
    COUNT(DISTINCT user_id) as active_users
FROM analytics.dim_sessions
WHERE enterprise_id = '{ENTERPRISE_ID}'
  AND created_at >= '{PILOT_START}'
  AND created_at < DATEADD('day', 1, '{PILOT_END}')
```

**Action:**
- If customer_sessions < 10, flag to customer that data is sparse
- Note the earliest_session and latest_session dates for reporting
- Document the actual pilot window used in all subsequent outputs

### Step 2: Calculate Pilot Baseline Totals
These three metrics provide the foundation for all productivity calculations.

**Metric 2.0a: Total ACUs Consumed**

```sql
SELECT 
    SUM(c.total_acus) as total_acus_consumed
FROM analytics.dim_sessions s
LEFT JOIN analytics.consumption_by_devin c ON s.devin_id = c.devin_id
WHERE s.enterprise_id = '{ENTERPRISE_ID}'
  AND s.created_at >= '{PILOT_START}'
  AND s.created_at < DATEADD('day', 1, '{PILOT_END}')
  AND s.is_cognition_session_in_customer_org = false
```

**Interpretation:**
- Total ACUs Consumed: Raw Agent Compute Unit consumption during pilot
- This is the denominator for all efficiency calculations (PRs per 1,000 ACUs)

**Metric 2.0b: Total Ask Devin Sessions**

```sql
SELECT 
    COUNT(*) as total_sessions,
    COUNT(DISTINCT CASE WHEN tags LIKE '%Devin Search prompt%' OR tags LIKE '%from-devin:%' THEN devin_id END) as ask_devin_sessions,
    ROUND(100.0 * COUNT(DISTINCT CASE WHEN tags LIKE '%Devin Search prompt%' OR tags LIKE '%from-devin:%' THEN devin_id END) / NULLIF(COUNT(*), 0), 1) as ask_devin_pct
FROM analytics.dim_sessions
WHERE enterprise_id = '{ENTERPRISE_ID}'
  AND created_at >= '{PILOT_START}'
  AND created_at < DATEADD('day', 1, '{PILOT_END}')
  AND is_cognition_session_in_customer_org = false
```

**Interpretation:**
- Total Ask Devin Sessions: Count and percentage of sessions using structured Ask Devin workflow
- This is a key adoption metric - target 50%+ for production

**Metric 2.0c: Total PRs Produced**

```sql
SELECT
  COUNT(DISTINCT CASE WHEN gh_pr IS NOT NULL AND gh_pr != '' THEN gh_pr END) AS total_unique_prs,
  COUNT(DISTINCT CASE WHEN gh_pr IS NOT NULL AND gh_pr != '' THEN devin_id END) AS sessions_with_prs
FROM analytics.dim_sessions
WHERE enterprise_id = '{ENTERPRISE_ID}'
  AND created_at >= '{PILOT_START}'
  AND created_at < DATEADD('day', 1, '{PILOT_END}')
  AND is_cognition_session_in_customer_org = false
```

**Interpretation:**
- Total PRs Produced: Unique pull requests created during pilot (primary output metric)
- Sessions with PRs: Number of sessions that produced PRs (may differ from unique PRs if multi-session PRs exist)

### Step 3: Calculate Core Productivity Metrics
These are the key metrics for the executive dashboard.

**Metric 3.1: Total Sessions and Adoption Breadth**

```sql
SELECT 
    COUNT(*) as total_sessions,
    COUNT(DISTINCT org_id) as active_sub_orgs,
    COUNT(DISTINCT user_id) as active_users,
    COUNT(DISTINCT CASE WHEN user_id IN (
        SELECT user_id 
        FROM analytics.dim_sessions 
        WHERE enterprise_id = '{ENTERPRISE_ID}'
          AND created_at >= '{PILOT_START}'
          AND created_at < DATEADD('day', 1, '{PILOT_END}')
          AND is_cognition_session_in_customer_org = false
        GROUP BY user_id 
        HAVING COUNT(*) >= 2
    ) THEN user_id END) as repeat_users,
    ROUND(100.0 * COUNT(DISTINCT CASE WHEN user_id IN (
        SELECT user_id 
        FROM analytics.dim_sessions 
        WHERE enterprise_id = '{ENTERPRISE_ID}'
          AND created_at >= '{PILOT_START}'
          AND created_at < DATEADD('day', 1, '{PILOT_END}')
          AND is_cognition_session_in_customer_org = false
        GROUP BY user_id 
        HAVING COUNT(*) >= 2
    ) THEN user_id END) / NULLIF(COUNT(DISTINCT user_id), 0), 1) as repeat_usage_rate_pct
FROM analytics.dim_sessions
WHERE enterprise_id = '{ENTERPRISE_ID}'
  AND created_at >= '{PILOT_START}'
  AND created_at < DATEADD('day', 1, '{PILOT_END}')
  AND is_cognition_session_in_customer_org = false
```

**Interpretation:**
- Adoption Breadth: Shows how widely Devin is being used across the enterprise
- Repeat Usage Rate: Indicates user satisfaction and value perception (target: >60%)

**Metric 3.2: PR Success Rate (Sessions → PRs)**

```sql
SELECT 
    COUNT(*) as total_completed_sessions,
    COUNT(DISTINCT CASE WHEN gh_pr IS NOT NULL AND gh_pr != '' THEN devin_id END) as sessions_with_prs,
    ROUND(100.0 * COUNT(DISTINCT CASE WHEN gh_pr IS NOT NULL AND gh_pr != '' THEN devin_id END) / NULLIF(COUNT(*), 0), 1) as pr_success_rate_pct
FROM analytics.dim_sessions
WHERE enterprise_id = '{ENTERPRISE_ID}'
  AND created_at >= '{PILOT_START}'
  AND created_at < DATEADD('day', 1, '{PILOT_END}')
  AND is_cognition_session_in_customer_org = false
  AND status IN ('exit', 'suspended')  -- Only completed sessions
```

**Interpretation:**
- PR Success Rate: Percentage of completed sessions that resulted in a PR
- Target Baseline: 20-40% for early pilots (varies by use case mix)
- Improvement Path: Better prompts, knowledge, playbooks, and use case selection

**Metric 3.3: Overall ACU Efficiency (PRs per 1,000 ACUs)**

```sql
SELECT 
    COUNT(DISTINCT CASE WHEN gh_pr IS NOT NULL AND gh_pr != '' THEN gh_pr END) AS total_unique_prs,
    SUM(c.total_acus) AS total_acus,
    ROUND(1000.0 * COUNT(DISTINCT CASE WHEN gh_pr IS NOT NULL AND gh_pr != '' THEN gh_pr END) / NULLIF(SUM(c.total_acus), 0), 2) AS prs_per_1000_acus
FROM analytics.dim_sessions s
LEFT JOIN analytics.consumption_by_devin c ON s.devin_id = c.devin_id
WHERE s.enterprise_id = '{ENTERPRISE_ID}'
  AND s.created_at >= '{PILOT_START}'
  AND s.created_at < DATEADD('day', 1, '{PILOT_END}')
  AND s.is_cognition_session_in_customer_org = false
```

**Interpretation:**
- PRs per 1,000 ACUs: North star efficiency metric (higher is better)
- This is an end-to-end metric including all sessions (successful and unsuccessful)
- Target: 20-30 for early pilots, 40+ for optimized production

### Step 4: Calculate ACU Efficiency by Cohort

**Query 4.1: Ask Devin vs Regular ACU Efficiency**

```sql
WITH cohort AS (
    SELECT 
        s.devin_id,
        s.gh_pr,
        CASE 
            WHEN s.tags LIKE '%Devin Search prompt%' OR s.tags LIKE '%from-devin:%' THEN true
            ELSE false
        END AS is_ask_devin,
        c.total_acus
    FROM analytics.dim_sessions s
    LEFT JOIN analytics.consumption_by_devin c ON s.devin_id = c.devin_id
    WHERE s.enterprise_id = '{ENTERPRISE_ID}'
      AND s.created_at >= '{PILOT_START}'
      AND s.created_at < DATEADD('day', 1, '{PILOT_END}')
      AND s.is_cognition_session_in_customer_org = false
)
SELECT 
    is_ask_devin,
    COUNT(DISTINCT CASE WHEN gh_pr IS NOT NULL AND gh_pr != '' THEN gh_pr END) AS unique_prs,
    ROUND(SUM(total_acus), 2) AS total_acus,
    ROUND(1000.0 * COUNT(DISTINCT CASE WHEN gh_pr IS NOT NULL AND gh_pr != '' THEN gh_pr END) / NULLIF(SUM(total_acus), 0), 2) AS prs_per_1000_acus
FROM cohort
GROUP BY is_ask_devin
ORDER BY is_ask_devin DESC
```

**Interpretation:**
- Compare end-to-end ACU efficiency between Ask Devin and regular sessions
- Expected pattern: Ask Devin shows 1.5-2x higher efficiency due to better success rates

**Query 4.2: PR Success Rate by Cohort**

```sql
WITH ask_devin_sessions AS (
    SELECT 
        s.devin_id,
        s.gh_pr,
        c.total_acus,
        CASE 
            WHEN s.tags LIKE '%Devin Search prompt%' OR s.tags LIKE '%from-devin:%' THEN true
            ELSE false
        END as is_ask_devin
    FROM analytics.dim_sessions s
    LEFT JOIN analytics.consumption_by_devin c ON s.devin_id = c.devin_id
    WHERE s.enterprise_id = '{ENTERPRISE_ID}'
      AND s.created_at >= '{PILOT_START}'
      AND s.created_at < DATEADD('day', 1, '{PILOT_END}')
      AND s.is_cognition_session_in_customer_org = false
      AND s.status IN ('exit', 'suspended')
)
SELECT 
    is_ask_devin,
    COUNT(*) as total_sessions,
    COUNT(DISTINCT CASE WHEN gh_pr IS NOT NULL AND gh_pr != '' THEN devin_id END) as sessions_with_prs,
    ROUND(100.0 * COUNT(DISTINCT CASE WHEN gh_pr IS NOT NULL AND gh_pr != '' THEN devin_id END) / NULLIF(COUNT(*), 0), 1) as pr_success_rate_pct
FROM ask_devin_sessions
GROUP BY is_ask_devin
ORDER BY is_ask_devin DESC
```

**Interpretation:**
- Compare PR success rates between Ask Devin and regular sessions
- Expected pattern: Ask Devin shows 2-3x higher PR success rates

### Step 5: Calculate Per-Session ACU Metrics (Conditional)
These metrics show ACU consumption for successful sessions only (excludes failed attempts).

**Query 5.1: ACUs per PR-Producing Session**

```sql
SELECT 
    COUNT(*) as pr_producing_sessions,
    ROUND(AVG(c.total_acus), 2) AS avg_acus_per_pr_session,
    ROUND(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY c.total_acus), 2) AS median_acus_per_pr_session
FROM analytics.dim_sessions s
LEFT JOIN analytics.consumption_by_devin c ON s.devin_id = c.devin_id
WHERE s.enterprise_id = '{ENTERPRISE_ID}'
  AND s.created_at >= '{PILOT_START}'
  AND s.created_at < DATEADD('day', 1, '{PILOT_END}')
  AND s.is_cognition_session_in_customer_org = false
  AND s.status IN ('exit','suspended')
  AND s.gh_pr IS NOT NULL 
  AND s.gh_pr != ''
```

**Interpretation:**
- Median ACUs per PR Session: Typical ACU cost for a successful session
- This is a conditional metric - it excludes the cost of unsuccessful attempts
- Use this for unit economics, but use end-to-end metrics for overall efficiency

**Query 5.2: ACUs per PR-Producing Session by Cohort**

```sql
-- Ask Devin PR-producing sessions
WITH ask_devin_pr_sessions AS (
    SELECT c.total_acus
    FROM analytics.dim_sessions s
    LEFT JOIN analytics.consumption_by_devin c ON s.devin_id = c.devin_id
    WHERE s.enterprise_id = '{ENTERPRISE_ID}'
      AND s.created_at >= '{PILOT_START}'
      AND s.created_at < DATEADD('day', 1, '{PILOT_END}')
      AND s.is_cognition_session_in_customer_org = false
      AND s.status IN ('exit','suspended')
      AND s.gh_pr IS NOT NULL 
      AND s.gh_pr != ''
      AND (s.tags LIKE '%Devin Search prompt%' OR s.tags LIKE '%from-devin:%')
)
SELECT 
    COUNT(*) as ask_devin_pr_sessions,
    ROUND(AVG(total_acus), 2) as avg_acus,
    ROUND(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY total_acus), 2) as median_acus
FROM ask_devin_pr_sessions;

-- Regular PR-producing sessions
WITH regular_pr_sessions AS (
    SELECT c.total_acus
    FROM analytics.dim_sessions s
    LEFT JOIN analytics.consumption_by_devin c ON s.devin_id = c.devin_id
    WHERE s.enterprise_id = '{ENTERPRISE_ID}'
      AND s.created_at >= '{PILOT_START}'
      AND s.created_at < DATEADD('day', 1, '{PILOT_END}')
      AND s.is_cognition_session_in_customer_org = false
      AND s.status IN ('exit','suspended')
      AND s.gh_pr IS NOT NULL 
      AND s.gh_pr != ''
      AND (s.tags NOT LIKE '%Devin Search prompt%' AND s.tags NOT LIKE '%from-devin:%')
)
SELECT 
    COUNT(*) as regular_pr_sessions,
    ROUND(AVG(total_acus), 2) as avg_acus,
    ROUND(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY total_acus), 2) as median_acus
FROM regular_pr_sessions;
```

**Interpretation:**
- Compare per-success ACU consumption between Ask Devin and regular sessions
- Use this to calculate "overhead ACU per PR" (waste on failed attempts)

### Step 6: Calculate ROI Metrics (ACU-Based)

**Query 6.1: Calculate Overhead ACU per PR**

Using results from Steps 4 and 5:

```
Overhead ACU per PR = End-to-End ACU/PR - Per Successful Session ACU/PR

Where:
- End-to-End ACU/PR = 1000 / (PRs per 1,000 ACUs from Query 4.1)
- Per Successful Session ACU/PR = Median from Query 5.2

Example for Ask Devin:
- End-to-End: 1000 / 38.79 = 25.78 ACU/PR
- Per Success: 12.28 ACU/PR (median)
- Overhead: 25.78 - 12.28 = 13.50 ACU/PR wasted on failures
```

**Interpretation:**
- Overhead ACU per PR: ACUs wasted on failed attempts per successful PR
- Lower is better - indicates higher success rate and less waste
- This is the KEY metric for explaining Ask Devin's efficiency advantage

**Query 6.2: Calculate Cost-Equivalency (PRs per Dollar)**

Using ACU efficiency and ACU price:

```
PRs per Dollar = (PRs per 1,000 ACUs / 1,000) / ACU_price × Budget

Example for Overall efficiency (24.54 PRs per 1,000 ACUs):
- At $400 budget: (24.54 / 1,000) / 3.33 × 400 = 2.95 Devin PRs
- At $800 budget: (24.54 / 1,000) / 3.33 × 800 = 5.90 Devin PRs
- At $1,200 budget: (24.54 / 1,000) / 3.33 × 1,200 = 8.84 Devin PRs
```

**Interpretation:**
- PRs per Dollar: How many Devin PRs you can produce for a given budget
- Compare to human baseline (1 PR per day at $400/$800/$1,200)
- Show all three hourly rates ($50, $100, $150) to accommodate different cost structures

### Step 7: Generate Executive Dashboard
Create two deliverables with IDENTICAL metrics using a publicly accessible web application with access code protection:

1. **Web UI** - Interactive dashboard (publicly hosted with 4-digit access code)
2. **Markdown Report** - Detailed report with explanations

#### Step 7.1: Set Up Backend with Public API
**Action:** Create a FastAPI backend that queries Redshift and serves the dashboard with public API endpoints

Create a backend service that:
1. Executes all queries from Steps 0-6 using MCP to query Redshift
2. Aggregates all metrics into a single JSON object
3. Serves metrics via public API endpoint (no authentication required)
4. Implements proper CORS for public access

**Backend Structure:**

```
app/
├── main.py              # FastAPI application with public endpoints
├── metrics.py           # Metrics calculation from Redshift queries
├── metrics_provider.py  # Abstraction layer (mock or Redshift)
└── config.py            # Configuration (enterprise ID, ACU price)

frontend/
├── src/
│   ├── components/      # Dashboard components
│   ├── App.tsx          # Main app with access gate
│   └── api.ts           # API client for backend
└── package.json
```

**Backend Implementation:**

```python
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.metrics_provider import get_metrics_provider

app = FastAPI(title="Devin Pilot Dashboard API")

# Open CORS for public access
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/api/pilot-metrics")
async def get_pilot_metrics():
    """Get pilot metrics (public endpoint for demo)."""
    provider = get_metrics_provider(settings.metrics_provider)
    metrics = provider.get_pilot_metrics(settings.enterprise_id)
    
    metrics["acu_price"] = settings.acu_price
    metrics["roi_metrics"] = calculate_roi_metrics(metrics)
    
    return metrics
```

**Metrics API Response Structure:**

```json
{
  "enterprise_id": "string",
  "pilot_start": "YYYY-MM-DD",
  "pilot_end": "YYYY-MM-DD",
  "pilot_days": number,
  "acu_price": number,
  "total_acus_consumed": number,
  "total_sessions": number,
  "ask_devin_sessions": number,
  "ask_devin_pct": number,
  "total_prs": number,
  "sessions_with_prs": number,
  "active_sub_orgs": number,
  "active_users": number,
  "repeat_users": number,
  "repeat_usage_rate": number,
  "pr_success_rate": number,
  "prs_per_1000_acus": number,
  "ask_devin_prs_per_1000_acus": number,
  "ask_devin_total_acus": number,
  "ask_devin_unique_prs": number,
  "regular_prs_per_1000_acus": number,
  "regular_total_acus": number,
  "regular_unique_prs": number,
  "ask_devin_pr_success_rate": number,
  "ask_devin_completed_sessions": number,
  "regular_pr_success_rate": number,
  "regular_completed_sessions": number,
  "median_acus_per_pr": number,
  "avg_acus_per_pr": number,
  "ask_devin_median_acus_per_pr": number,
  "ask_devin_avg_acus_per_pr": number,
  "regular_median_acus_per_pr": number,
  "regular_avg_acus_per_pr": number
}
```

#### Step 7.2: Build Frontend Application with 4-Digit Access Code Gate
**Tech Stack:**
- Vite + React + TypeScript
- Tailwind CSS for styling
- shadcn/ui for UI components
- Recharts for data visualization

**Frontend Authentication Flow:**

```typescript
// AccessGate component with 4-digit code validation
import { useState, useEffect } from 'react';

const ACCESS_CODE = import.meta.env.VITE_ACCESS_CODE || '8421';
const SESSION_KEY = 'pilot_dashboard_access';

export function AccessGate({ onAccessGranted }: AccessGateProps) {
  const [code, setCode] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    // Check for query parameter bypass
    const urlParams = new URLSearchParams(window.location.search);
    const queryCode = urlParams.get('code');
    
    if (queryCode && queryCode === ACCESS_CODE) {
      sessionStorage.setItem(SESSION_KEY, 'granted');
      onAccessGranted();
      return;
    }
    
    // Check for existing session
    const storedAccess = sessionStorage.getItem(SESSION_KEY);
    if (storedAccess === 'granted') {
      onAccessGranted();
      return;
    }
  }, [onAccessGranted]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (code === ACCESS_CODE) {
      sessionStorage.setItem(SESSION_KEY, 'granted');
      onAccessGranted();
    } else {
      setError('Invalid access code. Please try again.');
      setCode('');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center">
      <Card>
        <CardHeader>
          <Lock className="h-8 w-8 text-blue-600" />
          <CardTitle>Devin Pilot Dashboard</CardTitle>
          <CardDescription>Enter the 4-digit access code to continue</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit}>
            <Input
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={4}
              placeholder="####"
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
              className="text-center text-2xl tracking-widest"
              autoFocus
            />
            {error && <Alert variant="destructive">{error}</Alert>}
            <Button type="submit" disabled={code.length !== 4}>
              Access Dashboard
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
```

**App.tsx with Access Gate:**

```typescript
import { useState } from 'react'
import { AccessGate } from './components/AccessGate'
import { Dashboard } from './components/Dashboard'

function App() {
  const [hasAccess, setHasAccess] = useState(false)

  const handleAccessGranted = () => {
    setHasAccess(true)
  }

  const handleLogout = () => {
    sessionStorage.removeItem('pilot_dashboard_access')
    setHasAccess(false)
  }

  if (!hasAccess) {
    return <AccessGate onAccessGranted={handleAccessGranted} />
  }

  return <Dashboard onLogout={handleLogout} />
}

export default App
```

**API Client (no authentication required):**

```typescript
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8000';

export const api = {
  async getMetrics(): Promise<PilotMetrics> {
    const response = await fetch(`${API_URL}/api/pilot-metrics`);
    
    if (!response.ok) {
      throw new Error('Failed to fetch metrics');
    }
    
    return response.json();
  },
};
```

**Frontend Structure:**

```
src/
├── components/
│   ├── AccessGate.tsx      # 4-digit access code gate
│   ├── Dashboard.tsx       # Main dashboard (protected)
│   ├── MetricCard.tsx      # Reusable metric card
│   └── Charts.tsx          # Chart components
├── App.tsx                 # Router with access gate
├── api.ts                  # API client functions
└── main.tsx                # Entry point
```

**Visual Design Specifications:**

**Header Section:**
- Gradient background: `bg-gradient-to-r from-blue-600 to-blue-800`
- White text with large title: "Devin Pilot Success Report"
- Subtitle with customer name
- Pilot period details with icons (Calendar, dates, timezone)

**Key Finding Highlight:**
- Blue background box: `bg-blue-50 border-l-4 border-blue-600`
- Target icon and bold "KEY FINDING" label
- Highlight Ask Devin impact with bold metrics

**Main Metric Cards (3 cards):**
- Shadow and hover effects: `shadow-md hover:shadow-lg transition-shadow`
- Color-coded titles: green for Ask Devin, blue for baseline, purple for adoption
- Large font sizes for primary metrics: `text-3xl font-bold`

**KPI Cards Grid (9 cards):**
- 4-column responsive grid: `grid-cols-1 md:grid-cols-2 lg:grid-cols-4`
- Icons from lucide-react (Users, TrendingUp, Target, Zap, Calendar, DollarSign)
- Badges for confidence levels: "High Confidence", "Strong Signal", "North Star"
- Color-coded badges: blue-100, green-100, purple-100

**Charts:**
- Bar Chart for ACU Efficiency Comparison (PRs per 1,000 ACUs and PR Success Rate)
- Stacked Bar Chart for Overhead ACU Analysis (green for successful, red for overhead)
- Line Chart for Cost-Equivalency Analysis (3 lines: Human Baseline, Devin Overall, Ask Devin)
- Responsive containers: `<ResponsiveContainer width="100%" height={300}>`

**Data Tables:**
- Clean borders and alternating row colors
- Bold headers and key metrics
- Footnotes for conditional metrics

**Insights Section:**
- 6 colored cards with border-left accent: `border-l-4 border-l-{color}-500`
- Emoji icons for visual interest
- Bullet points with key findings
- Color scheme: green, blue, purple, orange, teal, indigo

**Recommended Next Steps:**
- Gradient card: `bg-gradient-to-r from-blue-600 to-purple-600 text-white`
- Numbered action items
- Bold priority labels

**Footer:**
- Centered text with generation date
- Summary of baseline productivity

#### Step 7.3: Deploy to Public URL with Access Code Protection

**Deployment Steps:**

1. **Generate 4-digit access code:**
```bash
# Generate random 4-digit code
echo $((1000 + RANDOM % 9000))
```

2. **Configure frontend environment:**
```bash
cd frontend
echo "VITE_API_URL=https://your-backend.fly.dev" > .env
echo "VITE_ACCESS_CODE=3275" >> .env
```

3. **Add noindex meta tag to prevent search engine indexing:**
```html
<!-- frontend/index.html -->
<head>
  <meta name="robots" content="noindex, nofollow" />
  <title>Devin Pilot Dashboard</title>
</head>
```

4. **Build frontend:**
```bash
npm run build
```

5. **Deploy backend to Fly.io:**
```bash
cd backend
# Use deploy tool with command "backend" and backend directory
```

6. **Deploy frontend to public URL:**
```bash
cd frontend
# Use deploy tool with command "frontend" and dist directory
# Result: https://devin-pilot-dashboard-xxxxx.devinapps.com
```

7. **Share with stakeholders:**

**Method 1: Manual Code Entry**
```
URL: https://devin-pilot-dashboard-xxxxx.devinapps.com
Access Code: 3275
```

**Method 2: Query Parameter Bypass (Recommended)**
```
https://devin-pilot-dashboard-xxxxx.devinapps.com/?code=3275
```

**Result:** Publicly accessible dashboard with 4-digit access code protection

**Security Features:**
- Client-side validation (suitable for pilot demos with non-sensitive data)
- sessionStorage persistence for browser session
- Query parameter bypass for easy sharing
- noindex meta tag prevents search engine indexing
- No backend authentication required (public API)

**Benefits Over Localhost HTTP Basic Auth:**
- ✅ No VPN, SSH tunnel, or network configuration required
- ✅ Works on any device with internet access
- ✅ Clean, branded UI instead of browser dialog
- ✅ Easy to share with remote stakeholders
- ✅ Session persistence with sessionStorage

**Required Sections in Dashboard:**

**Section 1: Pilot Baseline Totals**
- Display in header and summary cards
- Total ACUs Consumed
- Total Ask Devin Sessions (count and %)
- Total PRs Produced

**Section 2: Core Productivity Metrics**
- 9 KPI cards with icons and badges:
  - Total Sessions
  - Active Users
  - Repeat Usage Rate
  - PRs per 1,000 ACUs
  - PR Success Rate
  - Median ACUs per PR Session
  - Total ACUs Consumed
  - Total PRs Produced
  - Active Sub-Organizations

**Section 3: ACU Efficiency Table**

| Metric | PRs per 1,000 ACUs | ACUs per PR | Devin PR Cost (@$3.33/ACU) |
|--------|-------------------|-------------|---------------------------|
| Overall (All Sessions) | {value} | {value} | ${value} |
| Ask Devin Sessions | {value} | {value} | ${value} |
| Regular Sessions | {value} | {value} | ${value} |
| Per PR-Producing Session (Median) | {value}* | {value} | ${value} |
| Per PR-Producing Session (Average) | {value}* | {value} | ${value} |

*Conditional metric - only includes sessions that produced PRs

**Section 4: Overhead ACU Analysis**

Visual: Stacked Bar Chart
- X-axis: Ask Devin vs Regular
- Y-axis: ACUs per PR
- Green bars: Per Successful Session (Median ACU/PR)
- Red bars: Overhead (wasted on failures)

Table:

| Metric | Ask Devin | Regular | Difference |
|--------|-----------|---------|------------|
| Per Successful Session (Median ACU/PR) | {value} | {value} | {diff} |
| End-to-End (ACU/PR, all sessions) | {value} | {value} | {diff} |
| Overhead ACU per PR (wasted on failures) | {value} | {value} | {diff} |

Explanation: Ask Devin reduces ACU waste by X% ({ask_devin_overhead} vs {regular_overhead} overhead ACUs per PR)

**Section 5: Cost-Equivalency Analysis**

Visual: Line Chart
- X-axis: Budget levels ($400/day, $800/day, $1200/day)
- Y-axis: PRs produced
- 3 lines:
  - Gray line (flat at 1): Human Baseline
  - Blue line: Devin (Overall)
  - Teal line: Devin (Ask Devin)

Text Summary:

Overall (All Sessions): {prs_per_1000_acus} PRs per 1,000 ACUs
- Cost per PR: ${cost_per_pr}
- At $400 human day: {value} Devin PRs (vs 1 human PR)
- At $800 human day: {value} Devin PRs (vs 1 human PR)
- At $1,200 human day: {value} Devin PRs (vs 1 human PR)

Ask Devin Sessions: {prs_per_1000_acus} PRs per 1,000 ACUs
- Cost per PR: ${cost_per_pr} ({pct}% cheaper than regular)
- At $400 human day: {value} Devin PRs (vs 1 human PR)
- At $800 human day: {value} Devin PRs (vs 1 human PR)
- At $1,200 human day: {value} Devin PRs (vs 1 human PR)

**Section 6: What This Means for Your Organization**

Visual: 3 comparison cards
- Without Devin baseline
- With Devin (equal investment, overall efficiency)
- With Ask Devin workflow (equal investment)
- Show productivity multipliers at different salary bands ($50/hr, $100/hr, $150/hr)

**Section 7: Key Insights and Recommendations**

Visual: 6 colored insight cards

1. **Ask Devin Impact (green, MOST IMPORTANT)**
   - 2.1x PR success rate
   - 1.68x ACU efficiency
   - 59% less waste

2. **ACU Efficiency is Measurable (blue)**
   - Fully reproducible metrics
   - Clear baseline established

3. **Exponential Value (purple)**
   - Fixed ACU cost creates exponential value at higher salary bands
   - Show multipliers at $50, $100, $150/hr

4. **Strong Adoption Breadth (orange)**
   - X active users across Y sub-organizations
   - Z% repeat usage rate

5. **Excellent Repeat Usage (teal)**
   - Exceeds 60% target threshold
   - Strong value perception

6. **Baseline Productivity Established (indigo)**
   - Foundation for production optimization
   - Clear improvement path identified

**Section 8: Recommended Next Steps**

Visual: Gradient card with numbered action items

- Priority 1: Expand Ask Devin usage - Train users on Ask Devin workflow
- Priority 2: Expand playbook usage - Develop organization-specific playbooks
- Conduct training sessions on effective prompting
- Identify top 5 use cases for playbook development
- Implement knowledge bases for most-used repositories
- Set target: increase PR success rate and ACU efficiency

## Specifications

### Success Criteria:
- Executive dashboard generated with all baseline totals and core metrics
- ACU-based ROI analysis with overhead calculations
- Cost-equivalency shown at three hourly rates ($50, $100, $150) with line graph visualization
- Ask Devin efficiency advantage clearly explained
- Recommendations provided for production improvement
- Beautiful, visually appealing UI matching or exceeding reference design
- Publicly accessible URL with 4-digit access code protection
- Secure session-based access with sessionStorage persistence

### Output Format:
- **Web UI:** Interactive dashboard running on public URL (access code protected)
- **Markdown Report:** Detailed report file (generated from same metrics)
- All timestamps in UTC, clearly labeled
- All metrics identical between both deliverables
- Access code required: Users must enter 4-digit code or use query parameter bypass

### Data Quality Standards:
- All queries filtered by enterprise_id
- All queries use consistent pilot time window
- All metrics include sample size (N=) for context
- Medians preferred over means for robustness
- Confidence levels labeled for each metric

### Forbidden Actions:
- Do NOT present ROI as a single number - always show multiple scenarios
- Do NOT claim metrics are "proof" that Devin works - frame as baseline for improvement
- Do NOT ignore data quality issues - document all limitations clearly
- Do NOT use cherry-picked examples - use aggregate metrics across all sessions
- Do NOT use email authentication for pilot demos - use 4-digit access code
- Do NOT require backend session management - use client-side validation
- Do NOT use HTTP Basic Auth - use clean branded UI
- Do NOT require VPN or SSH tunnel - deploy to public URL
- Do NOT use complex security - this is for pilot demos with non-sensitive data
- Do NOT index in search engines - add noindex meta tag

## Advice and Pointers

### Framing the ACU-Based ROI:
- Lead with "productivity throughput" not "cost savings"
- Emphasize that ACU metrics are fully measurable and reproducible
- Show that Devin's fixed ACU cost makes value exponential at higher salary bands
- Use the overhead ACU calculation to explain Ask Devin's advantage

### Explaining the Overhead Metric:
- "Ask Devin successful sessions use slightly MORE ACUs per PR (12.28 vs 10.78)"
- "But Ask Devin's higher success rate means you waste far fewer ACUs on failures"
- "Ask Devin reduces ACU waste by 59% (13.50 vs 32.62 overhead ACUs per PR)"
- "Net result: 1.68× better overall efficiency"

### Building Trust:
- Always provide Metabase playground links for transparency
- Document all assumptions clearly (ACU price, human baseline, oversight time)
- Label confidence levels for each metric
- Acknowledge limitations upfront
- Invite customer to validate queries and suggest improvements

### Customization by Customer:
- Adjust ACU price based on customer's contract
- Show all three hourly rates ($50, $100, $150) to accommodate different cost structures
- Adjust pilot window based on actual pilot start date

### Public URL Deployment Workflow:
1. Set up FastAPI backend with public API endpoints
2. Build React frontend with 4-digit access code gate
3. Deploy backend to Fly.io
4. Deploy frontend to public URL using deploy tool
5. Generate and configure 4-digit access code
6. Add noindex meta tag to prevent search indexing
7. Share public URL and access code with stakeholders
8. Support query parameter bypass for easy sharing

### Access Code Best Practices:
- Use environment variables for VITE_ACCESS_CODE
- Generate random 4-digit codes for each deployment
- Rotate codes after each demo
- Use different codes for different customers
- Support query parameter bypass (?code=####) for easy sharing
- Implement sessionStorage persistence for browser session
- Provide clear error messages for invalid codes
- Include logout functionality in the dashboard header

### Sharing Dashboard with Customers:
- Provide public URL and 4-digit access code
- Share query parameter URL for instant access: https://url.com/?code=####
- Works on any device with internet access
- No VPN, SSH tunnel, or network configuration required
- Clean, branded UI instead of browser dialog
- Session persists for browser session only

### Follow-up Actions:
- Schedule 30-day post-pilot check-in to measure improvement
- Set specific targets: e.g., "increase Ask Devin adoption from 7% to 50% in 90 days"
- Identify 2-3 high-impact use cases for playbook development
- Plan training sessions on Ask Devin workflow
- Implement knowledge bases for top 5 repositories

## Example Output

The dashboard is deployed to a public URL (e.g., https://devin-pilot-dashboard-lmjc0btp.devinapps.com) with 4-digit access code protection. Users can access it by:

1. **Manual Entry:** Navigate to the URL and enter the 4-digit code
2. **Query Parameter:** Use the URL with ?code=#### for instant access

The interface features a clean, branded access gate with Lock icon and professional UI. After authentication, users access the full dashboard with all metrics and visualizations. The session persists using sessionStorage, and logout functionality clears the session and returns to the access gate.

Benefits:
- Works on any device with internet access
- No VPN or network configuration required
- Easy to share with remote stakeholders
- Professional branded experience
- Session persistence for convenience
- Simple 4-digit code rotation for security

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
