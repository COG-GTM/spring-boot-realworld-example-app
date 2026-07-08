from __future__ import annotations

from typing import Optional

from fastapi import APIRouter, Depends, Header, Request

from conduit.api.body import unwrap_root
from conduit.api.dependencies import (
    get_current_user_required,
    get_user_query_service,
    get_user_repository,
    get_user_service,
)
from conduit.api.validation import validate_update_user
from conduit.application.data import UserWithToken
from conduit.application.params import UpdateUserParam

router = APIRouter()


def _user_response(user_with_token: UserWithToken) -> dict:
    return {"user": user_with_token.to_json()}


@router.get("/user")
async def current_user(
    authorization: Optional[str] = Header(default=None),
    current_user=Depends(get_current_user_required),
    user_query_service=Depends(get_user_query_service),
):
    user_data = user_query_service.find_by_id(current_user.id)
    token = authorization.split(" ")[1]
    return _user_response(UserWithToken.of(user_data, token))


@router.put("/user")
async def update_profile(
    request: Request,
    authorization: Optional[str] = Header(default=None),
    current_user=Depends(get_current_user_required),
    user_query_service=Depends(get_user_query_service),
    user_repository=Depends(get_user_repository),
    user_service=Depends(get_user_service),
):
    body = await unwrap_root(request, "user")
    param = UpdateUserParam(
        email=body.get("email", "") or "",
        password=body.get("password", "") or "",
        username=body.get("username", "") or "",
        bio=body.get("bio", "") or "",
        image=body.get("image", "") or "",
    )
    validate_update_user(param, current_user, user_repository)
    user_service.update_user(current_user, param)
    user_data = user_query_service.find_by_id(current_user.id)
    token = authorization.split(" ")[1]
    return _user_response(UserWithToken.of(user_data, token))
