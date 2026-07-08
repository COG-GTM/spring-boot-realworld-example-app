from __future__ import annotations

from conduit.application.page import CursorPageParameter, Direction
from conduit.application.query_services import CommentQueryService
from conduit.core.article import Article
from conduit.core.comment import Comment
from conduit.core.user import FollowRelation, User
from conduit.infrastructure.read_services import (
    CommentReadService,
    UserRelationshipQueryService,
)
from conduit.infrastructure.repositories import (
    SqlArticleRepository,
    SqlCommentRepository,
    SqlUserRepository,
)


def _seed(engine):
    user_repo = SqlUserRepository(engine)
    article_repo = SqlArticleRepository(engine)
    comment_repo = SqlCommentRepository(engine)

    user = User("a@test.com", "aisensiy", "123", "", "")
    other = User("b@test.com", "other", "123", "", "")
    user_repo.save(user)
    user_repo.save(other)
    article = Article("t", "d", "b", ["java"], user.id)
    article_repo.save(article)
    comment = Comment("content", other.id, article.id)
    comment_repo.save(comment)
    return user_repo, user, other, article, comment


def test_should_read_comment_with_following(engine):
    user_repo, user, other, article, comment = _seed(engine)
    service = CommentQueryService(
        CommentReadService(engine), UserRelationshipQueryService(engine)
    )

    comments = service.find_by_article_id(article.id, user)
    assert len(comments) == 1
    assert comments[0].profile_data.following is False

    user_repo.save_relation(FollowRelation(user.id, other.id))
    comments = service.find_by_article_id(article.id, user)
    assert comments[0].profile_data.following is True


def test_should_read_comment_with_cursor(engine):
    _, user, other, article, comment = _seed(engine)
    service = CommentQueryService(
        CommentReadService(engine), UserRelationshipQueryService(engine)
    )
    page = CursorPageParameter(cursor=None, limit=20, direction=Direction.NEXT)
    pager = service.find_by_article_id_with_cursor(article.id, user, page)
    assert len(pager.data) == 1
    assert pager.has_next() is False
