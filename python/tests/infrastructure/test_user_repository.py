from __future__ import annotations

from conduit.core.user import FollowRelation, User
from conduit.infrastructure.repositories import SqlUserRepository


def test_should_save_and_fetch_user_success(engine):
    repo = SqlUserRepository(engine)
    user = User("aisensiy@163.com", "aisensiy", "123", "", "default")
    repo.save(user)

    assert repo.find_by_username("aisensiy") == user
    assert repo.find_by_email("aisensiy@163.com") == user


def test_should_update_user_success(engine):
    repo = SqlUserRepository(engine)
    user = User("aisensiy@163.com", "aisensiy", "123", "", "default")
    repo.save(user)

    new_email = "newemail@email.com"
    user.update(new_email, "", "", "", "")
    repo.save(user)
    fetched = repo.find_by_username(user.username)
    assert fetched is not None
    assert fetched.email == new_email

    new_username = "newUsername"
    user.update("", new_username, "", "", "")
    repo.save(user)
    fetched = repo.find_by_email(user.email)
    assert fetched is not None
    assert fetched.username == new_username
    assert fetched.image == user.image


def test_should_create_new_user_follow_success(engine):
    repo = SqlUserRepository(engine)
    user = User("aisensiy@163.com", "aisensiy", "123", "", "default")
    repo.save(user)
    other = User("other@example.com", "other", "123", "", "")
    repo.save(other)

    repo.save_relation(FollowRelation(user.id, other.id))
    assert repo.find_relation(user.id, other.id) is not None


def test_should_unfollow_user_success(engine):
    repo = SqlUserRepository(engine)
    user = User("aisensiy@163.com", "aisensiy", "123", "", "default")
    repo.save(user)
    other = User("other@example.com", "other", "123", "", "")
    repo.save(other)

    relation = FollowRelation(user.id, other.id)
    repo.save_relation(relation)
    repo.remove_relation(relation)
    assert repo.find_relation(user.id, other.id) is None
