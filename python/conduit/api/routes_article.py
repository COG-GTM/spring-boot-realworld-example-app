from __future__ import annotations

from fastapi import APIRouter, Depends, Request
from fastapi.responses import JSONResponse

from conduit.api.body import unwrap_root
from conduit.api.dependencies import (
    get_article_command_service,
    get_article_query_service,
    get_article_repository,
    get_current_user_optional,
    get_current_user_required,
)
from conduit.api.exceptions import NoAuthorizationException, ResourceNotFoundException
from conduit.application.params import UpdateArticleParam
from conduit.core.service import AuthorizationService

router = APIRouter()


@router.get("/articles/{slug}")
async def get_article(
    slug: str,
    current_user=Depends(get_current_user_optional),
    article_query_service=Depends(get_article_query_service),
):
    article_data = article_query_service.find_by_slug(slug, current_user)
    if article_data is None:
        raise ResourceNotFoundException()
    return {"article": article_data.to_json()}


@router.put("/articles/{slug}")
async def update_article(
    slug: str,
    request: Request,
    current_user=Depends(get_current_user_required),
    article_repository=Depends(get_article_repository),
    article_command_service=Depends(get_article_command_service),
    article_query_service=Depends(get_article_query_service),
):
    article = article_repository.find_by_slug(slug)
    if article is None:
        raise ResourceNotFoundException()
    if not AuthorizationService.can_write_article(current_user, article):
        raise NoAuthorizationException()
    body = await unwrap_root(request, "article")
    param = UpdateArticleParam(
        title=body.get("title", "") or "",
        body=body.get("body", "") or "",
        description=body.get("description", "") or "",
    )
    updated = article_command_service.update_article(article, param)
    article_data = article_query_service.find_by_slug(updated.slug, current_user)
    return {"article": article_data.to_json()}


@router.delete("/articles/{slug}")
async def delete_article(
    slug: str,
    current_user=Depends(get_current_user_required),
    article_repository=Depends(get_article_repository),
):
    article = article_repository.find_by_slug(slug)
    if article is None:
        raise ResourceNotFoundException()
    if not AuthorizationService.can_write_article(current_user, article):
        raise NoAuthorizationException()
    article_repository.remove(article)
    return JSONResponse(status_code=204, content=None)
