---
name: skills-parser
description: "Converted from Devin playbook: Skills Parser"
triggers:
  - user
  - model
---

# Skills Parser

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Skills Parser (playbook-8b1940d92e2d406583f5589d01444cd4), macro `!skills_parser`

## Procedure

# Skills Parser Playbook

## Overview

This playbook converts Anthropic Skills files (SKILL.md format) into Devin-compatible playbooks and knowledge entries. Use this when integrating any repository that uses the Anthropic Agent Skills format into Devin.

## What's Needed From User

- Path to a skill directory or skills parent directory containing SKILL.md files
- Optional: Output directory for generated markdown files (defaults to current directory)

## Procedure

1. Clone or navigate to the repository containing Anthropic Skills files
   - Skills are typically located in a `/skills` directory
   - Each skill is a subdirectory containing a `SKILL.md` file

2. Locate the Skills Parser scripts in the COG-GTM/skills repository
   - Clone: `git clone https://github.com/COG-GTM/skills.git`
   - Scripts are at: `skills/skills-parser/scripts/`

3. Validate the skill structure before parsing
   ```bash
   python skills/skills-parser/scripts/validate_skill.py <path-to-skill-directory>
   ```
   - Confirms SKILL.md exists and is readable
   - Validates YAML frontmatter has required `name` and `description` fields
   - Checks name follows conventions (lowercase, hyphens, max 40 chars)
   - Validates Python scripts have correct syntax
   - Use `--strict` flag to treat warnings as errors

4. Parse a single skill to generate Devin files
   ```bash
   python skills/skills-parser/scripts/parse_skill.py <path-to-skill-directory> --output-dir ./devin-output
   ```
   - Generates `<skill-name>-playbook.md` (Devin playbook)
   - Generates `<skill-name>-knowledge.md` (Devin knowledge entry)
   - Use `--quiet` flag to suppress output except errors

5. For batch processing multiple skills
   ```bash
   python skills/skills-parser/scripts/batch_parse.py <skills-directory> --output-dir ./devin-output
   ```
   - Discovers all subdirectories containing SKILL.md files
   - Creates organized output structure:
     - `devin-output/playbooks/*.md`
     - `devin-output/knowledge/*.md`
   - Use `--stop-on-error` to halt on first failure
   - Use `--quiet` to suppress detailed output

6. Present the generated files as deliverables to the user
   - Send a message to the user with all generated playbook and knowledge files attached
   - For single skill parsing: attach both `<skill-name>-playbook.md` and `<skill-name>-knowledge.md`
   - For batch processing: attach all files from `playbooks/` and `knowledge/` directories
   - Include a summary listing each generated file with its skill name

7. Review generated files for accuracy
   - Playbooks include: title, description, "When to Use" section, full instructions, available scripts, references, and assets
   - Knowledge entries include: overview, detailed instructions, categorized resources, source attribution

8. Instruct user on adding files to Devin
   - Add `*-playbook.md` files as Devin playbooks in organization settings
   - Add `*-knowledge.md` files as Devin knowledge entries

## Specifications

**Input Format (Anthropic Skills):**
```
skill-name/
├── SKILL.md (required)
│   ├── YAML frontmatter (name, description)
│   └── Markdown instructions
└── Bundled Resources (optional)
    ├── scripts/          - Executable code
    ├── references/       - Documentation
    └── assets/           - Templates, images, etc.
```

**SKILL.md Format:**
```yaml
---
name: skill-name
description: What the skill does and when to use it
---

# Skill Title

Instructions and guidelines...
```

**Output:**
- Devin playbook markdown file with structured instructions
- Devin knowledge markdown file with reference information
- Both files include source attribution and generation timestamp

**Validation Rules:**
- Name: lowercase letters, numbers, hyphens only (`^[a-z0-9-]+$`)
- Name: max 40 characters, no leading/trailing/consecutive hyphens
- Description: non-empty, minimum 20 characters recommended
- SKILL.md must exist in skill root directory
- Python scripts must have valid syntax

**Success Criteria:**
- All skills validate without errors
- Generated playbook and knowledge files are created
- All generated files are presented to the user as attachments in the session
- Files are ready for immediate use in Devin without additional formatting

## Advice and Pointers

- The parser uses a three-level loading system: metadata (always loaded), SKILL.md body (when triggered), bundled resources (as needed)
- Generated playbooks include full paths to scripts, references, and assets from the original skill
- Batch processing continues on errors by default; use `--stop-on-error` to change this behavior
- Directory names should match the skill name in frontmatter for consistency

## Forbidden Actions

- Do not modify the original SKILL.md files during parsing
- Do not skip validation before parsing production skills
- Do not manually edit generated files if re-parsing is expected (changes will be overwritten)

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
