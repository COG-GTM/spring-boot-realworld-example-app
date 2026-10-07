"""Port of ``infrastructure/favorite/MyBatisArticleFavoriteRepositoryTest.java``.

Uses a real user/article because the Django schema enforces foreign keys (the Java test used
dangling ids ``"123"``/``"456"``).
"""

import pytest

from conduit.application import commands
from conduit.models import ArticleFavorite


@pytest.fixture
def article(user):
    return commands.create_article(user, "title", "desc", "body", [])


def test_should_save_and_fetch_article_favorite_success(user, article):
    commands.favorite_article(article, user)
    assert ArticleFavorite.objects.filter(article=article, user=user).exists()


def test_should_save_favorite_idempotently(user, article):
    commands.favorite_article(article, user)
    commands.favorite_article(article, user)
    assert ArticleFavorite.objects.filter(article=article, user=user).count() == 1


def test_should_remove_favorite_success(user, article):
    commands.favorite_article(article, user)
    commands.unfavorite_article(article, user)
    assert not ArticleFavorite.objects.filter(article=article, user=user).exists()
