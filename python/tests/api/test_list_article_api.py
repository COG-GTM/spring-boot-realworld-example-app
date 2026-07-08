from __future__ import annotations

from conduit.application.data import ArticleDataList

from tests.api.helpers import article_data


def test_should_list_articles_public(api):
    api.mocks["article_query_service"].find_recent_articles.return_value = (
        ArticleDataList([article_data(api.user)], 1)
    )
    resp = api.client.get("/articles")
    assert resp.status_code == 200
    body = resp.json()
    assert body["articlesCount"] == 1
    assert body["articles"][0]["author"]["username"] == api.user.username


def test_should_pass_query_params(api):
    api.mocks["article_query_service"].find_recent_articles.return_value = (
        ArticleDataList([], 0)
    )
    resp = api.client.get("/articles?offset=5&limit=10&tag=java&author=bob&favorited=jane")
    assert resp.status_code == 200
    call = api.mocks["article_query_service"].find_recent_articles.call_args
    tag, author, favorited, page, _user = call.args
    assert tag == "java"
    assert author == "bob"
    assert favorited == "jane"
    assert page.offset == 5
    assert page.limit == 10
