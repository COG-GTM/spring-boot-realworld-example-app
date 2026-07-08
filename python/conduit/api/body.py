from __future__ import annotations

from typing import Any, Dict

from fastapi import Request


async def unwrap_root(request: Request, key: str) -> Dict[str, Any]:
    """Extract a root-wrapped JSON body, e.g. {"user": {...}} -> {...}.

    Mirrors Jackson's UNWRAP_ROOT_VALUE=true deserialization.
    """
    try:
        payload = await request.json()
    except Exception:
        return {}
    if isinstance(payload, dict) and key in payload and isinstance(payload[key], dict):
        return payload[key]
    if isinstance(payload, dict):
        return payload
    return {}
