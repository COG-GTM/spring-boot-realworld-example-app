from __future__ import annotations

from typing import Optional

from fastapi import APIRouter, Depends, Query, Request

from conduit.api.body import unwrap_root
from conduit.api.dependencies import (
    get_article_command_service,
    get_article_query_service,
    get_current_user_optional,
    get_current_user_required,
)
from conduit.api.validation import validate_new_article
from conduit.application.page import Page
from conduit.application.params import NewArticleParam

router = APIRouter()


@router.post("/articles")
async def create_article(
    request: Request,
    current_user=Depends(get_current_user_required),
    article_command_service=Depends(get_article_command_service),
    article_query_service=Depends(get_article_query_service),
):
    body = await unwrap_root(request, "article")
    param = NewArticleParam(
        title=body.get("title", "") or "",
        description=body.get("description", "") or "",
        body=body.get("body", "") or "",
        tag_list=body.get("tagList") or [],
    )
    validate_new_article(param, article_query_service)
    article = article_command_service.create_article(param, current_user)
    article_data = article_query_service.find_by_id(article.id, current_user)
    return {"article": article_data.to_json()}


@router.get("/articles/feed")
async def get_feed(
    offset: int = Query(default=0),
    limit: int = Query(default=20),
    current_user=Depends(get_current_user_required),
    article_query_service=Depends(get_article_query_service),
):
    return article_query_service.find_user_feed(
        current_user, Page(offset, limit)
    ).to_json()


@router.get("/articles")
async def get_articles(
    offset: int = Query(default=0),
    limit: int = Query(default=20),
    tag: Optional[str] = Query(default=None),
    favorited: Optional[str] = Query(default=None),
    author: Optional[str] = Query(default=None),
    current_user=Depends(get_current_user_optional),
    article_query_service=Depends(get_article_query_service),
):
    return article_query_service.find_recent_articles(
        tag, author, favorited, Page(offset, limit), current_user
    ).to_json()
