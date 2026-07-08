from __future__ import annotations

from dataclasses import dataclass
from typing import Optional
from unittest.mock import MagicMock

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine

from conduit.api import dependencies as deps
from conduit.config import Settings
from conduit.core.user import User
from conduit.main import create_app

TOKEN = "token"
AUTH_HEADER = {"Authorization": f"Token {TOKEN}"}


@dataclass
class ApiContext:
    client: TestClient
    user: User
    token: str
    mocks: dict


@pytest.fixture()
def api():
    """FastAPI app with every service/repository dependency replaced by a mock,
    plus a stubbed current-user resolution (parity with TestWithCurrentUser)."""
    engine = create_engine("sqlite://")
    app = create_app(settings=Settings(), engine=engine)

    user = User("john@jacob.com", "johnjacob", "123", "", "")

    jwt_service = MagicMock()
    jwt_service.get_sub_from_token.side_effect = (
        lambda t: user.id if t == TOKEN else None
    )
    jwt_service.to_token.return_value = TOKEN

    user_repository = MagicMock()
    user_repository.find_by_id.return_value = user
    user_repository.find_by_username.return_value = user
    user_repository.find_by_email.return_value = None
    user_repository.find_relation.return_value = None

    mocks = {
        "jwt_service": jwt_service,
        "user_repository": user_repository,
        "article_repository": MagicMock(),
        "comment_repository": MagicMock(),
        "article_favorite_repository": MagicMock(),
        "password_encoder": MagicMock(),
        "user_query_service": MagicMock(),
        "profile_query_service": MagicMock(),
        "tags_query_service": MagicMock(),
        "article_query_service": MagicMock(),
        "comment_query_service": MagicMock(),
        "user_service": MagicMock(),
        "article_command_service": MagicMock(),
    }

    overrides = {
        deps.get_jwt_service: mocks["jwt_service"],
        deps.get_user_repository: mocks["user_repository"],
        deps.get_article_repository: mocks["article_repository"],
        deps.get_comment_repository: mocks["comment_repository"],
        deps.get_article_favorite_repository: mocks["article_favorite_repository"],
        deps.get_password_encoder: mocks["password_encoder"],
        deps.get_user_query_service: mocks["user_query_service"],
        deps.get_profile_query_service: mocks["profile_query_service"],
        deps.get_tags_query_service: mocks["tags_query_service"],
        deps.get_article_query_service: mocks["article_query_service"],
        deps.get_comment_query_service: mocks["comment_query_service"],
        deps.get_user_service: mocks["user_service"],
        deps.get_article_command_service: mocks["article_command_service"],
    }
    def _provide(value):
        def _dep():
            return value

        return _dep

    for dep, value in overrides.items():
        app.dependency_overrides[dep] = _provide(value)

    client = TestClient(app)
    return ApiContext(client=client, user=user, token=TOKEN, mocks=mocks)
