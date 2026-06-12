---
name: pilot-demo-success
description: "Converted from Devin playbook: Pilot_demo_success"
triggers:
  - user
  - model
---

# Pilot_demo_success

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Pilot_demo_success (playbook-849b3be16d644d81b9a84949c4049948)

## Procedure

# Executive Demo Web UI Playbook
## Showcasing the Power of Devin for Pilot Programs

## Overview

**PRIMARY PURPOSE: This playbook is designed for the beginning of a pilot program to showcase the power of Devin AI to commercial enterprise customers and executive stakeholders.**

This comprehensive guide provides everything you need to build executive-level demo web UIs that demonstrate Devin's capabilities in modernizing legacy systems, ensuring compliance with industry standards, and accelerating development velocity. Use this playbook when starting a Devin pilot to create compelling, data-driven demonstrations that prove Devin's value to decision-makers.

## When to Use This Playbook

### Primary Use Case: Devin Pilot Kickoff
- **Starting a Devin pilot program** with a commercial enterprise customer
- **Demonstrating Devin's capabilities** before full-scale adoption
- **Building proof-of-concept demos** that showcase AI-assisted development
- **Creating executive presentations** that justify Devin investment
- **Establishing baseline metrics** for pilot success measurement

### Additional Use Cases
- Creating executive presentations for strategic enterprise customers (Fortune 500, technology companies, financial services, etc.)
- Demonstrating compliance with industry standards (SOC 2, ISO 27001, PCI DSS, etc.)
- Showcasing AI-assisted modernization efforts
- Presenting complex technical initiatives to non-technical stakeholders
- Building comprehensive demos that map requirements → controls → tasks → implementation

## Why This Matters for Devin Pilots

When starting a Devin pilot, you need to quickly demonstrate tangible value to secure buy-in and continued investment. This playbook helps you:

1. **Prove Devin's Speed** - Show how Devin analyzed a legacy system, identified compliance gaps, and created comprehensive implementation plans in hours instead of weeks
2. **Demonstrate Thoroughness** - Showcase 100% coverage of compliance requirements with detailed traceability
3. **Highlight Quality** - Display concrete examples of security issues identified and fixed automatically
4. **Quantify Impact** - Present metrics like "29 parallelizable tasks" and "8 compliance gaps closed" that prove ROI
5. **Build Confidence** - Provide executive-friendly visualizations that make complex technical work understandable

**The demo you build using this playbook becomes the foundation for pilot success measurement and expansion justification.**

## Core Principles

### 1. Executive-Friendly Language
- **Avoid technical jargon** - Use plain language that business executives understand
- **Focus on business outcomes** - Explain "what this prevents" not "how it works"
- **Use concrete examples** - "Prevents 99.9% of account takeover attacks" vs "Implements MFA"
- **Emphasize risk reduction** - Frame security controls as risk mitigation

**Example Transformations:**
- ❌ "Implements PBKDF2 with salting for credential storage"
- ✅ "Protects passwords with industry-standard encryption that prevents theft even if databases are compromised"

- ❌ "Mutual TLS with X.509 certificate validation"
- ✅ "Services prove their identity to each other using encryption, preventing fake services from stealing data"

### 2. Visual Hierarchy
- **Dashboard metrics** at the top (100% compliance, task counts, time savings)
- **Visual diagrams** for architecture (current vs. future state)
- **Interactive charts** for data visualization (pie charts, bar charts, timelines)
- **Color coding** for risk levels (Critical=Red, High=Orange, Medium=Green)
- **Progressive disclosure** - Summary first, details on demand

### 3. Comprehensive Traceability
- Map business requirements → compliance controls → implementation tasks → acceptance criteria
- Provide direct links to Jira tasks, GitHub PRs, documentation
- Show dependency relationships between tasks
- Document audit trail for compliance verification

## Technology Stack

### Frontend Framework
```json
{
  "framework": "React + Vite + TypeScript",
  "styling": "Tailwind CSS",
  "components": "shadcn/ui",
  "charts": "Recharts",
  "icons": "Lucide React"
}
```

### Why This Stack?
- **React + Vite** - Fast development, modern tooling, excellent developer experience
- **TypeScript** - Type safety prevents runtime errors, better IDE support
- **Tailwind CSS** - Rapid styling, consistent design system, responsive by default
- **shadcn/ui** - High-quality, accessible components that can be customized
- **Recharts** - Declarative charts built on D3, easy to customize
- **Lucide React** - Consistent icon set with excellent coverage

### Project Structure
```
demo-project/
├── src/
│   ├── components/
│   │   ├── ui/              # shadcn/ui components (Button, Card, etc.)
│   │   ├── sections/        # Demo sections (ExecutiveSummary, ComplianceOverview, etc.)
│   │   └── AccessGate.tsx   # 4-digit access code gate
│   ├── data/                # JSON data files
│   │   ├── epic-tasks.json
│   │   ├── compliance-controls.json
│   │   ├── security-standards.json
│   │   ├── sonarqube-remediation.json
│   │   └── devin-impact.json
│   ├── App.tsx              # Main app with navigation
│   └── main.tsx             # Entry point with AccessGate wrapper
├── .env.production          # Access code configuration
├── package.json
├── tsconfig.json
├── vite.config.ts
└── tailwind.config.js
```

## Section Templates

### 1. Executive Summary Section

**Purpose:** High-level overview with key metrics and value proposition

**Components:**
- **Hero metrics cards** - 4 key metrics (compliance %, total tasks, parallelizable tasks, issues fixed)
- **Value proposition** - Why modernization? Why AI-assisted? How compliance anchors trust?
- **Compliance visualization** - Pie chart showing control distribution by risk level
- **Key achievements** - Top 6-8 concrete wins with checkmarks
- **Modernization overview** - Current state vs. target state comparison

**Data Structure:**
```json
{
  "summary": {
    "totalTasks": 36,
    "tasksWithNoDependencies": 29,
    "complianceControlsCovered": 12,
    "coveragePercentage": 100
  }
}
```

**Key Visualizations:**
- Metric cards with icons (Shield, CheckCircle, TrendingUp, AlertTriangle)
- Pie chart for control distribution by risk level
- Grid layout for achievements with checkmarks

### 2. Compliance Section (SOC 2 / ISO 27001 / PCI DSS)

**Purpose:** Demonstrate 100% coverage of compliance requirements

**Components:**
- **Coverage dashboard** - Pie chart showing control distribution
- **Control cards** - One card per control with:
  - Control ID and name (e.g., "CC6.1: Logical Access Controls")
  - Risk level badge (Critical/High/Medium)
  - Executive summary (business-friendly explanation)
  - Business outcome (what this prevents)
  - Mapped tasks with Jira links
  - Coverage status (complete/partial/pending)

**Data Structure:**
```json
{
  "controls": [
    {
      "id": "CC6.1",
      "name": "Logical Access Controls",
      "executiveSummary": "Ensures every person accessing the system proves their identity through multiple factors...",
      "businessOutcome": "Protects against the most common attack vector (stolen passwords)...",
      "riskLevel": "Critical",
      "tasks": ["MBA-743", "MBA-714"],
      "coverage": "complete"
    }
  ]
}
```

**Key Visualizations:**
- Pie chart for risk level distribution
- Bar chart for tasks per control
- Color-coded risk badges (red/orange/green)
- Checkmarks for complete coverage

### 3. Architecture Diagrams Section

**Purpose:** Visual comparison of current state vs. future state

**Components:**
- **Current state diagram** - Legacy architecture with red/warning styling
  - Show monolithic structure
  - Highlight missing security controls (❌ No Authentication, ❌ No Audit Logging)
  - Use simple icons and flow arrows
  
- **Future state diagram** - Modern architecture with green/success styling
  - Show microservices structure
  - Highlight security controls (✅ MFA, ✅ Audit Logging, ✅ mTLS)
  - Layer diagram: Users → Web UI → API Gateway → Auth Service → Business Services → Storage
  
- **Security architecture details** - Authentication flow, service-to-service communication, data protection

**Design Patterns:**
- Use Card components with colored borders (red for current, green for future)
- Use Lucide icons (Users, Server, Database, Shield, Lock, Globe)
- Use ArrowRight icons to show flow
- Grid layouts for service components
- Color-coded callout boxes for security features

**Example Implementation:**
```tsx
<Card className="p-6 border-red-200 bg-red-50">
  <h2>Current Legacy Architecture</h2>
  <div className="space-y-4">
    {currentState.components.map((component) => (
      <div className="bg-white rounded-lg p-4 border border-red-200">
        <Icon className="w-5 h-5" />
        <h3>{component.name}</h3>
        <ul>
          {component.issues.map((issue) => (
            <li><X className="text-red-600" /> {issue}</li>
          ))}
        </ul>
      </div>
    ))}
  </div>
</Card>
```

### 4. Traceability Matrix Section

**Purpose:** Complete audit trail from requirements to implementation

**Components:**
- **Control cards** with progressive disclosure:
  - Requirement (what must be done)
  - Business outcome (why it matters)
  - Mapped tasks (how it's implemented) with Jira links
  - Risk reduction (impact of control)

**Data Structure:**
```json
{
  "control": {
    "id": "IA-2",
    "requirement": "Executive summary of requirement",
    "businessOutcome": "Business impact explanation",
    "tasks": ["MBA-743", "MBA-714"],
    "riskLevel": "Critical"
  }
}
```

**Key Features:**
- Color-coded by risk level
- Direct links to Jira tasks
- Expandable sections for details
- Audit-ready documentation

### 5. EPIC & Tasks Explorer Section

**Purpose:** Detailed breakdown of all implementation tasks

**Components:**
- **EPIC summary card** - Title, description, link to Jira
- **Summary metrics** - Total tasks, parallelizable tasks, compliance controls covered
- **Part cards** - Grouped by logical parts (Authentication, CLI Refactoring, Web Services, etc.)
  - Show/hide tasks button
  - Task count per part
  - Expandable task list
  
- **Task cards** - Individual task details:
  - Task key with Jira link
  - Task title
  - Compliance controls mapped (badges)
  - Dependencies (if any)
  - Security issues (if any)
  - No dependencies indicator (checkmark)

**Data Structure:**
```json
{
  "epic": {
    "key": "MBA-713",
    "title": "Modernize Legacy Application to Microservices",
    "url": "https://cog-gtm.atlassian.net/browse/MBA-713"
  },
  "parts": [
    {
      "id": 1,
      "name": "Authentication Service Module",
      "description": "Create authentication service implementing SOC 2 controls",
      "tasks": [
        {
          "key": "MBA-714",
          "title": "Create AuthenticationService Module Structure",
          "complianceControls": ["CC6.1"],
          "dependencies": [],
          "url": "https://cog-gtm.atlassian.net/browse/MBA-714"
        }
      ]
    }
  ]
}
```

**Key Features:**
- Collapsible sections for each part
- Color-coded badges for compliance controls
- Dependency indicators
- Security issue callouts
- Direct Jira links

### 6. AI Impact Section (Devin in Action)

**Purpose:** Showcase AI contributions and concrete wins

**Components:**
- **Impact metrics** - Tasks created, tasks enhanced, security issues fixed, coverage %
- **Timeline** - Chronological phases of AI contributions:
  - Initial Analysis
  - Gap Detection
  - Task Creation
  - Acceptance Criteria Enhancement
  - Security Issue Remediation
  - Traceability and Documentation
  
- **Concrete wins** - Specific achievements with category badges:
  - Coverage (12/12 compliance controls mapped)
  - Gap Closure (8 new compliance tasks)
  - Security (critical token storage issue fixed)
  - Quality (enhanced acceptance criteria)
  - Velocity (29 parallelizable tasks)
  
- **Rigor maintenance** - How AI maintains quality and thoroughness

**Data Structure:**
```json
{
  "timeline": [
    {
      "phase": "Initial Analysis",
      "date": "November 7, 2025",
      "activities": [
        "Analyzed legacy application repository structure",
        "Reviewed SOC 2 security standards document"
      ]
    }
  ],
  "concreteWins": [
    {
      "category": "Coverage",
      "achievement": "12/12 Security Controls Mapped",
      "description": "Achieved 100% coverage...",
      "impact": "Ensures enterprise compliance requirements are fully addressed"
    }
  ],
  "metrics": {
    "tasksCreated": 8,
    "tasksEnhanced": 4,
    "securityIssuesIdentified": 1,
    "coveragePercentage": 100
  }
}
```

### 7. Automated Remediation Section (SonarQube/Snyk)

**Purpose:** Demonstrate automated code quality and security fixes

**Components:**
- **Overview card** - Title, description, workflow link
- **Metrics cards** - Avg remediation time, success rate, issues fixed per session, time saved
- **Workflow steps** - Step-by-step process:
  1. Code scan (SonarQube/Snyk)
  2. Issue detection
  3. Changed files analysis
  4. AI session trigger
  5. Automated remediation
  6. PR comment
  7. Re-scan verification
  
- **Issue types** - Bugs, Vulnerabilities, Code Smells, Security Hotspots
  - Examples for each type
  - AI approach for each type
  
- **Business benefits** - Velocity, Quality, Security, Developer Experience, Compliance, Learning

**Data Structure:**
```json
{
  "overview": {
    "title": "Automated SonarQube Remediation with Devin AI",
    "workflowUrl": "https://github.com/ORG/REPO/blob/master/.github/workflows/sonarcloud.yml"
  },
  "workflow": {
    "steps": [
      {
        "step": 1,
        "name": "SonarCloud Scan",
        "description": "GitHub Actions triggers scan",
        "technology": "SonarCloud",
        "output": "Code quality and security issues report"
      }
    ]
  },
  "metrics": {
    "averageRemediationTime": "5-15 minutes",
    "successRate": "95%+",
    "issuesFixedPerSession": "5-20"
  }
}
```

### 8. RFI/Requirements Alignment Section

**Purpose:** Map modernization to customer requirements

**Components:**
- **RFI context card** - Explain the RFI and how modernization aligns
- **Theme cards** - One card per RFI theme:
  - Theme name and description
  - Mapped tasks with Jira links
  - Outcomes (bullet list of achievements)
  
**Common RFI Themes:**
- Data Privacy and Protection
- Secure Cloud Boundary
- Stakeholder Engagement
- Transparency and Accountability
- IT Infrastructure Modernization
- Model Evaluation and Quality Assurance

## Design System

### Color Palette
```css
/* Risk Levels */
--critical: #ef4444 (red-600)
--high: #f59e0b (orange-500)
--medium: #10b981 (green-500)

/* Status */
--success: #10b981 (green-500)
--warning: #f59e0b (orange-500)
--error: #ef4444 (red-600)
--info: #3b82f6 (blue-600)

/* Backgrounds */
--bg-primary: #f8fafc (slate-50)
--bg-secondary: #ffffff (white)
--bg-accent: gradient from blue-50 to purple-50
```

### Typography
```css
/* Headings */
h1: text-3xl font-bold (30px)
h2: text-2xl font-semibold (24px)
h3: text-xl font-semibold (20px)
h4: text-lg font-medium (18px)

/* Body */
body: text-base (16px)
small: text-sm (14px)
tiny: text-xs (12px)

/* Colors */
heading: text-slate-900
body: text-slate-700
muted: text-slate-600
subtle: text-slate-500
```

### Spacing
```css
/* Consistent spacing scale */
gap-2: 0.5rem (8px)
gap-4: 1rem (16px)
gap-6: 1.5rem (24px)
gap-8: 2rem (32px)

/* Section spacing */
space-y-8: 2rem between sections
space-y-6: 1.5rem between subsections
space-y-4: 1rem between elements
```

### Components

#### Metric Card
```tsx
<Card className="p-6">
  <div className="flex items-center gap-4">
    <div className="p-3 bg-green-100 rounded-lg">
      <Shield className="w-8 h-8 text-green-600" />
    </div>
    <div>
      <p className="text-sm text-slate-600">SOC 2 Security Coverage</p>
      <p className="text-3xl font-bold text-slate-900">100%</p>
      <p className="text-xs text-green-600 mt-1">12/12 Controls</p>
    </div>
  </div>
</Card>
```

#### Risk Badge
```tsx
<span className={`px-3 py-1 rounded-full text-xs font-medium ${
  riskLevel === 'Critical' ? 'bg-red-100 text-red-700' :
  riskLevel === 'High' ? 'bg-orange-100 text-orange-700' :
  'bg-green-100 text-green-700'
}`}>
  {riskLevel} Risk
</span>
```

#### Control Card
```tsx
<Card className="p-6">
  <div className="flex items-start gap-4">
    <div className="p-3 bg-red-100 rounded-lg">
      <Shield className="w-6 h-6 text-red-600" />
    </div>
    <div className="flex-1">
      <h3 className="text-xl font-semibold text-slate-900 mb-2">
        CC6.1: Logical Access Controls
      </h3>
      <div className="space-y-3">
        <div>
          <p className="text-sm font-medium text-slate-700 mb-1">Executive Summary</p>
          <p className="text-sm text-slate-600">Business-friendly explanation...</p>
        </div>
        <div>
          <p className="text-sm font-medium text-slate-700 mb-1">Business Outcome</p>
          <p className="text-sm text-slate-600">What this prevents...</p>
        </div>
      </div>
    </div>
  </div>
</Card>
```

## Data Collection Process

### 1. Gather Source Materials
- **Jira EPIC and tasks** - Export task list with keys, titles, descriptions, dependencies
- **Compliance standards** - SOC 2, ISO 27001, PCI DSS requirements
- **RFI/Requirements documents** - Customer requirements, technical specifications
- **Architecture diagrams** - Current state and future state architecture
- **GitHub repositories** - PR links, workflow files, code examples
- **Metrics** - Task counts, coverage percentages, time savings

### 2. Create Data Files

#### epic-tasks.json
```json
{
  "epic": {
    "key": "MBA-713",
    "title": "EPIC Title",
    "summary": "Brief summary",
    "url": "Jira URL"
  },
  "parts": [
    {
      "id": 1,
      "name": "Part Name",
      "description": "Part description",
      "tasks": [
        {
          "key": "MBA-714",
          "title": "Task title",
          "complianceControls": ["CC6.1"],
          "dependencies": [],
          "url": "Jira URL"
        }
      ]
    }
  ],
  "summary": {
    "totalTasks": 36,
    "tasksWithNoDependencies": 29,
    "complianceControlsCovered": 12
  }
}
```

#### compliance-controls.json
```json
{
  "controls": [
    {
      "id": "CC6.1",
      "name": "Logical Access Controls",
      "executiveSummary": "Executive-friendly explanation",
      "businessOutcome": "What this prevents",
      "riskLevel": "Critical",
      "tasks": ["MBA-743", "MBA-714"],
      "coverage": "complete"
    }
  ],
  "summary": {
    "totalControls": 12,
    "coveredControls": 12,
    "coveragePercentage": 100,
    "criticalControls": 3,
    "highControls": 6,
    "mediumControls": 3
  }
}
```

### 3. Map Relationships
- **Control → Tasks** - Which tasks implement each control?
- **Task → Controls** - Which controls does each task address?
- **Task → Dependencies** - Which tasks depend on others?
- **RFI Theme → Tasks** - Which tasks address each RFI requirement?

## Building Process

### 1. Setup Project
```bash
# Create Vite + React + TypeScript project with Tailwind and shadcn/ui pre-installed
create_react_app demo-project
cd demo-project

# This automatically installs:
# - React + Vite + TypeScript
# - Tailwind CSS (configured)
# - shadcn/ui components (50+ pre-installed in src/components/ui/)
# - Lucide React icons
# - Recharts for data visualization
```

### 2. Create Data Files
- Place all JSON data files in `src/data/`
- Ensure proper TypeScript types for data structures

### 3. Create Access Gate Component

Create `src/components/AccessGate.tsx`:

```tsx
import { useEffect, useState } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Lock } from 'lucide-react';

const ACCESS_CODE = (import.meta.env.VITE_ACCESS_CODE as string) || '1234';

export default function AccessGate({ children }: { children: React.ReactNode }) {
  const [granted, setGranted] = useState(false);
  const [code, setCode] = useState('');
  const [err, setErr] = useState('');

  useEffect(() => {
    // Check if already authenticated in this session
    if (sessionStorage.getItem('access-ok') === '1') {
      setGranted(true);
      return;
    }
    
    // Check for query param ?code=####
    const url = new URL(window.location.href);
    const qp = url.searchParams.get('code');
    if (qp && qp === ACCESS_CODE) {
      sessionStorage.setItem('access-ok', '1');
      setGranted(true);
    }
  }, []);

  if (granted) return <>{children}</>;

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (code.trim() === ACCESS_CODE) {
      sessionStorage.setItem('access-ok', '1');
      setGranted(true);
      setErr('');
    } else {
      setErr('Incorrect code. Please try again.');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 p-6">
      <Card className="p-8 w-full max-w-md">
        <div className="flex items-center gap-3 mb-6">
          <div className="p-3 bg-blue-100 rounded-lg">
            <Lock className="w-6 h-6 text-blue-600" />
          </div>
          <div>
            <h1 className="text-xl font-semibold text-slate-900">Pilot Demo Access</h1>
            <p className="text-sm text-slate-600">Enter your 4-digit access code</p>
          </div>
        </div>
        
        <form onSubmit={submit} className="space-y-4">
          <div>
            <input
              type="password"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={4}
              className="w-full border border-slate-300 rounded-lg px-4 py-3 text-center text-2xl tracking-widest focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="••••"
              value={code}
              onChange={(e) => {
                setCode(e.target.value);
                setErr('');
              }}
              autoFocus
            />
          </div>
          
          {err && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-sm text-red-600">{err}</p>
            </div>
          )}
          
          <Button type="submit" className="w-full" disabled={code.length !== 4}>
            Continue to Demo
          </Button>
        </form>
        
        <div className="mt-6 pt-6 border-t border-slate-200">
          <p className="text-xs text-slate-500 text-center">
            This is a password-protected pilot demonstration.
            <br />
            Contact your Devin representative for access.
          </p>
        </div>
      </Card>
    </div>
  );
}
```

**Key Features:**
- 4-digit numeric input with password masking
- sessionStorage persistence (code required once per browser session)
- Query param support: `?code=####` auto-unlocks for easy sharing
- Error handling for incorrect codes
- Clean, professional UI matching demo design system
- Disabled button until 4 digits entered

### 4. Configure Access Code

Create `.env.production` in project root:

```
VITE_ACCESS_CODE=8421
```

**Best Practices:**
- Use a unique 4-digit code for each pilot demo
- Rotate codes after each demo completion
- Document which codes were used for which customers
- Use different codes for different stakeholders

### 5. Wrap App in Access Gate

Update `src/main.tsx`:

```tsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import AccessGate from './components/AccessGate'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AccessGate>
      <App />
    </AccessGate>
  </StrictMode>,
)
```

### 6. Add Search Engine Protection

Update `index.html` to prevent indexing:

```html
<head>
  <meta charset="UTF-8" />
  <link rel="icon" type="image/svg+xml" href="/vite.svg" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <meta name="robots" content="noindex,nofollow" />
  <title>Your Project Name - Pilot Demo</title>
</head>
```

### 7. Build Section Components
- Create one component per section in `src/components/sections/`
- Each component receives data from JSON files
- Use shadcn/ui components for consistency
- Follow design system for colors, typography, spacing

### 8. Build Main App
- Create navigation sidebar with all sections
- Implement section routing/switching
- Add header with project title and compliance badge
- Add footer with deployment information

### 9. Test Locally
```bash
npm run dev
# Open http://localhost:5173
# Test access gate with correct and incorrect codes
# Test all sections and navigation
# Verify all data displays correctly
# Check responsive design
```

### 10. Build Production Version
```bash
npm run build
# Creates optimized build in dist/ folder
# Includes access gate with configured code
# All assets optimized and minified
```

### 11. Deploy to Public URL

Use the deploy tool to publish to a public staging URL:

```bash
# Deploy the dist folder to get a public URL
deploy --command frontend --dir /path/to/project/dist
```

This returns a public URL like: `https://your-demo-name.devinapps.com`

**Deployment automatically provides:**
- Public HTTPS URL accessible from anywhere
- No VPN or network configuration required
- Works on all devices (desktop, mobile, tablet)
- Fast global CDN delivery
- Automatic SSL certificate

### 12. Test Deployed Demo

**Test Checklist:**

- [ ] Open demo URL in incognito/private window
- [ ] Verify access gate appears (not the main demo)
- [ ] Test incorrect code (e.g., 1234) - should show error
- [ ] Test correct code (e.g., 8421) - should grant access
- [ ] Verify all sections load and display correctly
- [ ] Test navigation between sections
- [ ] Test charts and visualizations render properly
- [ ] Refresh page - should not ask for code again (sessionStorage)
- [ ] Close browser and reopen - should ask for code again
- [ ] Test query param: `?code=8421` - should bypass gate
- [ ] Test on mobile device
- [ ] Test on different browsers (Chrome, Firefox, Safari, Edge)

## Deployment Checklist

Use this checklist for every pilot demo deployment:

- [ ] Build production version: `npm run build`
- [ ] Generate unique 4-digit access code (different from previous demos)
- [ ] Update `.env.production` with new access code
- [ ] Rebuild with new code: `npm run build`
- [ ] Deploy dist folder to get public URL
- [ ] Test access gate in incognito window
- [ ] Verify incorrect code is rejected
- [ ] Verify correct code grants access
- [ ] Test all sections and navigation
- [ ] Test on mobile device
- [ ] Document access instructions for stakeholders
- [ ] Prepare credential sharing method (encrypted message, password manager)
- [ ] Plan to rotate credentials after demo completion
- [ ] Set calendar reminder to take down demo after pilot ends

## Sharing with Stakeholders

### Access Instructions Template

```
Subject: Devin AI Pilot Demo - [Project Name]

Hello [Stakeholder Name],

Thank you for your interest in the Devin AI pilot program. I've prepared an executive demonstration showcasing our AI-assisted [enterprise modernization / compliance / development] capabilities.

Demo URL: https://your-demo-name.devinapps.com
Access Code: 8421

Instructions:
1. Click the demo URL above
2. Enter the 4-digit access code when prompted
3. Explore the sections using the sidebar navigation

The demo is accessible from any device with a modern web browser. No VPN or special software required.

Key Highlights:
- [Highlight 1: e.g., "100% SOC 2 compliance coverage"]
- [Highlight 2: e.g., "29 parallelizable implementation tasks"]
- [Highlight 3: e.g., "Critical security vulnerability identified and fixed"]

Please let me know if you have any questions or would like to schedule a walkthrough.

Best regards,
[Your Name]
Devin AI Team
```

### Quick Share Link (Trusted Recipients Only)

For trusted recipients, you can share a direct link with the code embedded:

```
https://your-demo-name.devinapps.com?code=8421
```

This bypasses the access gate and loads the demo immediately.

**Security Warning**: Only share this link with trusted recipients via secure channels (encrypted email, password manager, in-person).

## Security Best Practices

### What This Protects Against

✅ **Casual discovery**: Random internet users cannot access the demo without the code
✅ **Search engine indexing**: noindex meta tag prevents Google/Bing from indexing
✅ **Accidental sharing**: Code requirement prevents unauthorized access from URL alone
✅ **Session hijacking**: sessionStorage clears when browser closes

### What This Does NOT Protect Against

❌ **Determined attackers**: Code is visible in compiled JavaScript (client-side only)
❌ **Code sharing**: Anyone with the code can access the demo
❌ **Brute force**: 4-digit codes have only 10,000 combinations (acceptable for time-limited demos)
❌ **Data exfiltration**: No rate limiting or audit logging (use deployment analytics)

### Security Guidelines

1. **Rotate codes frequently**: Change the code after each pilot demo
2. **Use unique codes**: Different code for each customer/demo
3. **Share securely**: Use encrypted channels to share codes (never plain email)
4. **Monitor access**: Check deployment analytics for unusual traffic patterns
5. **Time-limit demos**: Take down demos after pilot completion
6. **Document access**: Keep records of who received codes and when
7. **Use demo data only**: Never use real customer data or sensitive information
8. **Review before sharing**: Always test the demo before sharing with stakeholders

### When to Use This Approach

✅ **Pilot demos**: Perfect for executive presentations and proof-of-concept
✅ **Internal reviews**: Good for stakeholder previews and feedback sessions
✅ **Time-limited access**: Suitable for demos with defined end dates
✅ **Non-sensitive data**: Appropriate for demo data and sample content
✅ **Remote stakeholders**: Ideal when VPN access is not feasible

### When NOT to Use This Approach

❌ **Production applications**: Use proper authentication (OAuth, SAML, SSO)
❌ **Sensitive data**: Use backend authentication with encryption
❌ **Long-term access**: Use account-based authentication with user management
❌ **Compliance requirements**: Use auditable authentication systems with logging
❌ **Public demos**: Use no authentication or proper user registration

## Best Practices

### Executive Communication
1. **Lead with business value** - Start with "what this means for the organization"
2. **Use analogies** - "Like a security guard checking ID at the door" for authentication
3. **Quantify impact** - "Prevents 99.9% of attacks" vs "Very secure"
4. **Show progress** - "100% coverage" vs "All controls implemented"
5. **Highlight wins** - "8 compliance gaps closed" vs "Added some tasks"

### Visual Design
1. **Consistent spacing** - Use Tailwind's spacing scale (4, 6, 8)
2. **Color coding** - Red=Critical, Orange=High, Green=Medium/Success
3. **Icon usage** - Use Lucide icons consistently throughout
4. **White space** - Don't overcrowd, let content breathe
5. **Responsive design** - Use grid layouts that adapt to screen size

### Data Organization
1. **Separate data from UI** - Keep JSON files in `src/data/`
2. **Type safety** - Define TypeScript interfaces for all data structures
3. **Single source of truth** - Don't duplicate data across files
4. **Computed values** - Calculate summaries from base data
5. **Validation** - Ensure all links and references are valid

### Performance
1. **Code splitting** - Use dynamic imports for large sections (if needed)
2. **Image optimization** - Use SVG for icons, optimize PNGs
3. **Lazy loading** - Load sections on demand (if needed)
4. **Memoization** - Use React.memo for expensive components (if needed)
5. **Bundle size** - Keep under 500KB when possible (Recharts adds ~165KB gzipped)

## Customization Guide

### Adapting for Different Compliance Frameworks

#### SOC 2
- Focus on Trust Services Criteria (Security, Availability, Confidentiality, Processing Integrity, Privacy)
- Add control categories (CC1-CC9)
- Include audit readiness status
- Add continuous monitoring section

#### ISO 27001
- Map to ISO 27001 Annex A controls
- Add risk assessment section
- Include Statement of Applicability (SoA)
- Add audit findings section

#### PCI DSS
- Focus on 12 PCI DSS requirements
- Add cardholder data environment (CDE) scope
- Include quarterly scan status
- Add compensating controls section

### Adapting for Different Audiences

#### Technical Leadership (CTO, VP Engineering)
- Add more technical details in expandable sections
- Include architecture diagrams with technical specifics
- Add code examples and implementation details
- Include performance metrics and benchmarks

#### Business Executives (CEO, CFO)
- Focus on ROI and cost savings
- Emphasize risk reduction and compliance
- Use more analogies and less jargon
- Include timeline and resource requirements

#### Auditors and Compliance Officers
- Emphasize traceability matrix
- Include detailed acceptance criteria
- Add evidence collection section
- Include audit trail documentation

## Troubleshooting

### Common Issues

#### Access Gate Not Appearing

**Symptom**: Demo loads immediately without asking for code

**Solutions:**
1. Check that AccessGate is imported in main.tsx
2. Verify App is wrapped in `<AccessGate>` component
3. Clear browser cache and try again
4. Check browser console for JavaScript errors
5. Verify build includes the AccessGate component

#### Incorrect Code Accepted

**Symptom**: Wrong code grants access to demo

**Solutions:**
1. Check `.env.production` file has correct VITE_ACCESS_CODE
2. Rebuild the app: `npm run build`
3. Redeploy the dist folder
4. Clear sessionStorage: Open browser console, run `sessionStorage.clear()`
5. Test in incognito window to avoid cached sessions

#### Code Not Persisting

**Symptom**: Code required on every page refresh

**Solutions:**
1. Check browser allows sessionStorage (not in strict privacy mode)
2. Verify sessionStorage.setItem is called in AccessGate component
3. Check browser console for storage errors
4. Try different browser to rule out browser-specific issues

#### Demo Not Loading After Correct Code

**Symptom**: Correct code entered but demo doesn't appear

**Solutions:**
1. Check browser console for JavaScript errors
2. Verify all components are properly imported
3. Check that `if (granted) return <>{children}</>` is present in AccessGate
4. Try clearing sessionStorage and re-entering code
5. Verify all data files are present in dist folder

#### Build Errors

**Symptom**: `npm run build` fails with errors

**Solutions:**
1. Check TypeScript errors - ensure all imports have proper types
2. Run `npm install` to ensure all packages are installed
3. Check that all data files are valid JSON
4. Verify all component imports are correct
5. Check vite.config.ts for proper configuration

#### Deployment Issues

**Symptom**: Deploy command fails or returns error

**Solutions:**
1. Verify dist folder exists and contains built files
2. Check that build completed successfully
3. Ensure you have proper permissions for deployment
4. Try deploying again (temporary network issues)
5. Check deployment logs for specific error messages

## Example Use Cases

### Use Case 1: Enterprise Application Modernization
**Scenario:** Modernizing legacy system for Fortune 500 company
**Focus:** SOC 2, ISO 27001, PCI DSS compliance
**Sections:** Executive Summary, Compliance Overview, Security Standards Alignment, Architecture Diagrams, Traceability Matrix
**Access Code:** 7531 (unique for this customer)

### Use Case 2: AI-Assisted Development Showcase
**Scenario:** Demonstrating AI capabilities to potential enterprise customers
**Focus:** Devin's contributions, automated remediation, velocity improvements
**Sections:** Executive Summary, Devin in Action, SonarQube Remediation, EPIC Explorer
**Access Code:** 9246 (unique for this customer)

### Use Case 3: Technical Debt Reduction
**Scenario:** Presenting technical debt reduction initiative to executive leadership
**Focus:** Before/after comparison, code quality improvements, risk reduction
**Sections:** Executive Summary, Architecture Diagrams, Code Quality Metrics, Implementation Roadmap
**Access Code:** 4183 (unique for this customer)

## Maintenance and Updates

### Keeping Demo Current
1. **Update metrics** - Refresh task counts, coverage percentages as work progresses
2. **Add new sections** - Create new sections for additional features or requirements
3. **Update links** - Ensure all Jira/GitHub links remain valid
4. **Refresh screenshots** - Update architecture diagrams as design evolves
5. **Add new wins** - Document new achievements and milestones
6. **Rotate access codes** - Change codes after each demo or monthly

### Version Control
1. **Tag releases** - Create git tags for each presentation version
2. **Document changes** - Maintain changelog of updates
3. **Archive old versions** - Keep historical versions for reference
4. **Branch strategy** - Use branches for different audiences or variations

## Pilot Success Metrics to Showcase

When building your demo for a Devin pilot, make sure to highlight these key metrics that demonstrate value:

### Speed Metrics
- **Time to Analysis** - "Analyzed 10,000+ lines of legacy code in 2 hours"
- **Time to Plan** - "Created 36 detailed implementation tasks in 4 hours"
- **Time to Remediation** - "Fixed critical security issue in 15 minutes"
- **Comparison** - "10-100x faster than manual analysis"

### Quality Metrics
- **Compliance Coverage** - "100% coverage of SOC 2 security controls"
- **Gap Detection** - "Identified 8 missing compliance requirements"
- **Security Issues** - "Detected and fixed 1 critical security vulnerability"
- **Acceptance Criteria** - "Enhanced 4 tasks with detailed, testable criteria"

### Velocity Metrics
- **Parallelization** - "29 of 36 tasks (80%) can run in parallel"
- **Dependencies** - "Optimized task ordering to minimize blocking"
- **Throughput** - "Multiple Devin sessions working simultaneously"

### Business Impact Metrics
- **Risk Reduction** - "Prevents 99.9% of account takeover attacks"
- **Compliance Readiness** - "Audit-ready documentation from day one"
- **Cost Savings** - "2-4 hours saved per PR with automated remediation"
- **Time to Market** - "Accelerates product delivery and deployment timelines"

## Pilot Expansion Strategy

After successfully demonstrating Devin's capabilities with this demo:

1. **Measure Baseline** - Use the demo metrics as baseline for pilot success
2. **Expand Scope** - Apply Devin to additional repositories or compliance frameworks
3. **Scale Teams** - Enable multiple teams to use Devin in parallel
4. **Integrate Workflows** - Embed Devin into CI/CD pipelines (SonarQube, Snyk, etc.)
5. **Track ROI** - Compare pilot metrics to traditional development approaches

## Conclusion

**This playbook is your starting point for demonstrating Devin's power at the beginning of a pilot program.** By following these guidelines, you can create professional, data-driven demos that prove Devin's value to enterprise executives, technical leadership, and decision-makers.

The demo you build becomes:
- **Proof of concept** for Devin's capabilities
- **Baseline metrics** for pilot success measurement
- **Justification** for expanded Devin adoption
- **Template** for future modernization efforts
- **Evidence** of ROI for continued investment

Key takeaways:
- **Executive-friendly language** is critical for stakeholder buy-in
- **Visual design** should prioritize clarity and consistency
- **Comprehensive traceability** builds trust and enables audits
- **Data-driven storytelling** makes abstract concepts concrete
- **Reusable components** accelerate future demo development
- **Pilot metrics** prove Devin's value and justify expansion
- **Public URL with access code** provides simple, secure access for stakeholders

**Remember: This demo is not just a presentation—it's the foundation for your Devin pilot's success and the key to unlocking full-scale adoption.**

For questions or improvements to this playbook, contact the Devin AI team or submit a pull request to the playbook repository.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
