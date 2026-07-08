from __future__ import annotations

from typing import Dict, List

from fastapi import Request
from fastapi.responses import JSONResponse


class ResourceNotFoundException(Exception):
    """Maps to HTTP 404 (parity with @ResponseStatus(NOT_FOUND))."""


class NoAuthorizationException(Exception):
    """Maps to HTTP 403 (parity with @ResponseStatus(FORBIDDEN))."""


class InvalidAuthenticationException(Exception):
    """Maps to HTTP 422 with {"message": ...} (parity with Java handler)."""

    def __init__(self) -> None:
        super().__init__("invalid email or password")
        self.message = "invalid email or password"


class InvalidRequestException(Exception):
    """Maps to HTTP 422 with {"errors": {field: [messages]}}."""

    def __init__(self, errors: Dict[str, List[str]]) -> None:
        super().__init__("")
        self.errors = errors


class UnauthenticatedException(Exception):
    """Maps to HTTP 401 (parity with Spring Security HttpStatusEntryPoint)."""


def register_exception_handlers(app) -> None:
    @app.exception_handler(ResourceNotFoundException)
    async def _not_found(request: Request, exc: ResourceNotFoundException):
        return JSONResponse(status_code=404, content=None)

    @app.exception_handler(NoAuthorizationException)
    async def _forbidden(request: Request, exc: NoAuthorizationException):
        return JSONResponse(status_code=403, content=None)

    @app.exception_handler(UnauthenticatedException)
    async def _unauthenticated(request: Request, exc: UnauthenticatedException):
        return JSONResponse(status_code=401, content=None)

    @app.exception_handler(InvalidAuthenticationException)
    async def _invalid_auth(request: Request, exc: InvalidAuthenticationException):
        return JSONResponse(status_code=422, content={"message": exc.message})

    @app.exception_handler(InvalidRequestException)
    async def _invalid_request(request: Request, exc: InvalidRequestException):
        return JSONResponse(status_code=422, content={"errors": exc.errors})
