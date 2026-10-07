"""Port of ``infrastructure/comment/MyBatisCommentRepositoryTest.java``.

Uses a real user/article because the Django schema enforces foreign keys (the Java test used
dangling ids ``"123"``/``"456"``).
"""

from conduit.application import commands
from conduit.models import Comment


def test_should_create_and_fetch_comment_success(user):
    article = commands.create_article(user, "title", "desc", "body", [])
    comment = commands.add_comment(user, article, "content")

    fetched = Comment.objects.filter(article_id=article.id, pk=comment.id).first()
    assert fetched == comment
    assert fetched.body == "content"


def test_should_remove_comment_success(user):
    article = commands.create_article(user, "title", "desc", "body", [])
    comment = commands.add_comment(user, article, "content")

    commands.delete_comment(comment)
    assert not Comment.objects.filter(pk=comment.id).exists()
