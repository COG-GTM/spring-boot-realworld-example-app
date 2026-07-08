from __future__ import annotations

import pytest

from conduit.core.article import Article


@pytest.mark.parametrize(
    "title,expected",
    [
        ("a new   title", "a-new-title"),
        ("a new title", "a-new-title"),
        ("A NEW TITLE", "a-new-title"),
        ("it's a title", "it's-a-title"),
    ],
)
def test_should_get_lower_case_slug(title, expected):
    article = Article(title, "desc", "body", ["java"], "123")
    assert article.slug == expected


def test_should_handle_other_language():
    article = Article("中文：标题", "desc", "body", ["java"], "123")
    assert article.slug == "中文-标题"
