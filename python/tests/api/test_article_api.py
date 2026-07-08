from __future__ import annotations

from conduit.core.article import Article
from conduit.core.user import User

from tests.api.conftest import AUTH_HEADER
from tests.api.helpers import article_data


def _article_owned_by(user_id: str) -> Article:
    article = Article("Test Article", "desc", "body", ["java"], user_id)
    article.id = "article-id"
    article.slug = "test-article"
    return article


def test_should_get_article_success(api):
    api.mocks["article_query_service"].find_by_slug.return_value = article_data(api.user)
    resp = api.client.get("/articles/test-article")
    assert resp.status_code == 200
    assert resp.json()["article"]["slug"] == "test-article"


def test_should_404_when_article_missing(api):
    api.mocks["article_query_service"].find_by_slug.return_value = None
    resp = api.client.get("/articles/missing")
    assert resp.status_code == 404


def test_should_update_article_success(api):
    api.mocks["article_repository"].find_by_slug.return_value = _article_owned_by(
        api.user.id
    )
    updated = _article_owned_by(api.user.id)
    api.mocks["article_command_service"].update_article.return_value = updated
    api.mocks["article_query_service"].find_by_slug.return_value = article_data(api.user)

    resp = api.client.put(
        "/articles/test-article",
        headers=AUTH_HEADER,
        json={"article": {"title": "new title"}},
    )
    assert resp.status_code == 200


def test_should_403_when_update_not_author(api):
    api.mocks["article_repository"].find_by_slug.return_value = _article_owned_by(
        "another-user"
    )
    resp = api.client.put(
        "/articles/test-article",
        headers=AUTH_HEADER,
        json={"article": {"title": "new title"}},
    )
    assert resp.status_code == 403


def test_should_delete_article_success(api):
    api.mocks["article_repository"].find_by_slug.return_value = _article_owned_by(
        api.user.id
    )
    resp = api.client.delete("/articles/test-article", headers=AUTH_HEADER)
    assert resp.status_code == 204
    api.mocks["article_repository"].remove.assert_called_once()


def test_should_403_when_delete_not_author(api):
    api.mocks["article_repository"].find_by_slug.return_value = _article_owned_by(
        "another-user"
    )
    resp = api.client.delete("/articles/test-article", headers=AUTH_HEADER)
    assert resp.status_code == 403
