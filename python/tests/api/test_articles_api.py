from __future__ import annotations

from tests.api.conftest import AUTH_HEADER
from tests.api.helpers import article_data


def test_should_create_article_success(api):
    api.mocks["article_query_service"].find_by_slug.return_value = None
    api.mocks["article_command_service"].create_article.return_value = type(
        "A", (), {"id": "article-id"}
    )()
    api.mocks["article_query_service"].find_by_id.return_value = article_data(
        api.user, title="How to train your dragon", tags=["reactjs", "angularjs"]
    )

    resp = api.client.post(
        "/articles",
        headers=AUTH_HEADER,
        json={
            "article": {
                "title": "How to train your dragon",
                "description": "Ever wonder how?",
                "body": "You have to believe",
                "tagList": ["reactjs", "angularjs"],
            }
        },
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["article"]["author"]["username"] == api.user.username
    assert body["article"]["tagList"] == ["reactjs", "angularjs"]
    assert body["article"]["favoritesCount"] == 0


def test_should_get_error_with_empty_body(api):
    api.mocks["article_query_service"].find_by_slug.return_value = None
    resp = api.client.post(
        "/articles",
        headers=AUTH_HEADER,
        json={
            "article": {
                "title": "How to train your dragon",
                "description": "Ever wonder how?",
                "body": "",
            }
        },
    )
    assert resp.status_code == 422
    assert resp.json()["errors"]["body"] == ["can't be empty"]


def test_should_get_401_without_token(api):
    resp = api.client.post(
        "/articles",
        json={"article": {"title": "t", "description": "d", "body": "b"}},
    )
    assert resp.status_code == 401


def test_should_get_feed(api):
    from conduit.application.data import ArticleDataList

    api.mocks["article_query_service"].find_user_feed.return_value = ArticleDataList(
        [article_data(api.user)], 1
    )
    resp = api.client.get("/articles/feed", headers=AUTH_HEADER)
    assert resp.status_code == 200
    body = resp.json()
    assert body["articlesCount"] == 1
    assert len(body["articles"]) == 1


def test_feed_requires_auth(api):
    resp = api.client.get("/articles/feed")
    assert resp.status_code == 401
