#!/usr/bin/env python3
import argparse
import json
import re
from pathlib import Path


def slugify(value):
    value = value.lower().strip()
    value = re.sub(r"[^a-z0-9]+", "-", value)
    return value.strip("-") or "converted-playbook"


def load_playbooks(path):
    data = json.loads(Path(path).read_text())
    if isinstance(data, list):
        return data
    for key in ("playbooks", "items", "data", "results"):
        value = data.get(key)
        if isinstance(value, list):
            return value
    raise SystemExit("Could not find playbooks array in JSON")


def clean_body(body):
    body = (body or "").strip()
    body = re.sub(r"(?i)(authorization:\s*bearer\s+)[^\s`\'\"]+", r"\1<token>", body)
    body = re.sub(r"cog_[A-Za-z0-9_\-]+", "<DEVIN_API_KEY>", body)
    return body


def render_skill(playbook):
    title = playbook.get("title") or playbook.get("name") or playbook.get("macro") or "Converted Playbook"
    playbook_id = playbook.get("playbook_id") or playbook.get("id") or "unknown"
    macro = playbook.get("macro")
    body = clean_body(playbook.get("body") or playbook.get("content") or playbook.get("prompt") or "")
    skill_name = slugify(title)
    description = f"Converted from Devin playbook: {title}".replace('"', "'")
    source_line = f"- Source playbook: {title} ({playbook_id})"
    if macro:
        source_line += f", macro `{macro}`"
    return skill_name, f"""---
name: {skill_name}
description: \"{description}\"
triggers:
  - user
  - model
---

# {title}

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

{source_line}

## Procedure

{body or 'Review the source playbook and replace this placeholder with the durable procedure.'}

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
"""


def main():
    parser = argparse.ArgumentParser(description="Generate Devin CLI skill skeletons from playbook JSON")
    parser.add_argument("json_file")
    parser.add_argument("--out", default=".devin/skills")
    parser.add_argument("--overwrite", action="store_true")
    args = parser.parse_args()

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    written = []
    skipped = []

    for playbook in load_playbooks(args.json_file):
        if not isinstance(playbook, dict):
            continue
        name, content = render_skill(playbook)
        target = out / name / "SKILL.md"
        if target.exists() and not args.overwrite:
            skipped.append(str(target))
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content)
        written.append(str(target))

    print(json.dumps({"written": written, "skipped_existing": skipped}, indent=2))


if __name__ == "__main__":
    main()
