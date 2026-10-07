"""Port of ``api.security.JwtTokenFilter``.

Reads ``Authorization: Token <jwt>``. A missing, malformed or invalid token leaves the
request anonymous (no error), exactly like the Spring filter; protected endpoints then
answer 401 via the permission class.
"""

from rest_framework.authentication import BaseAuthentication, get_authorization_header

from conduit.infrastructure import jwt_service
from conduit.models import User

KEYWORD = "Token"


class JwtTokenAuthentication(BaseAuthentication):
    def authenticate(self, request):
        parts = get_authorization_header(request).split()
        if len(parts) < 2 or parts[0].decode(errors="ignore") != KEYWORD:
            return None
        token = parts[1].decode(errors="ignore")
        user_id = jwt_service.get_sub_from_token(token)
        if user_id is None:
            return None
        user = User.objects.filter(pk=user_id).first()
        if user is None:
            return None
        return user, token

    def authenticate_header(self, request):
        return KEYWORD
