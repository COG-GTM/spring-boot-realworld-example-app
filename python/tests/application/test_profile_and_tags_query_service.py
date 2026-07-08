from __future__ import annotations

from conduit.application.query_services import ProfileQueryService, TagsQueryService
from conduit.core.article import Article
from conduit.core.user import FollowRelation, User
from conduit.infrastructure.read_services import (
    TagReadService,
    UserReadService,
    UserRelationshipQueryService,
)
from conduit.infrastructure.repositories import SqlArticleRepository, SqlUserRepository


def test_profile_following_flag(engine):
    user_repo = SqlUserRepository(engine)
    user = User("a@test.com", "aisensiy", "123", "", "")
    other = User("b@test.com", "other", "123", "", "")
    user_repo.save(user)
    user_repo.save(other)

    service = ProfileQueryService(
        UserReadService(engine), UserRelationshipQueryService(engine)
    )

    profile = service.find_by_username("other", user)
    assert profile.username == "other"
    assert profile.following is False

    user_repo.save_relation(FollowRelation(user.id, other.id))
    profile = service.find_by_username("other", user)
    assert profile.following is True


def test_profile_anonymous(engine):
    user_repo = SqlUserRepository(engine)
    user_repo.save(User("b@test.com", "other", "123", "", ""))
    service = ProfileQueryService(
        UserReadService(engine), UserRelationshipQueryService(engine)
    )
    profile = service.find_by_username("other", None)
    assert profile.following is False


def test_profile_missing_returns_none(engine):
    service = ProfileQueryService(
        UserReadService(engine), UserRelationshipQueryService(engine)
    )
    assert service.find_by_username("nobody", None) is None


def test_all_tags(engine):
    user_repo = SqlUserRepository(engine)
    article_repo = SqlArticleRepository(engine)
    user = User("a@test.com", "aisensiy", "123", "", "")
    user_repo.save(user)
    article_repo.save(Article("t", "d", "b", ["java", "spring"], user.id))

    service = TagsQueryService(TagReadService(engine))
    assert set(service.all_tags()) == {"java", "spring"}
