package io.spring.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.spring.application.data.ArticleData;
import io.spring.application.data.ArticleDataList;
import io.spring.application.data.ArticleFavoriteCount;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.infrastructure.mybatis.readservice.ArticleFavoritesReadService;
import io.spring.infrastructure.mybatis.readservice.ArticleReadService;
import io.spring.infrastructure.mybatis.readservice.UserRelationshipQueryService;
import java.util.*;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArticleQueryServiceCoverageTest {

  private ArticleReadService articleReadService;
  private UserRelationshipQueryService userRelationshipQueryService;
  private ArticleFavoritesReadService articleFavoritesReadService;
  private ArticleQueryService articleQueryService;

  @BeforeEach
  void setUp() {
    articleReadService = mock(ArticleReadService.class);
    userRelationshipQueryService = mock(UserRelationshipQueryService.class);
    articleFavoritesReadService = mock(ArticleFavoritesReadService.class);
    articleQueryService =
        new ArticleQueryService(
            articleReadService, userRelationshipQueryService, articleFavoritesReadService);
  }

  @Test
  public void should_find_by_id_returns_empty_when_not_found() {
    when(articleReadService.findById("id1")).thenReturn(null);

    Optional<ArticleData> result = articleQueryService.findById("id1", null);

    assertTrue(result.isEmpty());
  }

  @Test
  public void should_find_by_id_without_user() {
    ArticleData articleData = createArticleData("id1");
    when(articleReadService.findById("id1")).thenReturn(articleData);

    Optional<ArticleData> result = articleQueryService.findById("id1", null);

    assertTrue(result.isPresent());
    assertEquals("id1", result.get().getId());
  }

  @Test
  public void should_find_by_id_with_user() {
    ArticleData articleData = createArticleData("id1");
    User user = new User("test@test.com", "testuser", "pass", "", "");
    when(articleReadService.findById("id1")).thenReturn(articleData);
    when(articleFavoritesReadService.isUserFavorite(eq(user.getId()), eq("id1")))
        .thenReturn(true);
    when(articleFavoritesReadService.articleFavoriteCount("id1")).thenReturn(5);
    when(userRelationshipQueryService.isUserFollowing(anyString(), anyString())).thenReturn(false);

    Optional<ArticleData> result = articleQueryService.findById("id1", user);

    assertTrue(result.isPresent());
    assertTrue(result.get().isFavorited());
    assertEquals(5, result.get().getFavoritesCount());
  }

  @Test
  public void should_find_by_slug_returns_empty_when_not_found() {
    when(articleReadService.findBySlug("slug")).thenReturn(null);

    Optional<ArticleData> result = articleQueryService.findBySlug("slug", null);

    assertTrue(result.isEmpty());
  }

  @Test
  public void should_find_by_slug_without_user() {
    ArticleData articleData = createArticleData("id1");
    when(articleReadService.findBySlug("test-slug")).thenReturn(articleData);

    Optional<ArticleData> result = articleQueryService.findBySlug("test-slug", null);

    assertTrue(result.isPresent());
  }

  @Test
  public void should_find_by_slug_with_user() {
    ArticleData articleData = createArticleData("id1");
    User user = new User("test@test.com", "testuser", "pass", "", "");
    when(articleReadService.findBySlug("test-slug")).thenReturn(articleData);
    when(articleFavoritesReadService.isUserFavorite(anyString(), anyString())).thenReturn(false);
    when(articleFavoritesReadService.articleFavoriteCount("id1")).thenReturn(3);
    when(userRelationshipQueryService.isUserFollowing(anyString(), anyString())).thenReturn(true);

    Optional<ArticleData> result = articleQueryService.findBySlug("test-slug", user);

    assertTrue(result.isPresent());
  }

  @Test
  public void should_find_recent_articles_empty() {
    when(articleReadService.queryArticles(any(), any(), any(), any()))
        .thenReturn(Collections.emptyList());
    when(articleReadService.countArticle(any(), any(), any())).thenReturn(0);

    ArticleDataList result =
        articleQueryService.findRecentArticles(null, null, null, new Page(0, 20), null);

    assertNotNull(result);
    assertEquals(0, result.getCount());
    assertTrue(result.getArticleDatas().isEmpty());
  }

  @Test
  public void should_find_recent_articles_with_results() {
    ArticleData articleData = createArticleData("id1");
    when(articleReadService.queryArticles(any(), any(), any(), any()))
        .thenReturn(Arrays.asList("id1"));
    when(articleReadService.countArticle(any(), any(), any())).thenReturn(1);
    when(articleReadService.findArticles(anyList())).thenReturn(Arrays.asList(articleData));
    when(articleFavoritesReadService.articlesFavoriteCount(anyList()))
        .thenReturn(Arrays.asList(new ArticleFavoriteCount("id1", 2)));

    ArticleDataList result =
        articleQueryService.findRecentArticles("java", null, null, new Page(0, 20), null);

    assertNotNull(result);
    assertEquals(1, result.getCount());
    assertEquals(1, result.getArticleDatas().size());
  }

  @Test
  public void should_find_recent_articles_with_current_user() {
    ArticleData articleData = createArticleData("id1");
    User user = new User("test@test.com", "testuser", "pass", "", "");
    when(articleReadService.queryArticles(any(), any(), any(), any()))
        .thenReturn(Arrays.asList("id1"));
    when(articleReadService.countArticle(any(), any(), any())).thenReturn(1);
    when(articleReadService.findArticles(anyList())).thenReturn(Arrays.asList(articleData));
    when(articleFavoritesReadService.articlesFavoriteCount(anyList()))
        .thenReturn(Arrays.asList(new ArticleFavoriteCount("id1", 2)));
    when(articleFavoritesReadService.userFavorites(anyList(), any()))
        .thenReturn(new HashSet<>(Arrays.asList("id1")));
    when(userRelationshipQueryService.followingAuthors(anyString(), anyList()))
        .thenReturn(new HashSet<>());

    ArticleDataList result =
        articleQueryService.findRecentArticles(null, null, null, new Page(0, 20), user);

    assertNotNull(result);
    assertEquals(1, result.getArticleDatas().size());
  }

  @Test
  public void should_find_user_feed_empty_when_no_follows() {
    User user = new User("test@test.com", "testuser", "pass", "", "");
    when(userRelationshipQueryService.followedUsers(user.getId()))
        .thenReturn(Collections.emptyList());

    ArticleDataList result = articleQueryService.findUserFeed(user, new Page(0, 20));

    assertNotNull(result);
    assertEquals(0, result.getCount());
    assertTrue(result.getArticleDatas().isEmpty());
  }

  @Test
  public void should_find_user_feed_with_follows() {
    User user = new User("test@test.com", "testuser", "pass", "", "");
    ArticleData articleData = createArticleData("id1");
    when(userRelationshipQueryService.followedUsers(user.getId()))
        .thenReturn(Arrays.asList("followed-user"));
    when(articleReadService.findArticlesOfAuthors(anyList(), any()))
        .thenReturn(Arrays.asList(articleData));
    when(articleFavoritesReadService.articlesFavoriteCount(anyList()))
        .thenReturn(Arrays.asList(new ArticleFavoriteCount("id1", 1)));
    when(articleFavoritesReadService.userFavorites(anyList(), any()))
        .thenReturn(new HashSet<>());
    when(userRelationshipQueryService.followingAuthors(anyString(), anyList()))
        .thenReturn(new HashSet<>());
    when(articleReadService.countFeedSize(anyList())).thenReturn(1);

    ArticleDataList result = articleQueryService.findUserFeed(user, new Page(0, 20));

    assertNotNull(result);
    assertEquals(1, result.getCount());
  }

  @Test
  public void should_find_recent_articles_with_cursor_empty() {
    CursorPageParameter<DateTime> page =
        new CursorPageParameter<>(null, 20, CursorPager.Direction.NEXT);
    when(articleReadService.findArticlesWithCursor(any(), any(), any(), any()))
        .thenReturn(Collections.emptyList());

    CursorPager<ArticleData> result =
        articleQueryService.findRecentArticlesWithCursor(null, null, null, page, null);

    assertNotNull(result);
    assertTrue(result.getData().isEmpty());
    assertFalse(result.hasNext());
  }

  @Test
  public void should_find_recent_articles_with_cursor_has_extra() {
    CursorPageParameter<DateTime> page =
        new CursorPageParameter<>(null, 1, CursorPager.Direction.NEXT);
    ArticleData a1 = createArticleData("id1");
    ArticleData a2 = createArticleData("id2");
    when(articleReadService.findArticlesWithCursor(any(), any(), any(), any()))
        .thenReturn(new ArrayList<>(Arrays.asList("id1", "id2")));
    when(articleReadService.findArticles(anyList())).thenReturn(new ArrayList<>(Arrays.asList(a1)));
    when(articleFavoritesReadService.articlesFavoriteCount(anyList()))
        .thenReturn(Arrays.asList(new ArticleFavoriteCount("id1", 1)));

    CursorPager<ArticleData> result =
        articleQueryService.findRecentArticlesWithCursor(null, null, null, page, null);

    assertNotNull(result);
    assertTrue(result.hasNext());
  }

  @Test
  public void should_find_recent_articles_with_cursor_prev_direction() {
    CursorPageParameter<DateTime> page =
        new CursorPageParameter<>(null, 20, CursorPager.Direction.PREV);
    ArticleData a1 = createArticleData("id1");
    when(articleReadService.findArticlesWithCursor(any(), any(), any(), any()))
        .thenReturn(new ArrayList<>(Arrays.asList("id1")));
    when(articleReadService.findArticles(anyList())).thenReturn(new ArrayList<>(Arrays.asList(a1)));
    when(articleFavoritesReadService.articlesFavoriteCount(anyList()))
        .thenReturn(Arrays.asList(new ArticleFavoriteCount("id1", 0)));

    CursorPager<ArticleData> result =
        articleQueryService.findRecentArticlesWithCursor(null, null, null, page, null);

    assertNotNull(result);
    assertFalse(result.hasPrevious());
  }

  @Test
  public void should_find_user_feed_with_cursor_empty() {
    User user = new User("test@test.com", "testuser", "pass", "", "");
    CursorPageParameter<DateTime> page =
        new CursorPageParameter<>(null, 20, CursorPager.Direction.NEXT);
    when(userRelationshipQueryService.followedUsers(user.getId()))
        .thenReturn(Collections.emptyList());

    CursorPager<ArticleData> result =
        articleQueryService.findUserFeedWithCursor(user, page);

    assertNotNull(result);
    assertTrue(result.getData().isEmpty());
  }

  @Test
  public void should_find_user_feed_with_cursor_has_results() {
    User user = new User("test@test.com", "testuser", "pass", "", "");
    CursorPageParameter<DateTime> page =
        new CursorPageParameter<>(null, 20, CursorPager.Direction.NEXT);
    ArticleData a1 = createArticleData("id1");
    when(userRelationshipQueryService.followedUsers(user.getId()))
        .thenReturn(Arrays.asList("followed-user"));
    when(articleReadService.findArticlesOfAuthorsWithCursor(anyList(), any()))
        .thenReturn(new ArrayList<>(Arrays.asList(a1)));
    when(articleFavoritesReadService.articlesFavoriteCount(anyList()))
        .thenReturn(Arrays.asList(new ArticleFavoriteCount("id1", 1)));
    when(articleFavoritesReadService.userFavorites(anyList(), any()))
        .thenReturn(new HashSet<>());
    when(userRelationshipQueryService.followingAuthors(anyString(), anyList()))
        .thenReturn(new HashSet<>());

    CursorPager<ArticleData> result =
        articleQueryService.findUserFeedWithCursor(user, page);

    assertNotNull(result);
    assertFalse(result.getData().isEmpty());
  }

  @Test
  public void should_find_user_feed_with_cursor_prev_direction() {
    User user = new User("test@test.com", "testuser", "pass", "", "");
    CursorPageParameter<DateTime> page =
        new CursorPageParameter<>(null, 20, CursorPager.Direction.PREV);
    ArticleData a1 = createArticleData("id1");
    when(userRelationshipQueryService.followedUsers(user.getId()))
        .thenReturn(Arrays.asList("followed-user"));
    when(articleReadService.findArticlesOfAuthorsWithCursor(anyList(), any()))
        .thenReturn(new ArrayList<>(Arrays.asList(a1)));
    when(articleFavoritesReadService.articlesFavoriteCount(anyList()))
        .thenReturn(Arrays.asList(new ArticleFavoriteCount("id1", 0)));
    when(articleFavoritesReadService.userFavorites(anyList(), any()))
        .thenReturn(new HashSet<>());
    when(userRelationshipQueryService.followingAuthors(anyString(), anyList()))
        .thenReturn(new HashSet<>());

    CursorPager<ArticleData> result =
        articleQueryService.findUserFeedWithCursor(user, page);

    assertNotNull(result);
  }

  private ArticleData createArticleData(String id) {
    ProfileData profileData = new ProfileData("author-id", "author", "bio", "img", false);
    return new ArticleData(
        id,
        "test-slug",
        "Test Title",
        "Test Desc",
        "Test Body",
        false,
        0,
        new DateTime(),
        new DateTime(),
        Arrays.asList("tag1"),
        profileData);
  }
}
