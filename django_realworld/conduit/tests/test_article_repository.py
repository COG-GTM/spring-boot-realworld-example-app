"""Port of ``infrastructure/article/MyBatisArticleRepositoryTest.java`` and
``ArticleRepositoryTransactionTest.java`` (MyBatis repository -> models + ``commands``).
"""

import pytest

from conduit.application import commands
from conduit.application.errors import ValidationFailed
from conduit.models import Article, ArticleTag, Tag


@pytest.fixture
def user(make_user):
    return make_user("aisensiy", email="aisensiy@gmail.com", bio="bio")


@pytest.fixture
def article(user):
    return commands.create_article(user, "test", "desc", "body", ["java", "spring"])


def test_should_create_and_fetch_article_success(article):
    fetched = Article.objects.filter(pk=article.id).first()
    assert fetched == article
    tag_names = set(fetched.tags.values_list("name", flat=True))
    assert {"java", "spring"} <= tag_names


def test_should_update_and_fetch_article_success(article):
    new_title = "new test 2"
    commands.update_article(article, new_title, "", "")

    fetched = Article.objects.filter(slug=article.slug).first()
    assert fetched is not None
    assert fetched.slug == "new-test-2"
    assert fetched.title == new_title
    assert fetched.body != ""


def test_should_delete_article(article):
    commands.delete_article(article)
    assert not Article.objects.filter(pk=article.id).exists()


def test_transactional_duplicate_article_does_not_create_tags(user, article):
    with pytest.raises(ValidationFailed):
        commands.create_article(user, "test", "desc", "body", ["java", "spring", "other"])
    assert not Tag.objects.filter(name="other").exists()


def test_create_article_rolls_back_on_failure(user, monkeypatch):
    real_get_or_create = ArticleTag.objects.get_or_create

    def failing_get_or_create(*args, **kwargs):
        if kwargs["tag"].name == "other":
            raise RuntimeError("boom")
        return real_get_or_create(*args, **kwargs)

    monkeypatch.setattr(ArticleTag.objects, "get_or_create", failing_get_or_create)
    with pytest.raises(RuntimeError):
        commands.create_article(user, "rollback", "desc", "body", ["java", "other"])

    assert not Article.objects.filter(slug="rollback").exists()
    assert not Tag.objects.filter(name__in=["java", "other"]).exists()
