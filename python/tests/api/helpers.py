from __future__ import annotations

from datetime import datetime, timezone
from typing import List, Optional

from conduit.application.data import (
    ArticleData,
    CommentData,
    ProfileData,
    UserData,
)


def user_data(user) -> UserData:
    return UserData(
        id=user.id,
        email=user.email,
        username=user.username,
        bio=user.bio,
        image=user.image,
    )


def profile_data(user, following: bool = False) -> ProfileData:
    return ProfileData(
        id=user.id,
        username=user.username,
        bio=user.bio,
        image=user.image,
        following=following,
    )


def article_data(
    author,
    title: str = "Test Article",
    tags: Optional[List[str]] = None,
    favorited: bool = False,
    favorites_count: int = 0,
) -> ArticleData:
    now = datetime(2020, 1, 1, tzinfo=timezone.utc)
    return ArticleData(
        id="article-id",
        slug="test-article",
        title=title,
        description="desc",
        body="body",
        favorited=favorited,
        favorites_count=favorites_count,
        created_at=now,
        updated_at=now,
        tag_list=tags or ["java"],
        profile_data=profile_data(author),
    )


def comment_data(author, body: str = "comment body") -> CommentData:
    now = datetime(2020, 1, 1, tzinfo=timezone.utc)
    return CommentData(
        id="comment-id",
        body=body,
        article_id="article-id",
        created_at=now,
        updated_at=now,
        profile_data=profile_data(author),
    )
