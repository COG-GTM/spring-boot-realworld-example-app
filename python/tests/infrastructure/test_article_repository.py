from __future__ import annotations

from datetime import datetime, timezone

from conduit.core.article import Article, Tag
from conduit.core.user import User
from conduit.infrastructure.repositories import SqlArticleRepository, SqlUserRepository


def _setup(engine):
    user_repo = SqlUserRepository(engine)
    article_repo = SqlArticleRepository(engine)
    user = User("aisensiy@gmail.com", "aisensiy", "123", "bio", "default")
    user_repo.save(user)
    article = Article("test", "desc", "body", ["java", "spring"], user.id)
    return article_repo, article


def test_should_create_and_fetch_article_success(engine):
    article_repo, article = _setup(engine)
    article_repo.save(article)
    fetched = article_repo.find_by_id(article.id)
    assert fetched is not None
    assert fetched == article
    assert Tag("java") in fetched.tags
    assert Tag("spring") in fetched.tags


def test_should_update_and_fetch_article_success(engine):
    article_repo, article = _setup(engine)
    article_repo.save(article)

    new_title = "new test 2"
    article.update(new_title, "", "")
    article_repo.save(article)
    fetched = article_repo.find_by_slug(article.slug)
    assert fetched is not None
    assert fetched.title == new_title
    assert fetched.body != ""


def test_should_delete_article(engine):
    article_repo, article = _setup(engine)
    article_repo.save(article)
    article_repo.remove(article)
    assert article_repo.find_by_id(article.id) is None


def test_transactional_rolls_back_tags_on_duplicate(engine):
    user_repo = SqlUserRepository(engine)
    article_repo = SqlArticleRepository(engine)
    user = User("aisensiy@gmail.com", "aisensiy", "123", "bio", "default")
    user_repo.save(user)

    article = Article("test", "desc", "body", ["java", "spring"], user.id)
    article_repo.save(article)

    another = Article("test", "desc", "body", ["java", "spring", "other"], user.id)
    try:
        article_repo.save(another)
    except Exception:
        pass

    # the new "other" tag must not have been persisted (transaction rolled back)
    from sqlalchemy import text

    with engine.connect() as conn:
        found = conn.execute(
            text("select id from tags where name = 'other'")
        ).first()
    assert found is None
