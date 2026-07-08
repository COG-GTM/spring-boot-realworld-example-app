from __future__ import annotations

import enum
from datetime import datetime, timezone
from typing import Generic, List, Optional, TypeVar

MAX_LIMIT = 100
CURSOR_MAX_LIMIT = 1000


class Page:
    def __init__(self, offset: int = 0, limit: int = 20) -> None:
        self.offset = 0
        self.limit = 20
        self._set_offset(offset)
        self._set_limit(limit)

    def _set_offset(self, offset: int) -> None:
        if offset > 0:
            self.offset = offset

    def _set_limit(self, limit: int) -> None:
        if limit > MAX_LIMIT:
            self.limit = MAX_LIMIT
        elif limit > 0:
            self.limit = limit

    def __eq__(self, other: object) -> bool:
        return (
            isinstance(other, Page)
            and other.offset == self.offset
            and other.limit == self.limit
        )

    def __hash__(self) -> int:
        return hash((self.offset, self.limit))


class Direction(enum.Enum):
    PREV = "PREV"
    NEXT = "NEXT"


class DateTimeCursor:
    @staticmethod
    def to_string(value: datetime) -> str:
        millis = int(value.timestamp() * 1000)
        return str(millis)

    @staticmethod
    def parse(cursor: Optional[str]) -> Optional[datetime]:
        if cursor is None:
            return None
        millis = int(cursor)
        return datetime.fromtimestamp(millis / 1000.0, tz=timezone.utc)


T = TypeVar("T")


class CursorPageParameter(Generic[T]):
    def __init__(
        self,
        cursor: Optional[T] = None,
        limit: int = 20,
        direction: Direction = Direction.NEXT,
    ) -> None:
        self.limit = 20
        self._set_limit(limit)
        self.cursor = cursor
        self.direction = direction

    def is_next(self) -> bool:
        return self.direction == Direction.NEXT

    @property
    def query_limit(self) -> int:
        return self.limit + 1

    def _set_limit(self, limit: int) -> None:
        if limit > CURSOR_MAX_LIMIT:
            self.limit = CURSOR_MAX_LIMIT
        elif limit > 0:
            self.limit = limit


class CursorPager(Generic[T]):
    def __init__(
        self, data: List[T], direction: Direction, has_extra: bool
    ) -> None:
        self.data = data
        if direction == Direction.NEXT:
            self.previous = False
            self.next = has_extra
        else:
            self.next = False
            self.previous = has_extra

    def has_next(self) -> bool:
        return self.next

    def has_previous(self) -> bool:
        return self.previous

    def get_start_cursor(self) -> Optional[object]:
        if not self.data:
            return None
        return _cursor_of(self.data[0])

    def get_end_cursor(self) -> Optional[object]:
        if not self.data:
            return None
        return _cursor_of(self.data[-1])


def _cursor_of(node: object) -> Optional[str]:
    cursor = getattr(node, "cursor", None)
    if cursor is None:
        return None
    return DateTimeCursor.to_string(cursor)
