"""Fixture builders shared by the article API tests."""

from datetime import timedelta

from django.utils import timezone

from conduit.application import commands
from conduit.models import Article


def create_article(author, title, description="desc", body="body", tags=None, minutes_ago=0):
    article = commands.create_article(author, title, description, body, tags or [])
    if minutes_ago:
        created = timezone.now() - timedelta(minutes=minutes_ago)
        Article.objects.filter(pk=article.pk).update(created_at=created, updated_at=created)
        article.refresh_from_db()
    return article


def article_body(title, description, body, tag_list=None) -> dict:
    article = {"title": title, "description": description, "body": body}
    if tag_list is not None:
        article["tagList"] = tag_list
    return {"article": article}
