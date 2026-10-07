"""Port of ListArticleApiTest.java plus pagination and filter coverage (real test DB)."""

import pytest

from conduit.application import commands
from conduit.tests.helpers_articles import create_article

pytestmark = pytest.mark.django_db


@pytest.fixture
def jane(make_user):
    return make_user("jane")


def _slugs(response):
    return [a["slug"] for a in response.json()["articles"]]


def test_should_get_default_article_list(api_client, user):
    create_article(user, "older", minutes_ago=2)
    create_article(user, "newer", minutes_ago=1)

    response = api_client.get("/articles")

    assert response.status_code == 200
    body = response.json()
    assert body["articlesCount"] == 2
    assert _slugs(response) == ["newer", "older"]
    assert body["articles"][0]["author"]["username"] == user.username
    assert body["articles"][0]["favorited"] is False


def test_empty_article_list(api_client):
    response = api_client.get("/articles")
    assert response.status_code == 200
    assert response.json() == {"articles": [], "articlesCount": 0}


def test_default_page_is_offset_0_limit_20(api_client, user):
    for i in range(25):
        create_article(user, f"article {i}", minutes_ago=100 - i)

    body = api_client.get("/articles").json()

    assert body["articlesCount"] == 25
    assert len(body["articles"]) == 20
    assert body["articles"][0]["slug"] == "article-24"


def test_offset_and_limit(api_client, user):
    for i in range(5):
        create_article(user, f"article {i}", minutes_ago=100 - i)

    response = api_client.get("/articles", {"offset": 1, "limit": 2})

    assert response.json()["articlesCount"] == 5
    assert _slugs(response) == ["article-3", "article-2"]


def test_limit_is_capped_at_100(api_client, user):
    for i in range(105):
        create_article(user, f"article {i}")

    body = api_client.get("/articles", {"limit": 500}).json()

    assert body["articlesCount"] == 105
    assert len(body["articles"]) == 100


@pytest.mark.parametrize("limit", [0, -5])
def test_non_positive_limit_falls_back_to_default(api_client, user, limit):
    for i in range(21):
        create_article(user, f"article {i}")
    assert len(api_client.get("/articles", {"limit": limit}).json()["articles"]) == 20


def test_negative_offset_falls_back_to_zero(api_client, user):
    create_article(user, "only")
    assert _slugs(api_client.get("/articles", {"offset": -3})) == ["only"]


def test_filter_by_tag(api_client, user):
    create_article(user, "java one", tags=["java", "spring"])
    create_article(user, "python one", tags=["python"])

    response = api_client.get("/articles", {"tag": "java"})

    assert response.json()["articlesCount"] == 1
    assert _slugs(response) == ["java-one"]


def test_filter_by_author(api_client, user, jane):
    create_article(user, "mine")
    create_article(jane, "janes")

    response = api_client.get("/articles", {"author": "jane"})

    assert response.json()["articlesCount"] == 1
    assert _slugs(response) == ["janes"]


def test_filter_by_favorited(api_client, user, jane):
    liked = create_article(user, "liked")
    create_article(user, "not liked")
    commands.favorite_article(liked, jane)

    response = api_client.get("/articles", {"favorited": "jane"})

    assert response.json()["articlesCount"] == 1
    assert _slugs(response) == ["liked"]
    assert response.json()["articles"][0]["favoritesCount"] == 1


def test_combined_filters(api_client, user, jane):
    create_article(user, "user java", tags=["java"])
    create_article(jane, "jane java", tags=["java"])
    create_article(jane, "jane go", tags=["go"])

    response = api_client.get("/articles", {"tag": "java", "author": "jane"})

    assert _slugs(response) == ["jane-java"]


def test_unknown_filter_values_return_empty(api_client, user):
    create_article(user, "something", tags=["java"])
    for params in ({"tag": "nope"}, {"author": "nobody"}, {"favorited": "nobody"}):
        assert api_client.get("/articles", params).json() == {"articles": [], "articlesCount": 0}


def test_list_shows_favorited_for_current_user(auth_client, user, jane):
    article = create_article(jane, "janes")
    commands.favorite_article(article, user)
    data = auth_client.get("/articles").json()["articles"][0]
    assert data["favorited"] is True


def test_should_get_feeds_401_without_login(api_client):
    assert api_client.get("/articles/feed").status_code == 401


def test_should_get_feeds_success(auth_client, user, jane, make_user):
    stranger = make_user("stranger")
    commands.follow(user, jane)
    create_article(jane, "janes old", minutes_ago=2)
    create_article(jane, "janes new", minutes_ago=1)
    create_article(stranger, "stranger article")
    create_article(user, "my own")

    response = auth_client.get("/articles/feed")

    assert response.status_code == 200
    assert response.json()["articlesCount"] == 2
    assert _slugs(response) == ["janes-new", "janes-old"]
    assert response.json()["articles"][0]["author"]["following"] is True


def test_feed_is_empty_when_following_nobody(auth_client, jane):
    create_article(jane, "janes")
    assert auth_client.get("/articles/feed").json() == {"articles": [], "articlesCount": 0}


def test_feed_pagination(auth_client, user, jane):
    commands.follow(user, jane)
    for i in range(5):
        create_article(jane, f"article {i}", minutes_ago=100 - i)

    response = auth_client.get("/articles/feed", {"offset": 2, "limit": 2})

    assert response.json()["articlesCount"] == 5
    assert _slugs(response) == ["article-2", "article-1"]
