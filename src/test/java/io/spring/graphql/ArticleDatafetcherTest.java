package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.DateTimeCursor;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.ArticlesConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class ArticleDatafetcherTest {

  private ArticleQueryService articleQueryService;
  private UserRepository userRepository;
  private ArticleDatafetcher articleDatafetcher;
  private User user;

  @BeforeEach
  public void setUp() {
    articleQueryService = mock(ArticleQueryService.class);
    userRepository = mock(UserRepository.class);
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);

    user = new User("test@test.com", "testuser", "password", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(user, null));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_get_feed_with_first_parameter() {
    DateTime now = new DateTime();
    ArticleData articleData = buildArticleData("1", "test-title", now);
    CursorPager<ArticleData> pager =
        new CursorPager<>(Arrays.asList(articleData), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, null);
    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  public void should_get_feed_with_last_parameter() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(), Direction.PREV, false);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(null, null, 10, null, null);
    assertNotNull(result);
    assertEquals(0, result.getData().getEdges().size());
  }

  @Test
  public void should_throw_when_feed_has_neither_first_nor_last() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getFeed(null, null, null, null, null));
  }

  @Test
  public void should_get_articles_with_first_parameter() {
    DateTime now = new DateTime();
    ArticleData articleData = buildArticleData("1", "test-title", now);
    CursorPager<ArticleData> pager =
        new CursorPager<>(Arrays.asList(articleData), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, null, null, null, null);
    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  public void should_get_articles_with_last_parameter() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(), Direction.PREV, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(null, null, 5, null, null, null, null, null);
    assertNotNull(result);
  }

  @Test
  public void should_throw_when_articles_has_neither_first_nor_last() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getArticles(null, null, null, null, null, null, null, null));
  }

  @Test
  public void should_find_article_by_slug() {
    DateTime now = new DateTime();
    ArticleData articleData = buildArticleData("1", "test-title", now);
    when(articleQueryService.findBySlug(eq("test-title"), any()))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("test-title");
    assertNotNull(result);
    assertEquals("test-title", result.getData().getSlug());
  }

  @Test
  public void should_throw_when_article_not_found_by_slug() {
    when(articleQueryService.findBySlug(eq("not-found"), any())).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class,
        () -> articleDatafetcher.findArticleBySlug("not-found"));
  }

  private ArticleData buildArticleData(String id, String slug, DateTime now) {
    ProfileData profile = new ProfileData("user-id", "testuser", "bio", "image", false);
    return new ArticleData(
        id, slug, "Title", "Desc", "Body", false, 0, now, now, new ArrayList<>(), profile);
  }
}
