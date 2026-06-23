package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.TestHelper;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.ArticlesConnection;
import io.spring.graphql.types.Profile;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArticleDatafetcherTest {

  private ArticleQueryService articleQueryService;
  private UserRepository userRepository;
  private ArticleDatafetcher datafetcher;
  private User user;

  @BeforeEach
  void setUp() {
    articleQueryService = mock(ArticleQueryService.class);
    userRepository = mock(UserRepository.class);
    datafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    user = new User("user@test.com", "user", "123", "bio", "image");
  }

  @AfterEach
  void tearDown() {
    GraphQLTestSecurity.clear();
  }

  private CursorPager<ArticleData> pagerWith(ArticleData... data) {
    return new CursorPager<>(List.of(data), Direction.NEXT, false);
  }

  private DgsDataFetchingEnvironment dgsEnvWithProfile(String username) {
    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    Profile profile = Profile.newBuilder().username(username).build();
    doReturn(profile).when(delegate).getSource();
    return new DgsDataFetchingEnvironment(delegate);
  }

  @Test
  void getFeed_with_first_returns_connection_and_local_context() {
    GraphQLTestSecurity.login(user);
    ArticleData a = TestHelper.articleDataFixture("1", user);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pagerWith(a));

    DataFetcherResult<ArticlesConnection> result = datafetcher.getFeed(10, null, null, null, null);

    assertEquals(1, result.getData().getEdges().size());
    assertEquals(a.getSlug(), result.getData().getEdges().get(0).getNode().getSlug());
    assertNotNull(result.getData().getPageInfo());
    Map<String, ArticleData> ctx = (Map<String, ArticleData>) result.getLocalContext();
    assertTrue(ctx.containsKey(a.getSlug()));
  }

  @Test
  void getFeed_with_last_uses_prev_direction() {
    GraphQLTestSecurity.anonymous();
    ArticleData a = TestHelper.articleDataFixture("2", user);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pagerWith(a));

    DataFetcherResult<ArticlesConnection> result = datafetcher.getFeed(null, null, 10, "100", null);

    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void getFeed_without_first_and_last_throws() {
    GraphQLTestSecurity.anonymous();
    assertThrows(
        IllegalArgumentException.class, () -> datafetcher.getFeed(null, null, null, null, null));
  }

  @Test
  void userFeed_resolves_profile_user_and_returns_connection() {
    GraphQLTestSecurity.anonymous();
    DgsDataFetchingEnvironment dfe = dgsEnvWithProfile(user.getUsername());
    when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    ArticleData a = TestHelper.articleDataFixture("3", user);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any())).thenReturn(pagerWith(a));

    DataFetcherResult<ArticlesConnection> result = datafetcher.userFeed(5, null, null, null, dfe);

    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void userFeed_with_last_uses_prev_direction() {
    GraphQLTestSecurity.anonymous();
    DgsDataFetchingEnvironment dfe = dgsEnvWithProfile(user.getUsername());
    when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    when(articleQueryService.findUserFeedWithCursor(eq(user), any())).thenReturn(pagerWith());

    DataFetcherResult<ArticlesConnection> result = datafetcher.userFeed(null, null, 5, "100", dfe);

    assertTrue(result.getData().getEdges().isEmpty());
  }

  @Test
  void userFeed_without_first_and_last_throws() {
    DgsDataFetchingEnvironment dfe = dgsEnvWithProfile("ignored");
    assertThrows(
        IllegalArgumentException.class, () -> datafetcher.userFeed(null, null, null, null, dfe));
  }

  @Test
  void userFeed_with_unknown_profile_user_throws() {
    GraphQLTestSecurity.anonymous();
    DgsDataFetchingEnvironment dfe = dgsEnvWithProfile("ghost");
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> datafetcher.userFeed(5, null, null, null, dfe));
  }

  @Test
  void userFavorites_returns_connection() {
    GraphQLTestSecurity.login(user);
    DgsDataFetchingEnvironment dfe = dgsEnvWithProfile(user.getUsername());
    ArticleData a = TestHelper.articleDataFixture("4", user);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pagerWith(a));

    DataFetcherResult<ArticlesConnection> first =
        datafetcher.userFavorites(5, null, null, null, dfe);
    DataFetcherResult<ArticlesConnection> last =
        datafetcher.userFavorites(null, null, 5, "100", dfe);

    assertEquals(1, first.getData().getEdges().size());
    assertEquals(1, last.getData().getEdges().size());
  }

  @Test
  void userFavorites_without_first_and_last_throws() {
    DgsDataFetchingEnvironment dfe = dgsEnvWithProfile("ignored");
    assertThrows(
        IllegalArgumentException.class,
        () -> datafetcher.userFavorites(null, null, null, null, dfe));
  }

  @Test
  void userArticles_returns_connection() {
    GraphQLTestSecurity.login(user);
    DgsDataFetchingEnvironment dfe = dgsEnvWithProfile(user.getUsername());
    ArticleData a = TestHelper.articleDataFixture("5", user);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pagerWith(a));

    DataFetcherResult<ArticlesConnection> first =
        datafetcher.userArticles(5, null, null, null, dfe);
    DataFetcherResult<ArticlesConnection> last =
        datafetcher.userArticles(null, null, 5, "100", dfe);

    assertEquals(1, first.getData().getEdges().size());
    assertEquals(1, last.getData().getEdges().size());
  }

  @Test
  void userArticles_without_first_and_last_throws() {
    DgsDataFetchingEnvironment dfe = dgsEnvWithProfile("ignored");
    assertThrows(
        IllegalArgumentException.class,
        () -> datafetcher.userArticles(null, null, null, null, dfe));
  }

  @Test
  void getArticles_with_first_returns_connection() {
    GraphQLTestSecurity.login(user);
    ArticleData a = TestHelper.articleDataFixture("6", user);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pagerWith(a));

    DataFetcherResult<ArticlesConnection> result =
        datafetcher.getArticles(10, null, null, null, "author", "fav", "tag", null);

    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void getArticles_with_last_and_empty_pager_has_null_cursors() {
    GraphQLTestSecurity.anonymous();
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pagerWith());

    DataFetcherResult<ArticlesConnection> result =
        datafetcher.getArticles(null, null, 10, "100", null, null, null, null);

    assertTrue(result.getData().getEdges().isEmpty());
    assertNull(result.getData().getPageInfo().getStartCursor());
    assertNull(result.getData().getPageInfo().getEndCursor());
  }

  @Test
  void getArticles_without_first_and_last_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> datafetcher.getArticles(null, null, null, null, null, null, null, null));
  }

  @Test
  void getArticle_from_local_context_returns_article() {
    GraphQLTestSecurity.login(user);
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "title", "desc", "body", Collections.singletonList("java"), user.getId());
    doReturn(article).when(dfe).getLocalContext();
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);
    when(articleQueryService.findById(eq(article.getId()), any()))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<io.spring.graphql.types.Article> result = datafetcher.getArticle(dfe);

    assertEquals(articleData.getSlug(), result.getData().getSlug());
    assertEquals(articleData.getTitle(), result.getData().getTitle());
    Map<String, ArticleData> ctx = (Map<String, ArticleData>) result.getLocalContext();
    assertTrue(ctx.containsKey(articleData.getSlug()));
  }

  @Test
  void getArticle_not_found_throws() {
    GraphQLTestSecurity.anonymous();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "title", "desc", "body", Collections.singletonList("java"), user.getId());
    doReturn(article).when(dfe).getLocalContext();
    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> datafetcher.getArticle(dfe));
  }

  @Test
  void getCommentArticle_resolves_article_from_comment() {
    GraphQLTestSecurity.login(user);
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    CommentData comment =
        new CommentData("cid", "body", "article-id", new DateTime(), new DateTime(), null);
    doReturn(comment).when(dfe).getLocalContext();
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "title", "desc", "body", Collections.singletonList("java"), user.getId());
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);
    when(articleQueryService.findById(eq("article-id"), any()))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<io.spring.graphql.types.Article> result = datafetcher.getCommentArticle(dfe);

    assertEquals(articleData.getSlug(), result.getData().getSlug());
  }

  @Test
  void getCommentArticle_not_found_throws() {
    GraphQLTestSecurity.anonymous();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    CommentData comment =
        new CommentData("cid", "body", "missing", new DateTime(), new DateTime(), null);
    doReturn(comment).when(dfe).getLocalContext();
    when(articleQueryService.findById(eq("missing"), any())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> datafetcher.getCommentArticle(dfe));
  }

  @Test
  void findArticleBySlug_returns_article() {
    GraphQLTestSecurity.login(user);
    ArticleData a = TestHelper.articleDataFixture("7", user);
    when(articleQueryService.findBySlug(eq(a.getSlug()), any())).thenReturn(Optional.of(a));

    DataFetcherResult<io.spring.graphql.types.Article> result =
        datafetcher.findArticleBySlug(a.getSlug());

    assertEquals(a.getSlug(), result.getData().getSlug());
    assertEquals(a.getBody(), result.getData().getBody());
  }

  @Test
  void findArticleBySlug_not_found_throws() {
    GraphQLTestSecurity.anonymous();
    when(articleQueryService.findBySlug(eq("nope"), any())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> datafetcher.findArticleBySlug("nope"));
  }
}
