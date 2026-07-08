from __future__ import annotations

from typing import List, Optional

from conduit.application.data import ArticleData, ArticleDataList, CommentData, ProfileData, UserData
from conduit.application.page import CursorPageParameter, CursorPager, Page
from conduit.core.user import User
from conduit.infrastructure.read_services import (
    ArticleFavoritesReadService,
    ArticleReadService,
    CommentReadService,
    TagReadService,
    UserReadService,
    UserRelationshipQueryService,
)


class UserQueryService:
    def __init__(self, user_read_service: UserReadService) -> None:
        self.user_read_service = user_read_service

    def find_by_id(self, id: str) -> Optional[UserData]:
        return self.user_read_service.find_by_id(id)


class ProfileQueryService:
    def __init__(
        self,
        user_read_service: UserReadService,
        user_relationship_query_service: UserRelationshipQueryService,
    ) -> None:
        self.user_read_service = user_read_service
        self.user_relationship_query_service = user_relationship_query_service

    def find_by_username(
        self, username: str, current_user: Optional[User]
    ) -> Optional[ProfileData]:
        user_data = self.user_read_service.find_by_username(username)
        if user_data is None:
            return None
        following = current_user is not None and self.user_relationship_query_service.is_user_following(
            current_user.id, user_data.id
        )
        return ProfileData(
            id=user_data.id,
            username=user_data.username,
            bio=user_data.bio,
            image=user_data.image,
            following=following,
        )


class TagsQueryService:
    def __init__(self, tag_read_service: TagReadService) -> None:
        self.tag_read_service = tag_read_service

    def all_tags(self) -> List[str]:
        return self.tag_read_service.all()


class CommentQueryService:
    def __init__(
        self,
        comment_read_service: CommentReadService,
        user_relationship_query_service: UserRelationshipQueryService,
    ) -> None:
        self.comment_read_service = comment_read_service
        self.user_relationship_query_service = user_relationship_query_service

    def find_by_id(self, id: str, user: User) -> Optional[CommentData]:
        comment_data = self.comment_read_service.find_by_id(id)
        if comment_data is None:
            return None
        comment_data.profile_data.following = (
            self.user_relationship_query_service.is_user_following(
                user.id, comment_data.profile_data.id
            )
        )
        return comment_data

    def find_by_article_id(
        self, article_id: str, user: Optional[User]
    ) -> List[CommentData]:
        comments = self.comment_read_service.find_by_article_id(article_id)
        if comments and user is not None:
            following_authors = self.user_relationship_query_service.following_authors(
                user.id, [c.profile_data.id for c in comments]
            )
            for comment_data in comments:
                if comment_data.profile_data.id in following_authors:
                    comment_data.profile_data.following = True
        return comments

    def find_by_article_id_with_cursor(
        self, article_id: str, user: Optional[User], page: CursorPageParameter
    ) -> CursorPager[CommentData]:
        comments = self.comment_read_service.find_by_article_id_with_cursor(
            article_id, page
        )
        if not comments:
            return CursorPager([], page.direction, False)
        if user is not None:
            following_authors = self.user_relationship_query_service.following_authors(
                user.id, [c.profile_data.id for c in comments]
            )
            for comment_data in comments:
                if comment_data.profile_data.id in following_authors:
                    comment_data.profile_data.following = True
        has_extra = len(comments) > page.limit
        if has_extra:
            del comments[page.limit]
        if not page.is_next():
            comments.reverse()
        return CursorPager(comments, page.direction, has_extra)


class ArticleQueryService:
    def __init__(
        self,
        article_read_service: ArticleReadService,
        user_relationship_query_service: UserRelationshipQueryService,
        article_favorites_read_service: ArticleFavoritesReadService,
    ) -> None:
        self.article_read_service = article_read_service
        self.user_relationship_query_service = user_relationship_query_service
        self.article_favorites_read_service = article_favorites_read_service

    def find_by_id(self, id: str, user: Optional[User]) -> Optional[ArticleData]:
        article_data = self.article_read_service.find_by_id(id)
        if article_data is None:
            return None
        if user is not None:
            self._fill_extra_info_single(id, user, article_data)
        return article_data

    def find_by_slug(self, slug: str, user: Optional[User]) -> Optional[ArticleData]:
        article_data = self.article_read_service.find_by_slug(slug)
        if article_data is None:
            return None
        if user is not None:
            self._fill_extra_info_single(article_data.id, user, article_data)
        return article_data

    def find_recent_articles(
        self,
        tag: Optional[str],
        author: Optional[str],
        favorited_by: Optional[str],
        page: Page,
        current_user: Optional[User],
    ) -> ArticleDataList:
        article_ids = self.article_read_service.query_articles(
            tag, author, favorited_by, page
        )
        article_count = self.article_read_service.count_article(
            tag, author, favorited_by
        )
        if not article_ids:
            return ArticleDataList([], article_count)
        articles = self.article_read_service.find_articles(article_ids)
        self._fill_extra_info(articles, current_user)
        return ArticleDataList(articles, article_count)

    def find_user_feed(self, user: User, page: Page) -> ArticleDataList:
        followed_users = self.user_relationship_query_service.followed_users(user.id)
        if not followed_users:
            return ArticleDataList([], 0)
        articles = self.article_read_service.find_articles_of_authors(
            followed_users, page
        )
        self._fill_extra_info(articles, user)
        count = self.article_read_service.count_feed_size(followed_users)
        return ArticleDataList(articles, count)

    def find_recent_articles_with_cursor(
        self,
        tag: Optional[str],
        author: Optional[str],
        favorited_by: Optional[str],
        page: CursorPageParameter,
        current_user: Optional[User],
    ) -> CursorPager[ArticleData]:
        article_ids = self.article_read_service.find_articles_with_cursor(
            tag, author, favorited_by, page
        )
        if not article_ids:
            return CursorPager([], page.direction, False)
        has_extra = len(article_ids) > page.limit
        if has_extra:
            del article_ids[page.limit]
        if not page.is_next():
            article_ids.reverse()
        articles = self.article_read_service.find_articles(article_ids)
        self._fill_extra_info(articles, current_user)
        return CursorPager(articles, page.direction, has_extra)

    def find_user_feed_with_cursor(
        self, user: User, page: CursorPageParameter
    ) -> CursorPager[ArticleData]:
        followed_users = self.user_relationship_query_service.followed_users(user.id)
        if not followed_users:
            return CursorPager([], page.direction, False)
        articles = self.article_read_service.find_articles_of_authors_with_cursor(
            followed_users, page
        )
        has_extra = len(articles) > page.limit
        if has_extra:
            del articles[page.limit]
        if not page.is_next():
            articles.reverse()
        self._fill_extra_info(articles, user)
        return CursorPager(articles, page.direction, has_extra)

    def _fill_extra_info(
        self, articles: List[ArticleData], current_user: Optional[User]
    ) -> None:
        self._set_favorite_count(articles)
        if current_user is not None:
            self._set_is_favorite(articles, current_user)
            self._set_is_following_author(articles, current_user)

    def _set_is_following_author(
        self, articles: List[ArticleData], current_user: User
    ) -> None:
        following_authors = self.user_relationship_query_service.following_authors(
            current_user.id, [a.profile_data.id for a in articles]
        )
        for article_data in articles:
            if article_data.profile_data.id in following_authors:
                article_data.profile_data.following = True

    def _set_favorite_count(self, articles: List[ArticleData]) -> None:
        favorites_counts = self.article_favorites_read_service.articles_favorite_count(
            [a.id for a in articles]
        )
        count_map = {item.id: item.count for item in favorites_counts}
        for article_data in articles:
            article_data.favorites_count = count_map.get(article_data.id, 0)

    def _set_is_favorite(
        self, articles: List[ArticleData], current_user: User
    ) -> None:
        favorited = self.article_favorites_read_service.user_favorites(
            [a.id for a in articles], current_user
        )
        for article_data in articles:
            if article_data.id in favorited:
                article_data.favorited = True

    def _fill_extra_info_single(
        self, id: str, user: User, article_data: ArticleData
    ) -> None:
        article_data.favorited = self.article_favorites_read_service.is_user_favorite(
            user.id, id
        )
        article_data.favorites_count = (
            self.article_favorites_read_service.article_favorite_count(id)
        )
        article_data.profile_data.following = (
            self.user_relationship_query_service.is_user_following(
                user.id, article_data.profile_data.id
            )
        )
