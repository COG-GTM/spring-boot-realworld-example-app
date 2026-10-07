"""Request bodies for the users/current-user endpoints.

Serializers only unwrap and type the ``{"user": {...}}`` body; validation rules live in
``conduit.application.commands`` so REST and GraphQL report identical errors.
"""

from rest_framework import serializers


def _text():
    return serializers.CharField(
        required=False, allow_blank=True, allow_null=True, trim_whitespace=False
    )


class RegisterParamSerializer(serializers.Serializer):
    email = _text()
    username = _text()
    password = _text()


class LoginParamSerializer(serializers.Serializer):
    email = _text()
    password = _text()


class UpdateUserParamSerializer(serializers.Serializer):
    email = _text()
    username = _text()
    password = _text()
    bio = _text()
    image = _text()


def parse_user_body(data, serializer_class) -> dict:
    """Unwrap ``{"user": {...}}`` (``@JsonRootName("user")``) and return the typed fields."""
    body = data.get("user") if isinstance(data, dict) else None
    serializer = serializer_class(data=body if body is not None else {})
    serializer.is_valid(raise_exception=True)
    return serializer.validated_data
