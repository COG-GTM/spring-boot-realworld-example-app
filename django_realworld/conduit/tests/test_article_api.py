"""Port of ArticleApiTest.java (GET/PUT/DELETE /articles/{slug}) against the real test DB."""

import pytest

from conduit.application.data import format_datetime
from conduit.models import Article
from conduit.tests.helpers_articles import article_body, create_article

pytestmark = pytest.mark.django_db


@pytest.fixture
def another_user(make_user):
    return make_user("test", email="test@test.com")


def test_should_read_article_success(api_client, user):
    article = create_article(user, "Test New Article", "Desc", "Body", ["java", "spring", "jpg"])

    response = api_client.get("/articles/test-new-article")

    assert response.status_code == 200
    data = response.json()["article"]
    assert data["slug"] == "test-new-article"
    assert data["body"] == "Body"
    assert data["createdAt"] == format_datetime(article.created_at)
    assert data["tagList"] == ["java", "jpg", "spring"]
    assert data["author"]["following"] is False


def test_read_article_reflects_current_user_state(auth_client, user, another_user):
    from conduit.application import commands

    article = create_article(another_user, "followed author")
    commands.follow(user, another_user)
    commands.favorite_article(article, user)

    data = auth_client.get(f"/articles/{article.slug}").json()["article"]
    assert data["favorited"] is True
    assert data["favoritesCount"] == 1
    assert data["author"]["following"] is True


def test_should_404_if_article_not_found(api_client):
    assert api_client.get("/articles/not-exists").status_code == 404


def test_should_update_article_content_success(auth_client, user):
    create_article(user, "old title", "old description", "old body", ["java", "spring", "jpg"])

    response = auth_client.put(
        "/articles/old-title",
        article_body("new title", "new description", "new body"),
        format="json",
    )

    assert response.status_code == 200
    data = response.json()["article"]
    assert data["slug"] == "new-title"
    assert data["title"] == "new title"
    assert data["description"] == "new description"
    assert data["body"] == "new body"
    assert data["tagList"] == ["java", "jpg", "spring"]
    assert not Article.objects.filter(slug="old-title").exists()
    assert auth_client.get("/articles/new-title").status_code == 200


def test_update_skips_empty_fields(auth_client, user):
    article = create_article(user, "old title", "old description", "old body", minutes_ago=5)

    response = auth_client.put(
        "/articles/old-title", {"article": {"body": "new body", "title": ""}}, format="json"
    )

    assert response.status_code == 200
    data = response.json()["article"]
    assert data["slug"] == "old-title"
    assert data["title"] == "old title"
    assert data["description"] == "old description"
    assert data["body"] == "new body"
    assert data["updatedAt"] > format_datetime(article.updated_at)


def test_update_with_duplicated_title_fails(auth_client, user):
    create_article(user, "first")
    create_article(user, "second")
    response = auth_client.put("/articles/second", article_body("first", "", ""), format="json")
    assert response.status_code == 422
    assert response.json()["errors"]["title"] == ["article name exists"]


def test_update_missing_article_is_404(auth_client):
    response = auth_client.put("/articles/nope", article_body("a", "b", "c"), format="json")
    assert response.status_code == 404


def test_should_get_403_if_not_author_to_update_article(auth_client, another_user):
    create_article(another_user, "new-title", "new description", "new body")

    response = auth_client.put(
        "/articles/new-title",
        article_body("new-title", "new description", "changed body"),
        format="json",
    )

    assert response.status_code == 403
    assert Article.objects.get(slug="new-title").body == "new body"


def test_update_requires_authentication(api_client, user):
    create_article(user, "title")
    response = api_client.put("/articles/title", article_body("x", "y", "z"), format="json")
    assert response.status_code == 401


def test_should_delete_article_success(auth_client, user):
    create_article(user, "title", "description", "body", ["java", "spring", "jpg"])

    response = auth_client.delete("/articles/title")

    assert response.status_code == 204
    assert not Article.objects.filter(slug="title").exists()


def test_should_403_if_not_author_delete_article(auth_client, another_user):
    create_article(another_user, "new-title", "new description", "new body")

    response = auth_client.delete("/articles/new-title")

    assert response.status_code == 403
    assert Article.objects.filter(slug="new-title").exists()


def test_delete_missing_article_is_404(auth_client):
    assert auth_client.delete("/articles/nope").status_code == 404
