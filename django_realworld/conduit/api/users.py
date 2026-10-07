"""Port of UsersApi.java + CurrentUserApi.java."""

from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView

from conduit.api.serializers_users import (
    LoginParamSerializer,
    RegisterParamSerializer,
    UpdateUserParamSerializer,
    parse_user_body,
)
from conduit.application import commands, queries
from conduit.application.data import user_with_token
from conduit.infrastructure import jwt_service


def _user_response(user_id: str, token: str, status_code: int = status.HTTP_200_OK) -> Response:
    user_data = queries.find_user_data(user_id)
    return Response({"user": user_with_token(user_data, token)}, status=status_code)


class RegisterView(APIView):
    def post(self, request):
        param = parse_user_body(request.data, RegisterParamSerializer)
        user = commands.register_user(
            email=param.get("email"),
            username=param.get("username"),
            password=param.get("password"),
        )
        return _user_response(user.id, jwt_service.to_token(user), status.HTTP_201_CREATED)


class LoginView(APIView):
    def post(self, request):
        param = parse_user_body(request.data, LoginParamSerializer)
        user = commands.login(email=param.get("email"), password=param.get("password"))
        return _user_response(user.id, jwt_service.to_token(user))


class CurrentUserView(APIView):
    def get(self, request):
        return _user_response(request.user.id, request.auth)

    def put(self, request):
        param = parse_user_body(request.data, UpdateUserParamSerializer)
        commands.update_user(request.user, **param)
        return _user_response(request.user.id, request.auth)
