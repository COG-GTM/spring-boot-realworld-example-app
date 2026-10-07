"""Port of ``CommentsApi.java``: ``/articles/{slug}/comments[/{id}]``."""

from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView

from conduit.api.serializers_comments import NewCommentSerializer
from conduit.application import commands, queries
from conduit.application.errors import NoAuthorization, ResourceNotFound
from conduit.core.authorization import can_write_comment
from conduit.models import Article, Comment


def _article_or_404(slug: str) -> Article:
    article = Article.objects.filter(slug=slug).first()
    if article is None:
        raise ResourceNotFound()
    return article


class CommentsView(APIView):
    def post(self, request, slug):
        article = _article_or_404(slug)
        serializer = NewCommentSerializer.from_request(request.data)
        serializer.is_valid(raise_exception=True)
        comment = commands.add_comment(request.user, article, serializer.validated_data.get("body"))
        comment_data = queries.find_comment_by_id(comment.id, request.user)
        return Response({"comment": comment_data.to_json()}, status=status.HTTP_201_CREATED)

    def get(self, request, slug):
        article = _article_or_404(slug)
        comments = queries.find_comments_by_article_id(article.id, request.user)
        return Response({"comments": [c.to_json() for c in comments]})


class CommentView(APIView):
    def delete(self, request, slug, comment_id):
        article = _article_or_404(slug)
        comment = Comment.objects.filter(pk=comment_id, article_id=article.id).first()
        if comment is None:
            raise ResourceNotFound()
        if not can_write_comment(request.user, article, comment):
            raise NoAuthorization()
        commands.delete_comment(comment)
        return Response(status=status.HTTP_204_NO_CONTENT)
