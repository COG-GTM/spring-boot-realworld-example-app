"""Root-wrapped request bodies for the article endpoints (``{"article": {...}}``).

Fields are deliberately lenient: business validation (blank checks, duplicated titles)
lives in ``conduit.application.commands`` so messages match the Java bean validation.
"""

from rest_framework import serializers


def _text():
    return serializers.CharField(
        required=False, allow_blank=True, allow_null=True, trim_whitespace=False
    )


class NewArticleSerializer(serializers.Serializer):
    title = _text()
    description = _text()
    body = _text()
    tagList = serializers.ListField(
        child=serializers.CharField(trim_whitespace=False), required=False, allow_null=True
    )


class UpdateArticleSerializer(serializers.Serializer):
    title = _text()
    description = _text()
    body = _text()


class NewArticleRequestSerializer(serializers.Serializer):
    article = NewArticleSerializer()


class UpdateArticleRequestSerializer(serializers.Serializer):
    article = UpdateArticleSerializer()


def parse_article(serializer_class, data) -> dict:
    serializer = serializer_class(data=data)
    serializer.is_valid(raise_exception=True)
    return serializer.validated_data["article"]
