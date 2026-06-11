"""Custom DRF exception handling mirroring the Java API error contract.

Mirrors ``CustomizeExceptionHandler`` / ``InvalidRequestException`` /
``ResourceNotFoundException`` / ``NoAuthorizationException``:

- Validation errors -> HTTP 422 ``{"errors": {"body": [...]}}``
- Not found -> 404, forbidden -> 403, missing/invalid auth -> 401
  (handled by DRF's default mapping for those exception types).
"""

from rest_framework import status
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response
from rest_framework.views import exception_handler as drf_exception_handler


def _flatten(detail):
    messages = []
    if isinstance(detail, dict):
        for value in detail.values():
            messages.extend(_flatten(value))
    elif isinstance(detail, (list, tuple)):
        for item in detail:
            messages.extend(_flatten(item))
    else:
        messages.append(str(detail))
    return messages


def custom_exception_handler(exc, context):
    response = drf_exception_handler(exc, context)
    if response is None:
        return response

    if isinstance(exc, ValidationError):
        return Response(
            {"errors": {"body": _flatten(exc.detail)}},
            status=status.HTTP_422_UNPROCESSABLE_ENTITY,
        )

    return response
