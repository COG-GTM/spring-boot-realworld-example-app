"""Port of ``api.exception.CustomizeExceptionHandler``.

- ValidationFailed / DRF ValidationError -> 422 ``{"errors": {"field": ["message"]}}``
- InvalidAuthentication -> 422 ``{"message": "invalid email or password"}``
- ResourceNotFound -> 404, NoAuthorization -> 403, unauthenticated -> 401 (empty body)
"""

from rest_framework import exceptions, status
from rest_framework.response import Response
from rest_framework.views import exception_handler

from conduit.application.errors import (
    InvalidAuthentication,
    NoAuthorization,
    ResourceNotFound,
    ValidationFailed,
)


def _flatten(detail) -> dict[str, list[str]]:
    if isinstance(detail, dict):
        return {k: [str(m) for m in (v if isinstance(v, list) else [v])] for k, v in detail.items()}
    return {
        "non_field_errors": [str(m) for m in (detail if isinstance(detail, list) else [detail])]
    }


def realworld_exception_handler(exc, context):
    if isinstance(exc, ValidationFailed):
        return Response({"errors": exc.errors}, status=status.HTTP_422_UNPROCESSABLE_ENTITY)
    if isinstance(exc, exceptions.ValidationError):
        return Response(
            {"errors": _flatten(exc.detail)}, status=status.HTTP_422_UNPROCESSABLE_ENTITY
        )
    if isinstance(exc, InvalidAuthentication):
        return Response({"message": str(exc)}, status=status.HTTP_422_UNPROCESSABLE_ENTITY)
    if isinstance(exc, ResourceNotFound):
        return Response(status=status.HTTP_404_NOT_FOUND)
    if isinstance(exc, NoAuthorization):
        return Response(status=status.HTTP_403_FORBIDDEN)
    if isinstance(exc, (exceptions.NotAuthenticated, exceptions.AuthenticationFailed)):
        return Response(status=status.HTTP_401_UNAUTHORIZED, headers={"WWW-Authenticate": "Token"})
    return exception_handler(exc, context)
