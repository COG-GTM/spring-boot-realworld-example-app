from __future__ import annotations

import re
import uuid
from datetime import datetime, timezone


def is_empty(value: str | None) -> bool:
    return value is None or value == ""


def new_id() -> str:
    return str(uuid.uuid4())


def now_utc() -> datetime:
    """Current UTC time truncated to millisecond precision (Joda DateTime parity)."""
    now = datetime.now(timezone.utc)
    return now.replace(microsecond=(now.microsecond // 1000) * 1000)


_SLUG_PATTERN = re.compile(r"[&\u2019\u201d\uFE30-\uFFA0\s?,.]+")


def to_slug(title: str) -> str:
    return _SLUG_PATTERN.sub("-", title.lower())
