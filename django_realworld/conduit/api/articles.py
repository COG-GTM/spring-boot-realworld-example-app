"""Ports of ``ArticlesApi``, ``ArticleApi`` and ``ArticleFavoriteApi``."""

from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView

from conduit.api.serializers_articles import (
    NewArticleRequestSerializer,
    UpdateArticleRequestSerializer,
    parse_article,
)
from conduit.application import commands, queries
from conduit.application.errors import NoAuthorization, ResourceNotFound
from conduit.application.page import Page
from conduit.core.authorization import can_write_article
from conduit.models import Article


def _page(request) -> Page:
    return Page.of(request.query_params.get("offset"), request.query_params.get("limit"))


def _article_response(article_data) -> Response:
    return Response({"article": article_data.to_json()})


def _load_article(slug: str) -> Article:
    article = Article.objects.filter(slug=slug).first()
    if article is None:
        raise ResourceNotFound()
    return article


def _load_writable_article(slug: str, user) -> Article:
    article = _load_article(slug)
    if not can_write_article(user, article):
        raise NoAuthorization()
    return article


class ArticlesView(APIView):
    def get(self, request):
        params = request.query_params
        result = queries.find_recent_articles(
            params.get("tag"),
            params.get("author"),
            params.get("favorited"),
            _page(request),
            request.user,
        )
        return Response(result.to_json())

    def post(self, request):
        data = parse_article(NewArticleRequestSerializer, request.data)
        article = commands.create_article(
            request.user,
            title=data.get("title"),
            description=data.get("description"),
            body=data.get("body"),
            tag_list=data.get("tagList"),
        )
        return _article_response(queries.find_article_by_id(article.id, request.user))


class FeedView(APIView):
    def get(self, request):
        return Response(queries.find_user_feed(request.user, _page(request)).to_json())


class ArticleView(APIView):
    def get(self, request, slug):
        article_data = queries.find_article_by_slug(slug, request.user)
        if article_data is None:
            raise ResourceNotFound()
        return _article_response(article_data)

    def put(self, request, slug):
        article = _load_writable_article(slug, request.user)
        data = parse_article(UpdateArticleRequestSerializer, request.data)
        updated = commands.update_article(
            article,
            title=data.get("title"),
            description=data.get("description"),
            body=data.get("body"),
        )
        return _article_response(queries.find_article_by_slug(updated.slug, request.user))

    def delete(self, request, slug):
        commands.delete_article(_load_writable_article(slug, request.user))
        return Response(status=status.HTTP_204_NO_CONTENT)


class FavoriteView(APIView):
    def post(self, request, slug):
        commands.favorite_article(_load_article(slug), request.user)
        return _article_response(queries.find_article_by_slug(slug, request.user))

    def delete(self, request, slug):
        commands.unfavorite_article(_load_article(slug), request.user)
        return _article_response(queries.find_article_by_slug(slug, request.user))
