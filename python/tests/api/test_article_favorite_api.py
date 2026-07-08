from __future__ import annotations

from conduit.core.article import Article

from tests.api.conftest import AUTH_HEADER
from tests.api.helpers import article_data


def _article() -> Article:
    article = Article("Test Article", "desc", "body", ["java"], "author-id")
    article.id = "article-id"
    article.slug = "test-article"
    return article


def test_should_favorite_article_success(api):
    api.mocks["article_repository"].find_by_slug.return_value = _article()
    api.mocks["article_query_service"].find_by_slug.return_value = article_data(
        api.user, favorited=True, favorites_count=1
    )
    resp = api.client.post("/articles/test-article/favorite", headers=AUTH_HEADER)
    assert resp.status_code == 200
    body = resp.json()
    assert body["article"]["favorited"] is True
    assert body["article"]["favoritesCount"] == 1
    api.mocks["article_favorite_repository"].save.assert_called_once()


def test_should_404_favorite_missing_article(api):
    api.mocks["article_repository"].find_by_slug.return_value = None
    resp = api.client.post("/articles/missing/favorite", headers=AUTH_HEADER)
    assert resp.status_code == 404


def test_should_unfavorite_article_success(api):
    from conduit.core.favorite import ArticleFavorite

    api.mocks["article_repository"].find_by_slug.return_value = _article()
    api.mocks["article_favorite_repository"].find.return_value = ArticleFavorite(
        "article-id", api.user.id
    )
    api.mocks["article_query_service"].find_by_slug.return_value = article_data(
        api.user, favorited=False, favorites_count=0
    )
    resp = api.client.delete("/articles/test-article/favorite", headers=AUTH_HEADER)
    assert resp.status_code == 200
    assert resp.json()["article"]["favorited"] is False
    api.mocks["article_favorite_repository"].remove.assert_called_once()
