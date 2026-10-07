from conduit.infrastructure import jwt_service
from conduit.models import FollowRelation, User
from conduit.tests.graphql_helpers import NOT_FOUND_MESSAGE, assert_auth_error, error_of, gql

CREATE_USER = """
mutation($input: CreateUserInput) {
  createUser(input: $input) {
    __typename
    ... on UserPayload { user { email username token profile { username bio image following } } }
    ... on Error { message errors { key value } }
  }
}
"""

LOGIN = """
mutation($email: String!, $password: String!) {
  login(email: $email, password: $password) { user { email username token } }
}
"""

ME = "{ me { email username token profile { username following } } }"

UPDATE_USER = """
mutation($changes: UpdateUserInput!) {
  updateUser(changes: $changes) { user { email username token profile { bio image } } }
}
"""

FOLLOW = "mutation($u: String!) { followUser(username: $u) { profile { username following } } }"
UNFOLLOW = "mutation($u: String!) { unfollowUser(username: $u) { profile { username following } } }"
PROFILE = (
    "query($u: String!) { profile(username: $u) { profile { username bio image following } } }"
)


def test_create_user_returns_user_payload(api_client, db):
    variables = {"input": {"email": "john@jacob.com", "username": "johnjacob", "password": "123"}}
    data = gql(api_client, CREATE_USER, variables)["data"]["createUser"]
    assert data["__typename"] == "UserPayload"
    user = data["user"]
    assert user["email"] == "john@jacob.com" and user["username"] == "johnjacob"
    assert user["profile"] == {
        "username": "johnjacob",
        "bio": "",
        "image": "https://static.productionready.io/images/smiley-cyrus.jpg",
        "following": False,
    }
    created = User.objects.get(username="johnjacob")
    assert jwt_service.get_sub_from_token(user["token"]) == created.id


def test_create_user_returns_error_union_on_validation_failure(api_client, db):
    variables = {"input": {"email": "bad", "username": "", "password": ""}}
    data = gql(api_client, CREATE_USER, variables)["data"]["createUser"]
    assert data["__typename"] == "Error"
    assert data["message"] == "BAD_REQUEST"
    assert {e["key"]: e["value"] for e in data["errors"]} == {
        "email": ["should be an email"],
        "username": ["can't be empty"],
        "password": ["can't be empty"],
    }


def test_create_user_reports_duplicates_in_error_union(api_client, user):
    variables = {"input": {"email": user.email, "username": user.username, "password": "123"}}
    data = gql(api_client, CREATE_USER, variables)["data"]["createUser"]
    assert {e["key"]: e["value"] for e in data["errors"]} == {
        "email": ["duplicated email"],
        "username": ["duplicated username"],
    }


def test_login_success(api_client, user):
    result = gql(api_client, LOGIN, {"email": user.email, "password": "123"})
    payload = result["data"]["login"]["user"]
    assert payload["email"] == user.email and payload["username"] == user.username
    assert jwt_service.get_sub_from_token(payload["token"]) == user.id


def test_login_with_wrong_password_is_unauthenticated(api_client, user):
    for email, password in [(user.email, "wrong"), ("nobody@example.com", "123"), ("bad", "x")]:
        result = gql(api_client, LOGIN, {"email": email, "password": password})
        error = error_of(result)
        assert error["message"] == "invalid email or password"
        assert error["extensions"] == {"errorType": "UNAUTHENTICATED"}
        assert error["path"] == ["login"]
        assert result["data"] == {"login": None}


def test_me_returns_current_user_with_request_token(auth_client, user, token):
    data = gql(auth_client, ME)["data"]["me"]
    assert data == {
        "email": user.email,
        "username": user.username,
        "token": token,
        "profile": {"username": user.username, "following": False},
    }


def test_me_is_null_when_anonymous_or_token_invalid(api_client, db):
    assert gql(api_client, ME) == {"data": {"me": None}}
    api_client.credentials(HTTP_AUTHORIZATION="Token garbage")
    assert gql(api_client, ME) == {"data": {"me": None}}


def test_update_user(auth_client, user):
    changes = {"username": "newname", "bio": "new bio", "image": "img.png", "email": ""}
    data = gql(auth_client, UPDATE_USER, {"changes": changes})["data"]["updateUser"]["user"]
    assert data["username"] == "newname" and data["email"] == user.email
    assert data["profile"] == {"bio": "new bio", "image": "img.png"}
    user.refresh_from_db()
    assert user.username == "newname" and user.bio == "new bio"


def test_update_user_password(auth_client, api_client, user):
    gql(auth_client, UPDATE_USER, {"changes": {"password": "456"}})
    result = gql(api_client, LOGIN, {"email": user.email, "password": "456"})
    assert result["data"]["login"]["user"]["username"] == user.username


def test_update_user_is_null_when_anonymous(api_client, db):
    assert gql(api_client, UPDATE_USER, {"changes": {"bio": "x"}}) == {"data": {"updateUser": None}}


def test_update_user_validation_error(auth_client, make_user, user):
    other = make_user("other")
    result = gql(auth_client, UPDATE_USER, {"changes": {"email": other.email}})
    error = error_of(result)
    assert error["message"] == "email: email already exist"
    assert error["extensions"] == {"email": ["email already exist"], "errorType": "BAD_REQUEST"}
    assert result["data"] == {"updateUser": None}


def test_follow_and_unfollow(auth_client, user, make_user):
    target = make_user("target")
    data = gql(auth_client, FOLLOW, {"u": "target"})["data"]["followUser"]["profile"]
    assert data == {"username": "target", "following": True}
    assert FollowRelation.objects.filter(user=user, target=target).exists()

    data = gql(auth_client, UNFOLLOW, {"u": "target"})["data"]["unfollowUser"]["profile"]
    assert data == {"username": "target", "following": False}
    assert not FollowRelation.objects.filter(user=user, target=target).exists()


def test_follow_requires_authentication(api_client, make_user):
    make_user("target")
    assert_auth_error(gql(api_client, FOLLOW, {"u": "target"}), "followUser")
    assert_auth_error(gql(api_client, UNFOLLOW, {"u": "target"}), "unfollowUser")


def test_follow_unknown_user_is_not_found(auth_client, user):
    error = error_of(gql(auth_client, FOLLOW, {"u": "nobody"}))
    assert error["message"] == NOT_FOUND_MESSAGE
    assert error["extensions"] == {"errorType": "INTERNAL"}


def test_unfollow_without_relation_is_not_found(auth_client, make_user, user):
    make_user("target")
    assert error_of(gql(auth_client, UNFOLLOW, {"u": "target"}))["message"] == NOT_FOUND_MESSAGE


def test_profile_query(api_client, client_for, make_user):
    me, target = make_user("me"), make_user("target", bio="hi")
    data = gql(api_client, PROFILE, {"u": "target"})["data"]["profile"]["profile"]
    assert data["username"] == "target" and data["bio"] == "hi" and data["following"] is False

    FollowRelation.objects.create(user=me, target=target)
    data = gql(client_for(me), PROFILE, {"u": "target"})["data"]["profile"]["profile"]
    assert data["following"] is True


def test_profile_query_unknown_user(api_client, db):
    result = gql(api_client, PROFILE, {"u": "nobody"})
    assert error_of(result)["message"] == NOT_FOUND_MESSAGE
    assert result["data"] == {"profile": None}
