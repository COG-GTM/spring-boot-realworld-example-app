"""Port of DefaultJwtServiceTest.java."""

import base64
import hashlib
import hmac
import json
import time

import pytest

from conduit.infrastructure import jwt_service
from conduit.models import User

SECRET = "123123123123123123123123123123123123123123123123123123123123"


@pytest.fixture(autouse=True)
def jwt_settings(settings):
    settings.JWT_SECRET = SECRET
    settings.JWT_SESSION_TIME = 3600


def _b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def _jjwt_token(claims: dict, secret: str) -> str:
    """Compact JWS exactly as jjwt 0.11 ``Jwts.builder()...signWith(HS512 key)`` emits it:
    header ``{"alg":"HS512"}`` (no ``typ``), key = ``secret.getBytes()``."""
    header = _b64url(json.dumps({"alg": "HS512"}, separators=(",", ":")).encode())
    payload = _b64url(json.dumps(claims, separators=(",", ":")).encode())
    signing_input = f"{header}.{payload}".encode()
    signature = hmac.new(secret.encode(), signing_input, hashlib.sha512).digest()
    return f"{header}.{payload}.{_b64url(signature)}"


def test_should_generate_and_parse_token():
    user = User(email="email@email.com", username="username", password="123")
    token = jwt_service.to_token(user)
    assert token
    assert jwt_service.get_sub_from_token(token) == user.id


def test_should_get_null_with_wrong_jwt():
    assert jwt_service.get_sub_from_token("123") is None


def test_should_get_null_with_expired_jwt():
    token = (
        "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhaXNlbnNpeSIsImV4cCI6MTUwMjE2MTIwNH0."
        "SJB-U60WzxLYNomqLo4G3v3LzFxJKuVrIud8D8Lz3-mgpo9pN1i7C8ikU_jQPJGm8HsC1CquGMI-rSuM7j6LDA"
    )
    assert jwt_service.get_sub_from_token(token) is None


def test_should_get_null_with_expired_jwt_signed_with_correct_key():
    token = _jjwt_token({"sub": "aisensiy", "exp": int(time.time()) - 10}, SECRET)
    assert jwt_service.get_sub_from_token(token) is None


def test_should_decode_token_signed_in_java_format():
    token = _jjwt_token({"sub": "java-user-id", "exp": int(time.time()) + 3600}, SECRET)
    assert jwt_service.get_sub_from_token(token) == "java-user-id"


def test_should_reject_token_signed_with_other_secret():
    token = _jjwt_token({"sub": "java-user-id", "exp": int(time.time()) + 3600}, "x" * 64)
    assert jwt_service.get_sub_from_token(token) is None
