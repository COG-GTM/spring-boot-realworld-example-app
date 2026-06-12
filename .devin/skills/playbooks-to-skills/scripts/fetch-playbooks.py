#!/usr/bin/env python3
import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

API_BASE = "https://api.devin.ai"


def request_json(url, token):
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise SystemExit(f"Devin API request failed: HTTP {exc.code}: {body}") from exc


def response_items(payload):
    for key in ("items", "data", "playbooks", "results"):
        value = payload.get(key)
        if isinstance(value, list):
            return value
    if isinstance(payload, list):
        return payload
    return []


def next_cursor(payload):
    if payload.get("has_next_page"):
        return payload.get("end_cursor")
    page_info = payload.get("page_info") or {}
    if page_info.get("has_next_page"):
        return page_info.get("end_cursor") or page_info.get("next_cursor")
    return None


def list_v3(org_id, token, first):
    playbooks = []
    after = None
    while True:
        query = {"first": str(first)}
        if after:
            query["after"] = after
        url = f"{API_BASE}/v3/organizations/{urllib.parse.quote(org_id)}/playbooks?{urllib.parse.urlencode(query)}"
        payload = request_json(url, token)
        page_items = response_items(payload)
        for item in page_items:
            playbook_id = item.get("playbook_id") or item.get("id")
            if playbook_id and not item.get("body"):
                detail_url = f"{API_BASE}/v3/organizations/{urllib.parse.quote(org_id)}/playbooks/{urllib.parse.quote(playbook_id)}"
                item = request_json(detail_url, token)
            playbooks.append(item)
        after = next_cursor(payload)
        if not after:
            break
    return playbooks


def list_v1(token):
    payload = request_json(f"{API_BASE}/v1/playbooks", token)
    playbooks = []
    for item in response_items(payload):
        playbook_id = item.get("playbook_id") or item.get("id")
        if playbook_id and not item.get("body"):
            item = request_json(f"{API_BASE}/v1/playbooks/{urllib.parse.quote(playbook_id)}", token)
        playbooks.append(item)
    return playbooks


def main():
    parser = argparse.ArgumentParser(description="Fetch Devin playbooks as JSON")
    parser.add_argument("--org-id", default=os.environ.get("DEVIN_ORG_ID"))
    parser.add_argument("--api-key", default=os.environ.get("DEVIN_API_KEY"))
    parser.add_argument("--first", type=int, default=100)
    parser.add_argument("--legacy-v1", action="store_true")
    args = parser.parse_args()

    if not args.api_key:
        raise SystemExit("DEVIN_API_KEY is required in the environment or via --api-key")
    if not args.legacy_v1 and not args.org_id:
        raise SystemExit("DEVIN_ORG_ID is required for v3 retrieval; pass --legacy-v1 only for legacy team playbooks")

    playbooks = list_v1(args.api_key) if args.legacy_v1 else list_v3(args.org_id, args.api_key, args.first)
    json.dump({"playbooks": playbooks}, sys.stdout, indent=2, sort_keys=True)
    sys.stdout.write("\n")


if __name__ == "__main__":
    main()
