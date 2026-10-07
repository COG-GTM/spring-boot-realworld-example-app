"""Port of ``application/profile/ProfileQueryServiceTest.java``."""

from conduit.application import queries
from conduit.models import User


def test_should_fetch_profile_success(make_user):
    current_user = User(email="a@test.com", username="a", password="123")
    profile_user = make_user("p", email="p@test.com")

    profile = queries.find_profile_by_username(profile_user.username, current_user)
    assert profile is not None
    assert profile.username == "p"
    assert profile.following is False


def test_should_return_none_for_unknown_profile(db):
    assert queries.find_profile_by_username("nobody", None) is None
