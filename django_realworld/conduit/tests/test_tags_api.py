"""Tags API (TagsApi.java has no Java test; covers GET /tags against the real DB)."""

import pytest

from conduit.tests.helpers_articles import create_article

pytestmark = pytest.mark.django_db


def test_tags_empty(api_client):
    response = api_client.get("/tags")
    assert response.status_code == 200
    assert response.json() == {"tags": []}


def test_tags_lists_all_distinct_tags_publicly(api_client, user, make_user):
    create_article(user, "one", tags=["java", "spring"])
    create_article(make_user("jane"), "two", tags=["java", "django"])

    response = api_client.get("/tags")

    assert response.status_code == 200
    assert sorted(response.json()["tags"]) == ["django", "java", "spring"]


def test_tags_with_token(auth_client, user):
    create_article(user, "one", tags=["java"])
    assert auth_client.get("/tags").json() == {"tags": ["java"]}
