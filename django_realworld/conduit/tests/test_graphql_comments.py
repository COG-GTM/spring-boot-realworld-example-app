import pytest

from conduit.models import Comment, FollowRelation
from conduit.tests.graphql_helpers import (
    NO_AUTHORIZATION_MESSAGE,
    NOT_FOUND_MESSAGE,
    assert_auth_error,
    error_of,
    gql,
    make_article,
    make_comment,
    millis,
    ms,
    ms_info,
)

COMMENTS = """
query($slug: String!, $first: Int, $after: String, $last: Int, $before: String) {
  article(slug: $slug) {
    comments(first: $first, after: $after, last: $last, before: $before) {
      edges { cursor node { id body createdAt updatedAt author { username following } } }
      pageInfo { hasNextPage hasPreviousPage startCursor endCursor }
    }
  }
}
"""
ADD = """
mutation($slug: String!, $body: String!) {
  addComment(slug: $slug, body: $body) {
    comment { id body author { username } article { slug title } }
  }
}
"""
DELETE = "mutation($slug: String!, $id: ID!) { deleteComment(slug: $slug, id: $id) { success } }"


@pytest.fixture
def article_with_comments(make_user):
    author = make_user("author")
    article = make_article(author, "commented")
    comments = [make_comment(author, article, f"comment {i}", minutes=i) for i in range(5)]
    return article, comments


def bodies(connection: dict) -> list[str]:
    return [e["node"]["body"] for e in connection["edges"]]


def comments_of(client, **variables) -> dict:
    result = gql(client, COMMENTS, {"slug": "commented", **variables})
    return result["data"]["article"]["comments"]


def test_comments_forward_pagination(api_client, article_with_comments):
    page = comments_of(api_client, first=2)
    assert bodies(page) == ["comment 4", "comment 3"]
    assert ms(page["edges"][0]["cursor"]) == millis(4)
    assert page["edges"][0]["node"]["createdAt"] == "2024-01-01T12:04:00.123Z"
    assert ms_info(page["pageInfo"]) == {
        "hasNextPage": True,
        "hasPreviousPage": False,
        "startCursor": millis(4),
        "endCursor": millis(3),
    }
    page = comments_of(api_client, first=2, after=page["pageInfo"]["endCursor"])
    assert bodies(page) == ["comment 2", "comment 1"]
    page = comments_of(api_client, first=2, after=page["pageInfo"]["endCursor"])
    assert bodies(page) == ["comment 0"]
    assert page["pageInfo"]["hasNextPage"] is False


def test_comments_backward_pagination(api_client, article_with_comments):
    page = comments_of(api_client, last=2, before=millis(1))
    assert bodies(page) == ["comment 3", "comment 2"]
    assert ms_info(page["pageInfo"]) == {
        "hasNextPage": False,
        "hasPreviousPage": True,
        "startCursor": millis(3),
        "endCursor": millis(2),
    }
    page = comments_of(api_client, last=2, before=page["pageInfo"]["startCursor"])
    assert bodies(page) == ["comment 4"]
    assert page["pageInfo"]["hasPreviousPage"] is False


def test_comments_require_first_or_last(api_client, article_with_comments):
    result = gql(api_client, COMMENTS, {"slug": "commented"})
    assert error_of(result)["path"] == ["article", "comments"]
    assert "first 和 last" in error_of(result)["message"]


def test_comment_author_following_reflects_current_user(client_for, make_user):
    author, reader = make_user("author"), make_user("reader")
    article = make_article(author, "commented")
    make_comment(author, article, "hi")
    FollowRelation.objects.create(user=reader, target=author)
    page = comments_of(client_for(reader), first=5)
    assert page["edges"][0]["node"]["author"] == {"username": "author", "following": True}


def test_add_comment(auth_client, user, make_user):
    make_article(make_user("author"), "commented")
    data = gql(auth_client, ADD, {"slug": "commented", "body": "nice"})["data"]["addComment"]
    comment = data["comment"]
    assert comment["body"] == "nice"
    assert comment["author"] == {"username": user.username}
    assert comment["article"] == {"slug": "commented", "title": "commented"}
    assert Comment.objects.filter(pk=comment["id"], user=user).exists()


def test_add_comment_errors(api_client, auth_client, make_user):
    make_article(make_user("author"), "commented")
    assert_auth_error(gql(api_client, ADD, {"slug": "commented", "body": "x"}), "addComment")
    missing = gql(auth_client, ADD, {"slug": "nope", "body": "x"})
    assert error_of(missing)["message"] == NOT_FOUND_MESSAGE
    empty = error_of(gql(auth_client, ADD, {"slug": "commented", "body": ""}))
    assert empty["extensions"] == {"body": ["can't be empty"], "errorType": "BAD_REQUEST"}


def test_delete_comment_by_comment_author(client_for, make_user):
    article = make_article(make_user("author"), "commented")
    commenter = make_user("commenter")
    comment = make_comment(commenter, article, "x")
    result = gql(client_for(commenter), DELETE, {"slug": "commented", "id": comment.id})
    assert result == {"data": {"deleteComment": {"success": True}}}
    assert not Comment.objects.filter(pk=comment.id).exists()


def test_delete_comment_by_article_author(client_for, make_user):
    author = make_user("author")
    article = make_article(author, "commented")
    comment = make_comment(make_user("commenter"), article, "x")
    result = gql(client_for(author), DELETE, {"slug": "commented", "id": comment.id})
    assert result["data"]["deleteComment"] == {"success": True}


def test_delete_comment_errors(api_client, client_for, make_user):
    article = make_article(make_user("author"), "commented")
    comment = make_comment(make_user("commenter"), article, "x")
    variables = {"slug": "commented", "id": comment.id}
    assert_auth_error(gql(api_client, DELETE, variables), "deleteComment")
    stranger = client_for(make_user("stranger"))
    assert error_of(gql(stranger, DELETE, variables))["message"] == NO_AUTHORIZATION_MESSAGE
    missing = gql(stranger, DELETE, {"slug": "commented", "id": "nope"})
    assert error_of(missing)["message"] == NOT_FOUND_MESSAGE
    missing_article = gql(stranger, DELETE, {"slug": "nope", "id": comment.id})
    assert error_of(missing_article)["message"] == NOT_FOUND_MESSAGE
    assert Comment.objects.filter(pk=comment.id).exists()
