"""Current-user resolution for GraphQL (port of ``JwtTokenFilter`` + ``graphql.SecurityUtil``).

``GraphQLView`` is not a DRF view, so the ``Authorization: Token <jwt>`` header is read here.
A missing or invalid token leaves the request anonymous; fields that need a user call
``require_user`` which raises ``AuthenticationException``.
"""

from conduit.graphql.errors import AuthenticationException
from conduit.infrastructure import jwt_service
from conduit.models import User

KEYWORD = "Token"
_USER_ATTR = "_conduit_graphql_user"


def current_token(info) -> str | None:
    parts = info.context.META.get("HTTP_AUTHORIZATION", "").split()
    if len(parts) < 2 or parts[0] != KEYWORD:
        return None
    return parts[1]


def current_user(info) -> User | None:
    request = info.context
    if not hasattr(request, _USER_ATTR):
        user = None
        token = current_token(info)
        user_id = jwt_service.get_sub_from_token(token) if token else None
        if user_id is not None:
            user = User.objects.filter(pk=user_id).first()
        setattr(request, _USER_ATTR, user)
    return getattr(request, _USER_ATTR)


def require_user(info) -> User:
    user = current_user(info)
    if user is None:
        raise AuthenticationException()
    return user
