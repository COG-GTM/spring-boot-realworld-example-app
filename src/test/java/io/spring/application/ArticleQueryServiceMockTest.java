package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.spring.application.data.ArticleData;
import io.spring.application.data.ArticleDataList;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.infrastructure.mybatis.readservice.ArticleFavoritesReadService;
import io.spring.infrastructure.mybatis.readservice.ArticleReadService;
import io.spring.infrastructure.mybatis.readservice.UserRelationshipQueryService;
import java.util.*;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleQueryServiceMockTest {

  @Mock private ArticleReadService articleReadService;
  @Mock private UserRelationshipQueryService userRelationshipQueryService;
  @Mock private ArticleFavoritesReadService articleFavoritesReadService;

  private ArticleQueryService articleQueryService;

  @BeforeEach
  void setUp() {
    articleQueryService =
        new ArticleQueryService(
            articleReadService, userRelationshipQueryService, articleFavoritesReadService);
  }

  @Test
  void should_return_empty_when_article_not_found_by_id() {
    when(articleReadService.findById("nonexistent")).thenReturn(null);

    Optional<ArticleData> result = articleQueryService.findById("nonexistent", null);

    assertFalse(result.isPresent());
  }

  @Test
  void should_find_article_by_id_without_user() {
    ProfileData profileData = new ProfileData("authorId", "author", "bio", "img", false);
    ArticleData articleData =
        new ArticleData(
            "id1",
            "test-slug",
            "Test Title",
            "desc",
            "body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Collections.emptyList(),
            profileData);
    when(articleReadService.findById("id1")).thenReturn(articleData);

    Optional<ArticleData> result = articleQueryService.findById("id1", null);

    assertTrue(result.isPresent());
    assertEquals("test-slug", result.get().getSlug());
  }

  @Test
  void should_find_article_by_id_with_user_and_fill_extra_info() {
    User currentUser = new User("user@example.com", "user", "pass", "", "");
    ProfileData profileData = new ProfileData("authorId", "author", "bio", "img", false);
    ArticleData articleData =
        new ArticleData(
            "id1",
            "test-slug",
            "Test Title",
            "desc",
            "body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Collections.emptyList(),
            profileData);
    when(articleReadService.findById("id1")).thenReturn(articleData);
    when(articleFavoritesReadService.isUserFavorite(currentUser.getId(), "id1")).thenReturn(true);
    when(articleFavoritesReadService.articleFavoriteCount("id1")).thenReturn(5);
    when(userRelationshipQueryService.isUserFollowing(currentUser.getId(), "authorId"))
        .thenReturn(true);

    Optional<ArticleData> result = articleQueryService.findById("id1", currentUser);

    assertTrue(result.isPresent());
    assertTrue(result.get().isFavorited());
    assertEquals(5, result.get().getFavoritesCount());
    assertTrue(result.get().getProfileData().isFollowing());
  }

  @Test
  void should_return_empty_when_article_not_found_by_slug() {
    when(articleReadService.findBySlug("nonexistent")).thenReturn(null);

    Optional<ArticleData> result = articleQueryService.findBySlug("nonexistent", null);

    assertFalse(result.isPresent());
  }

  @Test
  void should_find_article_by_slug_without_user() {
    ProfileData profileData = new ProfileData("authorId", "author", "bio", "img", false);
    ArticleData articleData =
        new ArticleData(
            "id1",
            "test-slug",
            "Test Title",
            "desc",
            "body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Collections.emptyList(),
            profileData);
    when(articleReadService.findBySlug("test-slug")).thenReturn(articleData);

    Optional<ArticleData> result = articleQueryService.findBySlug("test-slug", null);

    assertTrue(result.isPresent());
    assertEquals("id1", result.get().getId());
  }

  @Test
  void should_return_empty_cursor_pager_when_no_articles() {
    when(articleReadService.findArticlesWithCursor(any(), any(), any(), any()))
        .thenReturn(Collections.emptyList());

    CursorPager<ArticleData> result =
        articleQueryService.findRecentArticlesWithCursor(
            null,
            null,
            null,
            new CursorPageParameter<>(null, 10, CursorPager.Direction.NEXT),
            null);

    assertTrue(result.getData().isEmpty());
    assertFalse(result.hasNext());
  }

  @Test
  void should_return_empty_feed_when_no_followed_users() {
    User user = new User("user@example.com", "user", "pass", "", "");
    when(userRelationshipQueryService.followedUsers(user.getId()))
        .thenReturn(Collections.emptyList());

    CursorPager<ArticleData> result =
        articleQueryService.findUserFeedWithCursor(
            user, new CursorPageParameter<>(null, 10, CursorPager.Direction.NEXT));

    assertTrue(result.getData().isEmpty());
    assertFalse(result.hasNext());
  }

  @Test
  void should_return_empty_article_list_when_no_recent_articles() {
    when(articleReadService.queryArticles(any(), any(), any(), any()))
        .thenReturn(Collections.emptyList());
    when(articleReadService.countArticle(any(), any(), any())).thenReturn(0);

    ArticleDataList result =
        articleQueryService.findRecentArticles(null, null, null, new Page(0, 10), null);

    assertTrue(result.getArticleDatas().isEmpty());
    assertEquals(0, result.getCount());
  }

  @Test
  void should_return_empty_feed_list_when_no_followed_users() {
    User user = new User("user@example.com", "user", "pass", "", "");
    when(userRelationshipQueryService.followedUsers(user.getId()))
        .thenReturn(Collections.emptyList());

    ArticleDataList result = articleQueryService.findUserFeed(user, new Page(0, 10));

    assertTrue(result.getArticleDatas().isEmpty());
    assertEquals(0, result.getCount());
  }
}
