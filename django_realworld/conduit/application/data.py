"""Read models ported from ``application.data``. ``to_json()`` yields the REST JSON shape."""

from dataclasses import dataclass, field
from datetime import UTC, datetime


def format_datetime(value: datetime | None) -> str | None:
    """ISO-8601 in UTC with milliseconds, e.g. ``2016-02-18T03:22:56.637Z`` (Joda dateTime())."""
    if value is None:
        return None
    value = value.astimezone(UTC)
    return value.strftime("%Y-%m-%dT%H:%M:%S.") + f"{value.microsecond // 1000:03d}Z"


@dataclass
class UserData:
    id: str
    email: str
    username: str
    bio: str
    image: str


def user_with_token(user: UserData, token: str) -> dict:
    return {
        "email": user.email,
        "username": user.username,
        "bio": user.bio,
        "image": user.image,
        "token": token,
    }


@dataclass
class ProfileData:
    id: str
    username: str
    bio: str
    image: str
    following: bool = False

    def to_json(self) -> dict:
        return {
            "username": self.username,
            "bio": self.bio,
            "image": self.image,
            "following": self.following,
        }


@dataclass
class ArticleData:
    id: str
    slug: str
    title: str
    description: str
    body: str
    created_at: datetime
    updated_at: datetime
    author: ProfileData
    tag_list: list[str] = field(default_factory=list)
    favorited: bool = False
    favorites_count: int = 0

    def to_json(self) -> dict:
        return {
            "id": self.id,
            "slug": self.slug,
            "title": self.title,
            "description": self.description,
            "body": self.body,
            "favorited": self.favorited,
            "favoritesCount": self.favorites_count,
            "createdAt": format_datetime(self.created_at),
            "updatedAt": format_datetime(self.updated_at),
            "tagList": self.tag_list,
            "author": self.author.to_json(),
        }


@dataclass
class ArticleDataList:
    articles: list[ArticleData]
    count: int

    def to_json(self) -> dict:
        return {"articles": [a.to_json() for a in self.articles], "articlesCount": self.count}


@dataclass
class CommentData:
    id: str
    body: str
    article_id: str
    created_at: datetime
    updated_at: datetime
    author: ProfileData

    def to_json(self) -> dict:
        return {
            "id": self.id,
            "body": self.body,
            "createdAt": format_datetime(self.created_at),
            "updatedAt": format_datetime(self.updated_at),
            "author": self.author.to_json(),
        }
