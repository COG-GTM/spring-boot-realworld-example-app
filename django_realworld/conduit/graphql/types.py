"""Object and input types of ``schema.graphqls``.

Resolver roots are the read-side DTOs from ``conduit.application.data``: ``Article`` wraps
``ArticleData``, ``Comment`` wraps ``CommentData``, ``Profile`` wraps ``ProfileData`` and
``User`` wraps ``UserView``.
"""

from dataclasses import dataclass

import graphene

from conduit.application import cursor_queries, queries
from conduit.application.cursor_queries import CursorPageParameter, CursorPager, Direction
from conduit.application.data import format_datetime
from conduit.application.errors import ResourceNotFound
from conduit.graphql.auth import current_user
from conduit.graphql.errors import PAGINATION_ARGUMENT_MESSAGE, IllegalArgument, InvalidCursor
from conduit.models import User as UserModel


# --- pagination -------------------------------------------------------------------------------
def page_args() -> dict:
    return {
        "first": graphene.Int(),
        "after": graphene.String(),
        "last": graphene.Int(),
        "before": graphene.String(),
    }


def _parse_cursor(value: str | None):
    try:
        return cursor_queries.parse_cursor(value)
    except ValueError as exc:
        raise InvalidCursor(value) from exc


def cursor_page(first=None, after=None, last=None, before=None) -> CursorPageParameter:
    if first is None and last is None:
        raise IllegalArgument(PAGINATION_ARGUMENT_MESSAGE)
    if first is not None:
        return CursorPageParameter.of(_parse_cursor(after), first, Direction.NEXT)
    return CursorPageParameter.of(_parse_cursor(before), last, Direction.PREV)


def to_connection(pager: CursorPager) -> dict:
    return {
        "edges": [
            {"cursor": cursor_queries.to_cursor(item.created_at), "node": item}
            for item in pager.data
        ],
        "page_info": {
            "has_next_page": pager.has_next,
            "has_previous_page": pager.has_previous,
            "start_cursor": pager.start_cursor,
            "end_cursor": pager.end_cursor,
        },
    }


def query_profile(info, username: str):
    profile = queries.find_profile_by_username(username, current_user(info))
    if profile is None:
        raise ResourceNotFound()
    return profile


class PageInfo(graphene.ObjectType):
    end_cursor = graphene.String()
    has_next_page = graphene.Boolean(required=True)
    has_previous_page = graphene.Boolean(required=True)
    start_cursor = graphene.String()


# --- profile ----------------------------------------------------------------------------------
class Profile(graphene.ObjectType):
    username = graphene.String(required=True)
    bio = graphene.String()
    following = graphene.Boolean(required=True)
    image = graphene.String()
    articles = graphene.Field(lambda: ArticlesConnection, **page_args())
    favorites = graphene.Field(lambda: ArticlesConnection, **page_args())
    feed = graphene.Field(lambda: ArticlesConnection, **page_args())

    @staticmethod
    def resolve_articles(root, info, **page):
        pager = cursor_queries.find_recent_articles_with_cursor(
            None, root.username, None, cursor_page(**page), current_user(info)
        )
        return to_connection(pager)

    @staticmethod
    def resolve_favorites(root, info, **page):
        pager = cursor_queries.find_recent_articles_with_cursor(
            None, None, root.username, cursor_page(**page), current_user(info)
        )
        return to_connection(pager)

    @staticmethod
    def resolve_feed(root, info, **page):
        params = cursor_page(**page)
        target = UserModel.objects.filter(username=root.username).first()
        if target is None:
            raise ResourceNotFound()
        return to_connection(cursor_queries.find_user_feed_with_cursor(target, params))


class ProfilePayload(graphene.ObjectType):
    profile = graphene.Field(Profile)


# --- articles & comments ----------------------------------------------------------------------
class Article(graphene.ObjectType):
    author = graphene.Field(Profile, required=True)
    body = graphene.String(required=True)
    comments = graphene.Field(lambda: CommentsConnection, **page_args())
    created_at = graphene.String(required=True)
    description = graphene.String(required=True)
    favorited = graphene.Boolean(required=True)
    favorites_count = graphene.Int(required=True)
    slug = graphene.String(required=True)
    tag_list = graphene.List(graphene.String)
    title = graphene.String(required=True)
    updated_at = graphene.String(required=True)

    @staticmethod
    def resolve_author(root, info):
        return root.author

    @staticmethod
    def resolve_comments(root, info, **page):
        pager = cursor_queries.find_comments_by_article_id_with_cursor(
            root.id, current_user(info), cursor_page(**page)
        )
        return to_connection(pager)

    @staticmethod
    def resolve_created_at(root, info):
        return format_datetime(root.created_at)

    @staticmethod
    def resolve_updated_at(root, info):
        return format_datetime(root.updated_at)


class ArticleEdge(graphene.ObjectType):
    cursor = graphene.String(required=True)
    node = graphene.Field(Article)


class ArticlesConnection(graphene.ObjectType):
    edges = graphene.List(ArticleEdge)
    page_info = graphene.Field(PageInfo, required=True)


class Comment(graphene.ObjectType):
    id = graphene.ID(required=True)
    author = graphene.Field(Profile, required=True)
    article = graphene.Field(Article, required=True)
    body = graphene.String(required=True)
    created_at = graphene.String(required=True)
    updated_at = graphene.String(required=True)

    @staticmethod
    def resolve_author(root, info):
        return root.author

    @staticmethod
    def resolve_article(root, info):
        article = queries.find_article_by_id(root.article_id, current_user(info))
        if article is None:
            raise ResourceNotFound()
        return article

    @staticmethod
    def resolve_created_at(root, info):
        return format_datetime(root.created_at)

    @staticmethod
    def resolve_updated_at(root, info):
        return format_datetime(root.updated_at)


class CommentEdge(graphene.ObjectType):
    cursor = graphene.String(required=True)
    node = graphene.Field(Comment)


class CommentsConnection(graphene.ObjectType):
    edges = graphene.List(CommentEdge)
    page_info = graphene.Field(PageInfo, required=True)


class ArticlePayload(graphene.ObjectType):
    article = graphene.Field(Article)


class CommentPayload(graphene.ObjectType):
    comment = graphene.Field(Comment)


class DeletionStatus(graphene.ObjectType):
    success = graphene.Boolean(required=True)


# --- user -------------------------------------------------------------------------------------
@dataclass
class UserView:
    user: UserModel
    token: str


class User(graphene.ObjectType):
    email = graphene.String(required=True)
    profile = graphene.Field(Profile, required=True)
    token = graphene.String(required=True)
    username = graphene.String(required=True)

    @staticmethod
    def resolve_email(root, info):
        return root.user.email

    @staticmethod
    def resolve_username(root, info):
        return root.user.username

    @staticmethod
    def resolve_profile(root, info):
        return query_profile(info, root.user.username)


class UserPayload(graphene.ObjectType):
    user = graphene.Field(User)


class ErrorItem(graphene.ObjectType):
    key = graphene.String(required=True)
    value = graphene.List(graphene.NonNull(graphene.String), required=True)


class Error(graphene.ObjectType):
    message = graphene.String()
    errors = graphene.List(graphene.NonNull(ErrorItem))


class UserResult(graphene.Union):
    class Meta:
        types = (UserPayload, Error)


# --- inputs -----------------------------------------------------------------------------------
class UpdateArticleInput(graphene.InputObjectType):
    body = graphene.String()
    description = graphene.String()
    title = graphene.String()


class CreateArticleInput(graphene.InputObjectType):
    body = graphene.String(required=True)
    description = graphene.String(required=True)
    tag_list = graphene.List(graphene.String)
    title = graphene.String(required=True)


class CreateUserInput(graphene.InputObjectType):
    email = graphene.String(required=True)
    username = graphene.String(required=True)
    password = graphene.String(required=True)


class UpdateUserInput(graphene.InputObjectType):
    email = graphene.String()
    username = graphene.String()
    password = graphene.String()
    image = graphene.String()
    bio = graphene.String()
