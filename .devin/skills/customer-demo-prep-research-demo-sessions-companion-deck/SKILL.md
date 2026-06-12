---
name: customer-demo-prep-research-demo-sessions-companion-deck
description: "Converted from Devin playbook: Customer Demo Prep: Research → Demo Sessions → Companion Deck"
triggers:
  - user
  - model
---

# Customer Demo Prep: Research → Demo Sessions → Companion Deck

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Customer Demo Prep: Research → Demo Sessions → Companion Deck (playbook-289d30580af24bdfb1aaa498c598ef3a), macro `!demo_prep`

## Procedure

# Customer Demo Prep: Research → Demo Sessions → Companion Deck

## Overview
End-to-end playbook for preparing a customer-specific Devin demo. Given a target company and audience, it researches the account, maps their priorities to Devin use cases, assembles a set of demo scenarios (reusing existing COG-GTM repos/sessions or creating net-new ones), pre-runs the demo sessions (reuse-aware) so every demo has a live session + PR to walk through, and produces a customer-facing HTML companion deck following Cognition brand guidelines.

This is the motion behind the Jefferies, Citadel Securities, AmEx, SIG, MarketAxess, PIMCO, Experian, TIAA, and Northern Trust demo prep sessions.

## TL;DR (Quick Start)
1. Get **customer + audience + scenario count** (or meeting length) from the user (see *What's Needed From User*).
2. **Phase 1** — research the account; produce a cited research brief mapping priorities → Devin use cases.
3. **Phase 2** — pick demos; **Demo 1 is always the evented/autonomous flow**. Each demo binds to a real repo.
4. **Phase 3** — author each demo's prompt sequence (DeepWiki + 2× Ask Devin + high-fidelity execution prompt).
5. **Phase 4** — pre-run sessions (reuse-aware) so every demo has a live session/PR; compile a session map of canonical links.
6. **Phase 5** — build the customer-facing HTML deck (brand-compliant, one slide per demo, live session links).
7. **Phase 5.5** — red-team the full package (Roast Gate): demo-day simulation across 7 attack surfaces, fix Blockers, and produce a presenter-facing objection-handling guide.
8. **Phase 6** — deliver everything in one summary and iterate on feedback.

## What's Needed From User
- **Customer name + website/domain** (e.g., "Citadel Securities — citadelsecurities.com"). Disambiguate look-alikes (e.g., Citadel Securities the market maker vs. Citadel the hedge fund).
- **Audience / persona** for the demo (e.g., "COO of Technology", "exec", "platform eng leaders").
- **Number of demo scenarios** (default: 3–6). If a **meeting length** is given instead (e.g., "30-min slot"), scope the count to fit (~roughly one demo per 8–10 min) and note which demo to cut first.
- **Optional: discovery-call notes / explicit priorities** — if provided, these override generic research and drive both deck and demo selection.
- **Optional: deck export formats** — HTML by default; also produce PPTX and/or PDF only if the prompter asks.

The demo flow is fixed (see below) and sessions are always pre-run per the reuse rules in Phase 4 — these are not user choices.

## Related Playbooks & Knowledge (use these, don't reinvent)
- `@playbook:playbook-0f29f7cd2a0347eeab27188eb482aecb` — **Account Research** (`!account_research`)
- `@playbook:playbook-50252f7a0db9420b80d4cd81ea70639a` — **Create Bespoke Demo Surface for Event Driven Demos** (`!create-demo-page`) — for net-new evented incident-response verticals
- `@playbook:playbook-6bf2c8511cbc4ec9850d5a89c69c6f24` — **Clone Customer Page with Hidden Bug**
- Knowledge: **Cognition Brand Guidelines for Presentation Decks**, **Demo Narrative**, **Devin Use Case Mapping**, **Cognition Use Case Qualification & Discovery**, **Exa MCP for research**, **Skeptical-Exec Persona Question Banks** (Phase 5.5 roast).

## Default Demo Flow (all demos)
Every demo uses this loop: **DeepWiki → Ask Devin (discover) → Ask Devin (scope/plan) → Session with PR → Devin Review**.
1. **DeepWiki** — show instant architecture understanding of the repo (`deepwiki.com/<org>/<repo>`).
2. **Ask Devin #1 (discover)** — lightweight query that surfaces the repo's current state (e.g., audit files, analyze structure). Keep it short (≤2 sentences) — surfaces context, does not execute.
3. **Ask Devin #2 (scope/plan)** — short follow-up that scopes the work and produces a migration/fix plan, ending by recommending a full session to kick off. Keep it short (≤2 sentences) — still no execution.
4. **Session with PR** — full Devin session that executes the task and opens a PR (attach the relevant execution playbook). This prompt is high-fidelity, not short (see Phase 3).
5. **Devin Review** — Devin's automated review on that PR.

> Per the **Demo Narrative** knowledge, the progression is **Ask Devin (discover) → Ask Devin (scope/plan) → Devin session (execute with playbook)**. Each Ask Devin prompt is 2 sentences max.

## Reusable Demo Repo Catalog (starting point)
Consider **every repo this org has access to** — enumerate the org's repos (e.g., `git_list_repos` / `list_available_repos`) and pick the closest fit to the customer's stack/priorities. The table below is a high-signal starting point, not an exhaustive list. Prefer reuse before building anything new.

| Repo | Best demo use cases |
|------|---------------------|
| `traderXCognitiondemos` | Java 21 / Spring Boot upgrade, microservice extraction, OpenTelemetry observability, vuln remediation (trading platform) |
| `ftgo-monolith` | Monolith → microservices decomposition, coupling analysis |
| `bank` | Modern Java 21 / Spring Boot 3.x, CQRS/event sourcing, dependency-graph mapping, Jenkins CI/CD |
| `Springboot-BankApp` | DevSecOps on AWS EKS — Terraform generation, Sentinel governance policies, IaC docs, CVE remediation |
| `protobuf` / `llama.cpp` | C++ modernization (smart pointers, RAII, memory safety) |
| `aws-mainframe-modernization-carddemo`, `COBOL-Legacy-Benchmark-Suite` | COBOL / mainframe modernization to Java/Spring Boot |
| `spring-boot-realworld-example-app` | Spec-driven dev, REST↔GraphQL, article moderation |
| `Fixed-Income-RFQ-Trading-Platform` | Bug-fix loop, saga/edge-case auditing |
| `SampleMvcWebApp`, `angularjs-asp-net48-mvc5` | .NET / AngularJS modernization |
| `anypoint-examples` | MuleSoft → Spring Boot migration |
| `event-driven-devin` | Bespoke evented incident-response verticals (Sentry/Datadog alert → autonomous Devin fix) — **the canonical first/hero demo** |

<phase name="Research the Account" id="1">
## Research the Account

1. Run the **Account Research** playbook (or Exa MCP directly — `web_search_exa` with `category:company`/`category:people`) on the customer. Confirm the exact legal entity if the name is ambiguous.
2. Produce a short **research brief** covering: tech stack & primary languages, the named audience persona (role, mandate, recent public statements), 2–4 strategic engineering priorities, and 2–3 customer proof points relevant to their industry. Cite sources, and verify every cited URL resolves (not 404/403/redirect) and actually supports the claim — use `web_fetch_exa` (or open it) before including it. Never include plausible-but-unverified links.
3. Map each priority to specific Devin use cases (use the **Devin Use Case Mapping** knowledge). Pick the 1–2 strongest fits per priority — do not produce a laundry list.
4. Save the brief to a file (e.g., `<customer>_research_brief.md`) and present it in the session.

<verification>
- Customer entity is disambiguated and correct.
- Research brief identifies tech stack, persona, and 2–4 prioritized initiatives with citations.
- Each priority is mapped to concrete Devin use cases.
- Every cited source URL was verified with `web_fetch_exa` (or opened directly): it loads and its content supports the claim — no fabricated or plausible-but-wrong links.
- The brief is saved as a file and shown in the session.
</verification>
</phase>

<phase name="Select & Map Demo Scenarios" id="2">
## Select & Map Demo Scenarios

1. **Demo 1 is always the evented / API-driven / autonomous-engineer flow** — lead with an incident-response or API-triggered loop where an alert/event autonomously spins up a Devin session that fixes the issue and opens a PR. Build it via the **Create Bespoke Demo Surface** playbook (themed vertical in `event-driven-devin`: planted bug → alert → autonomous Devin session) themed to the customer's domain, then return here. This is the hero scenario.
2. For the remaining prioritized use cases, choose demo scenarios and bind each to a repo (search the full org repo list; the catalog is a starting point). Prefer reuse; only build net-new when no repo fits.
3. Confirm the chosen repo actually contains the artifacts the demo claims (e.g., don't promise a Terraform demo on a repo with no IaC — either pick another repo or scope the demo to "generate from scratch").
4. Scope the number of demos to the meeting length if one was given, ordering strongest-fit first so the weakest is the natural cut.
5. Produce a **demo plan table**: Demo # | Use case | Repo | Status (reuse/new). Demo 1 = evented/autonomous flow.

<verification>
- Demo 1 is an evented / API-driven / autonomous-engineer flow.
- Each demo is mapped to a real repo whose contents support the demo narrative.
- Net-new surfaces (if any) are built and verified before being included.
- A demo plan table exists with one row per scenario, ordered by strength of fit.
</verification>
</phase>

<phase name="Author Demo Prompt Sequences" id="3">
## Author Demo Prompt Sequences

For each demo, write the artifacts for the default flow:

1. **DeepWiki** — the `deepwiki.com/<org>/<repo>` link to open.
2. **Ask Devin (discover)** — a short prompt that surfaces the repo's current state (≤2 sentences; does not execute).
3. **Ask Devin (scope/plan)** — a short follow-up that scopes the work and produces a plan, recommending the full session to kick off (≤2 sentences; does not execute).
4. **Session (PR)** — the execution prompt that does the real work and opens a PR. Reference the relevant execution playbook via `@playbook:playbook-<id>` (e.g., Spring Boot/Java upgrade, COBOL modernization, microservice extraction, vuln remediation) where one exists.
5. **Devin Review** — note that Devin Review runs on the resulting PR.

Prompt fidelity:
- Both **Ask Devin** prompts are short (≤2 sentences) — discovery/scoping only, no execution.
- **Session (PR)** prompts do **not** need to be short. Capture the task with **as high fidelity as possible** so it reflects exactly what a human software engineer would actually do for that customer — include concrete scope, target versions, constraints, acceptance criteria, and the expected PR outcome.
- Save all prompts to a `*_demo_sessions.md` doc and present it in the session.

<verification>
- Every demo has DeepWiki link + two Ask Devin prompts (discover, scope/plan) + execution prompt + Devin Review step.
- Ask Devin prompts are short; execution prompts are high-fidelity and reference an appropriate `@playbook:` where one exists.
- A demo-sessions doc is produced and shown in the session.
</verification>
</phase>

<phase name="Pre-Run Demo Sessions (reuse-aware)" id="4">
## Pre-Run Demo Sessions (reuse-aware)

Apply this rule to every demo's session(s):
- **Net-new demo** → pre-run it as child session(s) so there is a live, finished session + PR to walk through.
- **Reusable existing session that is fresh** → just point to and reuse the existing session/PR; do not re-run.
- **Reusable session that has gone stale** (PR merged/closed/conflicting, repo moved on, broken links) → pre-run it again as a duplicate session.

Steps:
1. For each demo, decide reuse vs. new vs. stale-duplicate per the rule above.
2. Launch needed child sessions (attach the execution playbook on the Session-with-PR step) — follow the **managing-child-sessions** skill: create them with `devin_session_create`, passing the high-fidelity Phase 3 execution prompt, then wait for them to reach a settled state with `devin_session_gather` before collecting links. Confirm PRs with green CI where applicable.
3. Compile a **session map** with clickable links for every demo (DeepWiki link / Ask Devin session / Session+PR / Devin Review), marking each as reused or freshly run. This map is the single source of truth for the links the deck consumes in Phase 5 — capture the exact canonical URLs here.
4. If the user wants shareable artifacts, record short mp4 walkthroughs (session UI in dark mode reads best on camera).

<verification>
- Every demo resolves to a live session/PR via reuse, new run, or stale-duplicate run.
- Freshly run execute sessions produced PRs (CI passing where relevant).
- A complete session map with canonical links is delivered, labeled reused vs. run.
</verification>
</phase>

<phase name="Build the Companion Deck" id="5">
## Build the Companion Deck

Build a **customer-facing HTML slide deck** (one slide per screen, arrow-key/spacebar/click nav, dot nav) following the **Cognition Brand Guidelines** knowledge:
- Dark charcoal-navy background `#1A1F2E`, teal `#4ECDC4` as the only accent, white headings, light-gray body. Title format `White text [teal keyword]`.
- Footer on every slide: `✦ Cognition` / `PROPRIETARY & CONFIDENTIAL` / page number. 16:9 (13.333in × 7.5in).
- One slide per demo with two cards: **"How It Works"** (DeepWiki → Ask Devin → Session/PR → Devin Review) and **"What This Enables"** (value props).
- **Session links/buttons:** each demo slide must include a user-friendly link or button that opens the relevant Devin session (and PR) directly, wired to the **canonical URLs from the Phase 4 session map** — never placeholders. The presenter must be able to jump straight to the live session from the deck.

**Visual style — use-case slides should look McKinsey-like (critical):**
- **Visual-first, not text-heavy.** Each use-case slide leads with a diagram/visual, not paragraphs. Cap body copy to short phrases/bullets (aim ~3–5 bullets, ≈7 words each); no dense sentences or wall-of-text cards.
- **Use icons / inline SVGs and simple diagrams** to carry meaning: e.g., a left→right flow diagram for How-It-Works (DeepWiki → Ask Devin → Session/PR → Review), simple before/after or step graphics, and a small line icon per value prop.
- **Keep icons on-brand and purposeful, not decorative clutter:** render icons/SVGs as clean line art in the brand palette (teal `#4ECDC4`, white, gray) — monochrome, consistent stroke weight, no emojis, no stock clip-art, no multi-color icon packs. (This intentionally overrides the brand-guideline's blanket "no icons" note: tasteful single-color iconography is wanted; decorative emojis/icons are not.)
- Prefer consistent, repeatable visual motifs across use-case slides so the deck feels like one designed system.

**Customer-facing rules (critical):**
- No internal references: no repo names (e.g., `COG-GTM/...`), no contact names, no `@playbook` syntax, no "Target Repository" labels.
- No talking points, coaching notes, or raw prompt boxes on the slides.
- Large, legible text. Generous whitespace.

**Verify before delivering:** open the `index.html` in the browser and confirm slides render correctly (no overflow/clipping), navigation works (arrows/spacebar/dots), the brand palette is applied, and every session link/button resolves to a live URL.

**Output (do NOT deploy):** produce the `index.html` file and attach it in the session. Also generate a PPTX and/or PDF export **only if the prompter asks**. Add a numeric access gate if the user wants one.

<verification>
- Deck strictly follows brand palette (teal-only accent, dark background).
- Deck is fully customer-facing — no repo names, internal names, prompts, or talking points.
- Use-case slides are visual-first and not text-heavy: each has a diagram/icons (clean teal/white/gray line SVGs), short bullets only, no dense paragraphs.
- One slide per demo with How-It-Works + What-This-Enables, plus a button/link to open the relevant session.
- Deck was opened and verified: slides render, navigation works, and all session links resolve to live URLs.
- HTML is attached (not deployed); PPTX/PDF produced only if requested.
</verification>
</phase>

<phase name="Red-Team the Output (Roast Gate)" id="5.5">
## Red-Team the Output (Roast Gate)

Before delivering anything, run an adversarial "demo-day simulation" against the full package — in the persona of a skeptical customer exec and of the AE presenting tomorrow with zero prep. Do not review artifacts as files; simulate the meeting.

Attack the 7 surfaces and score each 0–2 (0 = meeting-embarrassing, 1 = degraded, 2 = clean):

1. **Link integrity (hard gate)** — click every link/button in the deck, session map, and brief from a clean browser context. Placeholder buttons, 404s, sessions still running/errored, unmerged-PR demo URLs, or unindexed DeepWiki pages = fail.
2. **Demo-day readiness (hard gate)** — re-run the hero demo end-to-end *now*: app loads → planted bug fires → alert pipeline triggers → session visible. Confirm the app will survive until meeting time (deployed, not a local VM process) and note a redeploy contingency. Any third-party leg (Slack/Sentry/Jira) must be verified with real credentials or explicitly flagged as unverified in the delivery message.
3. **Approval/dependency deadlock** — list everything still blocked on a human (child-session approvals, merges, expired integration auth). Never present the package as done while a dependency silently waits; ping the user with what's blocked and the deadline relative to the meeting.
4. **Customer-facing hygiene** — grep the deck source (not just the render) for internal repo names, `@playbook`, contact names, prompts, session IDs, emojis, placeholder text.
5. **Skeptical-exec narrative + objection guide** — role-play the exec personas attending (CIO: risk/governance/vendor/cost; CTO: differentiation/build-vs-buy/demo-depth — e.g., "your demo repo is a toy, show me at our scale", "we built our own agents, why you?"; SVP Eng: ROI/adoption/operational reality — "what metric moves in 90 days, measured how?"; Chief Architect: correctness/integration — "prove behavioral equivalence on the COBOL demo", "show me the worst PR Devin opened") and fire their hardest questions at every slide and claim — use the **Skeptical-Exec Persona Question Banks** knowledge for the full per-persona question list. Every demo ties to a cited priority; every citation re-opened and confirmed; every slide metric traceable to a linked session or labeled illustrative; persona titles re-validated as current. **Required output:** `<customer>_objection_guide.md` — an internal, presenter-facing objection-handling guide built only for the personas actually attending (per the research brief): each objection with likelihood anchored to a specific research signal, a ≤2-sentence verbatim first response, an evidence link that resolves (session/PR/slide/case study), a discovery pivot question to ask back, and a trap-to-avoid (never bluff — mark unanswerables with an honest deflection); plus the 3 "hardest questions in the room" with rehearsed 30-second answers. This guide is delivered alongside the deck in Phase 6 but is never embedded in customer-facing slides.
6. **Brand & visual** — palette/footer/16:9 checks at two resolutions; no wall-of-text cards; every use-case slide leads with a diagram; PDF/PPTX exports checked page-by-page if produced.
7. **Presenter logistics** — meeting runnable from the deck alone; demo count fits the time box with the cut demo marked; one-line fallback per demo (recording / merged PR / screenshots) if anything live is down.

Produce a short **roast report** (per-surface score, findings tagged Blocker / Embarrassing / Polish, exact fix). Fix all Blockers, re-roast failed surfaces (max 2 cycles), and carry any residual risks honestly into the Phase 6 delivery message.

When time allows, run the roast as a **fresh-eyes child session** given only the artifact links and this checklist — no build context, so it cannot rationalize its own work.

<verification>
- Surfaces 1 and 2 score 2/2; no surface scores 0.
- A roast report exists with findings and fixes; Blockers are resolved.
- An objection-handling guide exists, is customer-specific (≥3 objections cite research-brief signals), covers each attending persona, and every evidence link resolves.
- Anything still blocked on the user (approvals, expired auth) is explicitly called out with the meeting deadline.
- Residual risks (e.g., unverified Slack leg) appear verbatim in the delivery message.
</verification>
</phase>

<phase name="Deliver & Iterate" id="6">
## Deliver & Iterate

1. Deliver in one summary: research brief, demo plan, demo-sessions doc, session map (if pre-run), deck link, roast report, and the objection-handling guide — all viewable in the session.
2. Iterate on feedback. Common asks observed: make deck fully customer-facing, make use-case slides less text-heavy / more visual (McKinsey-like icons & SVGs), increase text size, remove specific slides, add per-demo session links, retune around discovery-call priorities, produce downloadable mp4s.

<verification>
- All deliverables are presented in the session (not just saved to disk).
- Outstanding user feedback has been incorporated.
</verification>
</phase>

## Specifications
- Deliverables: a research brief, a demo plan table, a demo-sessions prompt doc, a session map with live links (when pre-run), a customer-facing `index.html` deck, a roast report, and a presenter-facing `<customer>_objection_guide.md` — all presented in the session.
- The package passes the Phase 5.5 Roast Gate before delivery: link integrity and demo-day readiness score 2/2, no surface scores 0, and residual risks are stated verbatim in the delivery message.
- Demo 1 is always the evented / autonomous-engineer flow; every demo maps to a real repo that supports its narrative.
- Every demo resolves to a live session/PR (reuse, new, or stale-duplicate), and each deck slide links to its canonical session URL.
- The deck follows Cognition brand guidelines, contains no internal references, and is verified to render and navigate before delivery.
- Validation: open the deck and click through every slide and every session link, confirming rendering, navigation, and that all links are live.

## Forbidden Actions
- Do not deploy the deck to devinapps.com (or any public host) — produce the HTML file and attach it; add PPTX/PDF only on request.
- Do not put internal repo names, contact names, prompts, or talking points on customer-facing deck slides.
- Do not make use-case slides text-heavy — lead with visuals/diagrams/icons; no wall-of-text cards.
- Do not use emojis, stock clip-art, or multi-color icon packs — icons/SVGs must be clean single-color line art in the brand palette.
- Do not use any accent color other than teal, or a light/white deck background.
- Do not promise a demo a repo can't actually support — verify repo contents first.
- Do not skip the evented/autonomous flow as Demo 1.
- Do not re-run a fresh reusable session — reuse it; only duplicate-run if it has gone stale.
- Do not deliver a deck whose session buttons are placeholders — wire canonical URLs or explicitly mark the package as draft-pending-approvals.
- Do not claim the package is final while child-session approvals or integration auth (e.g., Jira) are pending.
- Do not embed the objection-handling guide (or any of its content) in customer-facing slides — it is an internal presenter doc only.

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
