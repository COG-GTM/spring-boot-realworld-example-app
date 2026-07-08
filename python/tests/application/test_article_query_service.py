from __future__ import annotations

import pytest

from conduit.application.page import Page
from conduit.application.query_services import ArticleQueryService
from conduit.core.article import Article
from conduit.core.favorite import ArticleFavorite
from conduit.core.user import FollowRelation, User
from conduit.infrastructure.read_services import (
    ArticleFavoritesReadService,
    ArticleReadService,
    UserRelationshipQueryService,
)
from conduit.infrastructure.repositories import (
    SqlArticleFavoriteRepository,
    SqlArticleRepository,
    SqlUserRepository,
)


@pytest.fixture()
def context(engine):
    user_repo = SqlUserRepository(engine)
    article_repo = SqlArticleRepository(engine)
    favorite_repo = SqlArticleFavoriteRepository(engine)

    user = User("aisensiy@test.com", "aisensiy", "123", "", "")
    user_repo.save(user)
    article = Article("test", "desc", "body", ["java", "spring"], user.id)
    article_repo.save(article)

    service = ArticleQueryService(
        ArticleReadService(engine),
        UserRelationshipQueryService(engine),
        ArticleFavoritesReadService(engine),
    )
    return {
        "engine": engine,
        "user_repo": user_repo,
        "article_repo": article_repo,
        "favorite_repo": favorite_repo,
        "user": user,
        "article": article,
        "service": service,
    }


def test_should_fetch_article_success(context):
    service = context["service"]
    user = context["user"]
    article = context["article"]

    found = service.find_by_id(article.id, user)
    assert found is not None
    assert found.favorited is False
    assert found.favorites_count == 0
    assert set(found.tag_list) == {"java", "spring"}
    assert found.profile_data.username == user.username


def test_should_get_favorite_and_favorite_count(context):
    service = context["service"]
    article = context["article"]
    favorite_repo = context["favorite_repo"]
    user_repo = context["user_repo"]

    another = User("other@test.com", "other", "123", "", "")
    user_repo.save(another)
    favorite_repo.save(ArticleFavorite(article.id, another.id))

    found = service.find_by_id(article.id, another)
    assert found.favorited is True
    assert found.favorites_count == 1


def test_should_get_articles(context):
    service = context["service"]
    user = context["user"]

    result = service.find_recent_articles(None, None, None, Page(0, 20), user)
    assert result.count == 1
    assert len(result.article_datas) == 1


def test_should_query_article_by_tag(context):
    service = context["service"]
    user = context["user"]

    result = service.find_recent_articles("spring", None, None, Page(0, 20), user)
    assert result.count == 1

    empty = service.find_recent_articles("notexists", None, None, Page(0, 20), user)
    assert empty.count == 0


def test_should_query_article_by_author(context):
    service = context["service"]
    user = context["user"]

    result = service.find_recent_articles(None, "aisensiy", None, Page(0, 20), user)
    assert result.count == 1


def test_should_get_user_feed(context):
    service = context["service"]
    user = context["user"]
    user_repo = context["user_repo"]
    article_repo = context["article_repo"]

    another = User("other@test.com", "other", "123", "", "")
    user_repo.save(another)
    another_article = Article("other title", "d", "b", ["x"], another.id)
    article_repo.save(another_article)
    user_repo.save_relation(FollowRelation(user.id, another.id))

    feed = service.find_user_feed(user, Page(0, 20))
    assert feed.count == 1
    assert feed.article_datas[0].profile_data.username == "other"
    assert feed.article_datas[0].profile_data.following is True
