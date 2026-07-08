from __future__ import annotations

from conduit.core.article import Article
from conduit.core.comment import Comment
from conduit.core.user import User
from conduit.infrastructure.repositories import (
    SqlArticleRepository,
    SqlCommentRepository,
    SqlUserRepository,
)


def test_should_create_and_fetch_comment_success(engine):
    user_repo = SqlUserRepository(engine)
    article_repo = SqlArticleRepository(engine)
    comment_repo = SqlCommentRepository(engine)

    user = User("aisensiy@gmail.com", "aisensiy", "123", "", "default")
    user_repo.save(user)
    article = Article("title", "desc", "body", ["java"], user.id)
    article_repo.save(article)

    comment = Comment("content", user.id, article.id)
    comment_repo.save(comment)

    fetched = comment_repo.find_by_id(article.id, comment.id)
    assert fetched is not None
    assert fetched == comment
    assert fetched.body == "content"


def test_should_delete_comment(engine):
    user_repo = SqlUserRepository(engine)
    article_repo = SqlArticleRepository(engine)
    comment_repo = SqlCommentRepository(engine)

    user = User("aisensiy@gmail.com", "aisensiy", "123", "", "default")
    user_repo.save(user)
    article = Article("title", "desc", "body", ["java"], user.id)
    article_repo.save(article)
    comment = Comment("content", user.id, article.id)
    comment_repo.save(comment)

    comment_repo.remove(comment)
    assert comment_repo.find_by_id(article.id, comment.id) is None
