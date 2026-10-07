import pytest

from conduit.models import Article, ArticleFavorite, FollowRelation
from conduit.tests.graphql_helpers import (
    NO_AUTHORIZATION_MESSAGE,
    NOT_FOUND_MESSAGE,
    assert_auth_error,
    error_of,
    gql,
    make_article,
    millis,
    ms,
    ms_info,
)

ARTICLE_FIELDS = """
  slug title description body tagList favorited favoritesCount createdAt updatedAt
  author { username following }
"""
ARTICLE = "query($slug: String!) { article(slug: $slug) { " + ARTICLE_FIELDS + " } }"
CONNECTION = """
  edges { cursor node { slug } }
  pageInfo { hasNextPage hasPreviousPage startCursor endCursor }
"""
ARTICLES = (
    """
query($first: Int, $after: String, $last: Int, $before: String,
      $authoredBy: String, $favoritedBy: String, $withTag: String) {
  articles(first: $first, after: $after, last: $last, before: $before,
           authoredBy: $authoredBy, favoritedBy: $favoritedBy, withTag: $withTag) {"""
    + CONNECTION
    + "} }"
)
FEED = (
    """
query($first: Int, $after: String, $last: Int, $before: String) {
  feed(first: $first, after: $after, last: $last, before: $before) {"""
    + CONNECTION
    + "} }"
)
CREATE = (
    "mutation($input: CreateArticleInput!) { createArticle(input: $input) { article { "
    + ARTICLE_FIELDS
    + " } } }"
)
UPDATE = (
    "mutation($slug: String!, $changes: UpdateArticleInput!) {"
    " updateArticle(slug: $slug, changes: $changes) { article { " + ARTICLE_FIELDS + " } } }"
)
FAVORITE = """mutation($slug: String!) {
  favoriteArticle(slug: $slug) { article { favorited favoritesCount } }
}"""
UNFAVORITE = """mutation($slug: String!) {
  unfavoriteArticle(slug: $slug) { article { favorited favoritesCount } }
}"""
DELETE = "mutation($slug: String!) { deleteArticle(slug: $slug) { success } }"


def slugs(connection: dict) -> list[str]:
    return [e["node"]["slug"] for e in connection["edges"]]


@pytest.fixture
def five_articles(make_user):
    author = make_user("author")
    return author, [make_article(author, f"article {i}", minutes=i) for i in range(5)]


# --- article ----------------------------------------------------------------------------------
def test_article_by_slug(api_client, make_user):
    author = make_user("author")
    make_article(author, "Hello World", tags=["java", "dragons"])
    data = gql(api_client, ARTICLE, {"slug": "hello-world"})["data"]["article"]
    assert data == {
        "slug": "hello-world",
        "title": "Hello World",
        "description": "desc",
        "body": "body",
        "tagList": ["dragons", "java"],
        "favorited": False,
        "favoritesCount": 0,
        "createdAt": "2024-01-01T12:00:00.123Z",
        "updatedAt": "2024-01-01T12:00:00.123Z",
        "author": {"username": "author", "following": False},
    }


def test_article_reflects_current_user(client_for, make_user):
    author, reader = make_user("author"), make_user("reader")
    article = make_article(author, "Hello")
    FollowRelation.objects.create(user=reader, target=author)
    ArticleFavorite.objects.create(user=reader, article=article)
    data = gql(client_for(reader), ARTICLE, {"slug": "hello"})["data"]["article"]
    assert data["favorited"] is True and data["favoritesCount"] == 1
    assert data["author"]["following"] is True


def test_article_not_found(api_client, db):
    result = gql(api_client, ARTICLE, {"slug": "missing"})
    error = error_of(result)
    assert error["message"] == NOT_FOUND_MESSAGE
    assert error["extensions"] == {"errorType": "INTERNAL"}
    assert result["data"] == {"article": None}


def test_tags(api_client, make_user):
    make_article(make_user("author"), "a", tags=["java", "python"])
    assert sorted(gql(api_client, "{ tags }")["data"]["tags"]) == ["java", "python"]


# --- articles pagination ----------------------------------------------------------------------
def test_articles_forward_pagination(api_client, five_articles):
    page1 = gql(api_client, ARTICLES, {"first": 2})["data"]["articles"]
    assert slugs(page1) == ["article-4", "article-3"]
    assert [ms(e["cursor"]) for e in page1["edges"]] == [millis(4), millis(3)]
    assert ms_info(page1["pageInfo"]) == {
        "hasNextPage": True,
        "hasPreviousPage": False,
        "startCursor": millis(4),
        "endCursor": millis(3),
    }
    after = page1["pageInfo"]["endCursor"]
    page2 = gql(api_client, ARTICLES, {"first": 2, "after": after})["data"]["articles"]
    assert slugs(page2) == ["article-2", "article-1"]
    assert page2["pageInfo"]["hasNextPage"] is True
    page3 = gql(api_client, ARTICLES, {"first": 2, "after": millis(1)})["data"]["articles"]
    assert slugs(page3) == ["article-0"]
    assert page3["pageInfo"]["hasNextPage"] is False


def test_articles_backward_pagination(api_client, five_articles):
    page = gql(api_client, ARTICLES, {"last": 2, "before": millis(0)})["data"]["articles"]
    assert slugs(page) == ["article-2", "article-1"]
    assert ms_info(page["pageInfo"]) == {
        "hasNextPage": False,
        "hasPreviousPage": True,
        "startCursor": millis(2),
        "endCursor": millis(1),
    }
    before = page["pageInfo"]["startCursor"]
    page = gql(api_client, ARTICLES, {"last": 2, "before": before})["data"]["articles"]
    assert slugs(page) == ["article-4", "article-3"]
    assert page["pageInfo"]["hasPreviousPage"] is False


def test_articles_last_without_cursor_returns_oldest(api_client, five_articles):
    page = gql(api_client, ARTICLES, {"last": 2})["data"]["articles"]
    assert slugs(page) == ["article-1", "article-0"]
    assert page["pageInfo"]["hasPreviousPage"] is True


def test_articles_empty_page(api_client, db):
    page = gql(api_client, ARTICLES, {"first": 5})["data"]["articles"]
    assert page == {
        "edges": [],
        "pageInfo": {
            "hasNextPage": False,
            "hasPreviousPage": False,
            "startCursor": None,
            "endCursor": None,
        },
    }


def test_articles_filters(api_client, make_user):
    jake, anna = make_user("jake"), make_user("anna")
    make_article(jake, "jake java", minutes=1, tags=["java"])
    favorite = make_article(anna, "anna java", minutes=2, tags=["java"])
    make_article(anna, "anna go", minutes=3, tags=["go"])
    ArticleFavorite.objects.create(user=jake, article=favorite)

    def query(**filters):
        return slugs(gql(api_client, ARTICLES, {"first": 10, **filters})["data"]["articles"])

    assert query(withTag="java") == ["anna-java", "jake-java"]
    assert query(authoredBy="anna") == ["anna-go", "anna-java"]
    assert query(favoritedBy="jake") == ["anna-java"]
    assert query(authoredBy="anna", withTag="java") == ["anna-java"]


def test_articles_require_first_or_last(api_client, db):
    result = gql(api_client, "{ articles { edges { cursor } } }")
    error = error_of(result)
    assert error["message"] == "java.lang.IllegalArgumentException: first 和 last 必须只存在一个"
    assert error["extensions"] == {"errorType": "INTERNAL"}
    assert result["data"] == {"articles": None}


def test_articles_invalid_cursor(api_client, db):
    error = error_of(gql(api_client, ARTICLES, {"first": 1, "after": "abc"}))
    assert error["message"] == 'java.lang.NumberFormatException: For input string: "abc"'


# --- feed -------------------------------------------------------------------------------------
def test_feed_pagination(auth_client, user, make_user):
    followed, stranger = make_user("followed"), make_user("stranger")
    FollowRelation.objects.create(user=user, target=followed)
    for i in range(3):
        make_article(followed, f"feed {i}", minutes=i)
    make_article(stranger, "not in feed", minutes=10)

    page = gql(auth_client, FEED, {"first": 2})["data"]["feed"]
    assert slugs(page) == ["feed-2", "feed-1"]
    assert page["pageInfo"]["hasNextPage"] is True
    page = gql(auth_client, FEED, {"first": 2, "after": page["pageInfo"]["endCursor"]})
    assert slugs(page["data"]["feed"]) == ["feed-0"]
    assert page["data"]["feed"]["pageInfo"]["hasNextPage"] is False

    page = gql(auth_client, FEED, {"last": 1, "before": millis(0)})["data"]["feed"]
    assert slugs(page) == ["feed-1"]
    assert ms_info(page["pageInfo"]) == {
        "hasNextPage": False,
        "hasPreviousPage": True,
        "startCursor": millis(1),
        "endCursor": millis(1),
    }


def test_feed_without_follows_is_empty(auth_client, user, make_user):
    make_article(make_user("author"), "a")
    assert gql(auth_client, FEED, {"first": 5})["data"]["feed"]["edges"] == []


def test_feed_requires_authentication(api_client, db):
    assert_auth_error(gql(api_client, FEED, {"first": 5}), "feed")


# --- profile connections ----------------------------------------------------------------------
PROFILE_CONNECTIONS = """
query($u: String!) {
  profile(username: $u) {
    profile {
      articles(first: 10) { edges { node { slug favorited } } }
      favorites(first: 10) { edges { node { slug } } }
      feed(first: 10) { edges { node { slug } } }
    }
  }
}
"""


def test_profile_articles_favorites_and_feed(api_client, make_user):
    jake, anna = make_user("jake"), make_user("anna")
    make_article(jake, "by jake", minutes=1)
    anna_article = make_article(anna, "by anna", minutes=2)
    ArticleFavorite.objects.create(user=jake, article=anna_article)
    FollowRelation.objects.create(user=jake, target=anna)

    profile = gql(api_client, PROFILE_CONNECTIONS, {"u": "jake"})["data"]["profile"]["profile"]
    assert [e["node"]["slug"] for e in profile["articles"]["edges"]] == ["by-jake"]
    assert [e["node"]["slug"] for e in profile["favorites"]["edges"]] == ["by-anna"]
    assert [e["node"]["slug"] for e in profile["feed"]["edges"]] == ["by-anna"]


# --- mutations --------------------------------------------------------------------------------
def test_create_article(auth_client, user):
    variables = {
        "input": {"title": "How to train", "description": "d", "body": "b", "tagList": ["x", "a"]}
    }
    data = gql(auth_client, CREATE, variables)["data"]["createArticle"]["article"]
    assert data["slug"] == "how-to-train" and data["title"] == "How to train"
    assert data["tagList"] == ["a", "x"]
    assert data["author"] == {"username": user.username, "following": False}
    assert Article.objects.filter(slug="how-to-train", user=user).exists()


def test_create_article_without_tags(auth_client, user):
    variables = {"input": {"title": "No tags", "description": "d", "body": "b"}}
    data = gql(auth_client, CREATE, variables)["data"]["createArticle"]["article"]
    assert data["tagList"] == []


def test_create_article_requires_authentication(api_client, db):
    variables = {"input": {"title": "t", "description": "d", "body": "b"}}
    assert_auth_error(gql(api_client, CREATE, variables), "createArticle")
    assert not Article.objects.exists()


def test_create_article_validation_error(auth_client, user):
    make_article(user, "taken")
    variables = {"input": {"title": "taken", "description": "", "body": "b"}}
    result = gql(auth_client, CREATE, variables)
    error = error_of(result)
    assert error["extensions"] == {
        "title": ["article name exists"],
        "description": ["can't be empty"],
        "errorType": "BAD_REQUEST",
    }
    assert error["message"] == "title: article name exists, description: can't be empty"
    assert result["data"] == {"createArticle": None}


def test_update_article(auth_client, user):
    make_article(user, "old title")
    variables = {"slug": "old-title", "changes": {"title": "new title", "body": "new body"}}
    data = gql(auth_client, UPDATE, variables)["data"]["updateArticle"]["article"]
    assert data["slug"] == "new-title" and data["body"] == "new body"
    assert data["description"] == "desc"


def test_update_article_errors(api_client, auth_client, client_for, user, make_user):
    make_article(user, "mine")
    changes = {"body": "x"}
    missing = gql(auth_client, UPDATE, {"slug": "missing", "changes": changes})
    assert error_of(missing)["message"] == NOT_FOUND_MESSAGE
    assert_auth_error(
        gql(api_client, UPDATE, {"slug": "mine", "changes": changes}), "updateArticle"
    )
    other = client_for(make_user("other"))
    forbidden = gql(other, UPDATE, {"slug": "mine", "changes": changes})
    assert error_of(forbidden)["message"] == NO_AUTHORIZATION_MESSAGE
    assert error_of(forbidden)["extensions"] == {"errorType": "INTERNAL"}


def test_favorite_and_unfavorite(auth_client, user, make_user):
    make_article(make_user("author"), "fav me")
    data = gql(auth_client, FAVORITE, {"slug": "fav-me"})["data"]["favoriteArticle"]["article"]
    assert data == {"favorited": True, "favoritesCount": 1}
    data = gql(auth_client, UNFAVORITE, {"slug": "fav-me"})["data"]["unfavoriteArticle"]
    assert data["article"] == {"favorited": False, "favoritesCount": 0}


def test_favorite_errors(api_client, auth_client, make_user):
    make_article(make_user("author"), "fav me")
    assert_auth_error(gql(api_client, FAVORITE, {"slug": "fav-me"}), "favoriteArticle")
    assert_auth_error(gql(api_client, UNFAVORITE, {"slug": "fav-me"}), "unfavoriteArticle")
    assert error_of(gql(auth_client, FAVORITE, {"slug": "nope"}))["message"] == NOT_FOUND_MESSAGE
    assert error_of(gql(auth_client, UNFAVORITE, {"slug": "nope"}))["message"] == NOT_FOUND_MESSAGE


def test_delete_article(auth_client, user):
    make_article(user, "bye")
    assert gql(auth_client, DELETE, {"slug": "bye"}) == {
        "data": {"deleteArticle": {"success": True}}
    }
    assert not Article.objects.filter(slug="bye").exists()


def test_delete_article_errors(api_client, auth_client, client_for, user, make_user):
    make_article(user, "mine")
    assert_auth_error(gql(api_client, DELETE, {"slug": "mine"}), "deleteArticle")
    assert error_of(gql(auth_client, DELETE, {"slug": "nope"}))["message"] == NOT_FOUND_MESSAGE
    other = client_for(make_user("other"))
    assert error_of(gql(other, DELETE, {"slug": "mine"}))["message"] == NO_AUTHORIZATION_MESSAGE
    assert Article.objects.filter(slug="mine").exists()


def test_articles_in_the_same_millisecond_are_not_skipped(api_client, make_user):
    author = make_user("author")
    for title in ("tie a", "tie b", "tie c"):
        make_article(author, title, minutes=7)
    page = gql(api_client, ARTICLES, {"first": 1})["data"]["articles"]
    seen = slugs(page)
    while page["pageInfo"]["hasNextPage"]:
        after = page["pageInfo"]["endCursor"]
        page = gql(api_client, ARTICLES, {"first": 1, "after": after})["data"]["articles"]
        seen += slugs(page)
    assert sorted(seen) == ["tie-a", "tie-b", "tie-c"]

    before = page["pageInfo"]["startCursor"]
    back = gql(api_client, ARTICLES, {"last": 5, "before": before})["data"]["articles"]
    assert len(slugs(back)) == 2
