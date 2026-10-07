from datetime import UTC

import pytest

from conduit.api.permissions import is_public
from conduit.application.data import format_datetime
from conduit.infrastructure.hashers import verify_password
from conduit.models import Article, User


def test_slug_generation_matches_java():
    assert Article.to_slug("A new article") == "a-new-article"
    assert Article.to_slug("What? Yes, ok.") == "what-yes-ok-"
    assert Article.to_slug("Tom & Jerry’s”") == "tom-jerry-s-"


def test_user_update_skips_empty_values():
    u = User(email="a@b.com", username="a", password="p", bio="bio", image="img")
    u.update(email="", username=None, password="", bio="new bio", image="")
    assert (u.email, u.username, u.password, u.bio, u.image) == (
        "a@b.com",
        "a",
        "p",
        "new bio",
        "img",
    )


def test_article_update_skips_empty_values_and_regenerates_slug():
    a = Article(title="old title", slug="old-title", description="d", body="b")
    before = a.updated_at
    a.update(title="new title", description="", body=None)
    assert a.slug == "new-title" and a.description == "d" and a.body == "b"
    assert a.updated_at >= before


def test_spring_bcrypt_hash_is_accepted():
    import bcrypt

    spring_hash = bcrypt.hashpw(b"123", bcrypt.gensalt(rounds=4, prefix=b"2a")).decode()
    assert spring_hash.startswith("$2a$")
    assert verify_password("123", spring_hash)
    assert not verify_password("wrong", spring_hash)


def test_datetime_format():
    from datetime import datetime

    assert format_datetime(datetime(2016, 2, 18, 3, 22, 56, 637000, tzinfo=UTC)) == (
        "2016-02-18T03:22:56.637Z"
    )


@pytest.mark.parametrize(
    "method,path,public",
    [
        ("POST", "/users", True),
        ("POST", "/users/login", True),
        ("GET", "/articles", True),
        ("GET", "/articles/some-slug/comments", True),
        ("GET", "/articles/feed", False),
        ("GET", "/profiles/jake", True),
        ("GET", "/tags", True),
        ("GET", "/user", False),
        ("POST", "/articles", False),
        ("OPTIONS", "/user", True),
    ],
)
def test_public_routes(method, path, public):
    assert is_public(method, path) is public


def test_protected_endpoint_returns_401_without_token(api_client, db):
    assert api_client.get("/user").status_code == 401
    assert api_client.get("/user", HTTP_AUTHORIZATION="Token garbage").status_code == 401


def test_settings_refuse_public_jwt_key_when_not_debug():
    import os
    import subprocess
    import sys

    env = {k: v for k, v in os.environ.items() if k != "JWT_SECRET"}
    env.update(DJANGO_DEBUG="false", DJANGO_SETTINGS_MODULE="realworld.settings")
    code = "import django; django.setup()"
    result = subprocess.run([sys.executable, "-c", code], env=env, capture_output=True, text=True)
    assert result.returncode != 0
    assert "JWT_SECRET" in result.stderr

    env["JWT_SECRET"] = "x" * 64
    assert subprocess.run([sys.executable, "-c", code], env=env).returncode == 0
