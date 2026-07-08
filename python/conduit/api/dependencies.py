from __future__ import annotations

from typing import Optional

from fastapi import Depends, Header, Request

from conduit.api.exceptions import UnauthenticatedException
from conduit.application.query_services import (
    ArticleQueryService,
    CommentQueryService,
    ProfileQueryService,
    TagsQueryService,
    UserQueryService,
)
from conduit.application.services import ArticleCommandService, UserService
from conduit.config import Settings
from conduit.core.article import ArticleRepository
from conduit.core.comment import CommentRepository
from conduit.core.favorite import ArticleFavoriteRepository
from conduit.core.service import JwtService
from conduit.core.user import User, UserRepository
from conduit.infrastructure.jwt_service import DefaultJwtService
from conduit.infrastructure.password import BCryptPasswordEncoder
from conduit.infrastructure.read_services import (
    ArticleFavoritesReadService,
    ArticleReadService,
    CommentReadService,
    TagReadService,
    UserReadService,
    UserRelationshipQueryService,
)
from conduit.infrastructure.repositories import (
    SqlArticleFavoriteRepository,
    SqlArticleRepository,
    SqlCommentRepository,
    SqlUserRepository,
)


class Container:
    def __init__(self, engine, settings: Settings) -> None:
        self.engine = engine
        self.settings = settings

        self.password_encoder = BCryptPasswordEncoder()
        self.jwt_service: JwtService = DefaultJwtService(
            settings.jwt_secret, settings.jwt_session_time
        )

        self.user_repository = SqlUserRepository(engine)
        self.article_repository = SqlArticleRepository(engine)
        self.comment_repository = SqlCommentRepository(engine)
        self.article_favorite_repository = SqlArticleFavoriteRepository(engine)

        user_read = UserReadService(engine)
        relationship = UserRelationshipQueryService(engine)
        article_read = ArticleReadService(engine)
        favorites_read = ArticleFavoritesReadService(engine)
        comment_read = CommentReadService(engine)
        tag_read = TagReadService(engine)

        self.user_query_service = UserQueryService(user_read)
        self.profile_query_service = ProfileQueryService(user_read, relationship)
        self.tags_query_service = TagsQueryService(tag_read)
        self.article_query_service = ArticleQueryService(
            article_read, relationship, favorites_read
        )
        self.comment_query_service = CommentQueryService(comment_read, relationship)

        self.user_service = UserService(
            self.user_repository, settings.default_image, self.password_encoder
        )
        self.article_command_service = ArticleCommandService(self.article_repository)


def _container(request: Request) -> Container:
    return request.app.state.container


def get_settings(request: Request) -> Settings:
    return _container(request).settings


def get_password_encoder(request: Request) -> BCryptPasswordEncoder:
    return _container(request).password_encoder


def get_jwt_service(request: Request) -> JwtService:
    return _container(request).jwt_service


def get_user_repository(request: Request) -> UserRepository:
    return _container(request).user_repository


def get_article_repository(request: Request) -> ArticleRepository:
    return _container(request).article_repository


def get_comment_repository(request: Request) -> CommentRepository:
    return _container(request).comment_repository


def get_article_favorite_repository(request: Request) -> ArticleFavoriteRepository:
    return _container(request).article_favorite_repository


def get_user_query_service(request: Request) -> UserQueryService:
    return _container(request).user_query_service


def get_profile_query_service(request: Request) -> ProfileQueryService:
    return _container(request).profile_query_service


def get_tags_query_service(request: Request) -> TagsQueryService:
    return _container(request).tags_query_service


def get_article_query_service(request: Request) -> ArticleQueryService:
    return _container(request).article_query_service


def get_comment_query_service(request: Request) -> CommentQueryService:
    return _container(request).comment_query_service


def get_user_service(request: Request) -> UserService:
    return _container(request).user_service


def get_article_command_service(request: Request) -> ArticleCommandService:
    return _container(request).article_command_service


def _resolve_user(
    authorization: Optional[str],
    jwt_service: JwtService,
    user_repository: UserRepository,
) -> Optional[User]:
    if authorization is None:
        return None
    split = authorization.split(" ")
    if len(split) < 2:
        return None
    token = split[1]
    sub = jwt_service.get_sub_from_token(token)
    if sub is None:
        return None
    return user_repository.find_by_id(sub)


def get_current_user_optional(
    authorization: Optional[str] = Header(default=None),
    jwt_service: JwtService = Depends(get_jwt_service),
    user_repository: UserRepository = Depends(get_user_repository),
) -> Optional[User]:
    return _resolve_user(authorization, jwt_service, user_repository)


def get_current_user_required(
    current_user: Optional[User] = Depends(get_current_user_optional),
) -> User:
    if current_user is None:
        raise UnauthenticatedException()
    return current_user
