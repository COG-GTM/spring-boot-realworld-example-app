from __future__ import annotations

from fastapi import APIRouter, Depends

from conduit.api.dependencies import (
    get_article_favorite_repository,
    get_article_query_service,
    get_article_repository,
    get_current_user_required,
)
from conduit.api.exceptions import ResourceNotFoundException
from conduit.core.favorite import ArticleFavorite

router = APIRouter()


@router.post("/articles/{slug}/favorite")
async def favorite_article(
    slug: str,
    current_user=Depends(get_current_user_required),
    article_favorite_repository=Depends(get_article_favorite_repository),
    article_repository=Depends(get_article_repository),
    article_query_service=Depends(get_article_query_service),
):
    article = article_repository.find_by_slug(slug)
    if article is None:
        raise ResourceNotFoundException()
    article_favorite_repository.save(ArticleFavorite(article.id, current_user.id))
    return {"article": article_query_service.find_by_slug(slug, current_user).to_json()}


@router.delete("/articles/{slug}/favorite")
async def unfavorite_article(
    slug: str,
    current_user=Depends(get_current_user_required),
    article_favorite_repository=Depends(get_article_favorite_repository),
    article_repository=Depends(get_article_repository),
    article_query_service=Depends(get_article_query_service),
):
    article = article_repository.find_by_slug(slug)
    if article is None:
        raise ResourceNotFoundException()
    favorite = article_favorite_repository.find(article.id, current_user.id)
    if favorite is not None:
        article_favorite_repository.remove(favorite)
    return {"article": article_query_service.find_by_slug(slug, current_user).to_json()}
