"""Cursor (keyset) pagination read side used by the GraphQL API.

Ports ``CursorPageParameter``, ``CursorPager``, ``DateTimeCursor`` and the ``*WithCursor``
methods of ``ArticleQueryService`` / ``CommentQueryService``. Cursors are the epoch
milliseconds of ``created_at`` plus ``:<id>`` as a tie-breaker for rows created in the same
millisecond (plain epoch-millis cursors from the Java API are still accepted). NEXT pages walk
towards older rows, PREV pages towards newer rows, and both return rows newest first.
"""

from dataclasses import dataclass
from datetime import UTC, datetime, timedelta
from enum import Enum
from typing import Generic, TypeVar

from django.db.models import Q

from conduit.application import queries
from conduit.application.data import ArticleData, CommentData
from conduit.models import Article, Comment, User

MAX_LIMIT = 1000
DEFAULT_LIMIT = 20
_EPOCH = datetime(1970, 1, 1, tzinfo=UTC)
_ONE_MILLI = timedelta(milliseconds=1)
_SEP = ":"

T = TypeVar("T", ArticleData, CommentData)


class Direction(Enum):
    PREV = "PREV"
    NEXT = "NEXT"


@dataclass(frozen=True)
class CursorPageParameter:
    cursor: tuple[datetime, str | None] | None = None
    limit: int = DEFAULT_LIMIT
    direction: Direction = Direction.NEXT

    @classmethod
    def of(cls, cursor: tuple[datetime, str | None] | None, limit: int, direction: Direction):
        if limit > MAX_LIMIT:
            limit = MAX_LIMIT
        elif limit <= 0:
            limit = DEFAULT_LIMIT
        return cls(cursor=cursor, limit=limit, direction=direction)

    @property
    def is_next(self) -> bool:
        return self.direction == Direction.NEXT

    @property
    def query_limit(self) -> int:
        return self.limit + 1


def to_cursor(value: datetime, row_id: str | None = None) -> str:
    """``DateTimeCursor.toString()`` (epoch millis), suffixed with ``:<id>`` when given."""
    millis = str((value - _EPOCH) // _ONE_MILLI)
    return f"{millis}{_SEP}{row_id}" if row_id else millis


def parse_cursor(value: str | None) -> tuple[datetime, str | None] | None:
    """``DateTimeCursor.parse()``. Raises ``ValueError`` for non-numeric cursors."""
    if value is None:
        return None
    millis, _, row_id = value.partition(_SEP)
    return _EPOCH + timedelta(milliseconds=int(millis)), row_id or None


@dataclass
class CursorPager(Generic[T]):  # noqa: UP046
    data: list[T]
    has_next: bool
    has_previous: bool

    @classmethod
    def of(cls, data: list[T], direction: Direction, has_extra: bool) -> "CursorPager[T]":
        if direction == Direction.NEXT:
            return cls(data, has_next=has_extra, has_previous=False)
        return cls(data, has_next=False, has_previous=has_extra)

    @property
    def start_cursor(self) -> str | None:
        return to_cursor(self.data[0].created_at, self.data[0].id) if self.data else None

    @property
    def end_cursor(self) -> str | None:
        return to_cursor(self.data[-1].created_at, self.data[-1].id) if self.data else None


def _page(qs, page: CursorPageParameter) -> tuple[list, bool]:
    """Apply the cursor window, fetch ``limit + 1`` rows and return them newest first.

    Cursors are truncated to milliseconds, so rows inside the cursor's millisecond are ordered
    and split by ``id``; legacy cursors without an id skip that whole millisecond.
    """
    if page.cursor is not None:
        at, row_id = page.cursor
        same_milli = Q(created_at__gte=at, created_at__lt=at + _ONE_MILLI)
        if page.is_next:
            window = Q(created_at__lt=at)
            if row_id:
                window |= same_milli & Q(id__lt=row_id)
        else:
            window = Q(created_at__gte=at + _ONE_MILLI)
            if row_id:
                window |= same_milli & Q(id__gt=row_id)
        qs = qs.filter(window)
    qs = qs.order_by(*(("-created_at", "-id") if page.is_next else ("created_at", "id")))
    rows = list(qs[: page.query_limit])
    has_extra = len(rows) > page.limit
    rows = rows[: page.limit]
    if not page.is_next:
        rows.reverse()
    return rows, has_extra


def find_recent_articles_with_cursor(
    tag: str | None,
    author: str | None,
    favorited_by: str | None,
    page: CursorPageParameter,
    current_user: User | None,
) -> CursorPager[ArticleData]:
    qs = queries.filter_articles(tag, author, favorited_by)
    articles, has_extra = _page(qs.select_related("user").prefetch_related("tags"), page)
    return CursorPager.of(
        queries.build_article_data(articles, current_user), page.direction, has_extra
    )


def find_user_feed_with_cursor(user: User, page: CursorPageParameter) -> CursorPager[ArticleData]:
    authors = queries.followed_user_ids(user)
    if not authors:
        return CursorPager.of([], page.direction, False)
    qs = Article.objects.filter(user_id__in=authors).select_related("user")
    articles, has_extra = _page(qs.prefetch_related("tags"), page)
    return CursorPager.of(queries.build_article_data(articles, user), page.direction, has_extra)


def find_comments_by_article_id_with_cursor(
    article_id: str, current_user: User | None, page: CursorPageParameter
) -> CursorPager[CommentData]:
    qs = Comment.objects.select_related("user").filter(article_id=article_id)
    comments, has_extra = _page(qs, page)
    return CursorPager.of(
        queries.build_comment_data(comments, current_user), page.direction, has_extra
    )
