"""Query root: ports of the ``@DgsQuery`` fetchers (Article, Me, Profile, Tag)."""

import graphene

from conduit.application import cursor_queries, queries
from conduit.application.errors import ResourceNotFound
from conduit.graphql.auth import current_token, current_user, require_user
from conduit.graphql.types import (
    Article,
    ArticlesConnection,
    ProfilePayload,
    User,
    UserView,
    cursor_page,
    page_args,
    query_profile,
    to_connection,
)


class Query(graphene.ObjectType):
    article = graphene.Field(Article, slug=graphene.String(required=True))
    articles = graphene.Field(
        ArticlesConnection,
        **page_args(),
        authored_by=graphene.String(),
        favorited_by=graphene.String(),
        with_tag=graphene.String(),
    )
    me = graphene.Field(User)
    feed = graphene.Field(ArticlesConnection, **page_args())
    profile = graphene.Field(ProfilePayload, username=graphene.String(required=True))
    tags = graphene.List(graphene.String)

    @staticmethod
    def resolve_article(root, info, slug):
        article = queries.find_article_by_slug(slug, current_user(info))
        if article is None:
            raise ResourceNotFound()
        return article

    @staticmethod
    def resolve_articles(root, info, authored_by=None, favorited_by=None, with_tag=None, **page):
        pager = cursor_queries.find_recent_articles_with_cursor(
            with_tag, authored_by, favorited_by, cursor_page(**page), current_user(info)
        )
        return to_connection(pager)

    @staticmethod
    def resolve_me(root, info):
        user = current_user(info)
        if user is None:
            return None
        return UserView(user, current_token(info))

    @staticmethod
    def resolve_feed(root, info, **page):
        params = cursor_page(**page)
        user = require_user(info)
        return to_connection(cursor_queries.find_user_feed_with_cursor(user, params))

    @staticmethod
    def resolve_profile(root, info, username):
        return ProfilePayload(profile=query_profile(info, username))

    @staticmethod
    def resolve_tags(root, info):
        return queries.all_tags()
