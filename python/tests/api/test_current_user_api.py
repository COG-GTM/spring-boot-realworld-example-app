from __future__ import annotations

from tests.api.conftest import AUTH_HEADER
from tests.api.helpers import user_data


def test_should_get_current_user_with_token(api):
    api.mocks["user_query_service"].find_by_id.return_value = user_data(api.user)
    resp = api.client.get("/user", headers=AUTH_HEADER)
    assert resp.status_code == 200
    body = resp.json()
    assert body["user"]["username"] == api.user.username
    assert body["user"]["token"] == api.token


def test_should_return_401_without_token(api):
    resp = api.client.get("/user")
    assert resp.status_code == 401


def test_should_return_401_with_invalid_token(api):
    resp = api.client.get("/user", headers={"Authorization": "Token invalid"})
    assert resp.status_code == 401


def test_should_update_current_user_profile(api):
    api.mocks["user_repository"].find_by_email.return_value = None
    api.mocks["user_repository"].find_by_username.return_value = None
    api.mocks["user_query_service"].find_by_id.return_value = user_data(api.user)

    resp = api.client.put(
        "/user",
        headers=AUTH_HEADER,
        json={"user": {"email": "newemail@example.com"}},
    )
    assert resp.status_code == 200
    api.mocks["user_service"].update_user.assert_called_once()


def test_should_get_error_if_email_exists(api):
    other = api.user
    # find_by_email returns a *different* user than current → not valid
    from conduit.core.user import User

    existing = User("newemail@example.com", "someone", "123", "", "")
    api.mocks["user_repository"].find_by_email.return_value = existing
    api.mocks["user_repository"].find_by_username.return_value = None

    resp = api.client.put(
        "/user",
        headers=AUTH_HEADER,
        json={"user": {"email": "newemail@example.com"}},
    )
    assert resp.status_code == 422
    assert resp.json()["errors"]["email"] == ["email already exist"]
