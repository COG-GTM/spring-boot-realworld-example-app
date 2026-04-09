package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.ArticlesConnection;
import io.spring.graphql.types.Profile;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ArticleDatafetcherTest {

  @Mock private ArticleQueryService articleQueryService;
  @Mock private UserRepository userRepository;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private ArticleDatafetcher articleDatafetcher;
  private User user;
  private ArticleData articleData;

  @BeforeEach
  void setUp() {
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    user = new User("test@test.com", "testuser", "password", "bio", "image");

    DateTime now = new DateTime();
    articleData =
        new ArticleData(
            "article-id",
            "test-article",
            "Test Article",
            "description",
            "body",
            false,
            0,
            now,
            now,
            Arrays.asList("java", "spring"),
            new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAuthentication(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(user, null));
  }

  private void setAnonymousAuthentication() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new org.springframework.security.authentication.AnonymousAuthenticationToken(
                "key",
                "anonymous",
                Collections.singletonList(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_ANONYMOUS"))));
  }

  private DgsDataFetchingEnvironment createDgsEnv() {
    return new DgsDataFetchingEnvironment(dataFetchingEnvironment);
  }

  private CursorPager<ArticleData> buildCursorPager(
      java.util.List<ArticleData> data, Direction direction, boolean hasExtra) {
    return new CursorPager<>(data, direction, hasExtra);
  }

  // ---- getFeed tests ----

  @Test
  void getFeed_withFirstParam_shouldReturnArticlesConnection() {
    setAuthentication(user);
    CursorPager<ArticleData> cursorPager =
        buildCursorPager(Arrays.asList(articleData), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any())).thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, createDgsEnv());

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(1, result.getData().getEdges().size());
    assertEquals("test-article", result.getData().getEdges().get(0).getNode().getSlug());
    verify(articleQueryService).findUserFeedWithCursor(eq(user), any());
  }

  @Test
  void getFeed_withLastParam_shouldReturnArticlesConnectionWithPrevDirection() {
    setAuthentication(user);
    CursorPager<ArticleData> cursorPager =
        buildCursorPager(Arrays.asList(articleData), Direction.PREV, false);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any())).thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(null, null, 10, null, createDgsEnv());

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(1, result.getData().getEdges().size());
    verify(articleQueryService).findUserFeedWithCursor(eq(user), any());
  }

  @Test
  void getFeed_withoutFirstAndLast_shouldThrowIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getFeed(null, null, null, null, createDgsEnv()));
  }

  @Test
  void getFeed_withEmptyResult_shouldReturnEmptyConnection() {
    setAuthentication(user);
    CursorPager<ArticleData> cursorPager =
        buildCursorPager(Collections.emptyList(), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any())).thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, createDgsEnv());

    assertNotNull(result);
    assertTrue(result.getData().getEdges().isEmpty());
  }

  @Test
  void getFeed_withNoAuthentication_shouldPassNullUser() {
    setAnonymousAuthentication();
    CursorPager<ArticleData> cursorPager =
        buildCursorPager(Collections.emptyList(), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(eq(null), any())).thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, createDgsEnv());

    assertNotNull(result);
    verify(articleQueryService).findUserFeedWithCursor(eq(null), any());
  }

  @Test
  void getFeed_withHasNext_shouldSetPageInfoCorrectly() {
    setAuthentication(user);
    CursorPager<ArticleData> cursorPager =
        buildCursorPager(Arrays.asList(articleData), Direction.NEXT, true);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any())).thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, createDgsEnv());

    assertNotNull(result.getData().getPageInfo());
    assertTrue(result.getData().getPageInfo().isHasNextPage());
    assertFalse(result.getData().getPageInfo().isHasPreviousPage());
  }

  @Test
  void getFeed_localContext_shouldContainArticleDataMap() {
    setAuthentication(user);
    CursorPager<ArticleData> cursorPager =
        buildCursorPager(Arrays.asList(articleData), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any())).thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, createDgsEnv());

    assertNotNull(result.getLocalContext());
    @SuppressWarnings("unchecked")
    Map<String, ArticleData> context = (Map<String, ArticleData>) result.getLocalContext();
    assertTrue(context.containsKey("test-article"));
    assertEquals(articleData, context.get("test-article"));
  }

  // ---- userFeed tests ----

  @Test
  void userFeed_withFirstParam_shouldReturnArticlesConnection() {
    Profile profile = Profile.newBuilder().username("testuser").build();
    when(dataFetchingEnvironment.getSource()).thenReturn(profile);
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    CursorPager<ArticleData> cursorPager =
        buildCursorPager(Arrays.asList(articleData), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any())).thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(10, null, null, null, createDgsEnv());

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
    verify(userRepository).findByUsername("testuser");
  }

  @Test
  void userFeed_withLastParam_shouldReturnArticlesConnectionWithPrevDirection() {
    Profile profile = Profile.newBuilder().username("testuser").build();
    when(dataFetchingEnvironment.getSource()).thenReturn(profile);
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    CursorPager<ArticleData> cursorPager =
        buildCursorPager(Arrays.asList(articleData), Direction.PREV, false);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any())).thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(null, null, 10, null, createDgsEnv());

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void userFeed_withoutFirstAndLast_shouldThrowIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFeed(null, null, null, null, createDgsEnv()));
  }

  @Test
  void userFeed_withNonExistentUser_shouldThrowResourceNotFoundException() {
    Profile profile = Profile.newBuilder().username("nonexistent").build();
    when(dataFetchingEnvironment.getSource()).thenReturn(profile);
    when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleDatafetcher.userFeed(10, null, null, null, createDgsEnv()));
  }

  // ---- userFavorites tests ----

  @Test
  void userFavorites_withFirstParam_shouldReturnArticlesConnection() {
    setAuthentication(user);
    Profile profile = Profile.newBuilder().username("testuser").build();
    when(dataFetchingEnvironment.getSource()).thenReturn(profile);
    CursorPager<ArticleData> cursorPager =
        buildCursorPager(Arrays.asList(articleData), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq(null), eq(null), eq("testuser"), any(), eq(user)))
        .thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(10, null, null, null, createDgsEnv());

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void userFavorites_withLastParam_shouldReturnArticlesConnectionWithPrevDirection() {
    setAuthentication(user);
    Profile profile = Profile.newBuilder().username("testuser").build();
    when(dataFetchingEnvironment.getSource()).thenReturn(profile);
    CursorPager<ArticleData> cursorPager =
        buildCursorPager(Arrays.asList(articleData), Direction.PREV, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq(null), eq(null), eq("testuser"), any(), eq(user)))
        .thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(null, null, 10, null, createDgsEnv());

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void userFavorites_withoutFirstAndLast_shouldThrowIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFavorites(null, null, null, null, createDgsEnv()));
  }

  // ---- userArticles tests ----

  @Test
  void userArticles_withFirstParam_shouldReturnArticlesConnection() {
    setAuthentication(user);
    Profile profile = Profile.newBuilder().username("testuser").build();
    when(dataFetchingEnvironment.getSource()).thenReturn(profile);
    CursorPager<ArticleData> cursorPager =
        buildCursorPager(Arrays.asList(articleData), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq(null), eq("testuser"), eq(null), any(), eq(user)))
        .thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(10, null, null, null, createDgsEnv());

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void userArticles_withLastParam_shouldReturnArticlesConnectionWithPrevDirection() {
    setAuthentication(user);
    Profile profile = Profile.newBuilder().username("testuser").build();
    when(dataFetchingEnvironment.getSource()).thenReturn(profile);
    CursorPager<ArticleData> cursorPager =
        buildCursorPager(Arrays.asList(articleData), Direction.PREV, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq(null), eq("testuser"), eq(null), any(), eq(user)))
        .thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(null, null, 10, null, createDgsEnv());

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void userArticles_withoutFirstAndLast_shouldThrowIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userArticles(null, null, null, null, createDgsEnv()));
  }

  // ---- getArticles tests ----

  @Test
  void getArticles_withFirstParam_shouldReturnArticlesConnection() {
    setAuthentication(user);
    CursorPager<ArticleData> cursorPager =
        buildCursorPager(Arrays.asList(articleData), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq(null), eq(null), eq(null), any(), eq(user)))
        .thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, null, null, null, createDgsEnv());

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void getArticles_withLastParam_shouldReturnArticlesConnectionWithPrevDirection() {
    setAuthentication(user);
    CursorPager<ArticleData> cursorPager =
        buildCursorPager(Arrays.asList(articleData), Direction.PREV, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq(null), eq(null), eq(null), any(), eq(user)))
        .thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(null, null, 10, null, null, null, null, createDgsEnv());

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void getArticles_withoutFirstAndLast_shouldThrowIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getArticles(null, null, null, null, null, null, null, createDgsEnv()));
  }

  @Test
  void getArticles_withFilterParams_shouldPassFiltersToQueryService() {
    setAuthentication(user);
    CursorPager<ArticleData> cursorPager =
        buildCursorPager(Arrays.asList(articleData), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), eq("testuser"), eq("favUser"), any(), eq(user)))
        .thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, "testuser", "favUser", "java", createDgsEnv());

    assertNotNull(result);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(eq("java"), eq("testuser"), eq("favUser"), any(), eq(user));
  }

  // ---- getArticle tests ----

  @Test
  void getArticle_shouldReturnArticleFromLocalContext() {
    setAuthentication(user);
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article(
            "Test Article",
            "desc",
            "body",
            Arrays.asList("java"),
            user.getId());
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(coreArticle);
    when(articleQueryService.findById(eq(coreArticle.getId()), eq(user)))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.getArticle(dataFetchingEnvironment);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals("body", result.getData().getBody());
  }

  @Test
  void getArticle_withNoArticleFound_shouldThrowResourceNotFoundException() {
    setAuthentication(user);
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article(
            "Test Article",
            "desc",
            "body",
            Arrays.asList("java"),
            user.getId());
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(coreArticle);
    when(articleQueryService.findById(eq(coreArticle.getId()), eq(user)))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleDatafetcher.getArticle(dataFetchingEnvironment));
  }

  @Test
  void getArticle_localContext_shouldContainArticleDataMap() {
    setAuthentication(user);
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article(
            "Test Article",
            "desc",
            "body",
            Arrays.asList("java"),
            user.getId());
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(coreArticle);
    when(articleQueryService.findById(eq(coreArticle.getId()), eq(user)))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.getArticle(dataFetchingEnvironment);

    @SuppressWarnings("unchecked")
    Map<String, Object> context = (Map<String, Object>) result.getLocalContext();
    assertTrue(context.containsKey(articleData.getSlug()));
  }

  // ---- getCommentArticle tests ----

  @Test
  void getCommentArticle_shouldReturnArticleForComment() {
    setAuthentication(user);
    DateTime now = new DateTime();
    CommentData commentData =
        new CommentData(
            "comment-id",
            "comment body",
            "article-id",
            now,
            now,
            new ProfileData(
                user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(commentData);
    when(articleQueryService.findById(eq("article-id"), eq(user)))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result =
        articleDatafetcher.getCommentArticle(dataFetchingEnvironment);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals("body", result.getData().getBody());
  }

  @Test
  void getCommentArticle_withNoArticleFound_shouldThrowResourceNotFoundException() {
    setAuthentication(user);
    DateTime now = new DateTime();
    CommentData commentData =
        new CommentData(
            "comment-id",
            "comment body",
            "article-id",
            now,
            now,
            new ProfileData(
                user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(commentData);
    when(articleQueryService.findById(eq("article-id"), eq(user))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleDatafetcher.getCommentArticle(dataFetchingEnvironment));
  }

  @Test
  void getCommentArticle_localContext_shouldContainArticleDataMap() {
    setAuthentication(user);
    DateTime now = new DateTime();
    CommentData commentData =
        new CommentData(
            "comment-id",
            "comment body",
            "article-id",
            now,
            now,
            new ProfileData(
                user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(commentData);
    when(articleQueryService.findById(eq("article-id"), eq(user)))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result =
        articleDatafetcher.getCommentArticle(dataFetchingEnvironment);

    @SuppressWarnings("unchecked")
    Map<String, Object> context = (Map<String, Object>) result.getLocalContext();
    assertTrue(context.containsKey(articleData.getSlug()));
  }

  // ---- findArticleBySlug tests ----

  @Test
  void findArticleBySlug_shouldReturnArticle() {
    setAuthentication(user);
    when(articleQueryService.findBySlug(eq("test-article"), eq(user)))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("test-article");

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals("Test Article", result.getData().getTitle());
    assertEquals("description", result.getData().getDescription());
    assertEquals("body", result.getData().getBody());
    assertEquals("test-article", result.getData().getSlug());
    assertEquals(Arrays.asList("java", "spring"), result.getData().getTagList());
    assertFalse(result.getData().getFavorited());
    assertEquals(0, result.getData().getFavoritesCount());
  }

  @Test
  void findArticleBySlug_withNonExistentSlug_shouldThrowResourceNotFoundException() {
    setAuthentication(user);
    when(articleQueryService.findBySlug(eq("non-existent"), eq(user)))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleDatafetcher.findArticleBySlug("non-existent"));
  }

  @Test
  void findArticleBySlug_localContext_shouldContainArticleDataMap() {
    setAuthentication(user);
    when(articleQueryService.findBySlug(eq("test-article"), eq(user)))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("test-article");

    @SuppressWarnings("unchecked")
    Map<String, Object> context = (Map<String, Object>) result.getLocalContext();
    assertTrue(context.containsKey("test-article"));
    assertEquals(articleData, context.get("test-article"));
  }

  @Test
  void findArticleBySlug_withNoAuthentication_shouldPassNullUser() {
    setAnonymousAuthentication();
    when(articleQueryService.findBySlug(eq("test-article"), eq(null)))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("test-article");

    assertNotNull(result);
    verify(articleQueryService).findBySlug(eq("test-article"), eq(null));
  }

  // ---- buildArticleResult coverage ----

  @Test
  void getFeed_shouldBuildArticleResultWithAllFields() {
    setAuthentication(user);
    CursorPager<ArticleData> cursorPager =
        buildCursorPager(Arrays.asList(articleData), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any())).thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, createDgsEnv());

    Article article = result.getData().getEdges().get(0).getNode();
    assertEquals("body", article.getBody());
    assertEquals("description", article.getDescription());
    assertEquals("Test Article", article.getTitle());
    assertEquals("test-article", article.getSlug());
    assertEquals(Arrays.asList("java", "spring"), article.getTagList());
    assertFalse(article.getFavorited());
    assertEquals(0, article.getFavoritesCount());
    assertNotNull(article.getCreatedAt());
    assertNotNull(article.getUpdatedAt());
  }

  // ---- Edge cursor coverage ----

  @Test
  void getFeed_edgeCursor_shouldMatchArticleDataCursor() {
    setAuthentication(user);
    CursorPager<ArticleData> cursorPager =
        buildCursorPager(Arrays.asList(articleData), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any())).thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, createDgsEnv());

    String cursor = result.getData().getEdges().get(0).getCursor();
    assertEquals(articleData.getCursor().toString(), cursor);
  }

  // ---- Multiple articles coverage ----

  @Test
  void getArticles_withMultipleArticles_shouldReturnAllInConnection() {
    setAuthentication(user);
    DateTime now = new DateTime();
    ArticleData articleData2 =
        new ArticleData(
            "article-id-2",
            "second-article",
            "Second Article",
            "desc2",
            "body2",
            true,
            5,
            now,
            now,
            Arrays.asList("python"),
            new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
    CursorPager<ArticleData> cursorPager =
        buildCursorPager(Arrays.asList(articleData, articleData2), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq(null), eq(null), eq(null), any(), eq(user)))
        .thenReturn(cursorPager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, null, null, null, createDgsEnv());

    assertEquals(2, result.getData().getEdges().size());
    assertEquals("test-article", result.getData().getEdges().get(0).getNode().getSlug());
    assertEquals("second-article", result.getData().getEdges().get(1).getNode().getSlug());
  }
}
