"""Mutation root: ports of ``UserMutation``, ``RelationMutation``, ``ArticleMutation`` and
``CommentMutation``. All writes go through ``conduit.application.commands``.
"""

import graphene

from conduit.application import commands, queries
from conduit.application.errors import (
    InvalidAuthentication,
    NoAuthorization,
    ResourceNotFound,
    ValidationFailed,
)
from conduit.core.authorization import can_write_article, can_write_comment
from conduit.graphql.auth import current_user, require_user
from conduit.graphql.types import (
    ArticlePayload,
    CommentPayload,
    CreateArticleInput,
    CreateUserInput,
    DeletionStatus,
    Error,
    ErrorItem,
    ProfilePayload,
    UpdateArticleInput,
    UpdateUserInput,
    UserPayload,
    UserResult,
    UserView,
)
from conduit.infrastructure import jwt_service
from conduit.models import Article, Comment, User


def _user_payload(user: User) -> UserPayload:
    return UserPayload(user=UserView(user, jwt_service.to_token(user)))


def _article_or_404(slug: str) -> Article:
    article = Article.objects.filter(slug=slug).first()
    if article is None:
        raise ResourceNotFound()
    return article


def _article_payload(article: Article, user: User) -> ArticlePayload:
    data = queries.find_article_by_id(article.id, user)
    if data is None:
        raise ResourceNotFound()
    return ArticlePayload(article=data)


def _profile_payload(username: str, user: User) -> ProfilePayload:
    return ProfilePayload(profile=queries.find_profile_by_username(username, user))


class Mutation(graphene.ObjectType):
    # User & Profile
    create_user = graphene.Field(UserResult, input=CreateUserInput())
    login = graphene.Field(
        UserPayload, password=graphene.String(required=True), email=graphene.String(required=True)
    )
    update_user = graphene.Field(UserPayload, changes=UpdateUserInput(required=True))
    follow_user = graphene.Field(ProfilePayload, username=graphene.String(required=True))
    unfollow_user = graphene.Field(ProfilePayload, username=graphene.String(required=True))

    # Article
    create_article = graphene.Field(ArticlePayload, input=CreateArticleInput(required=True))
    update_article = graphene.Field(
        ArticlePayload,
        slug=graphene.String(required=True),
        changes=UpdateArticleInput(required=True),
    )
    favorite_article = graphene.Field(ArticlePayload, slug=graphene.String(required=True))
    unfavorite_article = graphene.Field(ArticlePayload, slug=graphene.String(required=True))
    delete_article = graphene.Field(DeletionStatus, slug=graphene.String(required=True))

    # Comment
    add_comment = graphene.Field(
        CommentPayload, slug=graphene.String(required=True), body=graphene.String(required=True)
    )
    delete_comment = graphene.Field(
        DeletionStatus, slug=graphene.String(required=True), id=graphene.ID(required=True)
    )

    @staticmethod
    def resolve_create_user(root, info, input=None):
        input = input or {}
        try:
            user = commands.register_user(
                input.get("email"), input.get("username"), input.get("password")
            )
        except ValidationFailed as exc:
            items = [ErrorItem(key=k, value=v) for k, v in exc.errors.items()]
            return Error(message="BAD_REQUEST", errors=items)
        return _user_payload(user)

    @staticmethod
    def resolve_login(root, info, password, email):
        try:
            user = commands.login(email, password)
        except ValidationFailed as exc:
            raise InvalidAuthentication() from exc
        return _user_payload(user)

    @staticmethod
    def resolve_update_user(root, info, changes):
        user = current_user(info)
        if user is None:
            return None
        commands.update_user(
            user,
            email=changes.get("email"),
            username=changes.get("username"),
            password=changes.get("password"),
            bio=changes.get("bio"),
            image=changes.get("image"),
        )
        return _user_payload(user)

    @staticmethod
    def resolve_follow_user(root, info, username):
        user = require_user(info)
        target = User.objects.filter(username=username).first()
        if target is None:
            raise ResourceNotFound()
        commands.follow(user, target)
        return _profile_payload(username, user)

    @staticmethod
    def resolve_unfollow_user(root, info, username):
        user = require_user(info)
        target = User.objects.filter(username=username).first()
        if target is None or not commands.unfollow(user, target):
            raise ResourceNotFound()
        return _profile_payload(username, user)

    @staticmethod
    def resolve_create_article(root, info, input):
        user = require_user(info)
        article = commands.create_article(
            user,
            input.get("title"),
            input.get("description"),
            input.get("body"),
            input.get("tag_list") or [],
        )
        return _article_payload(article, user)

    @staticmethod
    def resolve_update_article(root, info, slug, changes):
        article = _article_or_404(slug)
        user = require_user(info)
        if not can_write_article(user, article):
            raise NoAuthorization()
        article = commands.update_article(
            article,
            title=changes.get("title"),
            description=changes.get("description"),
            body=changes.get("body"),
        )
        return _article_payload(article, user)

    @staticmethod
    def resolve_favorite_article(root, info, slug):
        user = require_user(info)
        article = _article_or_404(slug)
        commands.favorite_article(article, user)
        return _article_payload(article, user)

    @staticmethod
    def resolve_unfavorite_article(root, info, slug):
        user = require_user(info)
        article = _article_or_404(slug)
        commands.unfavorite_article(article, user)
        return _article_payload(article, user)

    @staticmethod
    def resolve_delete_article(root, info, slug):
        user = require_user(info)
        article = _article_or_404(slug)
        if not can_write_article(user, article):
            raise NoAuthorization()
        commands.delete_article(article)
        return DeletionStatus(success=True)

    @staticmethod
    def resolve_add_comment(root, info, slug, body):
        user = require_user(info)
        article = _article_or_404(slug)
        comment = commands.add_comment(user, article, body)
        return CommentPayload(comment=queries.find_comment_by_id(comment.id, user))

    @staticmethod
    def resolve_delete_comment(root, info, slug, id):
        user = require_user(info)
        article = _article_or_404(slug)
        comment = Comment.objects.filter(article_id=article.id, pk=id).first()
        if comment is None:
            raise ResourceNotFound()
        if not can_write_comment(user, article, comment):
            raise NoAuthorization()
        commands.delete_comment(comment)
        return DeletionStatus(success=True)
