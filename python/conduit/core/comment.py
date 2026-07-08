from __future__ import annotations

from datetime import datetime
from typing import Optional, Protocol

from conduit.core.util import new_id, now_utc


class Comment:
    def __init__(
        self,
        body: str = "",
        user_id: str = "",
        article_id: str = "",
        id: Optional[str] = None,
        created_at: Optional[datetime] = None,
    ) -> None:
        self.id = id if id is not None else new_id()
        self.body = body
        self.user_id = user_id
        self.article_id = article_id
        self.created_at = created_at if created_at is not None else now_utc()

    def __eq__(self, other: object) -> bool:
        return isinstance(other, Comment) and other.id == self.id

    def __hash__(self) -> int:
        return hash(self.id)


class CommentRepository(Protocol):
    def save(self, comment: Comment) -> None: ...

    def find_by_id(self, article_id: str, id: str) -> Optional[Comment]: ...

    def remove(self, comment: Comment) -> None: ...
