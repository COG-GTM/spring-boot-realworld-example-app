"""Port of ``application.Page`` (offset/limit with defaults 0/20 and max 100)."""

from dataclasses import dataclass

MAX_LIMIT = 100
DEFAULT_LIMIT = 20


@dataclass(frozen=True)
class Page:
    offset: int = 0
    limit: int = DEFAULT_LIMIT

    @classmethod
    def of(cls, offset=None, limit=None) -> "Page":
        offset = _to_int(offset, 0)
        limit = _to_int(limit, DEFAULT_LIMIT)
        offset = offset if offset > 0 else 0
        if limit > MAX_LIMIT:
            limit = MAX_LIMIT
        elif limit <= 0:
            limit = DEFAULT_LIMIT
        return cls(offset=offset, limit=limit)


def _to_int(value, default: int) -> int:
    if value is None or value == "":
        return default
    try:
        return int(value)
    except (TypeError, ValueError):
        return default
