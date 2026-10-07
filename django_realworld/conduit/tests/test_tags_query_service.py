"""Port of ``application/tag/TagsQueryServiceTest.java``."""

from conduit.application import commands, queries


def test_should_get_all_tags(user):
    commands.create_article(user, "test", "test", "test", ["java"])
    assert "java" in queries.all_tags()
