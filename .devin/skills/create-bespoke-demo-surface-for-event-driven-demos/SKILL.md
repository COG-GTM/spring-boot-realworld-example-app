---
name: create-bespoke-demo-surface-for-event-driven-demos
description: "Converted from Devin playbook: Create Bespoke Demo Surface for Event Driven Demos"
triggers:
  - user
  - model
---

# Create Bespoke Demo Surface for Event Driven Demos

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Create Bespoke Demo Surface for Event Driven Demos (playbook-50252f7a0db9420b80d4cd81ea70639a), macro `!create-demo-page`

## Procedure

# Create Bespoke Demo Surface

## Overview

Creates a new customer-specific, hidden demo vertical in the [COG-GTM/event-driven-devin](https://github.com/COG-GTM/event-driven-devin) application. Each demo is a fully branded frontend with backend API routes, business logic, and an intentional bug that triggers Devin's automated incident response pipeline. The demo is only accessible via its direct hex slug URL — it is never added to the hub page or the public verticals list.

## What's Needed From User

- **Customer/Brand name** and product area (e.g., "Vanguard 401(k) services")
- **Org scoping**: Use default org/user from hub page (base vertical pattern) or customer-specific API keys?

## Reference Files

Before writing any code, study these existing files in the repo:

| File | Purpose |
|------|--------|
| `app/services/verticals/banking.js` | Base vertical service pattern (default org scoping) |
| `app/services/verticals/a6b38c63.js` | Customer-specific service pattern (separate API keys) |
| `app/routes/verticals/banking.js` | Route handler pattern |
| `app/public/verticals/banking.html` | Frontend HTML pattern |
| `app/routes/verticals/index.js` | Route registration and vertical IDs |
| `config/customers.js` | Customer config registry |
| `AGENTS.md` | Full architecture documentation |

**Org scoping determines the pattern:**
- **Default org (base vertical)**: Follow `banking.js` — do NOT pass `customer:` in `createSessionAndAlert()`, do NOT add entry to `config/customers.js`
- **Customer-specific org**: Follow `a6b38c63.js` — pass `customer: '<SLUG>'` in `createSessionAndAlert()`, add entry to `config/customers.js`

<phase name="Slug Generation & Planning" id="1">
## Slug Generation & Planning

1. Generate a random 8-character hex string to use as the slug:
   ```bash
   python3 -c "import secrets; print(secrets.token_hex(4))"
   ```
2. Save the output — this is `<SLUG>` for the rest of the playbook.
3. Read the reference files listed above to understand conventions.
4. Only ever create the todo list for the current phase.

<verification>
- A unique 8-character hex slug has been generated
- All reference files have been read and understood
- The org scoping pattern (base vs customer-specific) has been determined
</verification>
</phase>

<phase name="Create Service File" id="2">
## Create Service File

Create `app/services/verticals/<SLUG>.js` with the following structure:

1. **Imports** (CommonJS `require`):
   - `uuid` (v4)
   - `../../telemetry/logger` (Winston)
   - `../../telemetry/datadog` (incrementMetric, recordTiming)
   - `../../telemetry/sentry` (Sentry)
   - `../devin-session` (createSessionAndAlert)

2. **Mock data**: Arrays/objects with realistic data for the customer's domain (e.g., fund holdings, product catalog, accounts). Use real-sounding names, tickers, IDs.

3. **Resolver function**: Returns data in a structure that has an intentional mismatch with the consumer function. Example: returns `{ targets: [...] }` when consumer expects `.allocation.equity`.

4. **Computation function**: Tries to access properties that don't exist on the resolver's return value — this is the **intentional bug** that produces a `TypeError`.

5. **Format function**: Formats output into a response payload.

6. **Main async function** (`processXxx(data)`):
   - Generate `requestId` via `uuidv4()`
   - Winston structured logging with `service: '<SLUG>-api'`
   - Simulated latency: `await new Promise(resolve => setTimeout(resolve, 70 + Math.random() * 110))`
   - Datadog metrics: `incrementMetric` for success/failure, `recordTiming` for latency
   - **catch block**: Sentry `captureException`, then `createSessionAndAlert()` with proper alert data (see below), then re-throw

7. **Exports**: `module.exports = { mainFunction, DATA_ARRAYS }`

### createSessionAndAlert() Call Pattern

```javascript
createSessionAndAlert({
  issueTitle: `${error.name}: ${error.message}`,
  issueUrl: `https://${process.env.SENTRY_ORG_SLUG || 'sentry-org'}.sentry.io/issues/...`,
  culprit: 'app/services/verticals/<SLUG>.js — <mainFunction>',
  errorType: error.name || 'Error',
  errorValue: error.message,
  devinUserId: data.devinUserId,
  devinEmail: data.devinEmail,
  devinOrgId: data.devinOrgId,
  service: '<SLUG>-api',
  verticalLabel: '<Descriptive Label>',
  // customer: '<SLUG>',  // ONLY include this line for customer-specific scoping
  tags: [
    { key: 'route', value: '/api/<SLUG>/<action>' },
    { key: 'service', value: '<SLUG>-api' },
  ],
  extra: { requestId, ...relevant_data },
  level: 'error',
  platform: 'node',
  firstSeen: '',
  lastSeen: new Date().toISOString(),
  count: '',
  shortId: '',
  project: 'event-driven-devin',
  release: process.env.SENTRY_RELEASE || '<SLUG>@1.0.0',
  environment: process.env.DD_ENV || 'prod',
  triggeredRule: '',
}).catch((err) => {
  logger.error('Failed to trigger Devin session', { error: err.message });
});
```

<verification>
- Service file created at `app/services/verticals/<SLUG>.js`
- Uses CommonJS require/module.exports (NOT ES modules)
- Contains realistic mock data for the customer's domain
- Contains an intentional property mismatch bug that produces a TypeError
- createSessionAndAlert call follows the correct scoping pattern (base or customer-specific)
- Service name is consistently `<SLUG>-api` across all references (logger, Sentry tags, alert data)
</verification>
</phase>

<phase name="Create Route Handler & Frontend" id="3">
## Create Route Handler

Create `app/routes/verticals/<SLUG>.js`:

1. Express router (`const router = express.Router()`)
2. `GET /api/<SLUG>/catalog` (or similar) — returns mock data arrays for the frontend
3. `POST /api/<SLUG>/<action>` — calls the main service function, passes `req.body` fields including `devinUserId`, `devinOrgId`, `devinEmail`. Returns result JSON or 500 error.
4. `module.exports = router;`

## Create Frontend HTML

Create `app/public/verticals/<SLUG>.html`:

1. **Branding**: Customer's visual identity (colors, logo, fonts, copy). This is the ONLY place the customer/brand name appears anywhere in the codebase.
2. **Top bar/header**: Navigation styled to match the customer brand
3. **Back link**: `<a href="/">← All Demos</a>` or similar link back to hub
4. **Data display**: Cards/tables showing mock data from the catalog endpoint
5. **Action form**: Form that triggers the POST endpoint (dropdowns, inputs, submit button)
6. **Result display**: Error and success boxes that show the API response inline
7. **JavaScript**:
   - `localStorage` check for `devinOrgId`/`devinUserId`/`devinEmail` (fetch from `/api/config` if missing)
   - Form submit handler calling `POST /api/<SLUG>/<action>` with JSON body
   - Dynamic result rendering (error in red box, success in green box)
8. **Session cap banner**: Include `<script src="/session-cap-banner.js"></script>` before closing `</body>`

### Logo Implementation

When possible, use the customer's actual public logo/icon rather than a text placeholder:

1. **Find the logo**: Check the customer's website for public CDN-hosted icons. Common paths:
   - `https://www.<domain>/content/dam/<brand>/global/logo/<brand>_apple_icon_180.png`
   - `https://www.<domain>/favicon.ico` or `/apple-touch-icon.png`
   - Check the customer's homepage `<link rel="icon">` or `<link rel="apple-touch-icon">` tags
2. **Verify hotlinkability**: `curl -s -o /dev/null -w '%{http_code}' <URL>` — must return 200
3. **Implementation**: Use an `<img>` tag with `onerror` fallback:
   ```html
   <img class="logo-mark" src="<LOGO_URL>" alt="<BRAND>" onerror="this.onerror=null;this.style.display='none';">
   ```
4. **Sizing**: Set `width: 40px; height: 40px; object-fit: contain;` in CSS for consistent sizing
5. **Position**: Place in the top-left header area, before the brand name text

**Example (TIAA):**
- Logo URL: `https://www.tiaa.org/content/dam/tiaa/global/logo/tiaa_apple_icon_180.png`
- CSS: `.logo-mark { width: 40px; height: 40px; object-fit: contain; }`

<verification>
- Route handler created at `app/routes/verticals/<SLUG>.js`
- GET endpoint returns mock data
- POST endpoint calls service function and handles errors
- Frontend HTML created at `app/public/verticals/<SLUG>.html`
- Customer/brand name appears ONLY in the HTML file, nowhere in JS files
- HTML includes session-cap-banner.js script
- JavaScript handles localStorage for devinOrgId/devinUserId/devinEmail
- Logo uses actual customer icon (img tag) if a public URL is available
</verification>
</phase>

<phase name="Register & Verify" id="4">
## Register the Vertical

Modify `app/routes/verticals/index.js`:

1. Add import: `const customer<CamelSlug>Routes = require('./<SLUG>');`
2. Add mount: `router.use(customer<CamelSlug>Routes);`
3. Add `'<SLUG>'` to the `verticalIds` array so the HTML page is served at `/<SLUG>`
4. Do **NOT** add an entry to the `VERTICALS` array — this keeps it hidden from `/api/verticals`

If using **customer-specific** org scoping, add to `config/customers.js`:
```javascript
'<SLUG>': {
  label: 'Customer <FIRST4>',
  triggerMode: 'api',
},
```

Do **NOT** add any card to `app/public/hub.html`. The demo must remain hidden.

## Verify Locally

1. Run `npm run lint` — must pass with 0 errors
2. Run `npm start` and navigate to `http://localhost:3000/<SLUG>` in the browser
3. Verify the branded UI renders correctly with all visual elements
4. Click the action button — verify the intentional bug triggers and the error displays
5. Take a screenshot of the rendered page
6. Verify the demo does NOT appear on the hub page (`/`) or in `/api/verticals`

<verification>
- Routes registered in index.js (import, mount, verticalIds entry)
- NOT added to VERTICALS array in index.js
- NOT added as a card in hub.html
- If customer-specific: entry added to config/customers.js
- npm run lint passes with 0 errors
- Server starts and the demo renders correctly at /<SLUG>
- Intentional bug triggers on form submission
- Screenshot taken of the rendered UI
- Demo is not visible on the hub page or /api/verticals
</verification>
</phase>

<phase name="Create PR" id="5">
## Create PR

1. Create branch: `git checkout -b devin/$(date +%s)-add-<SLUG>-demo`
2. Stage only the relevant files — do NOT use `git add .`
3. Commit with a descriptive message
4. Push and create PR using `git_pr` tool targeting `main`
5. Include screenshots in the PR description
6. Wait for CI to pass
7. Send PR link to user with screenshots

<verification>
- PR created with descriptive title and body
- Screenshots included in the PR description
- CI checks pass
- PR link shared with user
</verification>
</phase>

<phase name="UI Testing & Recording" id="6">
## UI Testing with Screen Recording

After the PR is created and CI passes, perform recorded UI testing to verify the demo works end-to-end.

### Write Test Plan

Create a test plan (`.md` file) with 5 concrete test scenarios:

1. **Branding & Logo**: Navigate to `/<SLUG>`, verify the customer logo renders (as `<img>`, not text), brand name appears in header, nav items are present
2. **Dashboard/Summary Data**: Verify key data values render correctly — compare against the mock data defined in the service file (e.g., total balance, counts, rates)
3. **Data Table/List**: Verify the data display (table, cards, list) is populated from the GET API endpoint with the correct number of rows and specific values from mock data
4. **Bug Trigger**: Click the action button and verify the intentional TypeError displays in the error box with the exact expected error message
5. **Hidden from Hub**: Navigate to `/` and confirm no card for this demo exists; verify the demo slug is absent

Each test must have **specific pass/fail criteria** with concrete expected values (e.g., "Total Balance = $269,902.20", not "balance displays correctly").

### Execute Tests

1. Start a screen recording before testing
2. Navigate to `http://localhost:3000/<SLUG>` in the browser
3. Use `annotate_recording` at each test step to mark test starts and assertions
4. Walk through all 5 test scenarios, capturing screenshots at each step
5. Stop the recording

### Report Results

1. Post a single GitHub comment on the PR with:
   - Pass/fail status for each test (one line per test)
   - Screenshots showing key evidence (dashboard render, error message, hub page)
   - Link to the Devin session
   - Use `<details>/<summary>` tags to collapse screenshot sections
2. Send the recording to the user

<verification>
- Test plan written with specific pass/fail criteria for all 5 scenarios
- Screen recording captured of the full UI test
- All 5 tests passed with evidence
- PR comment posted with test results and screenshots
- Recording sent to user
</verification>
</phase>

## Specifications

- All code uses CommonJS (`require`/`module.exports`), not ES modules
- Winston logger for all logging
- Datadog metrics for success/failure counts and latency
- Sentry for exception capture
- The customer/brand name appears ONLY in the frontend HTML — all JS references use the hex slug
- The demo is NEVER added to hub.html or the VERTICALS array
- Service name must be consistent across all references (logger, Sentry tags, createSessionAndAlert)

## Advice and Pointers

- Study `banking.js` (base vertical) or `a6b38c63.js` (customer-specific) as the closest reference implementation
- The intentional bug should be a subtle property mismatch, not an obvious crash — it should look like a real production bug
- Use realistic mock data (real product names, tickers, IDs) to make the demo convincing
- Match the customer's real visual identity (colors, typography, layout) in the HTML
- For logos, prefer the customer's public CDN icons (apple-touch-icon, favicon) over text placeholders — check their website's `<link>` tags for hotlinkable URLs
- When testing, derive expected values directly from the mock data in the service file to ensure assertions are concrete and verifiable

## Forbidden Actions

- Do NOT add any card or reference to `app/public/hub.html`
- Do NOT add an entry to the `VERTICALS` array in `app/routes/verticals/index.js`
- Do NOT use the customer/brand name in any JavaScript file — only in the HTML
- Do NOT use ES module syntax (`import`/`export`)
- Do NOT use `git add .`
- Do NOT force push to main/master
- Do NOT skip linting before commit

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
