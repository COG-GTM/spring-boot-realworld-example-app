"""Port of UsersApiTest.java, run against the real test database."""

import pytest

from conduit.infrastructure import jwt_service
from conduit.models import User

DEFAULT_AVATAR = "https://static.productionready.io/images/smiley-cyrus.jpg"

pytestmark = pytest.mark.django_db


def register_param(email, username, password="johnnyjacob"):
    return {"user": {"email": email, "password": password, "username": username}}


def test_should_create_user_success(api_client):
    response = api_client.post(
        "/users", register_param("john@jacob.com", "johnjacob"), format="json"
    )

    assert response.status_code == 201
    body = response.json()["user"]
    assert body["email"] == "john@jacob.com"
    assert body["username"] == "johnjacob"
    assert body["bio"] == ""
    assert body["image"] == DEFAULT_AVATAR
    created = User.objects.get(username="johnjacob")
    assert jwt_service.get_sub_from_token(body["token"]) == created.id
    assert set(body) == {"email", "username", "bio", "image", "token"}


def test_should_show_error_message_for_blank_username(api_client):
    response = api_client.post("/users", register_param("john@jacob.com", ""), format="json")

    assert response.status_code == 422
    assert response.json()["errors"]["username"][0] == "can't be empty"


def test_should_show_error_message_for_invalid_email(api_client):
    response = api_client.post(
        "/users", register_param("johnxjacob.com", "johnjacob"), format="json"
    )

    assert response.status_code == 422
    assert response.json()["errors"]["email"][0] == "should be an email"


def test_should_show_error_for_duplicated_username(api_client, make_user):
    make_user(username="johnjacob", email="other@jacob.com")

    response = api_client.post(
        "/users", register_param("john@jacob.com", "johnjacob"), format="json"
    )

    assert response.status_code == 422
    assert response.json()["errors"]["username"][0] == "duplicated username"


def test_should_show_error_for_duplicated_email(api_client, make_user):
    make_user(username="johnjacob", email="john@jacob.com")

    response = api_client.post(
        "/users", register_param("john@jacob.com", "johnjacob2"), format="json"
    )

    assert response.status_code == 422
    assert response.json()["errors"]["email"][0] == "duplicated email"


def test_should_show_errors_for_missing_body(api_client):
    response = api_client.post("/users", {}, format="json")

    assert response.status_code == 422
    errors = response.json()["errors"]
    assert errors["email"] == ["can't be empty"]
    assert errors["username"] == ["can't be empty"]
    assert errors["password"] == ["can't be empty"]


def test_should_login_success(api_client, make_user):
    user = make_user(username="johnjacob2", email="john@jacob.com", password="123")

    response = api_client.post(
        "/users/login",
        {"user": {"email": "john@jacob.com", "password": "123"}},
        format="json",
    )

    assert response.status_code == 200
    body = response.json()["user"]
    assert body["email"] == "john@jacob.com"
    assert body["username"] == "johnjacob2"
    assert body["bio"] == ""
    assert body["image"] == DEFAULT_AVATAR
    assert jwt_service.get_sub_from_token(body["token"]) == user.id


def test_should_fail_login_with_wrong_password(api_client, make_user):
    make_user(username="johnjacob2", email="john@jacob.com", password="123")

    response = api_client.post(
        "/users/login",
        {"user": {"email": "john@jacob.com", "password": "123123"}},
        format="json",
    )

    assert response.status_code == 422
    assert response.json() == {"message": "invalid email or password"}


def test_should_fail_login_with_unknown_email(api_client):
    response = api_client.post(
        "/users/login",
        {"user": {"email": "nobody@jacob.com", "password": "123"}},
        format="json",
    )

    assert response.status_code == 422
    assert response.json() == {"message": "invalid email or password"}


def test_should_show_validation_errors_for_login(api_client):
    response = api_client.post(
        "/users/login", {"user": {"email": "not-an-email", "password": ""}}, format="json"
    )

    assert response.status_code == 422
    errors = response.json()["errors"]
    assert errors["email"] == ["should be an email"]
    assert errors["password"] == ["can't be empty"]
