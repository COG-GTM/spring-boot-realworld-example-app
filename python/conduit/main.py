from __future__ import annotations

from typing import Optional

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from sqlalchemy.engine import Engine

from conduit.api import (
    routes_article,
    routes_article_favorite,
    routes_articles,
    routes_comments,
    routes_current_user,
    routes_profiles,
    routes_tags,
    routes_users,
)
from conduit.api.dependencies import Container
from conduit.api.exceptions import register_exception_handlers
from conduit.config import Settings
from conduit.infrastructure.db import create_sqlite_engine


def _to_sqlalchemy_url(datasource_url: str) -> str:
    # Accept both JDBC-style ("jdbc:sqlite:dev.db") and SQLAlchemy URLs.
    if datasource_url.startswith("jdbc:sqlite:"):
        path = datasource_url[len("jdbc:sqlite:") :]
        if path == ":memory:":
            return "sqlite://"
        return f"sqlite:///{path}"
    return datasource_url


def create_app(settings: Optional[Settings] = None, engine: Optional[Engine] = None) -> FastAPI:
    settings = settings or Settings.from_env()
    if engine is None:
        engine = create_sqlite_engine(_to_sqlalchemy_url(settings.datasource_url))

    app = FastAPI(title="Conduit (Python/FastAPI)")
    app.state.container = Container(engine, settings)
    app.state.engine = engine

    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=False,
        allow_methods=["HEAD", "GET", "POST", "PUT", "DELETE", "PATCH"],
        allow_headers=["Authorization", "Cache-Control", "Content-Type"],
    )

    register_exception_handlers(app)

    @app.exception_handler(RequestValidationError)
    async def _validation_error(request: Request, exc: RequestValidationError):
        errors: dict = {}
        for err in exc.errors():
            loc = err.get("loc", [])
            field = str(loc[-1]) if loc else "body"
            errors.setdefault(field, []).append(err.get("msg", ""))
        return JSONResponse(status_code=422, content={"errors": errors})

    # Order matters: /articles and /articles/feed must be registered before the
    # /articles/{slug} catch-all so "feed" is not consumed as a slug.
    app.include_router(routes_users.router)
    app.include_router(routes_current_user.router)
    app.include_router(routes_profiles.router)
    app.include_router(routes_articles.router)
    app.include_router(routes_article_favorite.router)
    app.include_router(routes_comments.router)
    app.include_router(routes_article.router)
    app.include_router(routes_tags.router)

    return app


app = create_app()
