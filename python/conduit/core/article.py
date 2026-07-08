from __future__ import annotations

from datetime import datetime
from typing import List, Optional, Protocol

from conduit.core.util import is_empty, new_id, now_utc, to_slug


class Tag:
    def __init__(self, name: str = "", id: Optional[str] = None) -> None:
        self.id = id if id is not None else new_id()
        self.name = name

    def __eq__(self, other: object) -> bool:
        return isinstance(other, Tag) and other.name == self.name

    def __hash__(self) -> int:
        return hash(self.name)


class Article:
    def __init__(
        self,
        title: str = "",
        description: str = "",
        body: str = "",
        tag_list: Optional[List[str]] = None,
        user_id: str = "",
        created_at: Optional[datetime] = None,
        id: Optional[str] = None,
    ) -> None:
        self.id = id if id is not None else new_id()
        self.title = title
        self.slug = to_slug(title)
        self.description = description
        self.body = body
        # de-duplicate tags while preserving Tag semantics
        unique_names: List[str] = []
        for name in tag_list or []:
            if name not in unique_names:
                unique_names.append(name)
        self.tags: List[Tag] = [Tag(name) for name in unique_names]
        self.user_id = user_id
        created = created_at if created_at is not None else now_utc()
        self.created_at = created
        self.updated_at = created

    def update(self, title: str, description: str, body: str) -> None:
        if not is_empty(title):
            self.title = title
            self.slug = to_slug(title)
            self.updated_at = now_utc()
        if not is_empty(description):
            self.description = description
            self.updated_at = now_utc()
        if not is_empty(body):
            self.body = body
            self.updated_at = now_utc()

    @staticmethod
    def to_slug(title: str) -> str:
        return to_slug(title)

    def __eq__(self, other: object) -> bool:
        return isinstance(other, Article) and other.id == self.id

    def __hash__(self) -> int:
        return hash(self.id)


class ArticleRepository(Protocol):
    def save(self, article: Article) -> None: ...

    def find_by_id(self, id: str) -> Optional[Article]: ...

    def find_by_slug(self, slug: str) -> Optional[Article]: ...

    def remove(self, article: Article) -> None: ...
