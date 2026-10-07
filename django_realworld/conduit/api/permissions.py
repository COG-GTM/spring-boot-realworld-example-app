"""Port of the ``authorizeRequests()`` rules in ``api.security.WebSecurityConfig``.

Public: OPTIONS, POST /users, POST /users/login, GET /articles/** (except /articles/feed),
GET /profiles/**, GET /tags. Everything else requires a valid token.
/graphql is public and is not served by DRF.
"""

from rest_framework.permissions import BasePermission


def is_public(method: str, path: str) -> bool:
    path = "/" + path.strip("/")
    if method == "OPTIONS":
        return True
    if method == "POST" and path in ("/users", "/users/login"):
        return True
    if method == "GET":
        if path == "/articles/feed":
            return False
        if path == "/articles" or path.startswith("/articles/"):
            return True
        if path.startswith("/profiles/"):
            return True
        if path == "/tags":
            return True
    return False


class RealWorldAccessPolicy(BasePermission):
    def has_permission(self, request, view):
        if is_public(request.method, request.path):
            return True
        return request.user is not None
