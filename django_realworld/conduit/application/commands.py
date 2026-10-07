"""Write side (CQRS-lite): ports of ``UserService``, ``ArticleCommandService`` and the
repository ``save``/``remove`` calls made by the controllers. Validation messages match the
Java bean-validation messages so error payloads stay identical.
"""

from django.conf import settings
from django.core.exceptions import ValidationError as DjangoValidationError
from django.core.validators import validate_email
from django.db import transaction

from conduit.application.errors import ErrorCollector, InvalidAuthentication
from conduit.infrastructure.hashers import hash_password, verify_password
from conduit.models import (
    Article,
    ArticleFavorite,
    ArticleTag,
    Comment,
    FollowRelation,
    Tag,
    User,
)

CANT_BE_EMPTY = "can't be empty"
SHOULD_BE_EMAIL = "should be an email"


def _blank(value) -> bool:
    return value is None or str(value).strip() == ""


def _is_email(value: str) -> bool:
    try:
        validate_email(value)
    except DjangoValidationError:
        return False
    return True


# --- users ------------------------------------------------------------------------------------
def register_user(email: str | None, username: str | None, password: str | None) -> User:
    errors = ErrorCollector()
    if _blank(email):
        errors.add("email", CANT_BE_EMPTY)
    else:
        if not _is_email(email):
            errors.add("email", SHOULD_BE_EMAIL)
        if User.objects.filter(email=email).exists():
            errors.add("email", "duplicated email")
    if _blank(username):
        errors.add("username", CANT_BE_EMPTY)
    elif User.objects.filter(username=username).exists():
        errors.add("username", "duplicated username")
    if _blank(password):
        errors.add("password", CANT_BE_EMPTY)
    errors.raise_if_any()

    return User.objects.create(
        email=email,
        username=username,
        password=hash_password(password),
        bio="",
        image=settings.DEFAULT_IMAGE,
    )


def login(email: str | None, password: str | None) -> User:
    errors = ErrorCollector()
    if _blank(email):
        errors.add("email", CANT_BE_EMPTY)
    elif not _is_email(email):
        errors.add("email", SHOULD_BE_EMAIL)
    if _blank(password):
        errors.add("password", CANT_BE_EMPTY)
    errors.raise_if_any()

    user = User.objects.filter(email=email).first()
    if user is None or not verify_password(password, user.password):
        raise InvalidAuthentication()
    return user


def update_user(
    user: User,
    email: str | None = "",
    username: str | None = "",
    password: str | None = "",
    bio: str | None = "",
    image: str | None = "",
) -> User:
    errors = ErrorCollector()
    if email and not _is_email(email):
        errors.add("email", SHOULD_BE_EMAIL)
    if email and User.objects.filter(email=email).exclude(pk=user.pk).exists():
        errors.add("email", "email already exist")
    if username and User.objects.filter(username=username).exclude(pk=user.pk).exists():
        errors.add("username", "username already exist")
    errors.raise_if_any()

    user.update(
        email=email,
        username=username,
        password=hash_password(password) if password else None,
        bio=bio,
        image=image,
    )
    user.save()
    return user


def follow(user: User, target: User) -> None:
    FollowRelation.objects.get_or_create(user=user, target=target)


def unfollow(user: User, target: User) -> bool:
    """Returns False when no relation existed (the REST API answers 404 in that case)."""
    deleted, _ = FollowRelation.objects.filter(user=user, target=target).delete()
    return deleted > 0


# --- articles ---------------------------------------------------------------------------------
@transaction.atomic
def create_article(
    user: User,
    title: str | None,
    description: str | None,
    body: str | None,
    tag_list: list[str] | None = None,
) -> Article:
    errors = ErrorCollector()
    if _blank(title):
        errors.add("title", CANT_BE_EMPTY)
    elif Article.objects.filter(slug=Article.to_slug(title)).exists():
        errors.add("title", "article name exists")
    if _blank(description):
        errors.add("description", CANT_BE_EMPTY)
    if _blank(body):
        errors.add("body", CANT_BE_EMPTY)
    errors.raise_if_any()

    article = Article.objects.create(
        user=user,
        title=title,
        slug=Article.to_slug(title),
        description=description,
        body=body,
    )
    for name in dict.fromkeys(tag_list or []):
        tag, _ = Tag.objects.get_or_create(name=name)
        ArticleTag.objects.get_or_create(article=article, tag=tag)
    return article


def update_article(
    article: Article,
    title: str | None = "",
    description: str | None = "",
    body: str | None = "",
) -> Article:
    if title:
        errors = ErrorCollector()
        if Article.objects.filter(slug=Article.to_slug(title)).exclude(pk=article.pk).exists():
            errors.add("title", "article name exists")
        errors.raise_if_any()
    article.update(title=title, description=description, body=body)
    article.save()
    return article


def delete_article(article: Article) -> None:
    article.delete()


def favorite_article(article: Article, user: User) -> None:
    ArticleFavorite.objects.get_or_create(article=article, user=user)


def unfavorite_article(article: Article, user: User) -> None:
    ArticleFavorite.objects.filter(article=article, user=user).delete()


# --- comments ---------------------------------------------------------------------------------
def add_comment(user: User, article: Article, body: str | None) -> Comment:
    errors = ErrorCollector()
    if _blank(body):
        errors.add("body", CANT_BE_EMPTY)
    errors.raise_if_any()
    return Comment.objects.create(user=user, article=article, body=body)


def delete_comment(comment: Comment) -> None:
    comment.delete()
