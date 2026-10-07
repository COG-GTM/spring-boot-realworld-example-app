"""Read side (CQRS-lite): ports of the ``*QueryService`` classes and MyBatis read services.

All functions return DTOs from ``conduit.application.data``, never model instances.
``current_user`` may be ``None`` for anonymous requests.
"""

from collections.abc import Iterable

from django.db.models import Count

from conduit.application.data import (
    ArticleData,
    ArticleDataList,
    CommentData,
    ProfileData,
    UserData,
)
from conduit.application.page import Page
from conduit.models import Article, ArticleFavorite, Comment, FollowRelation, Tag, User


# --- users / profiles -------------------------------------------------------------------------
def find_user_data(user_id: str) -> UserData | None:
    user = User.objects.filter(pk=user_id).first()
    if user is None:
        return None
    return UserData(user.id, user.email, user.username, user.bio, user.image)


def is_following(current_user: User | None, target_id: str) -> bool:
    if current_user is None:
        return False
    return FollowRelation.objects.filter(user_id=current_user.id, target_id=target_id).exists()


def following_ids(current_user: User | None, target_ids: Iterable[str]) -> set[str]:
    if current_user is None:
        return set()
    return set(
        FollowRelation.objects.filter(
            user_id=current_user.id, target_id__in=list(target_ids)
        ).values_list("target_id", flat=True)
    )


def _profile(user: User, following: bool = False) -> ProfileData:
    return ProfileData(user.id, user.username, user.bio, user.image, following)


def find_profile_by_username(username: str, current_user: User | None) -> ProfileData | None:
    user = User.objects.filter(username=username).first()
    if user is None:
        return None
    return _profile(user, is_following(current_user, user.id))


# --- articles ---------------------------------------------------------------------------------
def build_article_data(articles: list[Article], current_user: User | None) -> list[ArticleData]:
    """Assemble ArticleData (tags, author, favorites, following) for already-ordered articles."""
    if not articles:
        return []
    ids = [a.id for a in articles]
    counts = dict(
        ArticleFavorite.objects.filter(article_id__in=ids)
        .values("article_id")
        .annotate(c=Count("id"))
        .values_list("article_id", "c")
    )
    favorited: set[str] = set()
    if current_user is not None:
        favorited = set(
            ArticleFavorite.objects.filter(article_id__in=ids, user_id=current_user.id).values_list(
                "article_id", flat=True
            )
        )
    followed = following_ids(current_user, {a.user_id for a in articles})
    result = []
    for a in articles:
        result.append(
            ArticleData(
                id=a.id,
                slug=a.slug,
                title=a.title,
                description=a.description,
                body=a.body,
                created_at=a.created_at,
                updated_at=a.updated_at,
                author=_profile(a.user, a.user_id in followed),
                tag_list=sorted(t.name for t in a.tags.all()),
                favorited=a.id in favorited,
                favorites_count=counts.get(a.id, 0),
            )
        )
    return result


def _with_relations(qs):
    return qs.select_related("user").prefetch_related("tags")


def find_article_by_id(article_id: str, current_user: User | None) -> ArticleData | None:
    article = _with_relations(Article.objects.filter(pk=article_id)).first()
    return build_article_data([article], current_user)[0] if article else None


def find_article_by_slug(slug: str, current_user: User | None) -> ArticleData | None:
    article = _with_relations(Article.objects.filter(slug=slug)).first()
    return build_article_data([article], current_user)[0] if article else None


def filter_articles(
    tag: str | None = None, author: str | None = None, favorited_by: str | None = None
):
    """Base queryset for article listing filters (shared by offset and cursor pagination)."""
    qs = Article.objects.all()
    if tag:
        qs = qs.filter(tags__name=tag)
    if author:
        qs = qs.filter(user__username=author)
    if favorited_by:
        qs = qs.filter(favorites__user__username=favorited_by)
    return qs.distinct()


def find_recent_articles(
    tag: str | None,
    author: str | None,
    favorited_by: str | None,
    page: Page,
    current_user: User | None,
) -> ArticleDataList:
    qs = filter_articles(tag, author, favorited_by)
    count = qs.count()
    articles = list(
        _with_relations(qs.order_by("-created_at"))[page.offset : page.offset + page.limit]
    )
    return ArticleDataList(build_article_data(articles, current_user), count)


def followed_user_ids(user: User) -> list[str]:
    return list(FollowRelation.objects.filter(user_id=user.id).values_list("target_id", flat=True))


def find_user_feed(user: User, page: Page) -> ArticleDataList:
    authors = followed_user_ids(user)
    if not authors:
        return ArticleDataList([], 0)
    qs = Article.objects.filter(user_id__in=authors)
    count = qs.count()
    articles = list(
        _with_relations(qs.order_by("-created_at"))[page.offset : page.offset + page.limit]
    )
    return ArticleDataList(build_article_data(articles, user), count)


# --- comments ---------------------------------------------------------------------------------
def build_comment_data(comments: list[Comment], current_user: User | None) -> list[CommentData]:
    followed = following_ids(current_user, {c.user_id for c in comments})
    return [
        CommentData(
            id=c.id,
            body=c.body,
            article_id=c.article_id,
            created_at=c.created_at,
            updated_at=c.updated_at,
            author=_profile(c.user, c.user_id in followed),
        )
        for c in comments
    ]


def find_comment_by_id(comment_id: str, current_user: User | None) -> CommentData | None:
    comment = Comment.objects.select_related("user").filter(pk=comment_id).first()
    return build_comment_data([comment], current_user)[0] if comment else None


def find_comments_by_article_id(article_id: str, current_user: User | None) -> list[CommentData]:
    comments = list(
        Comment.objects.select_related("user").filter(article_id=article_id).order_by("created_at")
    )
    return build_comment_data(comments, current_user)


# --- tags -------------------------------------------------------------------------------------
def all_tags() -> list[str]:
    return list(Tag.objects.values_list("name", flat=True))
