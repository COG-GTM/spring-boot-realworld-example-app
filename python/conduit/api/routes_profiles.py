from __future__ import annotations

from fastapi import APIRouter, Depends

from conduit.api.dependencies import (
    get_current_user_optional,
    get_current_user_required,
    get_profile_query_service,
    get_user_repository,
)
from conduit.api.exceptions import ResourceNotFoundException
from conduit.application.data import ProfileData
from conduit.core.user import FollowRelation

router = APIRouter()


def _profile_response(profile: ProfileData) -> dict:
    return {"profile": profile.to_json()}


@router.get("/profiles/{username}")
async def get_profile(
    username: str,
    current_user=Depends(get_current_user_optional),
    profile_query_service=Depends(get_profile_query_service),
):
    profile = profile_query_service.find_by_username(username, current_user)
    if profile is None:
        raise ResourceNotFoundException()
    return _profile_response(profile)


@router.post("/profiles/{username}/follow")
async def follow(
    username: str,
    current_user=Depends(get_current_user_required),
    profile_query_service=Depends(get_profile_query_service),
    user_repository=Depends(get_user_repository),
):
    target = user_repository.find_by_username(username)
    if target is None:
        raise ResourceNotFoundException()
    user_repository.save_relation(FollowRelation(current_user.id, target.id))
    return _profile_response(
        profile_query_service.find_by_username(username, current_user)
    )


@router.delete("/profiles/{username}/follow")
async def unfollow(
    username: str,
    current_user=Depends(get_current_user_required),
    profile_query_service=Depends(get_profile_query_service),
    user_repository=Depends(get_user_repository),
):
    target = user_repository.find_by_username(username)
    if target is None:
        raise ResourceNotFoundException()
    relation = user_repository.find_relation(current_user.id, target.id)
    if relation is None:
        raise ResourceNotFoundException()
    user_repository.remove_relation(relation)
    return _profile_response(
        profile_query_service.find_by_username(username, current_user)
    )
