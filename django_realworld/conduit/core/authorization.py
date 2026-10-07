"""Port of ``core.service.AuthorizationService``."""

from conduit.models import Article, Comment, User


def can_write_article(user: User, article: Article) -> bool:
    return user.id == article.user_id


def can_write_comment(user: User, article: Article, comment: Comment) -> bool:
    return user.id == article.user_id or user.id == comment.user_id
