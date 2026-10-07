"""Port of ProfileApi.java."""

from rest_framework.response import Response
from rest_framework.views import APIView

from conduit.application import commands, queries
from conduit.application.errors import ResourceNotFound
from conduit.models import User


def _profile_response(username: str, current_user) -> Response:
    profile = queries.find_profile_by_username(username, current_user)
    if profile is None:
        raise ResourceNotFound()
    return Response({"profile": profile.to_json()})


def _target_user(username: str) -> User:
    target = User.objects.filter(username=username).first()
    if target is None:
        raise ResourceNotFound()
    return target


class ProfileView(APIView):
    def get(self, request, username):
        return _profile_response(username, request.user)


class FollowView(APIView):
    def post(self, request, username):
        commands.follow(request.user, _target_user(username))
        return _profile_response(username, request.user)

    def delete(self, request, username):
        if not commands.unfollow(request.user, _target_user(username)):
            raise ResourceNotFound()
        return _profile_response(username, request.user)
