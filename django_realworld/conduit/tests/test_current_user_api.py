"""Port of CurrentUserApiTest.java, run against the real test database."""

import pytest

from conduit.infrastructure.hashers import verify_password

DEFAULT_AVATAR = "https://static.productionready.io/images/smiley-cyrus.jpg"

pytestmark = pytest.mark.django_db


def update_param(email, bio, username):
    return {"user": {"email": email, "bio": bio, "username": username}}


def test_should_get_current_user_with_token(auth_client, user, token):
    response = auth_client.get("/user")

    assert response.status_code == 200
    body = response.json()["user"]
    assert body["email"] == user.email
    assert body["username"] == user.username
    assert body["bio"] == ""
    assert body["image"] == DEFAULT_AVATAR
    assert body["token"] == token


def test_should_get_401_without_token(api_client):
    assert api_client.get("/user").status_code == 401


def test_should_get_401_with_invalid_token(api_client):
    response = api_client.get("/user", HTTP_AUTHORIZATION="Token asdfasd")
    assert response.status_code == 401


def test_should_update_current_user_profile(auth_client, user, token):
    response = auth_client.put(
        "/user",
        update_param("newemail@example.com", "updated", "newusernamee"),
        format="json",
    )

    assert response.status_code == 200
    body = response.json()["user"]
    assert body["email"] == "newemail@example.com"
    assert body["bio"] == "updated"
    assert body["username"] == "newusernamee"
    assert body["image"] == DEFAULT_AVATAR
    assert body["token"] == token
    user.refresh_from_db()
    assert (user.email, user.bio, user.username) == (
        "newemail@example.com",
        "updated",
        "newusernamee",
    )


def test_should_skip_empty_values_on_update(auth_client, user):
    response = auth_client.put(
        "/user", {"user": {"email": "", "username": "", "bio": "only bio"}}, format="json"
    )

    assert response.status_code == 200
    user.refresh_from_db()
    assert user.email == "johnjacob@example.com"
    assert user.username == "johnjacob"
    assert user.bio == "only bio"


def test_should_update_password(auth_client, user):
    response = auth_client.put("/user", {"user": {"password": "newpass"}}, format="json")

    assert response.status_code == 200
    user.refresh_from_db()
    assert verify_password("newpass", user.password)


def test_should_get_error_if_email_exists_when_update_user_profile(auth_client, make_user, user):
    make_user(username="username", email="newemail@example.com")

    response = auth_client.put(
        "/user",
        update_param("newemail@example.com", "updated", "newusernamee"),
        format="json",
    )

    assert response.status_code == 422
    assert response.json()["errors"]["email"][0] == "email already exist"


def test_should_get_error_if_username_exists_when_update_user_profile(auth_client, make_user):
    make_user(username="taken", email="taken@example.com")

    response = auth_client.put("/user", {"user": {"username": "taken"}}, format="json")

    assert response.status_code == 422
    assert response.json()["errors"]["username"][0] == "username already exist"


def test_should_get_error_for_invalid_email_on_update(auth_client):
    response = auth_client.put("/user", {"user": {"email": "not-an-email"}}, format="json")

    assert response.status_code == 422
    assert response.json()["errors"]["email"][0] == "should be an email"


def test_should_get_401_if_not_login(api_client):
    response = api_client.put("/user", {"user": {}}, format="json")
    assert response.status_code == 401
