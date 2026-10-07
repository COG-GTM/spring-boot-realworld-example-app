"""Port of ArticleFavoriteApiTest.java against the real test DB."""

import pytest

from conduit.application import commands
from conduit.models import ArticleFavorite
from conduit.tests.helpers_articles import create_article

pytestmark = pytest.mark.django_db


@pytest.fixture
def article(make_user):
    other = make_user("other", email="other@test.com")
    return create_article(other, "title", "desc", "body", ["java"])


def test_should_favorite_an_article_success(auth_client, user, article):
    response = auth_client.post(f"/articles/{article.slug}/favorite")

    assert response.status_code == 200
    data = response.json()["article"]
    assert data["id"] == article.id
    assert data["favorited"] is True
    assert data["favoritesCount"] == 1
    assert ArticleFavorite.objects.filter(article=article, user=user).exists()


def test_favorite_twice_is_idempotent(auth_client, article):
    auth_client.post(f"/articles/{article.slug}/favorite")
    response = auth_client.post(f"/articles/{article.slug}/favorite")
    assert response.status_code == 200
    assert response.json()["article"]["favoritesCount"] == 1
    assert ArticleFavorite.objects.count() == 1


def test_should_unfavorite_an_article_success(auth_client, user, article):
    commands.favorite_article(article, user)

    response = auth_client.delete(f"/articles/{article.slug}/favorite")

    assert response.status_code == 200
    data = response.json()["article"]
    assert data["id"] == article.id
    assert data["favorited"] is False
    assert data["favoritesCount"] == 0
    assert not ArticleFavorite.objects.filter(article=article, user=user).exists()


def test_unfavorite_when_not_favorited_still_returns_article(auth_client, article):
    response = auth_client.delete(f"/articles/{article.slug}/favorite")
    assert response.status_code == 200
    assert response.json()["article"]["favorited"] is False


def test_unfavorite_keeps_other_users_favorites(auth_client, user, make_user, article):
    jane = make_user("jane")
    commands.favorite_article(article, user)
    commands.favorite_article(article, jane)

    data = auth_client.delete(f"/articles/{article.slug}/favorite").json()["article"]

    assert data["favoritesCount"] == 1
    assert data["favorited"] is False


@pytest.mark.parametrize("method", ["post", "delete"])
def test_favorite_missing_article_is_404(auth_client, method):
    assert getattr(auth_client, method)("/articles/not-exists/favorite").status_code == 404


@pytest.mark.parametrize("method", ["post", "delete"])
def test_favorite_requires_authentication(api_client, article, method):
    assert getattr(api_client, method)(f"/articles/{article.slug}/favorite").status_code == 401
