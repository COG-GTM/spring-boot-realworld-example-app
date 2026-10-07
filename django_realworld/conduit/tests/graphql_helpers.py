from datetime import UTC, datetime, timedelta

from conduit.models import Article, Comment, Tag

BASE_TIME = datetime(2024, 1, 1, 12, 0, 0, 123456, tzinfo=UTC)


def gql(client, query: str, variables: dict | None = None) -> dict:
    response = client.post(
        "/graphql", {"query": query, "variables": variables or {}}, format="json"
    )
    assert response.status_code == 200, response.content
    return response.json()


def error_of(result: dict) -> dict:
    assert "errors" in result, result
    return result["errors"][0]


def make_article(author, title: str, minutes: int = 0, tags=()) -> Article:
    when = BASE_TIME + timedelta(minutes=minutes)
    article = Article.objects.create(
        user=author,
        title=title,
        slug=Article.to_slug(title),
        description="desc",
        body="body",
        created_at=when,
        updated_at=when,
    )
    for name in tags:
        article.tags.add(Tag.objects.get_or_create(name=name)[0])
    return article


def make_comment(author, article, body: str, minutes: int = 0) -> Comment:
    when = BASE_TIME + timedelta(minutes=minutes)
    return Comment.objects.create(
        user=author, article=article, body=body, created_at=when, updated_at=when
    )


def millis(minutes: int) -> str:
    return str(int((BASE_TIME + timedelta(minutes=minutes)).timestamp() * 1000))


AUTH_ERROR = {
    "message": "io.spring.graphql.exception.AuthenticationException: null",
    "extensions": {"errorType": "INTERNAL"},
}
NOT_FOUND_MESSAGE = "io.spring.api.exception.ResourceNotFoundException: null"
NO_AUTHORIZATION_MESSAGE = "io.spring.api.exception.NoAuthorizationException: null"


def assert_auth_error(result: dict, field: str) -> None:
    error = error_of(result)
    assert error["message"] == AUTH_ERROR["message"]
    assert error["extensions"] == AUTH_ERROR["extensions"]
    assert error["path"] == [field]
    assert result["data"][field] is None


def ms(cursor: str | None) -> str | None:
    """Epoch-millis part of a cursor (drops the ``:<id>`` tie-breaker)."""
    return cursor.split(":")[0] if cursor else cursor


def ms_info(page_info: dict) -> dict:
    return {**page_info, **{k: ms(page_info[k]) for k in ("startCursor", "endCursor")}}
