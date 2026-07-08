from __future__ import annotations

from conduit.core.user import User

from tests.api.conftest import AUTH_HEADER
from tests.api.helpers import profile_data


def test_should_get_profile_success(api):
    target = User("target@test.com", "target", "123", "bio", "img")
    api.mocks["profile_query_service"].find_by_username.return_value = profile_data(
        target
    )
    resp = api.client.get("/profiles/target", headers=AUTH_HEADER)
    assert resp.status_code == 200
    body = resp.json()
    assert body["profile"]["username"] == "target"
    assert body["profile"]["following"] is False


def test_should_404_for_missing_profile(api):
    api.mocks["profile_query_service"].find_by_username.return_value = None
    resp = api.client.get("/profiles/nobody", headers=AUTH_HEADER)
    assert resp.status_code == 404


def test_should_follow_success(api):
    target = User("target@test.com", "target", "123", "bio", "img")
    api.mocks["user_repository"].find_by_username.return_value = target
    api.mocks["profile_query_service"].find_by_username.return_value = profile_data(
        target, following=True
    )
    resp = api.client.post("/profiles/target/follow", headers=AUTH_HEADER)
    assert resp.status_code == 200
    assert resp.json()["profile"]["following"] is True
    api.mocks["user_repository"].save_relation.assert_called_once()


def test_should_unfollow_success(api):
    from conduit.core.user import FollowRelation

    target = User("target@test.com", "target", "123", "bio", "img")
    api.mocks["user_repository"].find_by_username.return_value = target
    api.mocks["user_repository"].find_relation.return_value = FollowRelation(
        api.user.id, target.id
    )
    api.mocks["profile_query_service"].find_by_username.return_value = profile_data(
        target, following=False
    )
    resp = api.client.delete("/profiles/target/follow", headers=AUTH_HEADER)
    assert resp.status_code == 200
    assert resp.json()["profile"]["following"] is False
    api.mocks["user_repository"].remove_relation.assert_called_once()
