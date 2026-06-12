---
name: outreach-message-generator
description: "Converted from Devin playbook: Outreach Message Generator"
triggers:
  - user
  - model
---

# Outreach Message Generator

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Outreach Message Generator (playbook-61fe083ab9504906b399488852b2b013), macro `!outreach_gen`

## Procedure

# Outreach Message Generator

## Overview
Generate personalized outreach messages (LinkedIn or email) for the user's accounts at Cognition. Each message is tailored to the prospect's profile, the account's status, and the appropriate channel, all written in the user's personal voice.

## How to Invoke
```
!outreach_gen [AccountName] [ProspectName or LinkedIn URL] [email|linkedin]
```

The user may also include:
- **Message thread history** — paste previous messages exchanged with this prospect or account
- **Account context** — account overview, stakeholder map, active use cases, strategy notes
- **Additional direction** — specific angle, tone adjustment, what to lead with, what to avoid
- **LinkedIn profile** — pasted profile text for the prospect

All of the above are optional. The playbook will research what it can and generate the best message with whatever context is provided.

---

## Procedure

### Step 1: Parse Inputs
Extract from the user's prompt:
- **Account name** (required)
- **Prospect name or LinkedIn URL** (required)
- **Channel**: email or linkedin (required, defaults to linkedin if ambiguous)
- **Message thread history** (if pasted)
- **Account context** (if pasted)
- **Additional direction** (if provided)
- **LinkedIn profile data** (if pasted)

### Step 2: Load Knowledge Context
1. Read the user's messaging style guide: search knowledge notes for "the user — Outreach Messaging Style Guide" via `devin_knowledge_manage` → `list` with search query, then `get` the note. **This is the primary style reference. Every message must conform to it.**
2. Read the Anti-AI Messaging Rules knowledge note (search for "Anti-AI Messaging Rules"). Apply all rules.
3. Read the Devin Pre-Approved Customer List (search for "Pre-Approved Customer List"). Only cite verified customers.
4. Read the Devin Use Case Mapping note (search for "Devin Use Case Mapping"). Use for personalization.
5. Read the Devin Customer Proof Points note (search for "Customer Proof Points"). Use stats when relevant.
6. If the account name matches any existing account knowledge note (e.g., "{ACCOUNT_NAME} — Account Knowledge", "{ACCOUNT_NAME} — Account Knowledge"), read that note too.
7. If this is an email, also read the "Outreach Email Style Guide & Framework" note and apply its structure.
8. If this is an email, also read the "Gong Cold Email Best Practices" note.

### Step 3: Research the Prospect via Exa MCP
Use the Exa MCP server (`exa-mcp-server`) to research the prospect. Run these searches in parallel:

1. **LinkedIn profile search**:
   - `web_search_exa` with query: `category:people [ProspectName] [CompanyName]`
   - This returns career history, education, skills, connection count, recent activity

2. **Content search** (posts, talks, articles):
   - `web_search_exa` with query: `"[ProspectName] [CompanyName] blog post OR conference talk OR interview OR podcast"`

3. **Company intel** (if not already provided by user):
   - `web_search_exa` with query: `category:company [CompanyName] technology initiatives`

4. **Deep dive** on any promising URLs from search results:
   - `web_fetch_exa` on the best 2-3 URLs for full content

**From the research, extract:**
- Current role and title (for persona-level calibration)
- Tenure at company
- Previous employers (cross-reference against Devin customer list)
- Connection count (if <500, flag for email-only)
- Recent posts or content (best hook material)
- Education, location, interests (for personal connection triggers)
- Specific technical focus areas (for use case mapping)

### Step 4: Classify the Situation
Based on inputs and research, determine:

**Account type:**
- Existing customer (Devin/Windsurf already deployed)
- Warm (adjacent relationship or known footprint)
- Cold (no existing relationship)

**Prospect persona level:**
- CxO / VP
- Sr. Director / Director
- Principal / Staff Engineer
- IC / Early career
- Product / Non-engineering leader

**Touch number:**
- First touch (connection request or cold outreach)
- Follow-up (2nd-3rd message)
- Re-engagement (4+ messages, no response)
- Response to their reply

**Channel:**
- LinkedIn connection request (shortest)
- LinkedIn DM
- Email

### Step 5: Select the Hook
Using the hook hierarchy from the user's style guide, pick the single strongest hook:
1. Content hook (their post, podcast, talk)
2. Relationship hook (internal partnership, existing deployment)
3. Profile hook (new role, hiring activity)
4. Signal hook (profile view, mutual connection)
5. Industry/peer proof

If the user provided additional direction on what to lead with, that overrides this hierarchy.

### Step 6: Select Social Proof
- Pick 2-3 customer names most relevant to the prospect's industry from the Pre-Approved Customer List
- Select the most relevant stat or proof point from Customer Proof Points
- If this is an existing customer account, lead with internal stakeholder names instead of external proof

### Step 7: Map Use Cases
- Using the prospect's technical background and company initiatives, map to 1-2 Devin use cases from the Use Case Mapping note
- Only use custom use cases if there's real evidence from research
- Default to: large-scale migrations, async bug fixes/vuln remediation, documentation generation

### Step 8: Draft the Message
Write the message following the user's style guide precisely. Key rules:

**Structure (LinkedIn DM / first touch):**
1. Opener ("Hi [Name], great to connect!" or appropriate variant)
2. Hook (1-2 sentences: content reference, relationship anchor, or profile observation)
3. Positioning + value prop (1-2 sentences: adapt Devin/Windsurf framing to persona level)
4. CTA (1 sentence: interest-based, persona-appropriate)

**Structure (Email):**
Follow the Outreach Email Style Guide:
1. Opener (1 sentence): Trigger acknowledgment + 3 industry-relevant customer names
2. Personalization (1-2 sentences): Research-backed connection to Devin use case
3. CTA (1 sentence): Interest-based

**Structure (Follow-up / Re-engagement):**
- Shorter. 2-3 sentences.
- Bring new value (new stat, new angle, recent news)
- If 4+ messages unanswered, use breakup style

**Hard style rules (always enforce):**
- No em dashes, en dashes, or hyphens used as dashes (rewrite to avoid)
- No banned phrases (see style guide)
- No semicolons
- Contractions always
- Vary sentence length
- Don't start multiple sentences with "I"
- Skimmable in under 15 seconds
- Interest-based CTA, never time-based

### Step 9: Self-Review Checklist
Before presenting the message, verify:
- [ ] No em dashes, en dashes, or hyphens-as-dashes anywhere
- [ ] No banned phrases from Anti-AI rules or style guide
- [ ] All customer names cited are on the Pre-Approved list
- [ ] Message length matches channel (LinkedIn DM: 4-6 sentences, email: 5-6, CR: 2-4)
- [ ] CTA is interest-based, not time-based
- [ ] Persona level matches the framing
- [ ] Hook is the strongest available from research
- [ ] No multiple sentences starting with "I" in a row
- [ ] Contractions used throughout
- [ ] If prospect has <500 LinkedIn connections, flag that email may be better channel
- [ ] Message sounds like Joshua wrote it in 2-3 minutes, not like an AI drafted it

### Step 10: Present to Joshua
Present the final message(s) to Joshua with:

1. **The message** (ready to copy-paste)
2. **Brief rationale** (2-3 bullets max):
   - Why this hook was chosen
   - Key research findings that informed the personalization
   - Any flags (e.g., prospect may have left company, <500 connections, 4+ unanswered messages)
3. **Optional: one alternate version** if there are two strong angles
4. **If any additional context would strengthen the message**, ask Joshua for it

---

## Reference Knowledge Notes
- **the user's Style Guide**: Search for "the user — Outreach Messaging Style Guide"
- **Anti-AI Rules**: Search for "Anti-AI Messaging Rules"
- **Customer List**: Search for "Pre-Approved Customer List"
- **Use Case Mapping**: Search for "Devin Use Case Mapping"
- **Customer Proof Points**: Search for "Customer Proof Points"
- **Email Framework**: Search for "Outreach Email Style Guide & Framework"
- **Gong Best Practices**: Search for "Gong Cold Email Best Practices"
- **Competitive Positioning**: Search for "Competitive Positioning vs Claude Code & Cursor"
- **Cognition Platform Overview**: Search for "Cognition Platform & Agentic SDLC Overview"

## Tools
- **Exa MCP** (`exa-mcp-server`): Primary research tool for LinkedIn profiles and company intel
  - `web_search_exa`: Semantic search with `category:people` or `category:company`
  - `web_fetch_exa`: Deep-read specific URLs for full content
- **Knowledge Notes** (`devin_knowledge_manage`): Load account context and style rules

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
