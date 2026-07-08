from __future__ import annotations

from conduit.core.user import User

from tests.api.helpers import user_data


def test_should_create_user_success(api):
    api.mocks["user_repository"].find_by_email.return_value = None
    api.mocks["user_repository"].find_by_username.return_value = None
    api.mocks["user_service"].create_user.return_value = api.user
    api.mocks["user_query_service"].find_by_id.return_value = user_data(api.user)

    resp = api.client.post(
        "/users",
        json={"user": {"email": "john@jacob.com", "password": "123", "username": "johnjacob"}},
    )
    assert resp.status_code == 201
    body = resp.json()
    assert body["user"]["username"] == "johnjacob"
    assert body["user"]["token"] == api.token


def test_should_show_error_for_blank_email(api):
    api.mocks["user_repository"].find_by_username.return_value = None
    resp = api.client.post(
        "/users",
        json={"user": {"email": "", "password": "123", "username": "johnjacob"}},
    )
    assert resp.status_code == 422
    assert resp.json()["errors"]["email"] == ["can't be empty"]


def test_should_show_error_for_invalid_email(api):
    api.mocks["user_repository"].find_by_username.return_value = None
    api.mocks["user_repository"].find_by_email.return_value = None
    resp = api.client.post(
        "/users",
        json={"user": {"email": "johnxjacob.com", "password": "123", "username": "johnjacob"}},
    )
    assert resp.status_code == 422
    assert resp.json()["errors"]["email"] == ["should be an email"]


def test_should_show_error_for_duplicated_email(api):
    api.mocks["user_repository"].find_by_username.return_value = None
    api.mocks["user_repository"].find_by_email.return_value = api.user
    resp = api.client.post(
        "/users",
        json={"user": {"email": "john@jacob.com", "password": "123", "username": "johnjacob"}},
    )
    assert resp.status_code == 422
    assert resp.json()["errors"]["email"] == ["duplicated email"]


def test_should_show_error_for_duplicated_username(api):
    api.mocks["user_repository"].find_by_email.return_value = None
    api.mocks["user_repository"].find_by_username.return_value = api.user
    resp = api.client.post(
        "/users",
        json={"user": {"email": "john@jacob.com", "password": "123", "username": "johnjacob"}},
    )
    assert resp.status_code == 422
    assert resp.json()["errors"]["username"] == ["duplicated username"]


def test_should_login_success(api):
    stored = User("john@jacob.com", "johnjacob", "encoded", "", "")
    api.mocks["user_repository"].find_by_email.return_value = stored
    api.mocks["password_encoder"].matches.return_value = True
    api.mocks["user_query_service"].find_by_id.return_value = user_data(stored)

    resp = api.client.post(
        "/users/login",
        json={"user": {"email": "john@jacob.com", "password": "123"}},
    )
    assert resp.status_code == 200
    assert resp.json()["user"]["email"] == "john@jacob.com"
    assert resp.json()["user"]["token"] == api.token


def test_should_fail_login_with_wrong_password(api):
    stored = User("john@jacob.com", "johnjacob", "encoded", "", "")
    api.mocks["user_repository"].find_by_email.return_value = stored
    api.mocks["password_encoder"].matches.return_value = False

    resp = api.client.post(
        "/users/login",
        json={"user": {"email": "john@jacob.com", "password": "wrong"}},
    )
    assert resp.status_code == 422
    assert resp.json()["message"] == "invalid email or password"
