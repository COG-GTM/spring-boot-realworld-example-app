from __future__ import annotations

from fastapi import APIRouter, Depends, Request
from fastapi.responses import JSONResponse

from conduit.api.body import unwrap_root
from conduit.api.dependencies import (
    get_article_repository,
    get_comment_query_service,
    get_comment_repository,
    get_current_user_optional,
    get_current_user_required,
)
from conduit.api.exceptions import NoAuthorizationException, ResourceNotFoundException
from conduit.api.validation import validate_new_comment
from conduit.application.params import NewCommentParam
from conduit.core.comment import Comment
from conduit.core.service import AuthorizationService

router = APIRouter()


@router.post("/articles/{slug}/comments")
async def create_comment(
    slug: str,
    request: Request,
    current_user=Depends(get_current_user_required),
    article_repository=Depends(get_article_repository),
    comment_repository=Depends(get_comment_repository),
    comment_query_service=Depends(get_comment_query_service),
):
    article = article_repository.find_by_slug(slug)
    if article is None:
        raise ResourceNotFoundException()
    body = await unwrap_root(request, "comment")
    param = NewCommentParam(body=body.get("body", "") or "")
    validate_new_comment(param)
    comment = Comment(param.body, current_user.id, article.id)
    comment_repository.save(comment)
    comment_data = comment_query_service.find_by_id(comment.id, current_user)
    return JSONResponse(status_code=201, content={"comment": comment_data.to_json()})


@router.get("/articles/{slug}/comments")
async def get_comments(
    slug: str,
    current_user=Depends(get_current_user_optional),
    article_repository=Depends(get_article_repository),
    comment_query_service=Depends(get_comment_query_service),
):
    article = article_repository.find_by_slug(slug)
    if article is None:
        raise ResourceNotFoundException()
    comments = comment_query_service.find_by_article_id(article.id, current_user)
    return {"comments": [c.to_json() for c in comments]}


@router.delete("/articles/{slug}/comments/{id}")
async def delete_comment(
    slug: str,
    id: str,
    current_user=Depends(get_current_user_required),
    article_repository=Depends(get_article_repository),
    comment_repository=Depends(get_comment_repository),
):
    article = article_repository.find_by_slug(slug)
    if article is None:
        raise ResourceNotFoundException()
    comment = comment_repository.find_by_id(article.id, id)
    if comment is None:
        raise ResourceNotFoundException()
    if not AuthorizationService.can_write_comment(current_user, article, comment):
        raise NoAuthorizationException()
    comment_repository.remove(comment)
    return JSONResponse(status_code=204, content=None)
