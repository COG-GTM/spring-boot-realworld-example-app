from __future__ import annotations

from fastapi import APIRouter, Depends

from conduit.api.dependencies import get_tags_query_service

router = APIRouter()


@router.get("/tags")
async def get_tags(tags_query_service=Depends(get_tags_query_service)):
    return {"tags": tags_query_service.all_tags()}
