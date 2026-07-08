from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional


def format_datetime(value: Optional[datetime]) -> Optional[str]:
    """Match Joda ISODateTimeFormat.dateTime().withZoneUTC(): yyyy-MM-ddTHH:mm:ss.SSSZ."""
    if value is None:
        return None
    if value.tzinfo is None:
        value = value.replace(tzinfo=timezone.utc)
    value = value.astimezone(timezone.utc)
    millis = value.microsecond // 1000
    return value.strftime("%Y-%m-%dT%H:%M:%S") + f".{millis:03d}Z"


@dataclass
class ProfileData:
    id: Optional[str] = None
    username: Optional[str] = None
    bio: Optional[str] = None
    image: Optional[str] = None
    following: bool = False

    def to_json(self) -> Dict[str, Any]:
        # id is @JsonIgnore in Java
        return {
            "username": self.username,
            "bio": self.bio,
            "image": self.image,
            "following": self.following,
        }


@dataclass
class UserData:
    id: Optional[str] = None
    email: Optional[str] = None
    username: Optional[str] = None
    bio: Optional[str] = None
    image: Optional[str] = None


@dataclass
class UserWithToken:
    email: Optional[str]
    username: Optional[str]
    bio: Optional[str]
    image: Optional[str]
    token: Optional[str]

    @classmethod
    def of(cls, user_data: UserData, token: str) -> "UserWithToken":
        return cls(
            email=user_data.email,
            username=user_data.username,
            bio=user_data.bio,
            image=user_data.image,
            token=token,
        )

    def to_json(self) -> Dict[str, Any]:
        return {
            "email": self.email,
            "username": self.username,
            "bio": self.bio,
            "image": self.image,
            "token": self.token,
        }


@dataclass
class ArticleData:
    id: Optional[str] = None
    slug: Optional[str] = None
    title: Optional[str] = None
    description: Optional[str] = None
    body: Optional[str] = None
    favorited: bool = False
    favorites_count: int = 0
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None
    tag_list: List[str] = field(default_factory=list)
    profile_data: Optional[ProfileData] = None

    @property
    def cursor(self) -> Optional[datetime]:
        return self.updated_at

    def to_json(self) -> Dict[str, Any]:
        return {
            "id": self.id,
            "slug": self.slug,
            "title": self.title,
            "description": self.description,
            "body": self.body,
            "tagList": list(self.tag_list),
            "createdAt": format_datetime(self.created_at),
            "updatedAt": format_datetime(self.updated_at),
            "favorited": self.favorited,
            "favoritesCount": self.favorites_count,
            "author": self.profile_data.to_json() if self.profile_data else None,
        }


@dataclass
class CommentData:
    id: Optional[str] = None
    body: Optional[str] = None
    article_id: Optional[str] = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None
    profile_data: Optional[ProfileData] = None

    @property
    def cursor(self) -> Optional[datetime]:
        return self.created_at

    def to_json(self) -> Dict[str, Any]:
        # articleId is @JsonIgnore in Java
        return {
            "id": self.id,
            "body": self.body,
            "createdAt": format_datetime(self.created_at),
            "updatedAt": format_datetime(self.updated_at),
            "author": self.profile_data.to_json() if self.profile_data else None,
        }


@dataclass
class ArticleDataList:
    article_datas: List[ArticleData]
    count: int

    def to_json(self) -> Dict[str, Any]:
        return {
            "articles": [a.to_json() for a in self.article_datas],
            "articlesCount": self.count,
        }


@dataclass
class ArticleFavoriteCount:
    id: str
    count: int
