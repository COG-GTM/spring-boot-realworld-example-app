from __future__ import annotations

from typing import Optional, Protocol


class ArticleFavorite:
    def __init__(self, article_id: str = "", user_id: str = "") -> None:
        self.article_id = article_id
        self.user_id = user_id

    def __eq__(self, other: object) -> bool:
        return (
            isinstance(other, ArticleFavorite)
            and other.article_id == self.article_id
            and other.user_id == self.user_id
        )

    def __hash__(self) -> int:
        return hash((self.article_id, self.user_id))


class ArticleFavoriteRepository(Protocol):
    def save(self, article_favorite: ArticleFavorite) -> None: ...

    def find(self, article_id: str, user_id: str) -> Optional[ArticleFavorite]: ...

    def remove(self, favorite: ArticleFavorite) -> None: ...
