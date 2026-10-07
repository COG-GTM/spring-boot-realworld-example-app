"""Port of ``application/article/ArticleQueryServiceTest.java``.

``should_get_default_article_list_by_cursor`` is not ported: cursor pagination is GraphQL-only.
"""

from datetime import timedelta

import pytest
from django.utils import timezone

from conduit.application import commands, queries
from conduit.application.page import Page
from conduit.models import Article, ArticleFavorite, FollowRelation


@pytest.fixture
def user(make_user):
    return make_user("aisensiy", email="aisensiy@gmail.com")


@pytest.fixture
def another_user(make_user):
    return make_user("other", email="other@email.com")


@pytest.fixture
def article(user):
    return commands.create_article(user, "test", "desc", "body", ["java", "spring"])


def _create_older_article(author) -> Article:
    another = commands.create_article(author, "new article", "desc", "body", ["test"])
    Article.objects.filter(pk=another.pk).update(created_at=timezone.now() - timedelta(hours=1))
    return another


def test_should_fetch_article_success(user, article):
    fetched = queries.find_article_by_id(article.id, user)
    assert fetched is not None
    assert fetched.favorites_count == 0
    assert not fetched.favorited
    assert fetched.created_at is not None
    assert fetched.updated_at is not None
    assert "java" in fetched.tag_list


def test_should_get_article_with_right_favorite_and_favorite_count(another_user, article):
    ArticleFavorite.objects.create(article=article, user=another_user)

    article_data = queries.find_article_by_id(article.id, another_user)
    assert article_data is not None
    assert article_data.favorites_count == 1
    assert article_data.favorited


def test_should_get_default_article_list(user, article):
    _create_older_article(user)

    recent = queries.find_recent_articles(None, None, None, Page(), user)
    assert recent.count == 2
    assert len(recent.articles) == 2
    assert recent.articles[0].id == article.id

    nodata = queries.find_recent_articles(None, None, None, Page(2, 10), user)
    assert nodata.count == 2
    assert len(nodata.articles) == 0


def test_should_query_article_by_author(user, another_user, article):
    commands.create_article(another_user, "new article", "desc", "body", ["test"])

    recent = queries.find_recent_articles(None, user.username, None, Page(), user)
    assert len(recent.articles) == 1
    assert recent.count == 1


def test_should_query_article_by_favorite(another_user, article):
    commands.create_article(another_user, "new article", "desc", "body", ["test"])
    ArticleFavorite.objects.create(article=article, user=another_user)

    recent = queries.find_recent_articles(None, None, another_user.username, Page(), another_user)
    assert len(recent.articles) == 1
    assert recent.count == 1
    article_data = recent.articles[0]
    assert article_data.id == article.id
    assert article_data.favorites_count == 1
    assert article_data.favorited


def test_should_query_article_by_tag(user, article):
    commands.create_article(user, "new article", "desc", "body", ["test"])

    recent = queries.find_recent_articles("spring", None, None, Page(), user)
    assert len(recent.articles) == 1
    assert recent.count == 1
    assert recent.articles[0].id == article.id

    notag = queries.find_recent_articles("notag", None, None, Page(), user)
    assert notag.count == 0


def test_should_show_following_if_user_followed_author(user, another_user, article):
    FollowRelation.objects.create(user=another_user, target=user)

    recent = queries.find_recent_articles(None, None, None, Page(), another_user)
    assert recent.count == 1
    assert recent.articles[0].author.following


def test_should_get_user_feed(user, another_user, article):
    FollowRelation.objects.create(user=another_user, target=user)

    user_feed = queries.find_user_feed(user, Page())
    assert user_feed.count == 0

    another_user_feed = queries.find_user_feed(another_user, Page())
    assert another_user_feed.count == 1
    assert another_user_feed.articles[0].author.following
