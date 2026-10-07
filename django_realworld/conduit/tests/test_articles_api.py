"""Port of ArticlesApiTest.java (POST /articles) against the real test DB."""

import pytest

from conduit.models import Article, ArticleTag, Tag
from conduit.tests.helpers_articles import article_body, create_article

pytestmark = pytest.mark.django_db

TITLE = "How to train your dragon"
SLUG = "how-to-train-your-dragon"
DESCRIPTION = "Ever wonder how?"
BODY = "You have to believe"
TAGS = ["reactjs", "angularjs", "dragons"]


def test_should_create_article_success(auth_client, user):
    response = auth_client.post(
        "/articles", article_body(TITLE, DESCRIPTION, BODY, TAGS), format="json"
    )

    assert response.status_code == 200
    article = response.json()["article"]
    assert article["title"] == TITLE
    assert article["slug"] == SLUG
    assert article["description"] == DESCRIPTION
    assert article["favorited"] is False
    assert article["body"] == BODY
    assert article["favoritesCount"] == 0
    assert sorted(article["tagList"]) == sorted(TAGS)
    assert article["author"]["username"] == user.username
    assert "id" not in article["author"]
    assert article["createdAt"].endswith("Z")

    saved = Article.objects.get(slug=SLUG)
    assert saved.user_id == user.id
    assert set(saved.tags.values_list("name", flat=True)) == set(TAGS)


def test_create_article_without_tag_list(auth_client):
    response = auth_client.post("/articles", article_body(TITLE, DESCRIPTION, BODY), format="json")
    assert response.status_code == 200
    assert response.json()["article"]["tagList"] == []


def test_create_article_reuses_existing_tags(auth_client, make_user):
    create_article(make_user("other"), "existing", tags=["reactjs"])
    response = auth_client.post(
        "/articles", article_body(TITLE, DESCRIPTION, BODY, TAGS), format="json"
    )
    assert response.status_code == 200
    assert Tag.objects.filter(name="reactjs").count() == 1
    assert ArticleTag.objects.filter(tag__name="reactjs").count() == 2


def test_should_get_error_message_with_wrong_parameter(auth_client):
    response = auth_client.post(
        "/articles", article_body(TITLE, DESCRIPTION, "", TAGS), format="json"
    )
    assert response.status_code == 422
    assert response.json()["errors"]["body"][0] == "can't be empty"
    assert not Article.objects.exists()


def test_should_get_all_blank_field_errors(auth_client):
    response = auth_client.post("/articles", article_body("", " ", None), format="json")
    assert response.status_code == 422
    errors = response.json()["errors"]
    assert errors["title"] == ["can't be empty"]
    assert errors["description"] == ["can't be empty"]
    assert errors["body"] == ["can't be empty"]


def test_should_get_error_message_with_duplicated_title(auth_client, make_user):
    create_article(make_user("other"), TITLE)
    response = auth_client.post(
        "/articles", article_body(TITLE, DESCRIPTION, BODY, TAGS), format="json"
    )
    assert response.status_code == 422
    assert response.json()["errors"]["title"] == ["article name exists"]
    assert Article.objects.count() == 1


def test_create_article_requires_authentication(api_client):
    response = api_client.post(
        "/articles", article_body(TITLE, DESCRIPTION, BODY, TAGS), format="json"
    )
    assert response.status_code == 401
    assert not Article.objects.exists()
