from __future__ import annotations

from datetime import datetime, timezone
from typing import Optional

from sqlalchemy import create_engine
from sqlalchemy.engine import Engine

# Sortable, millisecond-precision UTC representation used for TIMESTAMP columns so
# that lexicographic ordering matches chronological ordering (parity with the
# MyBatis DateTimeHandler which persists UTC millis).
_DB_FORMAT = "%Y-%m-%d %H:%M:%S.%f"


def to_db(value: Optional[datetime]) -> Optional[str]:
    if value is None:
        return None
    if value.tzinfo is None:
        value = value.replace(tzinfo=timezone.utc)
    value = value.astimezone(timezone.utc)
    millis = value.microsecond // 1000
    return value.strftime("%Y-%m-%d %H:%M:%S") + f".{millis:03d}"


def from_db(value: Optional[object]) -> Optional[datetime]:
    if value is None:
        return None
    if isinstance(value, datetime):
        return value if value.tzinfo else value.replace(tzinfo=timezone.utc)
    text = str(value)
    for fmt in (_DB_FORMAT, "%Y-%m-%d %H:%M:%S"):
        try:
            return datetime.strptime(text, fmt).replace(tzinfo=timezone.utc)
        except ValueError:
            continue
    # last resort: ISO 8601
    return datetime.fromisoformat(text).replace(tzinfo=timezone.utc)


def create_sqlite_engine(url: str) -> Engine:
    connect_args = {"check_same_thread": False}
    return create_engine(url, connect_args=connect_args, future=True)
