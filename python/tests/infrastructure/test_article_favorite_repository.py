from __future__ import annotations

from conduit.core.favorite import ArticleFavorite
from conduit.infrastructure.repositories import SqlArticleFavoriteRepository


def test_should_save_and_fetch_favorite_success(engine):
    repo = SqlArticleFavoriteRepository(engine)
    favorite = ArticleFavorite("123", "456")
    repo.save(favorite)
    assert repo.find("123", "456") is not None


def test_should_remove_favorite_success(engine):
    repo = SqlArticleFavoriteRepository(engine)
    favorite = ArticleFavorite("123", "456")
    repo.save(favorite)
    repo.remove(favorite)
    assert repo.find("123", "456") is None
