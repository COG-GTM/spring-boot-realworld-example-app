"""Port of ``api/CommentsApiTest.java`` (against the real DB instead of mocked repositories)."""

import pytest

from conduit.application import commands
from conduit.models import Comment, FollowRelation


@pytest.fixture
def article(user):
    return commands.create_article(user, "title", "desc", "body", ["test", "java"])


@pytest.fixture
def comment(user, article):
    return commands.add_comment(user, article, "comment")


def _url(slug, comment_id=None):
    base = f"/articles/{slug}/comments"
    return f"{base}/{comment_id}" if comment_id else base


def test_should_create_comment_success(auth_client, user, article):
    response = auth_client.post(
        _url(article.slug), {"comment": {"body": "comment content"}}, format="json"
    )
    assert response.status_code == 201
    body = response.json()["comment"]
    assert body["body"] == "comment content"
    assert set(body) == {"id", "body", "createdAt", "updatedAt", "author"}
    assert body["author"] == {
        "username": user.username,
        "bio": user.bio,
        "image": user.image,
        "following": False,
    }
    assert Comment.objects.filter(pk=body["id"], article=article, user=user).exists()


@pytest.mark.parametrize("payload", [{"comment": {"body": ""}}, {"comment": {"body": "  "}}, {}])
def test_should_get_422_with_empty_body(auth_client, article, payload):
    response = auth_client.post(_url(article.slug), payload, format="json")
    assert response.status_code == 422
    assert response.json() == {"errors": {"body": ["can't be empty"]}}


def test_should_get_404_when_creating_comment_on_unknown_article(auth_client, db):
    response = auth_client.post(_url("missing"), {"comment": {"body": "x"}}, format="json")
    assert response.status_code == 404


def test_should_get_401_when_creating_comment_without_token(api_client, article):
    response = api_client.post(_url(article.slug), {"comment": {"body": "x"}}, format="json")
    assert response.status_code == 401


def test_should_get_comments_of_article_success(api_client, article, comment):
    response = api_client.get(_url(article.slug))
    assert response.status_code == 200
    comments = response.json()["comments"]
    assert [c["id"] for c in comments] == [comment.id]
    assert comments[0]["author"]["following"] is False


def test_should_fill_following_when_authenticated(make_user, client_for, user, article, comment):
    reader = make_user("reader")
    FollowRelation.objects.create(user=reader, target=user)
    response = client_for(reader).get(_url(article.slug))
    assert response.status_code == 200
    assert response.json()["comments"][0]["author"]["following"] is True


def test_should_get_404_for_comments_of_unknown_article(api_client, db):
    assert api_client.get(_url("missing")).status_code == 404


def test_should_delete_comment_success(auth_client, article, comment):
    response = auth_client.delete(_url(article.slug, comment.id))
    assert response.status_code == 204
    assert not Comment.objects.filter(pk=comment.id).exists()


def test_should_allow_article_author_to_delete_others_comment(
    make_user, client_for, auth_client, article
):
    commenter = make_user("commenter")
    other_comment = commands.add_comment(commenter, article, "hi")
    assert auth_client.delete(_url(article.slug, other_comment.id)).status_code == 204


def test_should_allow_comment_author_to_delete_on_others_article(make_user, client_for, article):
    commenter = make_user("commenter")
    own_comment = commands.add_comment(commenter, article, "hi")
    assert client_for(commenter).delete(_url(article.slug, own_comment.id)).status_code == 204


def test_should_get_403_if_not_author_of_article_or_author_of_comment_when_delete_comment(
    make_user, client_for, article, comment
):
    another_user = make_user("other", email="other@example.com")
    response = client_for(another_user).delete(_url(article.slug, comment.id))
    assert response.status_code == 403
    assert Comment.objects.filter(pk=comment.id).exists()


def test_should_get_404_when_deleting_missing_comment(auth_client, article):
    assert auth_client.delete(_url(article.slug, "missing")).status_code == 404


def test_should_get_404_when_deleting_from_unknown_article(auth_client, comment):
    assert auth_client.delete(_url("missing", comment.id)).status_code == 404


def test_should_get_404_when_comment_belongs_to_another_article(auth_client, user, comment):
    other_article = commands.create_article(user, "other title", "desc", "body", [])
    assert auth_client.delete(_url(other_article.slug, comment.id)).status_code == 404
    assert Comment.objects.filter(pk=comment.id).exists()


def test_should_get_401_when_deleting_without_token(api_client, article, comment):
    assert api_client.delete(_url(article.slug, comment.id)).status_code == 401
