---
name: smoketest-event-driven-devin
description: "Converted from Devin playbook: Smoketest: Event-Driven Devin"
triggers:
  - user
  - model
---

# Smoketest: Event-Driven Devin

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Smoketest: Event-Driven Devin (playbook-b646286609ec44b38a0a06808b8825c7)

## Procedure

# Application Smoketest

## Overview

Run a quick end-to-end smoketest to verify the event-driven-devin application starts correctly and all core endpoints respond as expected. Covers the health check, hub page, all vertical pages, API metadata, core services, and vertical-specific APIs.

Target repo: https://github.com/COG-GTM/event-driven-devin

## What's Needed From User

- No user input required — this playbook runs against localhost.

<phase name="Server Startup" id="1">
## Server Startup

1. Ensure dependencies are installed:
   ```bash
   cd /home/ubuntu/repos/event-driven-devin
   npm install
   ```

2. Kill any process already on port 3000:
   ```bash
   fuser -k 3000/tcp 2>/dev/null || true
   ```

3. Start the server in the background:
   ```bash
   PORT=3000 node app/server.js &
   SERVER_PID=$!
   sleep 2
   ```

4. Verify the startup banner appears and the server is listening on port 3000.

<verification>
- `node app/server.js` started without errors
- The startup banner printed showing port 3000
- `curl -sf http://localhost:3000/health` returns HTTP 200 with `"status": "ok"`
</verification>
</phase>

<phase name="Core Endpoints" id="2">
## Core Endpoints

Test the foundational, non-vertical endpoints.

1. **Health check:**
   ```bash
   curl -sf http://localhost:3000/health | jq .
   ```
   Expect JSON with `status`, `version`, `environment`, `scenario`, and `uptime`.

2. **Hub landing page:**
   ```bash
   curl -sf -o /dev/null -w "%{http_code}" http://localhost:3000/
   ```
   Expect HTTP 200.

3. **Product catalog:**
   ```bash
   curl -sf http://localhost:3000/api/products | jq '.products | length'
   ```
   Expect 8 products.

4. **Search:**
   ```bash
   curl -sf "http://localhost:3000/search?q=widget" | jq '.results | length'
   ```
   Expect results array with length > 0.

5. **Login:**
   ```bash
   curl -sf -X POST http://localhost:3000/login \
     -H 'Content-Type: application/json' \
     -d '{"username":"demo@acme.com","password":"demo"}' | jq .success
   ```
   Expect `true`.

6. **Admin scenario:**
   ```bash
   curl -sf http://localhost:3000/admin/scenario | jq .scenario
   ```
   Expect a scenario name (e.g. `"healthy"`).

7. **Admin info:**
   ```bash
   curl -sf http://localhost:3000/admin/info | jq .node
   ```
   Expect the Node.js version string.

8. **Verticals metadata:**
   ```bash
   curl -sf http://localhost:3000/api/verticals | jq '.verticals | length'
   ```
   Expect at least 9.

9. **404 handler:**
   ```bash
   curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/nonexistent-path
   ```
   Expect HTTP 404.

<verification>
- Health endpoint returned 200 with `"status": "ok"`
- Hub landing page returned 200
- Product catalog returned 8 products
- Search returned results for "widget"
- Login returned `success: true`
- Admin scenario returned a valid scenario name
- Admin info returned the Node.js version
- Verticals metadata returned at least 9 entries
- Unknown path returned 404
</verification>
</phase>

<phase name="Vertical Pages" id="3">
## Vertical Pages

Verify every standard vertical HTML page returns HTTP 200.

```bash
PASS=0; FAIL=0
for vpath in retail banking financial-services insurance cpg hightech industrials healthcare telco; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:3000/$vpath")
  if [ "$STATUS" = "200" ]; then
    echo "  OK   /$vpath"
    PASS=$((PASS+1))
  else
    echo "  FAIL /$vpath (HTTP $STATUS)"
    FAIL=$((FAIL+1))
  fi
done
echo "Vertical pages: $PASS passed, $FAIL failed"
```

All 9 must return HTTP 200.

<verification>
- /retail returned 200
- /banking returned 200
- /financial-services returned 200
- /insurance returned 200
- /cpg returned 200
- /hightech returned 200
- /industrials returned 200
- /healthcare returned 200
- /telco returned 200
</verification>
</phase>

<phase name="Vertical API Smoketest" id="4">
## Vertical API Smoketest

Each vertical has a POST endpoint that intentionally returns a 500 error with a TypeError (this is the demo bug). Verify all 9 trigger correctly.

Use this helper function, then call it for each vertical:

```bash
PASS=0; FAIL=0

run_post() {
  local label=$1 url=$2 body=$3
  RESP=$(curl -s -w "\n%{http_code}" -X POST "http://localhost:3000$url" \
    -H 'Content-Type: application/json' -d "$body")
  STATUS=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | sed '$d')
  HAS_ERROR=$(echo "$BODY" | jq -r '.error // empty' 2>/dev/null)
  if [ "$STATUS" = "500" ] && [ -n "$HAS_ERROR" ]; then
    echo "  OK   $label — 500 with error: $HAS_ERROR"
    PASS=$((PASS+1))
  else
    echo "  FAIL $label — HTTP $STATUS"
    FAIL=$((FAIL+1))
  fi
}

run_post "Banking" "/api/banking/transfer" \
  '{"fromAccount":"ACCT-1001","toAccount":"ACCT-1002","amount":500,"accountTier":"premium"}'

run_post "Financial Services" "/api/trading/execute" \
  '{"symbol":"AAPL","side":"buy","quantity":10,"tierId":"1","accountId":"ACCT-INV-001"}'

run_post "Insurance" "/api/insurance/claim" \
  '{"policyId":"POL-5001","claimType":"collision","amount":5000}'

run_post "CPG" "/api/cpg/order" \
  '{"distributorId":"DIST-001","items":[{"sku":"BEV-001","quantity":50}],"warehouseRegion":"northeast","fulfillmentZone":"southeast"}'

run_post "High Tech" "/api/licenses/provision" \
  '{"planName":"enterprise ","seats":15,"orgName":"Test","billingCycle":"monthly"}'

run_post "Industrials" "/api/maintenance/workorder" \
  '{"equipmentId":"EQ-001","equipmentCategory":"Rotating","issueType":"preventive","priority":"high","estimatedHours":4,"partsEstimate":500}'

run_post "Healthcare" "/api/healthcare/appointment" \
  '{"patientId":"PAT-2001","providerId":"DR-101","department":"primary-care","year":2026,"month":12,"day":15}'

run_post "Telco" "/api/telco/upgrade" \
  '{"accountId":"CUST-3001","currentPlanCode":"BASIC-12","targetPlanCode":"FAMILY-PLUS-12"}'

run_post "Retail" "/api/storefront/checkout" \
  '{"items":[{"sku":"WDG-001","quantity":1}],"region":"US","persona":"buyer_1"}'

echo "Vertical POST APIs (bug triggers): $PASS passed, $FAIL failed"
```

All 9 POST endpoints must return HTTP 500 with a JSON body containing an `error` field.

<verification>
- Banking POST returned 500 with a TypeError
- Financial Services POST returned 500 with a TypeError
- Insurance POST returned 500 with a TypeError
- CPG POST returned 500 with a TypeError
- High Tech POST returned 500 with a TypeError
- Industrials POST returned 500 with a TypeError
- Healthcare POST returned 500 with a TypeError
- Telco POST returned 500 with a TypeError
- Retail POST returned 500 with a TypeError
</verification>
</phase>

<phase name="Report Results" id="5">
## Report Results

1. Stop the server:
   ```bash
   kill $SERVER_PID 2>/dev/null
   fuser -k 3000/tcp 2>/dev/null || true
   echo "Server stopped."
   ```

2. Compile a results summary table and send it to the user:

| Category | Checks | Description |
|----------|--------|-------------|
| Health | 1 | Server status and metadata |
| Hub | 1 | Landing page loads |
| Core APIs | 6 | Products, search, login, admin, verticals metadata, 404 |
| Vertical pages | 9 | All standard vertical HTML pages |
| Vertical POSTs | 9 | All demo bug triggers fire correctly |
| **Total** | **26** | |

3. If any checks failed, list them with the HTTP status received and any error details.

<verification>
- The server has been stopped
- A results summary has been sent to the user
- Any failures have been clearly documented with details
</verification>
</phase>

## Specifications

- The smoketest runs entirely against `localhost:3000` — no external services (Sentry, Datadog, Slack) are required.
- All 9 standard verticals must be tested: retail, banking, financial-services, insurance, cpg, hightech, industrials, healthcare, telco.
- Vertical POST endpoints intentionally return 500 — this is expected demo behavior, not a failure.
- The playbook does not test custom customer verticals (hex-slug URLs) — only the 9 standard ones.

## Advice and Pointers

- If port 3000 is occupied, use `fuser -k 3000/tcp` before starting.
- The server starts without any environment variables — Sentry/Datadog/Slack are all optional.
- Express 5 may log warnings about deprecated features — these are safe to ignore.
- The retail checkout bug depends on a promo item missing a `name` field — this is intentional.

## Forbidden Actions

- Do not modify any application source code during the smoketest.
- Do not set `SENTRY_DSN`, `DD_API_KEY`, or `SLACK_BOT_TOKEN` — the smoketest must verify the app runs cleanly without external services.
- Do not skip the vertical POST checks — they verify the core demo functionality.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
