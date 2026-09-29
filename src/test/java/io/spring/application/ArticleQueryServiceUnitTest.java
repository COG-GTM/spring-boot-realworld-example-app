package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ArticleDataList;
import io.spring.application.data.ArticleFavoriteCount;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.infrastructure.mybatis.readservice.ArticleFavoritesReadService;
import io.spring.infrastructure.mybatis.readservice.ArticleReadService;
import io.spring.infrastructure.mybatis.readservice.UserRelationshipQueryService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArticleQueryServiceUnitTest {
  private ArticleReadService articleReadService;
  private UserRelationshipQueryService userRelationshipQueryService;
  private ArticleFavoritesReadService articleFavoritesReadService;
  private ArticleQueryService service;
  private User user;

  @BeforeEach
  public void setUp() {
    articleReadService = mock(ArticleReadService.class);
    userRelationshipQueryService = mock(UserRelationshipQueryService.class);
    articleFavoritesReadService = mock(ArticleFavoritesReadService.class);
    service =
        new ArticleQueryService(
            articleReadService, userRelationshipQueryService, articleFavoritesReadService);
    user = new User("a@b.com", "user", "pass", "", "");
  }

  private ArticleData article(String id, String authorId, DateTime time) {
    return new ArticleData(
        id,
        "slug-" + id,
        "title",
        "desc",
        "body",
        false,
        0,
        time,
        time,
        new ArrayList<>(),
        new ProfileData(authorId, "author" + authorId, "", "", false));
  }

  @Test
  public void should_return_empty_when_article_not_found_by_id_or_slug() {
    assertFalse(service.findById("missing", user).isPresent());
    assertFalse(service.findBySlug("missing", user).isPresent());
  }

  @Test
  public void should_find_by_id_and_slug_without_user_skipping_extra_info() {
    ArticleData data = article("1", "a1", new DateTime());
    when(articleReadService.findById("1")).thenReturn(data);
    when(articleReadService.findBySlug("slug-1")).thenReturn(data);

    assertTrue(service.findById("1", null).isPresent());
    assertTrue(service.findBySlug("slug-1", null).isPresent());
    verify(articleFavoritesReadService, never()).isUserFavorite(any(), any());
  }

  @Test
  public void should_fill_extra_info_when_finding_by_slug_with_user() {
    ArticleData data = article("1", "a1", new DateTime());
    when(articleReadService.findBySlug("slug-1")).thenReturn(data);
    when(articleFavoritesReadService.isUserFavorite(user.getId(), "1")).thenReturn(true);
    when(articleFavoritesReadService.articleFavoriteCount("1")).thenReturn(3);
    when(userRelationshipQueryService.isUserFollowing(user.getId(), "a1")).thenReturn(true);

    Optional<ArticleData> result = service.findBySlug("slug-1", user);

    assertTrue(result.isPresent());
    assertTrue(result.get().isFavorited());
    assertEquals(3, result.get().getFavoritesCount());
    assertTrue(result.get().getProfileData().isFollowing());
  }

  @Test
  public void should_return_empty_cursor_pager_when_no_articles() {
    CursorPageParameter<DateTime> page = new CursorPageParameter<>(null, 2, Direction.NEXT);
    when(articleReadService.findArticlesWithCursor(null, null, null, page))
        .thenReturn(new ArrayList<>());

    CursorPager<ArticleData> pager =
        service.findRecentArticlesWithCursor(null, null, null, page, user);

    assertTrue(pager.getData().isEmpty());
    assertFalse(pager.hasNext());
  }

  @Test
  public void should_trim_extra_article_and_mark_next_page() {
    CursorPageParameter<DateTime> page = new CursorPageParameter<>(null, 2, Direction.NEXT);
    DateTime now = new DateTime();
    when(articleReadService.findArticlesWithCursor(null, null, null, page))
        .thenReturn(new ArrayList<>(Arrays.asList("1", "2", "3")));
    when(articleReadService.findArticles(Arrays.asList("1", "2")))
        .thenReturn(Arrays.asList(article("1", "a1", now), article("2", "a2", now)));
    when(articleFavoritesReadService.articlesFavoriteCount(anyList()))
        .thenReturn(
            Arrays.asList(new ArticleFavoriteCount("1", 5), new ArticleFavoriteCount("2", 0)));
    when(articleFavoritesReadService.userFavorites(anyList(), eq(user)))
        .thenReturn(new HashSet<>(Collections.singletonList("2")));
    when(userRelationshipQueryService.followingAuthors(eq(user.getId()), anyList()))
        .thenReturn(new HashSet<>(Collections.singletonList("a1")));

    CursorPager<ArticleData> pager =
        service.findRecentArticlesWithCursor(null, null, null, page, user);

    assertEquals(2, pager.getData().size());
    assertTrue(pager.hasNext());
    assertFalse(pager.hasPrevious());
    ArticleData first = pager.getData().get(0);
    ArticleData second = pager.getData().get(1);
    assertEquals(5, first.getFavoritesCount());
    assertFalse(first.isFavorited());
    assertTrue(first.getProfileData().isFollowing());
    assertTrue(second.isFavorited());
    assertFalse(second.getProfileData().isFollowing());
  }

  @Test
  public void should_reverse_ids_for_previous_page_without_user() {
    CursorPageParameter<DateTime> page = new CursorPageParameter<>(null, 5, Direction.PREV);
    DateTime now = new DateTime();
    when(articleReadService.findArticlesWithCursor("tag", null, null, page))
        .thenReturn(new ArrayList<>(Arrays.asList("1", "2")));
    when(articleReadService.findArticles(Arrays.asList("2", "1")))
        .thenReturn(Arrays.asList(article("2", "a", now), article("1", "a", now)));
    when(articleFavoritesReadService.articlesFavoriteCount(anyList()))
        .thenReturn(
            Arrays.asList(new ArticleFavoriteCount("1", 0), new ArticleFavoriteCount("2", 0)));

    CursorPager<ArticleData> pager =
        service.findRecentArticlesWithCursor("tag", null, null, page, null);

    assertEquals("2", pager.getData().get(0).getId());
    assertFalse(pager.hasPrevious());
    verify(articleFavoritesReadService, never()).userFavorites(any(), any());
  }

  @Test
  public void should_return_empty_feed_with_cursor_when_following_nobody() {
    CursorPageParameter<DateTime> page = new CursorPageParameter<>(null, 2, Direction.NEXT);
    when(userRelationshipQueryService.followedUsers(user.getId())).thenReturn(new ArrayList<>());

    CursorPager<ArticleData> pager = service.findUserFeedWithCursor(user, page);

    assertTrue(pager.getData().isEmpty());
  }

  @Test
  public void should_page_user_feed_with_cursor_in_both_directions() {
    DateTime now = new DateTime();
    List<String> followed = Collections.singletonList("a1");
    when(userRelationshipQueryService.followedUsers(user.getId())).thenReturn(followed);
    when(articleFavoritesReadService.userFavorites(anyList(), eq(user)))
        .thenReturn(new HashSet<>());
    when(articleFavoritesReadService.articlesFavoriteCount(anyList()))
        .thenReturn(
            Arrays.asList(new ArticleFavoriteCount("1", 1), new ArticleFavoriteCount("2", 2)));
    when(userRelationshipQueryService.followingAuthors(eq(user.getId()), anyList()))
        .thenReturn(new HashSet<>(followed));

    CursorPageParameter<DateTime> next = new CursorPageParameter<>(null, 1, Direction.NEXT);
    when(articleReadService.findArticlesOfAuthorsWithCursor(followed, next))
        .thenReturn(
            new ArrayList<>(Arrays.asList(article("1", "a1", now), article("2", "a1", now))));
    CursorPager<ArticleData> nextPager = service.findUserFeedWithCursor(user, next);
    assertEquals(1, nextPager.getData().size());
    assertTrue(nextPager.hasNext());

    CursorPageParameter<DateTime> prev = new CursorPageParameter<>(null, 5, Direction.PREV);
    when(articleReadService.findArticlesOfAuthorsWithCursor(followed, prev))
        .thenReturn(
            new ArrayList<>(Arrays.asList(article("1", "a1", now), article("2", "a1", now))));
    CursorPager<ArticleData> prevPager = service.findUserFeedWithCursor(user, prev);
    assertEquals("2", prevPager.getData().get(0).getId());
    assertFalse(prevPager.hasPrevious());
  }

  @Test
  public void should_return_empty_article_list_but_keep_count() {
    Page page = new Page(0, 10);
    when(articleReadService.queryArticles(null, null, null, page)).thenReturn(new ArrayList<>());
    when(articleReadService.countArticle(null, null, null)).thenReturn(7);

    ArticleDataList result = service.findRecentArticles(null, null, null, page, null);

    assertTrue(result.getArticleDatas().isEmpty());
    assertEquals(7, result.getCount());
  }

  @Test
  public void should_return_empty_feed_when_following_nobody() {
    when(userRelationshipQueryService.followedUsers(user.getId())).thenReturn(new ArrayList<>());

    ArticleDataList result = service.findUserFeed(user, new Page(0, 10));

    assertTrue(result.getArticleDatas().isEmpty());
    assertEquals(0, result.getCount());
  }
}
