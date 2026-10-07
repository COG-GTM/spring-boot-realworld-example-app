"""Port of MyBatisUserRepositoryTest.java against the Django ORM and ``commands``."""

import pytest

from conduit.application import commands
from conduit.models import FollowRelation, User

pytestmark = pytest.mark.django_db


@pytest.fixture
def user():
    return User.objects.create(
        email="aisensiy@163.com", username="aisensiy", password="123", bio="", image="default"
    )


def test_should_save_and_fetch_user_success(user):
    assert User.objects.get(username="aisensiy") == user
    assert User.objects.get(email="aisensiy@163.com") == user


def test_should_update_user_success(user):
    new_email = "newemail@email.com"
    commands.update_user(user, email=new_email)
    fetched = User.objects.get(username=user.username)
    assert fetched.email == new_email

    new_username = "newUsername"
    commands.update_user(user, username=new_username)
    fetched = User.objects.get(email=user.email)
    assert fetched.username == new_username
    assert fetched.image == user.image == "default"


def test_should_create_new_user_follow_success(user):
    other = User.objects.create(email="other@example.com", username="other", password="123")

    commands.follow(user, other)

    assert FollowRelation.objects.filter(user=user, target=other).exists()


def test_should_unfollow_user_success(user):
    other = User.objects.create(email="other@example.com", username="other", password="123")
    commands.follow(user, other)

    assert commands.unfollow(user, other) is True

    assert not FollowRelation.objects.filter(user=user, target=other).exists()
    assert commands.unfollow(user, other) is False
