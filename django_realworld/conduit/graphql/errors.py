"""Port of ``graphql.exception.GraphQLCustomizeExceptionHandler`` and the DGS default handler.

Application errors are turned into GraphQL errors with the same ``message`` and
``extensions.errorType`` the Java service produced, so GraphQL clients see identical payloads:

- ``InvalidAuthentication`` -> ``UNAUTHENTICATED``, ``"invalid email or password"``
- ``ValidationFailed`` -> ``BAD_REQUEST`` with ``{field: [messages]}`` in ``extensions``
- anything else -> ``INTERNAL``, ``"<java exception class>: <message>"`` (DGS default handler)
"""

from graphql import GraphQLError

from conduit.application.errors import (
    InvalidAuthentication,
    NoAuthorization,
    ResourceNotFound,
    ValidationFailed,
)

PAGINATION_ARGUMENT_MESSAGE = "first 和 last 必须只存在一个"


class AuthenticationException(Exception):
    """A field needs a signed-in user (``graphql.exception.AuthenticationException``)."""


class IllegalArgument(Exception):
    pass


class InvalidCursor(Exception):
    def __init__(self, cursor: str):
        super().__init__(f'For input string: "{cursor}"')


_JAVA_NAMES = {
    AuthenticationException: "io.spring.graphql.exception.AuthenticationException",
    ResourceNotFound: "io.spring.api.exception.ResourceNotFoundException",
    NoAuthorization: "io.spring.api.exception.NoAuthorizationException",
    IllegalArgument: "java.lang.IllegalArgumentException",
    InvalidCursor: "java.lang.NumberFormatException",
}


def _internal(exc: Exception) -> GraphQLError:
    cls = type(exc)
    name = _JAVA_NAMES.get(cls, f"{cls.__module__}.{cls.__qualname__}")
    message = str(exc) or "null"
    return GraphQLError(f"{name}: {message}", extensions={"errorType": "INTERNAL"})


def to_graphql_error(exc: Exception) -> GraphQLError:
    if isinstance(exc, GraphQLError):
        return exc
    if isinstance(exc, InvalidAuthentication):
        return GraphQLError(str(exc), extensions={"errorType": "UNAUTHENTICATED"})
    if isinstance(exc, ValidationFailed):
        message = ", ".join(f"{field}: {msg}" for field, msgs in exc.errors.items() for msg in msgs)
        return GraphQLError(message, extensions={**exc.errors, "errorType": "BAD_REQUEST"})
    return _internal(exc)


class ExceptionMappingMiddleware:
    """Graphene middleware that maps exceptions raised by resolvers via ``to_graphql_error``."""

    def resolve(self, next_, root, info, **kwargs):
        try:
            return next_(root, info, **kwargs)
        except Exception as exc:
            raise to_graphql_error(exc) from exc
