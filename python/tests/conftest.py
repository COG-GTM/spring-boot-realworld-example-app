from __future__ import annotations

import pytest
from sqlalchemy import event
from sqlalchemy.pool import StaticPool

from sqlalchemy import create_engine

from conduit.infrastructure.schema import create_schema


@pytest.fixture()
def engine():
    """A real, per-test in-memory SQLite database with the Alembic schema applied.

    StaticPool keeps a single underlying connection so every repository/read-service
    (which open their own connections) share the same in-memory database.
    """
    eng = create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
        future=True,
    )
    with eng.begin() as conn:
        create_schema(conn)
    yield eng
    eng.dispose()
