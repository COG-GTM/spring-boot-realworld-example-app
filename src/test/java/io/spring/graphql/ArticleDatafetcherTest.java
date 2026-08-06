package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.ArticlesConnection;
import io.spring.graphql.types.Profile;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class ArticleDatafetcherTest {

  private ArticleQueryService articleQueryService;
  private UserRepository userRepository;
  private ArticleDatafetcher articleDatafetcher;
  private DataFetchingEnvironment inner;
  private DgsDataFetchingEnvironment dfe;

  private final User user = new User("me@example.com", "me", "123", "", "");

  @BeforeEach
  void setUp() {
    articleQueryService = mock(ArticleQueryService.class);
    userRepository = mock(UserRepository.class);
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    inner = mock(DataFetchingEnvironment.class);
    dfe = new DgsDataFetchingEnvironment(inner);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private ArticleData articleData() {
    ArticleData data = new ArticleData();
    data.setId("article-id");
    data.setSlug("a-slug");
    data.setTitle("title");
    data.setDescription("desc");
    data.setBody("body");
    data.setTagList(Arrays.asList("java"));
    data.setCreatedAt(new DateTime());
    data.setUpdatedAt(new DateTime());
    return data;
  }

  private CursorPager<ArticleData> pagerWithOne() {
    return new CursorPager<>(Collections.singletonList(articleData()), Direction.NEXT, false);
  }

  @Test
  void should_throw_when_feed_first_and_last_null() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getFeed(null, null, null, null, dfe));
  }

  @Test
  void should_get_feed() {
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pagerWithOne());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, dfe);

    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void should_get_user_feed() {
    when(inner.getSource()).thenReturn(Profile.newBuilder().username("target").build());
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(user));
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pagerWithOne());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(10, null, null, null, dfe);

    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void should_throw_when_user_feed_target_missing() {
    when(inner.getSource()).thenReturn(Profile.newBuilder().username("target").build());
    when(userRepository.findByUsername("target")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleDatafetcher.userFeed(10, null, null, null, dfe));
  }

  @Test
  void should_get_user_favorites() {
    when(inner.getSource()).thenReturn(Profile.newBuilder().username("target").build());
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pagerWithOne());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(10, null, null, null, dfe);

    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void should_get_user_articles() {
    when(inner.getSource()).thenReturn(Profile.newBuilder().username("target").build());
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pagerWithOne());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(null, null, 10, null, dfe);

    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void should_get_articles() {
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pagerWithOne());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, null, null, null, dfe);

    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void should_get_article_from_local_context() {
    DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article("title", "desc", "body", Arrays.asList("java"), "uid");
    when(env.getLocalContext()).thenReturn(coreArticle);
    when(articleQueryService.findById(eq(coreArticle.getId()), any()))
        .thenReturn(Optional.of(articleData()));

    DataFetcherResult<Article> result = articleDatafetcher.getArticle(env);

    assertEquals("a-slug", result.getData().getSlug());
  }

  @Test
  void should_throw_when_article_not_found() {
    DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article("title", "desc", "body", Arrays.asList("java"), "uid");
    when(env.getLocalContext()).thenReturn(coreArticle);
    when(articleQueryService.findById(eq(coreArticle.getId()), any())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleDatafetcher.getArticle(env));
  }

  @Test
  void should_get_comment_article() {
    DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
    CommentData comment = new CommentData("cid", "body", "article-id", null, null, null);
    when(env.getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById(eq("article-id"), any()))
        .thenReturn(Optional.of(articleData()));

    DataFetcherResult<Article> result = articleDatafetcher.getCommentArticle(env);

    assertEquals("a-slug", result.getData().getSlug());
  }

  @Test
  void should_find_article_by_slug() {
    when(articleQueryService.findBySlug(eq("a-slug"), any()))
        .thenReturn(Optional.of(articleData()));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("a-slug");

    assertEquals("a-slug", result.getData().getSlug());
  }

  @Test
  void should_throw_when_find_article_by_slug_missing() {
    when(articleQueryService.findBySlug(eq("missing"), any())).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class, () -> articleDatafetcher.findArticleBySlug("missing"));
  }
}
