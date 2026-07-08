from __future__ import annotations

import time

import jwt

from conduit.core.user import User
from conduit.infrastructure.jwt_service import DefaultJwtService

_SECRET = "AAAAABBBBBAAAAABBBBBAAAAABBBBBAAAAABBBBBAAAAABBBBBAAAAABBBBBAAAAABBBBB"


def test_should_generate_and_parse_token():
    service = DefaultJwtService(_SECRET, 3600)
    user = User("a@test.com", "aisensiy", "123", "", "")
    token = service.to_token(user)
    assert token
    assert service.get_sub_from_token(token) == user.id


def test_should_get_none_with_wrong_signature():
    service = DefaultJwtService(_SECRET, 3600)
    wrong = jwt.encode(
        {"sub": "123", "exp": int(time.time()) + 3600},
        "another-secret-value-that-differs-completely-1234567890",
        algorithm="HS512",
    )
    assert service.get_sub_from_token(wrong) is None


def test_should_get_none_with_expired_token():
    service = DefaultJwtService(_SECRET, 3600)
    expired = jwt.encode(
        {"sub": "123", "exp": int(time.time()) - 10},
        _SECRET,
        algorithm="HS512",
    )
    assert service.get_sub_from_token(expired) is None
