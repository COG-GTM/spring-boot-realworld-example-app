from __future__ import annotations


def test_should_get_tags(api):
    api.mocks["tags_query_service"].all_tags.return_value = ["java", "spring"]
    resp = api.client.get("/tags")
    assert resp.status_code == 200
    assert resp.json()["tags"] == ["java", "spring"]
