---
name: playbooks-to-skills
description: Retrieve Devin playbooks and transform them into reusable Devin CLI skills
argument-hint: "[playbooks.json|--remote] [--out .devin/skills]"
allowed-tools:
  - read
  - grep
  - glob
  - exec
permissions:
  ask:
    - Exec(curl*)
    - Write(.devin/skills/**)
    - Write(~/.config/devin/skills/**)
triggers:
  - user
  - model
---

Transform Devin playbooks into reusable skills.

## When to Use

Use this skill when the user wants to:

- Convert Devin Cloud/team/org playbooks into Devin CLI skills
- Migrate playbook bodies into `.devin/skills/<name>/SKILL.md`
- Generate skill skeletons from exported playbook JSON
- Compare playbooks and merge overlapping workflows into durable skills

## Inputs

Accept either:

1. A local JSON export containing playbooks, or
2. `--remote`, meaning retrieve playbooks from Devin Cloud through the Devin API.

For remote retrieval, require:

- `DEVIN_API_KEY` set in the environment
- `DEVIN_ORG_ID` set in the environment, or an org ID supplied by the user
- A service user API key with `ManageAccountPlaybooks` permission

Do not ask the user to paste API keys into chat. Ask them to export credentials in their shell environment instead.

## Workflow

1. Determine the source:
   - If the user provided a JSON file, read it.
   - If the user asked for remote retrieval, use `scripts/fetch-playbooks.sh`.
2. Inspect playbook titles, macros, and bodies.
3. Group playbooks that describe the same durable workflow.
4. For each workflow, create or update a skill under the requested output directory, defaulting to `.devin/skills/`.
5. Preserve playbook trigger conditions in the skill's "When to Use" section.
6. Preserve operational steps as concise ordered instructions.
7. Move examples, commands, checklists, and API calls into the skill body.
8. Remove session-specific details, one-off assumptions, credentials, and user/private data.
9. Cite source playbook titles and IDs in the generated skill body.
10. Verify each generated `SKILL.md` has frontmatter, a clear trigger/use section, required inputs, and step-by-step behavior.

## Remote retrieval

Use the Devin API when running in Devin Cloud or when no local playbook files are available:

```bash
.devin/skills/playbooks-to-skills/scripts/fetch-playbooks.sh > playbooks.json
```

Prefer v3 org-level endpoints:

- `GET /v3/organizations/{org_id}/playbooks` to list playbooks
- `GET /v3/organizations/{org_id}/playbooks/{playbook_id}` to fetch a full playbook

Page list results with `first` and `after` until `has_next_page` is false.

Only use legacy v1 endpoints when the user explicitly asks for legacy team playbook access.

## Generation helper

For a deterministic first pass from JSON to skill skeletons, run:

```bash
python3 .devin/skills/playbooks-to-skills/scripts/generate-skills.py playbooks.json --out .devin/skills
```

Then review and refine the generated skills manually. The script intentionally creates conservative skeletons; the agent should merge duplicates, improve triggers, and remove stale details.

## Safety

- Never print or store API tokens.
- Never include credentials, personal data, or session-only assumptions in generated skills.
- Ask before overwriting an existing skill unless the user explicitly requested updates.
- Ask before deleting or archiving existing playbooks or skills.
