"""Request body for ``CommentsApi`` (``NewCommentParam``). Validation lives in ``commands``."""

from rest_framework import serializers


class NewCommentSerializer(serializers.Serializer):
    body = serializers.CharField(
        required=False, allow_blank=True, allow_null=True, trim_whitespace=False
    )

    @classmethod
    def from_request(cls, data) -> "NewCommentSerializer":
        """Unwrap the ``{"comment": {...}}`` root (``@JsonRootName("comment")``)."""
        root = data.get("comment") if isinstance(data, dict) else None
        return cls(data=root if isinstance(root, dict) else {})
