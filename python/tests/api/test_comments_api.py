from __future__ import annotations

from conduit.core.article import Article
from conduit.core.comment import Comment

from tests.api.conftest import AUTH_HEADER
from tests.api.helpers import comment_data


def _article(user_id: str = "author-id") -> Article:
    article = Article("Test Article", "desc", "body", ["java"], user_id)
    article.id = "article-id"
    article.slug = "test-article"
    return article


def test_should_create_comment_success(api):
    api.mocks["article_repository"].find_by_slug.return_value = _article()
    api.mocks["comment_query_service"].find_by_id.return_value = comment_data(api.user)
    resp = api.client.post(
        "/articles/test-article/comments",
        headers=AUTH_HEADER,
        json={"comment": {"body": "hello"}},
    )
    assert resp.status_code == 201
    assert resp.json()["comment"]["author"]["username"] == api.user.username
    api.mocks["comment_repository"].save.assert_called_once()


def test_should_get_error_for_blank_comment(api):
    api.mocks["article_repository"].find_by_slug.return_value = _article()
    resp = api.client.post(
        "/articles/test-article/comments",
        headers=AUTH_HEADER,
        json={"comment": {"body": ""}},
    )
    assert resp.status_code == 422
    assert resp.json()["errors"]["body"] == ["can't be empty"]


def test_should_list_comments(api):
    api.mocks["article_repository"].find_by_slug.return_value = _article()
    api.mocks["comment_query_service"].find_by_article_id.return_value = [
        comment_data(api.user)
    ]
    resp = api.client.get("/articles/test-article/comments")
    assert resp.status_code == 200
    assert len(resp.json()["comments"]) == 1


def test_should_delete_comment_success(api):
    api.mocks["article_repository"].find_by_slug.return_value = _article()
    api.mocks["comment_repository"].find_by_id.return_value = Comment(
        "body", api.user.id, "article-id", id="comment-id"
    )
    resp = api.client.delete(
        "/articles/test-article/comments/comment-id", headers=AUTH_HEADER
    )
    assert resp.status_code == 204
    api.mocks["comment_repository"].remove.assert_called_once()


def test_should_403_delete_comment_not_author(api):
    api.mocks["article_repository"].find_by_slug.return_value = _article(
        user_id="another-author"
    )
    api.mocks["comment_repository"].find_by_id.return_value = Comment(
        "body", "another-user", "article-id", id="comment-id"
    )
    resp = api.client.delete(
        "/articles/test-article/comments/comment-id", headers=AUTH_HEADER
    )
    assert resp.status_code == 403


def test_should_404_delete_missing_comment(api):
    api.mocks["article_repository"].find_by_slug.return_value = _article()
    api.mocks["comment_repository"].find_by_id.return_value = None
    resp = api.client.delete(
        "/articles/test-article/comments/missing", headers=AUTH_HEADER
    )
    assert resp.status_code == 404
