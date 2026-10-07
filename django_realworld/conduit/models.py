"""Domain entities ported from ``io.spring.core`` and ``V1__create_tables.sql``.

Table and column names match the original Flyway schema so an existing ``dev.db``
layout is recognisable.
"""

import re
import uuid

from django.db import models
from django.utils import timezone

_SLUG_PATTERN = re.compile(r"[&|\uFE30-\uFFA0’”\s?,.]+")


def _uuid() -> str:
    return str(uuid.uuid4())


def _is_empty(value) -> bool:
    return value is None or value == ""


def now_ms():
    """Millisecond precision like Java ``DateTime``, so epoch-millis cursors are exact."""
    now = timezone.now()
    return now.replace(microsecond=now.microsecond // 1000 * 1000)


class User(models.Model):
    id = models.CharField(primary_key=True, max_length=255, default=_uuid, editable=False)
    username = models.CharField(max_length=255, unique=True)
    password = models.CharField(max_length=255)
    email = models.CharField(max_length=255, unique=True)
    bio = models.TextField(blank=True, default="")
    image = models.CharField(max_length=511, blank=True, default="")

    class Meta:
        db_table = "users"

    # DRF treats any non-None request.user exposing is_authenticated as signed in.
    is_authenticated = True
    is_anonymous = False

    def update(self, email=None, username=None, password=None, bio=None, image=None):
        """Apply only non-empty values, like ``core.user.User.update``.

        ``password`` must already be hashed.
        """
        if not _is_empty(email):
            self.email = email
        if not _is_empty(username):
            self.username = username
        if not _is_empty(password):
            self.password = password
        if not _is_empty(bio):
            self.bio = bio
        if not _is_empty(image):
            self.image = image

    def __str__(self) -> str:
        return self.username


class Tag(models.Model):
    id = models.CharField(primary_key=True, max_length=255, default=_uuid, editable=False)
    name = models.CharField(max_length=255, unique=True)

    class Meta:
        db_table = "tags"

    def __str__(self) -> str:
        return self.name


class Article(models.Model):
    id = models.CharField(primary_key=True, max_length=255, default=_uuid, editable=False)
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name="articles")
    slug = models.CharField(max_length=255, unique=True)
    title = models.CharField(max_length=255)
    description = models.TextField()
    body = models.TextField()
    created_at = models.DateTimeField(default=now_ms)
    updated_at = models.DateTimeField(default=now_ms)
    tags = models.ManyToManyField(Tag, through="ArticleTag", related_name="articles")

    class Meta:
        db_table = "articles"

    @staticmethod
    def to_slug(title: str) -> str:
        return _SLUG_PATTERN.sub("-", title.lower())

    def save(self, *args, **kwargs):
        if not self.slug and self.title:
            self.slug = self.to_slug(self.title)
        super().save(*args, **kwargs)

    def update(self, title=None, description=None, body=None):
        """Apply only non-empty values, like ``core.article.Article.update``."""
        if not _is_empty(title):
            self.title = title
            self.slug = self.to_slug(title)
            self.updated_at = now_ms()
        if not _is_empty(description):
            self.description = description
            self.updated_at = now_ms()
        if not _is_empty(body):
            self.body = body
            self.updated_at = now_ms()

    def __str__(self) -> str:
        return self.slug


class ArticleTag(models.Model):
    article = models.ForeignKey(Article, on_delete=models.CASCADE)
    tag = models.ForeignKey(Tag, on_delete=models.CASCADE)

    class Meta:
        db_table = "article_tags"
        constraints = [models.UniqueConstraint(fields=["article", "tag"], name="uniq_article_tag")]


class ArticleFavorite(models.Model):
    article = models.ForeignKey(Article, on_delete=models.CASCADE, related_name="favorites")
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name="favorites")

    class Meta:
        db_table = "article_favorites"
        constraints = [
            models.UniqueConstraint(fields=["article", "user"], name="uniq_article_favorite")
        ]


class FollowRelation(models.Model):
    """``user`` follows ``target`` (``follows.user_id`` -> ``follows.follow_id``)."""

    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name="following")
    target = models.ForeignKey(
        User, on_delete=models.CASCADE, related_name="followers", db_column="follow_id"
    )

    class Meta:
        db_table = "follows"
        constraints = [models.UniqueConstraint(fields=["user", "target"], name="uniq_follow")]


class Comment(models.Model):
    id = models.CharField(primary_key=True, max_length=255, default=_uuid, editable=False)
    body = models.TextField()
    article = models.ForeignKey(Article, on_delete=models.CASCADE, related_name="comments")
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name="comments")
    created_at = models.DateTimeField(default=now_ms)
    updated_at = models.DateTimeField(default=now_ms)

    class Meta:
        db_table = "comments"
