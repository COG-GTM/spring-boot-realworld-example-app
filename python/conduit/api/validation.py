from __future__ import annotations

import re
from typing import Dict, List

from conduit.api.exceptions import InvalidRequestException
from conduit.application.params import (
    NewArticleParam,
    NewCommentParam,
    RegisterParam,
    UpdateUserParam,
)
from conduit.core.article import Article
from conduit.core.user import User

_EMAIL_RE = re.compile(r"^[^@\s]+@[^@\s]+$")


def _is_blank(value) -> bool:
    return value is None or value == ""


def _add(errors: Dict[str, List[str]], field: str, message: str) -> None:
    errors.setdefault(field, []).append(message)


def _raise_if_any(errors: Dict[str, List[str]]) -> None:
    if errors:
        raise InvalidRequestException(errors)


def validate_register(param: RegisterParam, user_repository) -> None:
    errors: Dict[str, List[str]] = {}

    if _is_blank(param.email):
        _add(errors, "email", "can't be empty")
    else:
        if not _EMAIL_RE.match(param.email):
            _add(errors, "email", "should be an email")
        if user_repository.find_by_email(param.email) is not None:
            _add(errors, "email", "duplicated email")

    if _is_blank(param.username):
        _add(errors, "username", "can't be empty")
    elif user_repository.find_by_username(param.username) is not None:
        _add(errors, "username", "duplicated username")

    if _is_blank(param.password):
        _add(errors, "password", "can't be empty")

    _raise_if_any(errors)


def validate_update_user(
    param: UpdateUserParam, target_user: User, user_repository
) -> None:
    errors: Dict[str, List[str]] = {}

    if not _is_blank(param.email) and not _EMAIL_RE.match(param.email):
        _add(errors, "email", "should be an email")

    existing_email = user_repository.find_by_email(param.email)
    email_valid = existing_email is None or existing_email == target_user
    existing_username = user_repository.find_by_username(param.username)
    username_valid = existing_username is None or existing_username == target_user

    if not email_valid:
        _add(errors, "email", "email already exist")
    if not username_valid:
        _add(errors, "username", "username already exist")

    _raise_if_any(errors)


def validate_new_article(param: NewArticleParam, article_query_service) -> None:
    errors: Dict[str, List[str]] = {}

    if _is_blank(param.title):
        _add(errors, "title", "can't be empty")
    elif (
        article_query_service.find_by_slug(Article.to_slug(param.title), None)
        is not None
    ):
        _add(errors, "title", "article name exists")

    if _is_blank(param.description):
        _add(errors, "description", "can't be empty")

    if _is_blank(param.body):
        _add(errors, "body", "can't be empty")

    _raise_if_any(errors)


def validate_new_comment(param: NewCommentParam) -> None:
    errors: Dict[str, List[str]] = {}
    if _is_blank(param.body):
        _add(errors, "body", "can't be empty")
    _raise_if_any(errors)
