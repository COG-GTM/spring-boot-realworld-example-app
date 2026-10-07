"""Port of core/article/ArticleTest.java (slug generation) plus update semantics."""

import pytest

from conduit.models import Article


@pytest.mark.parametrize(
    "title,slug",
    [
        ("a new   title", "a-new-title"),
        ("a new title 2", "a-new-title-2"),
        ("A NEW TITLE", "a-new-title"),
        ("中文：标题", "中文-标题"),
        ("what?the.hell,w", "what-the-hell-w"),
    ],
)
def test_slug(title, slug):
    assert Article.to_slug(title) == slug
    article = Article(title="x", slug="x", description="d", body="b")
    article.update(title=title)
    assert article.slug == slug


def test_update_title_changes_title_slug_and_updated_at():
    article = Article(title="old", slug="old", description="d", body="b")
    before = article.updated_at
    article.update(title="New Title")
    assert (article.title, article.slug) == ("New Title", "new-title")
    assert article.updated_at >= before


@pytest.mark.parametrize("empty", ["", None])
def test_update_skips_empty_values(empty):
    article = Article(title="old title", slug="old-title", description="d", body="b")
    before = article.updated_at
    article.update(title=empty, description=empty, body=empty)
    assert (article.title, article.slug, article.description, article.body) == (
        "old title",
        "old-title",
        "d",
        "b",
    )
    assert article.updated_at == before


def test_update_description_and_body_only():
    article = Article(title="old title", slug="old-title", description="d", body="b")
    article.update(description="new d", body="new b")
    assert (article.slug, article.description, article.body) == ("old-title", "new d", "new b")


@pytest.mark.django_db
def test_save_assigns_slug_from_title(user):
    article = Article.objects.create(user=user, title="A Fresh Title", description="d", body="b")
    assert article.slug == "a-fresh-title"
