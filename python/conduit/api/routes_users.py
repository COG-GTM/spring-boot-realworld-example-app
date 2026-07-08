from __future__ import annotations

from fastapi import APIRouter, Depends, Request
from fastapi.responses import JSONResponse

from conduit.api.body import unwrap_root
from conduit.api.dependencies import (
    get_jwt_service,
    get_password_encoder,
    get_user_query_service,
    get_user_repository,
    get_user_service,
)
from conduit.api.exceptions import InvalidAuthenticationException
from conduit.api.validation import validate_register
from conduit.application.data import UserWithToken
from conduit.application.params import LoginParam, RegisterParam

router = APIRouter()


def _user_response(user_with_token: UserWithToken) -> dict:
    return {"user": user_with_token.to_json()}


@router.post("/users")
async def create_user(
    request: Request,
    user_repository=Depends(get_user_repository),
    user_query_service=Depends(get_user_query_service),
    jwt_service=Depends(get_jwt_service),
    user_service=Depends(get_user_service),
):
    body = await unwrap_root(request, "user")
    param = RegisterParam(
        email=body.get("email", "") or "",
        username=body.get("username", "") or "",
        password=body.get("password", "") or "",
    )
    validate_register(param, user_repository)
    user = user_service.create_user(param)
    user_data = user_query_service.find_by_id(user.id)
    return JSONResponse(
        status_code=201,
        content=_user_response(UserWithToken.of(user_data, jwt_service.to_token(user))),
    )


@router.post("/users/login")
async def user_login(
    request: Request,
    user_repository=Depends(get_user_repository),
    user_query_service=Depends(get_user_query_service),
    jwt_service=Depends(get_jwt_service),
    password_encoder=Depends(get_password_encoder),
):
    body = await unwrap_root(request, "user")
    param = LoginParam(
        email=body.get("email", "") or "",
        password=body.get("password", "") or "",
    )
    user = user_repository.find_by_email(param.email)
    if user is not None and password_encoder.matches(param.password, user.password):
        user_data = user_query_service.find_by_id(user.id)
        return JSONResponse(
            status_code=200,
            content=_user_response(
                UserWithToken.of(user_data, jwt_service.to_token(user))
            ),
        )
    raise InvalidAuthenticationException()
