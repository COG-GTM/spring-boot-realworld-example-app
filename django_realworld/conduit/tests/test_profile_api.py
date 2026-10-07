"""Port of ProfileApiTest.java, run against the real test database."""

import pytest

from conduit.models import FollowRelation

pytestmark = pytest.mark.django_db


@pytest.fixture
def another_user(make_user):
    return make_user(username="username", email="username@test.com")


def test_should_get_user_profile_success(api_client, another_user):
    response = api_client.get(f"/profiles/{another_user.username}")

    assert response.status_code == 200
    assert response.json() == {
        "profile": {
            "username": "username",
            "bio": "",
            "image": another_user.image,
            "following": False,
        }
    }


def test_should_get_404_for_unknown_profile(api_client):
    assert api_client.get("/profiles/nobody").status_code == 404


def test_should_show_following_for_current_user(auth_client, user, another_user):
    FollowRelation.objects.create(user=user, target=another_user)

    response = auth_client.get(f"/profiles/{another_user.username}")

    assert response.status_code == 200
    assert response.json()["profile"]["following"] is True


def test_should_follow_user_success(auth_client, user, another_user):
    response = auth_client.post(f"/profiles/{another_user.username}/follow")

    assert response.status_code == 200
    assert response.json()["profile"]["username"] == "username"
    assert response.json()["profile"]["following"] is True
    assert FollowRelation.objects.filter(user=user, target=another_user).exists()


def test_should_follow_user_idempotently(auth_client, user, another_user):
    auth_client.post(f"/profiles/{another_user.username}/follow")
    response = auth_client.post(f"/profiles/{another_user.username}/follow")

    assert response.status_code == 200
    assert FollowRelation.objects.filter(user=user, target=another_user).count() == 1


def test_should_get_404_when_following_unknown_user(auth_client):
    assert auth_client.post("/profiles/nobody/follow").status_code == 404


def test_should_get_401_when_following_without_token(api_client, another_user):
    assert api_client.post(f"/profiles/{another_user.username}/follow").status_code == 401


def test_should_unfollow_user_success(auth_client, user, another_user):
    FollowRelation.objects.create(user=user, target=another_user)

    response = auth_client.delete(f"/profiles/{another_user.username}/follow")

    assert response.status_code == 200
    assert response.json()["profile"]["following"] is False
    assert not FollowRelation.objects.filter(user=user, target=another_user).exists()


def test_should_get_404_when_unfollowing_without_relation(auth_client, another_user):
    assert auth_client.delete(f"/profiles/{another_user.username}/follow").status_code == 404


def test_should_get_404_when_unfollowing_unknown_user(auth_client):
    assert auth_client.delete("/profiles/nobody/follow").status_code == 404
