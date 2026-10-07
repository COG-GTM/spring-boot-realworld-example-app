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

ROOT_WRAPPERS = {"user", "article", "comment"}


def _messages(value) -> list[str]:
    return [str(m) for m in (value if isinstance(value, list) else [value])]


def _flatten(detail) -> dict[str, list[str]]:
    if not isinstance(detail, dict):
        return {"non_field_errors": _messages(detail)}
    errors: dict[str, list[str]] = {}
    for key, value in detail.items():
        if isinstance(value, dict):
            nested = _flatten(value)
            if key not in ROOT_WRAPPERS:
                nested = {f"{key}.{k}": v for k, v in nested.items()}
            for k, v in nested.items():
                errors.setdefault(k, []).extend(v)
        else:
            errors.setdefault(key, []).extend(_messages(value))
    return errors


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
