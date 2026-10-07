"""Port of ``application/comment/CommentQueryServiceTest.java``.

Comments reference a real article here because the Django schema enforces the foreign key
(the Java test used a dangling article id ``"123"``).
"""

import pytest

from conduit.application import commands, queries
from conduit.models import FollowRelation


@pytest.fixture
def user(make_user):
    return make_user("aisensiy", email="aisensiy@test.com")


@pytest.fixture
def article(user):
    return commands.create_article(user, "title", "desc", "body", ["java"])


def test_should_read_comment_success(user, article):
    comment = commands.add_comment(user, article, "content")

    comment_data = queries.find_comment_by_id(comment.id, user)
    assert comment_data is not None
    assert comment_data.author.username == user.username


def test_should_read_comments_of_article(make_user, user, article):
    user2 = make_user("user2", email="user2@email.com")
    FollowRelation.objects.create(user=user, target=user2)

    commands.add_comment(user, article, "content1")
    commands.add_comment(user2, article, "content2")

    comments = queries.find_comments_by_article_id(article.id, user)
    assert len(comments) == 2
    following = {c.author.username: c.author.following for c in comments}
    assert following == {"aisensiy": False, "user2": True}
