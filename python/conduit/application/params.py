from __future__ import annotations

from dataclasses import dataclass
from typing import List, Optional


@dataclass
class RegisterParam:
    email: str = ""
    username: str = ""
    password: str = ""


@dataclass
class UpdateUserParam:
    email: str = ""
    password: str = ""
    username: str = ""
    bio: str = ""
    image: str = ""


@dataclass
class NewArticleParam:
    title: str = ""
    description: str = ""
    body: str = ""
    tag_list: Optional[List[str]] = None


@dataclass
class UpdateArticleParam:
    title: str = ""
    body: str = ""
    description: str = ""


@dataclass
class NewCommentParam:
    body: str = ""


@dataclass
class LoginParam:
    email: str = ""
    password: str = ""
