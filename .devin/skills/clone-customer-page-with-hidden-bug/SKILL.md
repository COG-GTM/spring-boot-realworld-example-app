---
name: clone-customer-page-with-hidden-bug
description: "Converted from Devin playbook: Clone Customer Page with Hidden Bug"
triggers:
  - user
  - model
---

# Clone Customer Page with Hidden Bug

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Clone Customer Page with Hidden Bug (playbook-6bf2c8511cbc4ec9850d5a89c69c6f24), macro `!clone_customer_page_with_bug`

## Procedure

# Clone Customer Page with Hidden Bug

## Overview

This playbook creates a new industry vertical demo for the COG-GTM/event-driven-devin project by cloning a real customer's website page and recreating it locally with an intentional hidden bug. The bug triggers the full Sentry → Datadog → Slack → Devin incident response pipeline. Each session targets a specific customer URL provided by the user. The clone must be **pixel-perfect** — the creator of the original page should not be able to tell the difference at a glance.

## What's Needed From User

- **Customer website URL(s)**: A direct URL to a specific page on a customer's website (e.g., `https://example.com/pricing`). This is the page that will be visually cloned. The user may provide **multiple URLs** in a single session — if so, repeat Phases 1–4 for each URL sequentially, completing one vertical end-to-end before starting the next.
- **Target org ID**: The Devin org ID where events should be sent (e.g., `org-df79e7b98f9849bea354717441c90a33`). The user may provide one org ID per vertical or one shared org ID for all.
- **Devin user ID** *(auto-resolvable)*: The Devin user ID that sessions should be created as. **If the user references a person by name or says "reuse same user as X", look it up from existing verticals** (see Phase 1, step 3a). Only ask the user if you cannot find it.
- **Slack member ID** *(auto-resolvable)*: The Slack member ID for on-call @mentions. **Auto-resolve from existing verticals** (see Phase 1, step 3a). Only ask the user if you cannot find it.
- **Existing service key to reuse** *(optional)*: If the user wants to reuse the same `DEVIN_SERVICE_KEY` as another vertical (e.g., "use the same key as {DEMO_CUSTOMER_1}/{EXISTING_VERTICAL_ID_1}"), note which vertical's key to copy. If not provided, the user will need to supply a new service key value during Phase 4.

**Minimize questions to the user.** In a past session, Devin asked for org ID, user ID, Slack member ID, and service key in 4 separate back-and-forth messages. The user replied "these all should already be used in other demos." Auto-resolve everything possible from the existing codebase and EC2 config. Batch any remaining questions into a SINGLE message. Never ask for information one piece at a time.

<phase name="Research & Asset Extraction" id="1">
## Research & Asset Extraction

1. Clone the repo `COG-GTM/event-driven-devin` (or if already cloned, run `git checkout main && git pull origin main` to get the latest code) and run `npm install`.
2. **Generate a random 8-character hex slug** for the vertical ID (e.g., `a3f7b201`). Do NOT use the customer's brand name, company name, or any identifiable string as the vertical ID. The existing custom demo verticals all use anonymous hex slugs (e.g., `{EXISTING_VERTICAL_ID_1}`, `{EXISTING_VERTICAL_ID_2}`, `{EXISTING_VERTICAL_ID_3}`). Generate one with: `node -e "console.log(require('crypto').randomBytes(4).toString('hex'))"`.
3. **Auto-resolve user ID, Slack member ID, and service key from existing verticals.** This step eliminates unnecessary back-and-forth with the user:
   
   a. **Look up user ID and Slack member ID from existing service files:**
      ```
      grep -r 'devinUserId' app/services/verticals/*.js | head -10
      grep -r 'slackMemberId' app/services/verticals/*.js | head -10
      ```
      If the user references a person by name (e.g., "User: Russell"), match that name to the user ID already in use across existing verticals. If the same user ID appears in multiple verticals, it's the correct one.
   
   b. **Look up existing service keys from EC2 if reusing:**
      ```
      echo "$EC2_SSH_KEY" > /tmp/ec2_key.pem && chmod 600 /tmp/ec2_key.pem
      ssh -i /tmp/ec2_key.pem -o StrictHostKeyChecking=no {EC2_USER}@{EC2_HOST_IP} "grep DEVIN_SERVICE_KEY /home/ubuntu/.env | head -5"
      ```
      If the user says "reuse same key as {DEMO_CUSTOMER_3}/{DEMO_CUSTOMER_2}", find the key value from an existing vertical's env var.
   
   c. **Batch any remaining questions.** After auto-resolving, if you still need information (e.g., a brand-new org ID the user hasn't provided), ask ALL remaining questions in a **single message**. Never ask questions one at a time across multiple messages. Example of a good single message:
      ```
      I've auto-resolved from existing verticals:
      - User ID: clerk-user_xxx (from {DEMO_CUSTOMER_2}/{DEMO_CUSTOMER_3})
      - Slack member ID: {SLACK_MEMBER_ID} (from {DEMO_CUSTOMER_2}/{DEMO_CUSTOMER_3})
      - Service key: will reuse the {DEMO_CUSTOMER_3}/{DEMO_CUSTOMER_2} key
      
      I still need:
      - Target org ID for [Customer Name]
      ```

4. **Validate and normalize the org ID immediately.** This step is critical — skipping it has caused full round-trips of debugging in past sessions:
   - **Format normalization**: Org IDs use **hyphens**, not underscores. If the user provides an org ID with underscores (e.g., `org_D7Q13p1Vtu23p4H1`), normalize it to hyphen format (e.g., `org-D7Q13p1Vtu23p4H1`). The canonical format is `org-{id}`.
   - **Verify the org exists**: Call the enterprise organizations API to confirm the org ID is valid:
     ```
     curl -s -H "Authorization: Bearer <token>" \
       "https://api.devin.ai/v3/enterprise/organizations" | jq '.[] | select(.org_id == "ORG_ID_HERE")'
     ```
     If the org is NOT found in the enterprise, **stop and inform the user immediately**. List the available orgs from the API response so the user can pick the correct one. Do NOT proceed with an unverified org ID — this caused a full deploy-debug-fix cycle in past sessions where the Devin session silently failed with `404: "Organization not found"`.
   - If the user says "use the same org as {vertical}", look up that vertical's `devinOrgId` in its HTML file and reuse it.
5. **If reusing a service key from another vertical, verify it has access to the target org ID.** Retrieve the service key value from EC2's `.env` and test it against the target org:
   ```
   # Retrieve the key
   ssh -i /tmp/ec2_key.pem -o StrictHostKeyChecking=no {EC2_USER}@{EC2_HOST_IP} "grep DEVIN_SERVICE_KEY_{EXISTING_SLUG} /home/ubuntu/.env"
   # Test access to target org
   curl -s -H "Authorization: Bearer <token>" \
     "https://api.devin.ai/v3/sessions?org_id={target-org-id}&limit=1" -o /dev/null -w '%{http_code}'
   ```
   A `200` means the key has access. A `403` or `404` means the key does NOT have access to that org — inform the user that they need either a different service key or a different org ID. Do NOT proceed with a key that cannot access the target org — this caused Slack alerts to fire successfully but Devin sessions to silently fail.
6. Open the customer URL in the browser. Study the page section by section, scrolling from top to bottom. For each section, note:
   - **Exact layout**: Grid/flex structure, column counts, spacing, alignment, content width.
   - **Navigation**: Logo placement, nav link names, login/CTA button styles, sticky/fixed behavior.
   - **Hero/banner**: Background images or gradients, text overlay positioning, CTA buttons, card/product imagery.
   - **Content sections**: Product explorers, feature grids, promotional banners, testimonials, help sections — note their exact structure.
   - **Footer**: Link groups, social icons, legal text, background color.
   - **Interactive elements**: Buttons, links, dropdowns, tabs — note their exact styling.
   - **Brand identity**: Logo text/image, company name, tagline.
   - **Identify the CTA button to take over for the bug trigger**: Find a prominent, existing CTA button on the page (e.g., "Learn more", "Get started", "Explore benefits", "Contact us"). This button will be intercepted to trigger the hidden API call that causes the bug. Choose a button that is visible without excessive scrolling and feels natural to click during a demo. Do NOT create new form fields or submit buttons — use what already exists on the page.
   - **Page length**: Note how many sections the page has. If the page is very long (many content sections, carousels, product grids), identify which sections are essential for the visual clone and which are decorative filler. The CTA button must be reachable without excessive scrolling.
7. **Extract all image and asset URLs** from the customer page. Use curl and ripgrep to extract every asset reference:
   ```
   curl -s <CUSTOMER_URL> | rg -o 'src="[^"]*"' | rg -i '\.(svg|png|jpg|jpeg|webp|gif|ico)'
   curl -s <CUSTOMER_URL> | rg -o 'url\([^)]*\)' | rg -i '\.(svg|png|jpg|jpeg|webp|gif|ico)'
   ```
   For relative URLs (e.g., `/content/dam/.../logo.svg`), prepend the customer's domain to form absolute URLs (e.g., `https://example.com/content/dam/.../logo.svg`). Save this asset list — you will hotlink directly to these absolute URLs in your clone. Common patterns to look for:
   - Logo SVGs (e.g., `/content/dam/.../logo.svg`)
   - Hero/banner images (e.g., `.jpg`, `.png`, `.webp`)
   - Product card images
   - Social media icons (Facebook, Twitter/X, YouTube, Instagram SVGs)
   - Footer badges (e.g., FDIC, BBB logos)
   - Favicon / brand icons
8. **Verify hotlinkability of EVERY image URL with HTTP status checks.** Do not just spot-check 2–3 URLs — check ALL of them:
   ```
   curl -s -o /dev/null -w "%{http_code}" "<image-url>"
   ```
   Any URL returning non-200 (especially 403 or 404) must be replaced with an alternative. Known CDN hotlinking behavior:
   
   | CDN | Hotlinking | Notes |
   |-----|-----------|-------|
   | Unsplash (`images.unsplash.com`) | ✅ Allowed | Preferred fallback source |
   | jpmorganchase.com | ✅ Allowed | Can hotlink directly |
   | marriott.com / cache.marriott.com | ❌ Blocked (403) | Must use Unsplash alternatives |
   | seb.se | ❌ Blocked (403) | Must use Unsplash alternatives |
   
   When a CDN blocks hotlinking, find a visually similar Unsplash image as a replacement. **CRITICAL: Unsplash photo IDs are opaque** — `photo-1582167751370` tells you nothing about the image content. After finding an Unsplash URL, you MUST:
   - Open the URL directly in the browser
   - Visually confirm the image shows the correct subject (the right city, building, person, etc.)
   - A URL returning HTTP 200 does NOT guarantee the image content is correct — in a past session, a "Philadelphia" Unsplash image actually showed a different city
   
   For ANY non-Unsplash image source (customer CDN), add an `onerror` fallback to a known-good Unsplash URL:
   ```html
   <img src="https://realsite.com/image.jpg"
        onerror="this.onerror=null;this.src='https://images.unsplash.com/photo-xxx?w=800&q=80';"
        alt="Description">
   ```

9. **Verify the logo asset specifically**: The brand logo is the most visually prominent asset and a broken logo immediately destroys the illusion. Open the logo URL in the browser and verify:
   - The URL is complete and correctly formed (no truncated paths, no malformed query strings)
   - The SVG/PNG renders correctly at the expected size
   - If the logo is an SVG, check that it doesn't require inline styles or external CSS to render properly
   - If the logo needs color inversion for different backgrounds (e.g., white logo on dark header, dark logo on light footer), plan the CSS `filter` approach (e.g., `filter: brightness(0) invert(1)`)
   - If the logo URL doesn't work, search the page source for alternative logo references or download the logo directly
10. **Extract exact CSS values** using the browser console. Run `getComputedStyle()` on key elements to capture precise values — do not eyeball or approximate. Extract at minimum:
   - **Header**: `backgroundColor`, `height`, `padding`, `borderBottom`
   - **Nav links**: `color`, `fontSize`, `fontWeight`, `fontFamily`
   - **Hero section**: `height`, `backgroundColor`, background image positioning
   - **Headings (h1, h2)**: `color`, `fontSize`, `fontWeight`, `fontFamily`, `lineHeight`, `letterSpacing`
   - **Body text**: `color`, `fontSize`, `fontFamily`, `lineHeight`
   - **Buttons (primary CTA, login)**: `backgroundColor`, `color`, `borderRadius`, `padding`, `fontSize`, `fontWeight`
   - **Footer**: `backgroundColor`, `color`, `fontSize`
   Save these extracted values — you will use them directly as CSS property values in your clone.
11. **Take a full-page Puppeteer screenshot** of the customer site at 1400px viewport width for reference during implementation:
    ```
    take-screenshot <CUSTOMER_URL> /home/ubuntu/original-reference.png
    ```
12. Examine the existing verticals to understand established patterns:
    - Read `app/routes/verticals/index.js` to understand route registration and URL serving.
    - Read one existing service file (e.g., `app/services/verticals/banking.js`) to understand the service structure: mock data, business logic functions, the error handling with Sentry/Datadog/Slack/Devin alert wiring.
    - Read the corresponding route file (e.g., `app/routes/verticals/banking.js`) to understand the API endpoint pattern.
    - Read the corresponding HTML file (e.g., `app/public/verticals/banking.html`) to understand the frontend structure: CSS variables, fetch calls, error/success display.
    - **Read an existing custom demo service file** (e.g., one of `{EXISTING_VERTICAL_ID_1}.js`, `{EXISTING_VERTICAL_ID_2}.js`, `{EXISTING_VERTICAL_ID_3}.js`) to see how the `createSessionAndAlert` call includes `customer`, `slackMemberId`, and hardcoded `devinUserId`/`devinOrgId` fields. This pattern is required for custom demo verticals.
    - **Read an existing custom demo HTML file** to see how the CTA button takeover and toast notification pattern works — how the button click triggers a `fetch()` call to the API, and how errors are displayed as a subtle toast fly-in.

Only create the TODO list for the current phase.

<verification>
- The customer page has been studied section-by-section in the browser
- All image/SVG/icon asset URLs have been extracted from the page source via curl and saved
- EVERY image URL has been checked with `curl -s -o /dev/null -w "%{http_code}"` — all return 200
- For any blocked CDN images (403/404), Unsplash alternatives have been found AND visually verified to show the correct subject
- For non-Unsplash images, `onerror` fallback URLs have been identified
- Asset hotlinkability has been verified (all URLs tested, not just 2–3 samples)
- The brand logo has been specifically verified to render correctly at the expected size and color
- Exact CSS values have been extracted via getComputedStyle() for header, nav, hero, headings, body text, buttons, and footer
- A full-page Puppeteer screenshot of the original site has been saved for reference
- A random hex slug has been generated as the vertical ID (NOT a brand name)
- The org ID has been normalized to hyphen format and verified against the enterprise API
- If reusing a service key, its access to the target org has been confirmed
- The user ID and Slack member ID have been auto-resolved from existing verticals OR confirmed by the user
- All remaining questions to the user were batched into a single message (no multi-message back-and-forth)
- An existing CTA button has been identified on the page for the bug trigger (no new forms)
- The existing vertical file patterns (HTML, service, route, index registration) have been read and understood
- An existing custom demo service file AND HTML file have been read to understand the CTA takeover and toast notification pattern
</verification>
</phase>

<phase name="Implementation" id="2">
## Implementation

1. Create a new git branch: `git checkout -b devin/$(date +%s)-add-{vertical-id}-vertical`.
2. Create the HTML page at `app/public/verticals/{vertical-id}.html`. The goal is a **pixel-perfect clone** — the person who built the original page should not be able to distinguish the clone from the real thing:
   - **Page structure**: Reproduce every visible section of the customer's page in order — header/nav, banner bars, hero section, content sections, footer. However, **keep the page compact enough that the CTA button is reachable without excessive scrolling**. If the original page has many decorative content sections (product carousels, lifestyle galleries, video embeds, promotional grids), include only 1–2 representative content sections and omit the rest. The CTA must be visible within 1–2 scrolls of the hero section. The goal is a convincing clone, not a complete mirror of every section.
   - **Colors**: Use the exact computed color values extracted via `getComputedStyle()` in Phase 1. Define them as CSS variables (`:root` vars) for consistency with other verticals.
   - **Typography**: Load the same font family the customer uses via Google Fonts CDN (or hotlink their font if it's a web font). Use the exact `font-size`, `font-weight`, `line-height`, and `letter-spacing` values extracted in Phase 1 — do not round or approximate.
   - **Layout and spacing**: Use the exact `padding`, `margin`, `max-width`, and `gap` values extracted from the original. Match the customer's grid/flex structure precisely.
   - **Images and assets — USE REAL ASSETS, NOT PLACEHOLDERS**: Hotlink directly to the customer's CDN URLs extracted in Phase 1. This includes:
     - The actual brand logo SVG/image (not a recreated approximation)
     - Hero/banner background images (the real `.jpg`/`.png`/`.webp`, not a CSS gradient)
     - Product card images, lifestyle photos
     - Social media icon SVGs (the actual brand-specific ones from their CDN)
     - Footer badges and certification logos (FDIC, BBB, etc.)
     - If any asset cannot be hotlinked, download it to `app/public/verticals/assets/{vertical-id}/` and reference it locally. Use the hex slug for the asset subdirectory name — do NOT use the brand name for asset directory or filenames.
     - NEVER use placeholder blocks, CSS gradient approximations, or generic SVG icon replacements when the real asset URL is available.
     - **For ANY non-Unsplash image, add an `onerror` fallback** to a known-good Unsplash URL so the page degrades gracefully if the CDN starts blocking hotlinking later:
       ```html
       <img src="https://realsite.com/hero.jpg"
            onerror="this.onerror=null;this.src='https://images.unsplash.com/photo-xxx?w=800&q=80';"
            alt="Description">
       ```
   - **Hero image `object-position`**: For hero images containing people/portraits, use `object-position: center center` (NOT `center top`). Using `center top` on a portrait image will crop to show only the top of the person's head. In a past session, this caused a JPMC hero to show only the top of Jamie Dimon's head instead of a proper business portrait. Rule of thumb:
     - Portraits/headshots → `object-position: center center`
     - Landscapes/cityscapes → `object-position: center center` (default is fine)
     - Only use `center top` if the important content is specifically at the top of the image
   - **Navigation**: Reproduce the nav bar faithfully — same links, same logo (hotlinked from their CDN), same login/CTA button position and style. If the original has a sticky header, make yours sticky too.
   - **Interactive elements**: Reproduce buttons, links, and hover states with the exact `border-radius`, `padding`, `background-color`, `color`, and `transition` values extracted in Phase 1.
   - **Content text**: Use the same text/copy from the customer's page. Do not invent generic placeholder text.
   - **No brand name leakage in code**: The `<title>` tag, CSS variable names, and any code identifiers must use the hex slug, NOT the customer's brand name. For example, use `--v-primary` or `--{hex-slug}-primary` instead of `--acme-primary`. The page's visual content (logos, text) will naturally contain the brand, but code-level identifiers must be anonymous.
   - **CTA button takeover for triggering the bug (NO NEW FORMS)**: Do NOT add new form fields, input elements, submit buttons, or dashboard sections to the page. Instead, identify a prominent existing CTA button on the cloned page (e.g., "Learn more", "Get started", "Explore benefits", "Sign in") and intercept its click event to make a `fetch()` call to `/api/{vertical-id}/{action}`. The CTA should look and behave exactly like the original — the only difference is that clicking it triggers the hidden API call instead of navigating. This approach preserves the pixel-perfect clone while providing the bug trigger. Example:
     ```javascript
     document.querySelector('.hero-cta-btn').addEventListener('click', async (e) => {
       e.preventDefault();
       try {
         const res = await fetch('/api/{vertical-id}/inquiry', {
           method: 'POST',
           headers: { 'Content-Type': 'application/json' },
           body: JSON.stringify({
             devinUserId: '{user-id}',
             devinOrgId: '{org-id}'
           })
         });
         const data = await res.json();
         if (data.error) showToast(data.error);
       } catch (err) {
         showToast('An unexpected error occurred');
       }
     });
     ```
   - **Hardcode org ID and user ID in the CTA's fetch() call**: The CTA's `fetch()` call must include the user-provided `devinUserId` and `devinOrgId` in the request body, following the pattern used by other custom demo verticals. Use the **validated and normalized** org ID from Phase 1 step 3 (hyphen format, verified against enterprise API).
   - **Toast notification for errors**: Errors must appear as a **subtle fly-in toast notification** at the bottom-right of the page. The toast should:
     - Slide in from the bottom-right with a smooth CSS transition
     - Have a dark semi-transparent background (e.g., `rgba(0,0,0,0.85)`) with white text
     - Auto-dismiss after 5–6 seconds
     - Not obstruct the page content or break the visual clone's appearance
     - Be styled to look like a natural system notification, not a custom UI element
     Example toast implementation:
     ```javascript
     function showToast(message) {
       const toast = document.createElement('div');
       toast.textContent = message;
       toast.style.cssText = 'position:fixed;bottom:24px;right:24px;background:rgba(0,0,0,0.85);color:#fff;padding:14px 24px;border-radius:8px;font-size:14px;z-index:10000;opacity:0;transform:translateY(20px);transition:opacity 0.3s,transform 0.3s;max-width:400px;';
       document.body.appendChild(toast);
       requestAnimationFrame(() => { toast.style.opacity = '1'; toast.style.transform = 'translateY(0)'; });
       setTimeout(() => { toast.style.opacity = '0'; toast.style.transform = 'translateY(20px)'; setTimeout(() => toast.remove(), 300); }, 6000);
     }
     ```
   - Include a small `← All Demos` back link to `/` — keep it subtle so it doesn't disrupt the page design.

3. **First visual comparison**: Start the server with `node app/server.js`, open both the customer's original page and your clone in the browser side by side. Do a quick visual scan for any obvious differences before continuing to refinement.

4. **Verify all assets render correctly**, especially:
   - **The brand logo**: Check it renders in the header and footer (if applicable). If it appears broken, missing, or incorrectly sized, fix the URL or download the asset locally. A broken logo is the most visible flaw and must be fixed before any other refinement.
   - **Hero/banner images**: Verify they load AND are properly framed:
     - For portraits: the person's face and upper body must be visible, not just the top of their head
     - For cityscapes: recognizable landmarks must be visible, not a generic skyline
     - If cropping is wrong, adjust `object-position` (see guidance above)
   - **All other images**: Verify every image on the page loads (no broken image icons). Run curl against each `src=` URL to confirm 200 status.
   - **Icons and badges**: Verify social media icons, certification badges, etc. render.

5. **Iterative pixel-perfect refinement**: Compare the clone against the original section by section. For each section (header, hero, content blocks, footer), if you spot any difference:
   - Use the browser console on the **original** page to run `getComputedStyle()` on the specific element that looks different.
   - Update the clone's CSS to match the exact extracted values.
   - Reload and re-compare that section.
   - Repeat until the section is indistinguishable from the original.
   - Common issues to watch for and fix:
     - Font size or weight slightly off
     - Padding/margin differences causing layout shifts
     - Wrong background-color shade
     - Hero image positioning or sizing mismatch
     - Button border-radius or padding not matching
     - Logo size or placement offset
     - Missing or wrong icons/badges
     - Logo rendering issues (malformed SVG paths, missing CSS filters for color inversion)
     - **Hero image cropped to wrong part of image** — adjust `object-position`

6. **Take Puppeteer screenshots of both pages** at 1400px viewport width and visually compare them:
   ```
   take-screenshot <CUSTOMER_URL> /home/ubuntu/original-final.png
   take-screenshot http://localhost:3000/{vertical-id} /home/ubuntu/clone-final.png
   ```
   If any differences are still visible in the screenshots, fix them and re-screenshot until the pages are indistinguishable.

7. Create the service file at `app/services/verticals/{vertical-id}.js`:
   - Follow the exact structure of existing services:
     - Import `uuid`, `logger`, `incrementMetric`/`recordTiming`, `Sentry`, `createSessionAndAlert`.
     - Define realistic mock data constants relevant to the customer's domain.
     - Implement business logic helper functions.
     - Implement one main async function that processes the API action.
   - **Introduce a subtle bug that spans multiple functions or files.** The bug must meet two criteria:
     - **Easy to trigger**: Clicking the CTA button should trigger the error every single time. Any colleague doing the demo just clicks the button — no special inputs or edge cases required.
     - **Multi-line / multi-function / multi-file fix**: The root cause must NOT be fixable by changing a single line. The fix should require understanding and modifying code across at least 2–3 functions or across multiple files (e.g., the service file + the route file, or a helper function + the main processing function).
   - Example multi-part bug patterns (pick one or invent a similar pattern):
     - **Data shape mismatch across functions**: A helper function returns data in one shape (e.g., an array of objects), but the calling function destructures it expecting a different shape (e.g., a flat object). Fixing it requires changing the helper's return format AND updating the caller's destructuring AND updating any downstream consumers.
     - **Missing async propagation chain**: An inner helper is async but not awaited by a middle function, which is then called by the main function. The fix requires adding `async`/`await` at multiple levels of the call chain.
     - **Shared config/constant inconsistency**: A lookup key format is defined one way in a constants object (e.g., uppercase) but referenced differently in two separate functions (e.g., lowercase in one, mixed case in another). Fixing it requires updating the constant definition AND the lookup logic in multiple functions.
     - **Validation gap across layers**: The route passes raw user input to the service, the service passes it to a helper without validation, and the helper crashes on unexpected types. The fix requires adding validation in the route, type coercion in the service, and defensive checks in the helper.
     - **Incorrect data pipeline**: A data transformation flows through 3+ functions (fetch → transform → calculate), and an error in the transform step (e.g., mapping wrong fields) causes a crash in the calculate step. Fixing it requires correcting the transform function, updating the calculate function's expectations, and possibly fixing the mock data structure.
   - Do NOT choose a bug pattern already used by an existing vertical — pick a fresh one or create a new subtle pattern.
   - **Wire the `catch` block with `customer` and `slackMemberId`**: The `createSessionAndAlert()` call MUST include:
     - `customer: '{vertical-id}'` — so `getCustomerConfig()` resolves the correct customer-specific API key and user ID. Without this, the session will use the default/global credentials instead of the customer-specific ones.
     - `slackMemberId: '{slack-member-id}'` — (using the Slack member ID provided by the user) so the on-call person is @mentioned in the Slack alert.
   - Follow the exact pattern from other custom demo verticals (e.g., `{EXISTING_VERTICAL_ID_1}.js`, `{EXISTING_VERTICAL_ID_2}.js`). Also call `Sentry.captureException()` following the same pattern.
8. Create the route file at `app/routes/verticals/{vertical-id}.js`:
   - Define a GET endpoint returning mock data and a POST endpoint calling the service function.
   - Follow the same Express router pattern as other verticals.
   - The POST handler must pass `devinUserId` and `devinOrgId` from `req.body` through to the service function, following the pattern in other custom demo route files.
9. Register the new vertical in `app/routes/verticals/index.js`:
   - Import the new route file and add `router.use()`.
   - Add the vertical ID to the `verticalIds` array so the HTML page is served at `/{vertical-id}`.
   - Do NOT add the vertical to the `VERTICALS` metadata array (keeps it hidden from the hub page).
10. **Add customer entry to `config/customers.js`**: Add a new entry to the `CUSTOMERS` object with the vertical's slug as the key, a descriptive `label`, and `triggerMode: 'api'`. Follow the exact pattern of existing entries. This is required for the service key and user ID environment variables to resolve correctly per-customer.
11. **Add per-customer environment variables to `docker-compose.yml`**: Open `docker-compose.yml` and add the new customer's env vars to the `checkout-api` service's `environment` block, in the `# Per-customer overrides` section. The env var names are derived from the vertical slug converted to UPPER_SNAKE_CASE. At minimum, add:
    ```yaml
    - DEVIN_SERVICE_KEY_{SLUG}=${DEVIN_SERVICE_KEY_{SLUG}:-}
    - DEVIN_USER_ID_{SLUG}=${DEVIN_USER_ID_{SLUG}:-}
    ```
    For example, for a vertical with slug `a3f7b201`, the SLUG is `A3F7B201`, so add:
    ```yaml
    - DEVIN_SERVICE_KEY_A3F7B201=${DEVIN_SERVICE_KEY_A3F7B201:-}
    - DEVIN_USER_ID_A3F7B201=${DEVIN_USER_ID_A3F7B201:-}
    ```
    **This step is critical.** Docker Compose does NOT automatically pass `.env` variables into containers — only variables explicitly listed in the `environment` block are available inside the container. Skipping this step will cause the Devin session creation to fail with a 403 Unauthorized error because the container won't have the customer-specific service key.
12. **Add per-customer env vars to `.env.example`** so they are documented:
    ```bash
    # Customer {vertical-id}
    # DEVIN_SERVICE_KEY_{SLUG}=
    # DEVIN_USER_ID_{SLUG}=
    ```
13. Run `npm run lint` and fix any lint errors.

Only create the TODO list for the current phase.

<verification>
- HTML page created at `app/public/verticals/{vertical-id}.html` using real hotlinked assets from the customer's CDN (no placeholder images or gradient approximations)
- Non-Unsplash images have `onerror` fallback to Unsplash URLs
- Hero images with people/portraits use `object-position: center center` (NOT `center top`)
- Every image URL in the HTML has been curl-checked and returns HTTP 200
- Every Unsplash image has been visually verified to show the correct subject (not just 200 status)
- The hex slug is used for all code identifiers (title tag, CSS variable names, asset directory names, file names) — no brand name leakage in code
- NO new form fields, input elements, or submit buttons were added — an existing CTA button on the page was taken over to trigger the bug
- The CTA's fetch() call includes the validated and normalized devinUserId and devinOrgId (hyphen format, verified via enterprise API)
- Errors display as a subtle toast fly-in at the bottom-right — not in a form result box
- The CTA button is reachable without excessive scrolling (within 1–2 scrolls of the hero)
- All assets render correctly, especially the brand logo in both header and footer
- Iterative section-by-section comparison has been performed using getComputedStyle() to fix any CSS differences
- Puppeteer screenshots of both the original and clone have been taken and visually compared — the two are indistinguishable
- Service file created at `app/services/verticals/{vertical-id}.js` with a subtle bug and alert pipeline wiring
- The `createSessionAndAlert` call includes `customer: '{vertical-id}'` and `slackMemberId: '{slack-member-id}'`
- Route file created at `app/routes/verticals/{vertical-id}.js` with GET and POST endpoints
- New vertical registered in `app/routes/verticals/index.js` (in `verticalIds` array only, NOT in `VERTICALS`)
- Customer entry added to `config/customers.js` with the vertical slug, label, and `triggerMode: 'api'`
- Per-customer `DEVIN_SERVICE_KEY_{SLUG}` and `DEVIN_USER_ID_{SLUG}` env vars added to `docker-compose.yml` in the `checkout-api` service's `environment` block
- Per-customer env vars added to `.env.example`
- `npm run lint` passes with no errors
- No comments, variable names, or code hints reveal the bug's existence
</verification>
</phase>

<phase name="Verification & PR" id="3">
## Verification & PR

1. Start the server with `node app/server.js` and open `http://localhost:3000/{vertical-id}` in the browser.
2. Open the customer's original page in a separate browser tab. Compare the two side-by-side and verify pixel-level fidelity — layout, colors, fonts, spacing, images, and content should be indistinguishable.
3. **Verify the CTA button is easily reachable** — the CTA should be visible within 1–2 scrolls from the top of the page. If not, go back and trim decorative content sections.
4. **Complete image verification checklist** — this is the step that catches the issues that caused follow-up fix sessions:
   - Extract every `src=` and `background-image: url(...)` from the HTML file
   - Run `curl -s -o /dev/null -w "%{http_code}"` against EACH URL — all must return 200
   - Open each image URL directly in the browser and visually confirm it shows the correct subject
   - On the rendered page, verify:
     - No broken image icons anywhere
     - Hero images show the full intended subject (faces visible in portraits, landmarks visible in cityscapes)
     - No images are cropped to show only hair, sky, or other unintended portions
     - City/location images actually show the correct city (not a different one with a similar name)
5. **Verify one-click demo flow** — click the CTA button that was taken over for the bug trigger. It should trigger the intentional bug and display an error toast at the bottom-right of the page. Verify:
   - The toast appears with the error message
   - The toast auto-dismisses after 5–6 seconds
   - The toast does not break the page layout or obscure important content
   - The error is a TypeError/runtime error from the intentional bug, NOT a "not found" or "404" error (which would indicate the API endpoint or mock data is misconfigured)
6. **Verify Devin session pipeline locally** — check the server terminal logs after clicking the CTA:
   - Look for `"Posting alert and triggering Devin"` log line confirming `createSessionAndAlert()` was called
   - Verify the log shows the correct `customer` slug and `devinOrgId`
   - Note: On localhost without `SLACK_BOT_TOKEN`, the Slack post will fail — this is expected. The important thing is that `createSessionAndAlert()` is being called with the right parameters.
7. Take final Puppeteer screenshots of both the original and clone and attach them to the PR for reference:
   ```
   take-screenshot <CUSTOMER_URL> /home/ubuntu/original-final.png
   take-screenshot http://localhost:3000/{vertical-id} /home/ubuntu/clone-final.png
   ```
8. Commit all new and modified files, push the branch, and create a PR into `main`.
   - PR title: `Add {brand-name} demo vertical`
   - Do not mention the bug in the PR title or description.
   - Attach the side-by-side screenshots to the PR description.

Only create the TODO list for the current phase.

<verification>
- The page loads at `/{vertical-id}` and is visually indistinguishable from the customer's site when compared side-by-side
- Every image URL has been curl-checked for 200 status AND visually verified for correct content
- No broken image icons, no cropped heroes showing only hair/sky, no wrong-city images
- Hero images with people show full face and upper body (not just the top of the head)
- The CTA button is reachable within 1–2 scrolls from the top of the page
- Clicking the CTA button triggers the intentional bug and shows an error toast (not a "not found" error)
- The error toast is subtle, appears at bottom-right, and auto-dismisses
- Server logs show `createSessionAndAlert()` was called with the correct customer slug and org ID
- Side-by-side Puppeteer screenshots have been taken and attached to the PR
- A PR has been created with all files, and the PR does not mention the intentional bug
</verification>
</phase>

<phase name="Deploy to EC2 & End-to-End Verification" id="4">
## Deploy to EC2 & End-to-End Verification

**CRITICAL EC2 SAFETY RULES — READ BEFORE PROCEEDING:**
- **NEVER run `git clean`, `git reset --hard`, `git checkout -- .`, or ANY destructive git command on EC2.** In a past session, `git clean -fd` deleted `/home/ubuntu/.ssh/authorized_keys`, permanently locking Devin out of SSH. The user had to manually restore access via AWS Console. This wasted significant time and required emergency intervention.
- **NEVER use `git pull` on EC2 for deployments.** Git pulls cause merge conflicts with local `.env` changes. Use the **tarball deployment** method below exclusively.
- **NEVER modify or delete files in `/home/ubuntu/.ssh/` on EC2.**
- **NEVER run `rm -rf` or any recursive delete on EC2's home directory.**
- **Always back up `.env` BEFORE any file operations on EC2.** The `.env` contains production secrets that cannot be recovered if lost.
- **Use `grep -q` to avoid duplicate env vars** when appending to `.env`.

1. After creating the PR, poll for the PR to be merged. Use `git_view_pr` to check the PR status periodically (every 30 seconds) until the PR state is `merged`. Do not proceed until the PR is merged.
2. Once merged, pull the latest `main` locally:
   ```
   git checkout main && git pull origin main
   ```
3. Write the EC2 SSH key to a temp file:
   ```
   echo "$EC2_SSH_KEY" > /tmp/ec2_key.pem && chmod 600 /tmp/ec2_key.pem
   ```
4. Back up the `.env` on EC2 (it contains production secrets):
   ```
   ssh -i /tmp/ec2_key.pem -o StrictHostKeyChecking=no {EC2_USER}@{EC2_HOST_IP} "cp /home/ubuntu/.env /home/ubuntu/.env.bak"
   ```
5. **Set the new customer's environment variables in EC2's `.env` file.** Determine the service key value:
   - If the user specified an existing vertical's key to reuse (e.g., "use the same key as {DEMO_CUSTOMER_1}/{EXISTING_VERTICAL_ID_1}"), first retrieve that key's value from EC2:
     ```
     ssh -i /tmp/ec2_key.pem -o StrictHostKeyChecking=no {EC2_USER}@{EC2_HOST_IP} "grep DEVIN_SERVICE_KEY_{EXISTING_SLUG} /home/ubuntu/.env"
     ```
     Then set the new vertical's key to the same value.
   - If no reuse was specified, ask the user for the service key value.
   
   Add the env vars to EC2's `.env` (use `grep -q` to avoid duplicates):
   ```
   ssh -i /tmp/ec2_key.pem -o StrictHostKeyChecking=no {EC2_USER}@{EC2_HOST_IP} bash -s <<'EOF'
   grep -q 'DEVIN_SERVICE_KEY_{SLUG}' /home/ubuntu/.env || echo 'DEVIN_SERVICE_KEY_{SLUG}=<service-key-value>' >> /home/ubuntu/.env
   grep -q 'DEVIN_USER_ID_{SLUG}' /home/ubuntu/.env || echo 'DEVIN_USER_ID_{SLUG}=<user-id-value>' >> /home/ubuntu/.env
   EOF
   ```
   Replace `{SLUG}` with the customer's slug in UPPER_SNAKE_CASE (e.g., `A3F7B201`) and fill in the actual values. **This step is critical** — without these values in `.env`, the docker-compose env var passthrough (added in Phase 2) will resolve to empty strings and Devin session creation will fail.
6. Create a deployment tarball (excluding `node_modules`, `.git`, and `.env`):
   ```
   tar czf /tmp/demo-deploy.tar.gz --exclude=node_modules --exclude=.git --exclude=.env -C . .
   ```
7. Copy the tarball to EC2:
   ```
   scp -i /tmp/ec2_key.pem -o StrictHostKeyChecking=no /tmp/demo-deploy.tar.gz {EC2_USER}@{EC2_HOST_IP}:/home/ubuntu/demo-deploy.tar.gz
   ```
8. Extract the tarball on EC2:
   ```
   ssh -i /tmp/ec2_key.pem -o StrictHostKeyChecking=no {EC2_USER}@{EC2_HOST_IP} "cd /home/ubuntu && tar xzf demo-deploy.tar.gz"
   ```
9. Restore `.env` if it was overwritten:
   ```
   ssh -i /tmp/ec2_key.pem -o StrictHostKeyChecking=no {EC2_USER}@{EC2_HOST_IP} "test -f /home/ubuntu/.env || cp /home/ubuntu/.env.bak /home/ubuntu/.env"
   ```
10. Rebuild and restart the checkout-api container (zero-downtime):
    ```
    ssh -i /tmp/ec2_key.pem -o StrictHostKeyChecking=no {EC2_USER}@{EC2_HOST_IP} bash -s <<'EOF'
    cd /home/ubuntu
    docker compose build checkout-api
    docker compose up -d --no-deps checkout-api
    # Wait for health check
    for i in $(seq 1 30); do
      STATUS=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:3000/health || true)
      [ "$STATUS" = "200" ] && echo "Healthy on attempt $i" && break
      sleep 2
    done
    [ "$STATUS" != "200" ] && echo "HEALTH CHECK FAILED" && exit 1
    # Restart loadgen
    docker compose up -d --no-deps loadgen
    # Reconcile remaining services
    docker compose up -d
    EOF
    ```
11. **Verify the customer's env vars are inside the running container.** This confirms the full chain works: `.env` → `docker-compose.yml` passthrough → container environment:
    ```
    ssh -i /tmp/ec2_key.pem -o StrictHostKeyChecking=no {EC2_USER}@{EC2_HOST_IP} \
      "docker exec ubuntu-checkout-api-1 env | grep '{SLUG}' | sed 's/=.*/=<SET>/'"
    ```
    The output must show both `DEVIN_SERVICE_KEY_{SLUG}=<SET>` and `DEVIN_USER_ID_{SLUG}=<SET>`. If empty, check that:
    - The env var is set in EC2's `.env` file (step 5)
    - The env var is listed in `docker-compose.yml`'s `environment` block (Phase 2, step 11)
12. **Verify the page loads on production:**
    ```
    ssh -i /tmp/ec2_key.pem -o StrictHostKeyChecking=no {EC2_USER}@{EC2_HOST_IP} \
      "curl -s -o /dev/null -w '%{http_code}' http://localhost:3000/{vertical-id}"
    ```
    Must return `200`.

13. **CRITICAL: Verify the FULL Devin session pipeline end-to-end on production.** This is the step that was missing in past sessions and caused all 3 verticals to fail silently. Trigger the CTA endpoint on EC2:
    ```
    ssh -i /tmp/ec2_key.pem -o StrictHostKeyChecking=no {EC2_USER}@{EC2_HOST_IP} \
      'curl -s -X POST http://localhost:3000/api/{vertical-id}/inquiry \
       -H "Content-Type: application/json" \
       -d "{\"property\":\"test\",\"devinUserId\":\"{user-id}\",\"devinOrgId\":\"{org-id}\"}"'
    ```
    Then check the container logs for the FULL pipeline:
    ```
    ssh -i /tmp/ec2_key.pem -o StrictHostKeyChecking=no {EC2_USER}@{EC2_HOST_IP} \
      "docker logs ubuntu-checkout-api-1 --tail 30 2>&1 | grep -E 'alert|session|Devin|{vertical-id}'"
    ```
    You MUST see ALL 5 of these log lines (in order):
    1. `"Resolved customer-specific Devin config"` with `hasApiKey: true` and `hasDevinUserId: true`
    2. `"Posting alert and triggering Devin"` with the correct `devinOrgId`
    3. `"Alert posted to Slack"` with a `threadTs` value
    4. `"Devin session created via v3 API"` with a `sessionId`
    5. `"Devin session link posted to Slack thread"` with a `sessionUrl`
    
    **If any of these are missing, debug before reporting completion:**
    - Missing #1: Check `config/customers.js` has the customer entry, check env vars in container
    - Missing #2: Check service file catch block calls `createSessionAndAlert()` with correct params
    - Missing #3: Check `SLACK_BOT_TOKEN` and `SLACK_CHANNEL_ID` are set in EC2 `.env`
    - Missing #4: Check `DEVIN_SERVICE_KEY_{SLUG}` is set and the key has access to the target org
    - Missing #5: This only happens if #4 succeeded — check Slack thread posting

14. **Verify on public URL in browser** — open `https://devindemos.com/{vertical-id}` and:
    - Confirm the page loads with correct images and layout
    - Click the CTA button
    - Verify the error toast appears
    - Check the Slack channel (`#automated-alerts`) for the alert message with "View in Devin" button

15. Clean up the SSH key:
    ```
    rm -f /tmp/ec2_key.pem
    ```

Only create the TODO list for the current phase.

<verification>
- The PR has been merged into `main`
- The latest code has been deployed to EC2 at `{EC2_HOST_IP}`
- The customer's `DEVIN_SERVICE_KEY_{SLUG}` and `DEVIN_USER_ID_{SLUG}` env vars are set in EC2's `.env` file
- The customer's env vars are confirmed present inside the running container via `docker exec ... env`
- The health endpoint returns `200`
- The new vertical page returns `200` on EC2
- The FULL Devin session pipeline has been verified with a production CTA trigger — all 5 log lines confirmed:
  - Customer config resolved with `hasApiKey: true`
  - Alert posted to Slack with threadTs
  - Devin session created with sessionId
  - Session link posted to Slack thread
- The page has been verified at `https://devindemos.com/{vertical-id}` in the browser
- The `.env` file on EC2 is intact (not overwritten)
</verification>
</phase>

## Specifications

- The cloned page must be a **pixel-perfect visual clone** of the original customer page — the person who built the original page should not be able to distinguish the clone from the real thing at a glance.
- All visual assets (logos, images, icons, badges) must be the **real assets** hotlinked from the customer's CDN or downloaded locally — never placeholder blocks, CSS gradient approximations, or generic SVG replacements.
- All CSS values (colors, font sizes, spacing, border-radius, etc.) must be **extracted from the original page** using `getComputedStyle()`, not approximated or guessed.
- The page must be a single self-contained HTML file with inline CSS and JavaScript.
- The vertical ID must be a **random 8-character hex slug** — never a brand name, company name, or any identifiable string.
- **Do NOT add new form fields, input elements, or submit buttons.** Instead, take over an existing CTA button on the cloned page to trigger the hidden API call. Errors must be displayed as a **subtle toast fly-in** at the bottom-right of the page.
- The CTA button must be **reachable within 1–2 scrolls** from the top of the page. Trim decorative content sections if the original page is very long.
- The CTA's `fetch()` call must include the user-provided `devinUserId` and `devinOrgId` in the request body. The org ID must be **validated against the enterprise API** and use **hyphen format** (`org-xxx`, not `org_xxx`).
- The bug must trigger a runtime error every time the CTA is clicked — not intermittently. Any colleague doing the demo should be able to trigger it by simply clicking the button.
- The bug fix must require changes across multiple lines, functions, or files — it must NOT be a single-line fix. The root cause should span at least 2–3 functions or cross file boundaries.
- The bug must NOT be indicated by any comments, variable names, or code hints whatsoever.
- The error must be caught and wired to the alert pipeline via `createSessionAndAlert()`, which must include `customer: '{vertical-id}'` and `slackMemberId` (if provided by user).
- The vertical must only be accessible via its direct URL, not from the hub page.
- All files must pass `npm run lint`.
- **The customer must have an entry in `config/customers.js`** with the vertical slug, label, and `triggerMode: 'api'`.
- **The customer's per-customer env vars (`DEVIN_SERVICE_KEY_{SLUG}`, `DEVIN_USER_ID_{SLUG}`) must be added to `docker-compose.yml`** in the `checkout-api` service's `environment` block so they are passed into the container at runtime.
- **The customer's env var values must be set in EC2's `.env` file** before deployment so Docker Compose can resolve them.
- **Every image URL must return HTTP 200** — verified with curl before PR submission.
- **Every Unsplash image must be visually verified** to show the correct subject — Unsplash photo IDs are opaque.
- **Non-Unsplash images must have `onerror` fallbacks** to known-good Unsplash URLs.
- **Hero images with portraits must use `object-position: center center`** — NOT `center top` which crops to show only the top of the head.
- **The full Devin session pipeline must be verified on production** after deployment — not just health checks.
- No brand name leakage in code-level identifiers (title tags, CSS variable names, asset directory names, file names) — only the hex slug.
- Deliverable: A PR with the new vertical (with side-by-side screenshots), merged and deployed to EC2 with working Devin session triggering verified end-to-end.

## Advice and Pointers

- Always pull the latest `main` before branching. The repo is actively developed and skipping `git pull` will cause merge conflicts.
- **Use hex slugs, not brand names.** All custom demo verticals use anonymous 8-character hex slugs (e.g., `{EXISTING_VERTICAL_ID_1}`, `{EXISTING_VERTICAL_ID_2}`). Never use the customer's brand name as a slug — this was a recurring issue that required follow-up sessions to fix.
- **Use real assets, not approximations.** The single biggest cause of clones looking "off" is using placeholder gradients or generic icons instead of the actual images from the customer's CDN. Always extract and hotlink real asset URLs.
- **Add `onerror` fallbacks on non-Unsplash images.** CDNs can start blocking hotlinking at any time. In a past session, marriott.com and seb.se blocked hotlinking with 403 errors. The `onerror` fallback ensures the page degrades gracefully to an Unsplash alternative instead of showing a broken image icon.
- **Verify Unsplash image CONTENT, not just HTTP status.** In a past session, an Unsplash URL returned HTTP 200 but showed the wrong city — a "Philadelphia" replacement actually showed a completely different skyline. Always open Unsplash URLs in the browser and visually confirm the subject matches.
- **Use `object-position: center center` for portraits.** In a past session, `object-position: center top` caused a JPMC hero image to show only the top of Jamie Dimon's head instead of a proper business portrait. Use `center center` for any image containing a person.
- **Verify the logo renders correctly before moving on.** The brand logo is the most visually prominent element. In past sessions, malformed SVG paths or incomplete URLs caused broken logos that required follow-up fixes. After adding the logo to the HTML, immediately check it in the browser before continuing with other sections.
- **Extract CSS values programmatically.** Do not eyeball font sizes, colors, or spacing. Use `getComputedStyle()` in the browser console on the original page to get exact pixel values, then use those values directly in your CSS.
- Use the same text copy from the customer's page. Generic placeholder text like "Lorem ipsum" or invented marketing copy breaks the illusion.
- When comparing your clone to the original, work section by section (header → hero → content → footer) and fix each section completely before moving on.
- **Do NOT add new form fields or submit buttons.** Take over an existing CTA button on the page instead. This was explicitly requested to avoid drastically altering the page with custom form sections that don't exist on the original. The error should be shown as a subtle toast fly-in at the bottom, not in a form result box.
- **Keep the page compact for demos.** Long pages with many decorative sections require too much scrolling to reach the CTA. Trim excess content sections so the CTA is easily reachable.
- Take Puppeteer screenshots at the same viewport width for fair comparison — browser screenshots with different window sizes will show false differences.
- When hotlinking assets, always use absolute URLs (prepend the customer's domain to relative paths). Test each asset URL in the browser before using it — some CDNs block hotlinking or require specific referrer headers.
- **Validate and normalize the org ID BEFORE implementation.** This is one of the most common failure modes:
  - Org IDs use **hyphens** (`org-xxx`), not underscores (`org_xxx`). Always normalize to hyphen format.
  - Always verify the org exists by calling the enterprise organizations API. In a past session, an invalid org ID (`org_D7Q13p1Vtu23p4H1`) was used without verification, which caused the Devin session creation to fail with `404: "Organization not found"` — the Slack alert fired correctly but no Devin session was created. This required a full debug-and-fix PR cycle to resolve.
  - If the org ID doesn't exist, list the available orgs for the user to choose from. Don't guess or proceed with an unverified ID.
- **Verify the service key has access to the target org before deploying.** When reusing a service key from another vertical, that key may be scoped to a different org. In a past session, the {EXISTING_VERTICAL_ID_1} service key was scoped to `org-e603150b74c6424a85717880d7fc30ac` but was used with a different org, causing 404 errors. Test the key against the target org using the sessions API before wiring it up.
- **Docker Compose does NOT auto-load `.env` variables into containers.** Every per-customer env var that the app needs at runtime must be explicitly listed in the `environment` block of `docker-compose.yml`. If you add a new customer to `config/customers.js` but forget to add its env vars to `docker-compose.yml`, the container will not receive the service key and Devin session creation will fail with 403 Unauthorized. Always update both files together.
- **The env var naming convention is `DEVIN_SERVICE_KEY_{SLUG}`** where `{SLUG}` is the customer slug converted to UPPER_SNAKE_CASE (e.g., slug `a3f7b201` → `A3F7B201` → env var `DEVIN_SERVICE_KEY_A3F7B201`). The resolution logic is in `config/customers.js` — see the `getCustomerConfig()` function.
- **Always include `customer` in `createSessionAndAlert`.** Without `customer: '{vertical-id}'`, `getCustomerConfig()` falls back to the default customer and uses global env vars instead of the customer-specific ones. This was caught by Devin Review in multiple PRs.
- **Always include `slackMemberId` in `createSessionAndAlert`** for custom demo verticals, so the designated person is @mentioned in Slack alerts. This was a recurring miss that required follow-up PRs.
- **You have EC2 SSH access via the `EC2_SSH_KEY` secret.** Write it to `/tmp/ec2_key.pem` with `chmod 600` before use. Do not ask the user for SSH credentials — they are already available.
- **Service keys can be reused across verticals** but only if the key has access to the target org. If the user says to use the same key as another vertical (e.g., "use the same key as {DEMO_CUSTOMER_1}"), retrieve the existing key value from EC2's `.env` and verify it can access the target org before setting the new vertical's key to the same value. Do not blindly copy a key without verifying org access.
- **EC2 `.env` vars must be set BEFORE rebuilding containers.** In a past session, the env vars were missing from EC2's `.env` after the initial deploy, causing the Devin session to silently fail. The Slack alert fired (which doesn't need the service key), but `createDevinSession` returned null because the service key wasn't available in the container. Always set the env vars before `docker compose up`.
- **Always verify Devin sessions trigger on production — not just health checks.** In a past session, all 3 new verticals passed health checks and page-load checks, but none triggered Devin sessions because the per-customer env vars were missing from EC2's `.env`. The verification in Phase 4 step 13 (triggering the CTA and checking all 5 log lines) would have caught this immediately.
- **Use `grep -q` to avoid duplicate env vars.** When adding env vars to EC2's `.env`, use `grep -q 'VAR_NAME' .env || echo 'VAR_NAME=value' >> .env` to prevent appending duplicates if the playbook is run multiple times.
- **NEVER run destructive git commands on the EC2 host.** The EC2 host at `{EC2_HOST_IP}` is a production deployment target, NOT a development machine. Never run `git clean`, `git reset --hard`, `git checkout -- .`, or `rm -rf` on EC2. In a past session, `git clean -fd` was run on EC2 to resolve a merge conflict, which deleted `/home/ubuntu/.ssh/authorized_keys` and permanently locked Devin out of SSH. The user had to manually restore access via AWS Console. Use ONLY the tarball deployment method in Phase 4 — it avoids git entirely on EC2.
- **Tarball deployment is the ONLY approved deployment method for EC2.** Do NOT `git clone` or `git pull` on EC2. The tarball method (Phase 4, steps 6–9) safely copies code without touching `.env`, `.ssh`, or other local state. Using `git pull` on EC2 causes merge conflicts because `.env` has local modifications, and attempting to resolve those conflicts with `git clean` or `git checkout` has caused catastrophic data loss.
- **Auto-resolve user details before asking the user.** When the user references a person by name or says "reuse same as X", search existing vertical service files (`grep -r 'devinUserId' app/services/verticals/`) and EC2's `.env` to find the user ID, Slack member ID, and service key. Only ask the user for information that genuinely cannot be found in the codebase. In a past session, 4 separate messages were exchanged to collect information that was already available in existing vertical files.
- **Batch all questions into a single message.** If you need information from the user, collect ALL outstanding questions and ask them in ONE message. Never send multiple sequential messages each asking for one piece of information. This wastes the user's time and creates unnecessary back-and-forth.
- **When creating multiple verticals in one session,** complete each vertical end-to-end (all 4 phases) before starting the next. Do NOT create all verticals in Phase 2 and then deploy them all in Phase 4 — this makes it harder to debug if something goes wrong with a specific vertical.

## Forbidden Actions

- Do NOT add new form fields, input elements, submit buttons, or dashboard sections to the cloned page. Use an existing CTA button already on the page.
- Do NOT display errors in form result boxes or custom UI panels. Use a subtle toast fly-in at the bottom-right.
- Do NOT use placeholder images, CSS gradient blocks, or generic SVG icon replacements when the real asset URL is available from the customer's CDN.
- Do NOT approximate or guess CSS values — always extract them from the original page using `getComputedStyle()`.
- Do NOT use the customer's brand name as the vertical ID/slug — always generate a random hex slug.
- Do NOT use brand names in code-level identifiers (CSS variable names like `--ericsson-*`, asset directory names like `assets/ericsson/`, `<title>` tags with the company name). Only the hex slug.
- Do NOT add comments like `// BUG`, `// INTENTIONAL`, `// TODO`, `// FIXME`, or any hints about the bug.
- Do NOT add explanatory comments near the buggy code describing what should happen or what went wrong.
- Do NOT add the vertical to the `VERTICALS` array in `index.js`.
- Do NOT modify any existing vertical files or HTML pages.
- Do NOT skip the Puppeteer screenshot comparison between the clone and the original customer page.
- Do NOT skip the iterative section-by-section refinement step.
- Do NOT skip adding per-customer env vars to `docker-compose.yml` — this will cause Devin session creation to silently fail.
- Do NOT skip adding the customer entry to `config/customers.js` — this is required for env var resolution.
- Do NOT skip setting the env var values in EC2's `.env` file before rebuilding containers.
- Do NOT assume Docker Compose automatically passes all `.env` variables into containers — only explicitly listed variables are available.
- Do NOT omit `customer: '{vertical-id}'` from the `createSessionAndAlert` call — this causes the wrong API credentials to be used.
- Do NOT omit `slackMemberId` from the `createSessionAndAlert` call for custom demo verticals.
- Do NOT use an org ID without first validating it against the enterprise organizations API — invalid org IDs cause silent session creation failures.
- Do NOT use underscores in org IDs (`org_xxx`) — the canonical format uses hyphens (`org-xxx`). Always normalize.
- Do NOT assume a reused service key has access to the target org — always verify access before deploying.
- Do NOT create pages that require excessive scrolling to reach the CTA — trim decorative sections to keep the CTA accessible.
- Do NOT skip verifying that the brand logo renders correctly — a broken logo is the most visible flaw in a clone.
- Do NOT use `object-position: center top` for hero images containing people or portraits — this crops to show only the top of the head. Use `center center` instead.
- Do NOT skip curl-checking EVERY image URL for HTTP 200 status before submitting the PR — broken images were the #1 post-merge issue in past sessions.
- Do NOT assume an Unsplash image shows the correct subject based on the photo ID or URL — always open it in the browser and visually verify the content matches expectations.
- Do NOT skip the production Devin session pipeline verification (Phase 4, step 13) — this is the final gate that confirms the entire system works end-to-end. Health checks and page-load checks are NOT sufficient.
- Do NOT report the task as complete until all 5 pipeline log lines have been confirmed on production.
- Do NOT use non-Unsplash images without an `onerror` fallback — CDNs can start blocking hotlinking at any time.
- Do NOT run `git clean`, `git clean -fd`, `git reset --hard`, `git checkout -- .`, or ANY destructive git command on the EC2 host (`{EC2_HOST_IP}`). In a past session, `git clean -fd` on EC2 deleted `/home/ubuntu/.ssh/authorized_keys`, permanently locking Devin out of SSH access. The user had to manually restore access via AWS Console — this was a major incident that wasted significant time.
- Do NOT use `git pull` on the EC2 host for deployments. Git pulls cause merge conflicts with local `.env` changes and tempt you to run destructive git commands to resolve them. Use the **tarball deployment** method (Phase 4) exclusively.
- Do NOT modify, delete, or overwrite any files in `/home/ubuntu/.ssh/` on the EC2 host.
- Do NOT run `rm -rf` or any recursive delete command on the EC2 host's home directory or any parent directory containing `.ssh`.
- Do NOT ask the user for user ID, Slack member ID, or service key one-at-a-time in separate messages. Auto-resolve from existing verticals first (Phase 1, step 3), then batch ALL remaining questions into a single message. In a past session, 4 separate back-and-forth messages were needed to collect information that could have been auto-resolved or asked in one message.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
