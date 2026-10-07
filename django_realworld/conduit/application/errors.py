"""Adapter-agnostic application errors. REST and GraphQL map these to their own formats."""


class ValidationFailed(Exception):
    """Field validation errors, e.g. ``{"email": ["can't be empty"]}``."""

    def __init__(self, errors: dict[str, list[str]]):
        super().__init__("validation failed")
        self.errors = errors


class InvalidAuthentication(Exception):
    def __init__(self):
        super().__init__("invalid email or password")


class ResourceNotFound(Exception):
    pass


class NoAuthorization(Exception):
    pass


class ErrorCollector:
    def __init__(self):
        self.errors: dict[str, list[str]] = {}

    def add(self, field: str, message: str) -> None:
        self.errors.setdefault(field, []).append(message)

    def raise_if_any(self) -> None:
        if self.errors:
            raise ValidationFailed(self.errors)
