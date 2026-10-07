"""Shared pytest fixtures. Port of TestHelper / TestWithCurrentUser / DbTestBase helpers."""

import pytest
from rest_framework.test import APIClient

from conduit.infrastructure import jwt_service
from conduit.infrastructure.hashers import hash_password
from conduit.models import User

DEFAULT_PASSWORD = "123"


@pytest.fixture(autouse=True)
def fast_password_hashing(settings):
    """BCrypt is slow by design; hash new test passwords with MD5 but still verify BCrypt."""
    settings.PASSWORD_HASHERS = [
        "django.contrib.auth.hashers.MD5PasswordHasher",
        *settings.PASSWORD_HASHERS,
    ]


@pytest.fixture
def api_client() -> APIClient:
    return APIClient()


@pytest.fixture
def make_user(db):
    def _make(username="johnjacob", email=None, password=DEFAULT_PASSWORD, bio="", image=""):
        return User.objects.create(
            username=username,
            email=email or f"{username}@example.com",
            password=hash_password(password),
            bio=bio,
            image=image or "https://static.productionready.io/images/smiley-cyrus.jpg",
        )

    return _make


@pytest.fixture
def user(make_user) -> User:
    return make_user()


@pytest.fixture
def token(user) -> str:
    return jwt_service.to_token(user)


@pytest.fixture
def auth_client(token) -> APIClient:
    """APIClient sending ``Authorization: Token <jwt>`` for the ``user`` fixture."""
    client = APIClient()
    client.credentials(HTTP_AUTHORIZATION=f"Token {token}")
    return client


@pytest.fixture
def client_for():
    """Build an authenticated client for an arbitrary user: ``client_for(other_user)``."""

    def _client(u: User) -> APIClient:
        client = APIClient()
        client.credentials(HTTP_AUTHORIZATION=f"Token {jwt_service.to_token(u)}")
        return client

    return _client
