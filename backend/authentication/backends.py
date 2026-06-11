"""DRF authentication backend mirroring JwtTokenFilter.

Parses ``Authorization: Token <jwt>``, decodes the token and loads the user.
Returns ``None`` when no/invalid header so the request falls through to the
permission layer (yielding 401 for protected endpoints).
"""

from rest_framework import authentication

from authentication.models import User
from authentication.services import decode_token


class JWTAuthentication(authentication.BaseAuthentication):
    keyword = "Token"

    def authenticate(self, request):
        header = request.META.get("HTTP_AUTHORIZATION")
        if not header:
            return None

        parts = header.split(" ")
        if len(parts) < 2:
            return None

        token = parts[1]
        user_id = decode_token(token)
        if not user_id:
            return None

        try:
            user = User.objects.get(id=user_id)
        except (User.DoesNotExist, ValueError):
            return None

        return (user, token)

    def authenticate_header(self, request):
        # Returning a value makes DRF respond with 401 (not 403) for
        # unauthenticated requests, mirroring HttpStatusEntryPoint(UNAUTHORIZED).
        return self.keyword
