from __future__ import annotations

from typing import Optional, Protocol

from conduit.core.article import Article
from conduit.core.comment import Comment
from conduit.core.user import User


class JwtService(Protocol):
    def to_token(self, user: User) -> str: ...

    def get_sub_from_token(self, token: str) -> Optional[str]: ...


class AuthorizationService:
    @staticmethod
    def can_write_article(user: User, article: Article) -> bool:
        return user.id == article.user_id

    @staticmethod
    def can_write_comment(user: User, article: Article, comment: Comment) -> bool:
        return user.id == article.user_id or user.id == comment.user_id
