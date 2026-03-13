package io.spring.graphql;

import static java.util.Arrays.asList;
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
import java.util.ArrayList;
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

  private ArticleDatafetcher articleDatafetcher;

  private User user;
  private ArticleData articleData;

  @BeforeEach
  void setUp() {
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    user = new User("john@jacob.com", "johnjacob", "123", "", "https://example.com/avatar.jpg");
    articleData = createSampleArticleData();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAuthenticatedUser(User u) {
    TestingAuthenticationToken auth = new TestingAuthenticationToken(u, null);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  private DgsDataFetchingEnvironment createDfe() {
    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    return new DgsDataFetchingEnvironment(delegate);
  }

  private DgsDataFetchingEnvironment createDfeWithSource(Object source) {
    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    when(delegate.getSource()).thenReturn(source);
    return new DgsDataFetchingEnvironment(delegate);
  }

  private ArticleData createSampleArticleData() {
    return new ArticleData(
        "article-id-1",
        "test-article",
        "Test Article",
        "A test description",
        "Article body content",
        false,
        0,
        new DateTime(),
        new DateTime(),
        asList("java", "spring"),
        new ProfileData("user-id", "johnjacob", "", "https://example.com/avatar.jpg", false));
  }

  // ========== getFeed tests ==========

  @Test
  void getFeed_withFirstParam_returnsArticlesConnection() {
    setAuthenticatedUser(user);
    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(any(User.class), any())).thenReturn(pager);

    DgsDataFetchingEnvironment dfe = createDfe();
    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(1, result.getData().getEdges().size());
    assertEquals("test-article", result.getData().getEdges().get(0).getNode().getSlug());
  }

  @Test
  void getFeed_withLastParam_returnsArticlesConnection() {
    setAuthenticatedUser(user);
    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.PREV, false);
    when(articleQueryService.findUserFeedWithCursor(any(User.class), any())).thenReturn(pager);

    DgsDataFetchingEnvironment dfe = createDfe();
    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(null, null, 10, null, dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void getFeed_withNullFirstAndLast_throwsIllegalArgumentException() {
    DgsDataFetchingEnvironment dfe = createDfe();
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getFeed(null, null, null, null, dfe));
  }

  @Test
  void getFeed_emptyResult_returnsEmptyConnection() {
    setAuthenticatedUser(user);
    CursorPager<ArticleData> pager = new CursorPager<>(new ArrayList<>(), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pager);

    DgsDataFetchingEnvironment dfe = createDfe();
    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, dfe);

    assertNotNull(result);
    assertTrue(result.getData().getEdges().isEmpty());
  }

  @Test
  void getFeed_withAfterCursor_passesToService() {
    setAuthenticatedUser(user);
    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.NEXT, true);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pager);

    DgsDataFetchingEnvironment dfe = createDfe();
    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, "1234567890", null, null, dfe);

    assertNotNull(result);
    assertTrue(result.getData().getPageInfo().isHasNextPage());
  }

  @Test
  void getFeed_withBeforeCursor_passesToService() {
    setAuthenticatedUser(user);
    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.PREV, true);
    when(articleQueryService.findUserFeedWithCursor(any(), any())).thenReturn(pager);

    DgsDataFetchingEnvironment dfe = createDfe();
    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(null, null, 10, "1234567890", dfe);

    assertNotNull(result);
    assertTrue(result.getData().getPageInfo().isHasPreviousPage());
  }

  @Test
  void getFeed_withNoAuth_passesNullUser() {
    SecurityContextHolder.clearContext();
    TestingAuthenticationToken anonAuth = new TestingAuthenticationToken(null, null);
    SecurityContextHolder.getContext().setAuthentication(anonAuth);

    CursorPager<ArticleData> pager = new CursorPager<>(new ArrayList<>(), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(isNull(), any())).thenReturn(pager);

    DgsDataFetchingEnvironment dfe = createDfe();
    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, dfe);

    assertNotNull(result);
    verify(articleQueryService).findUserFeedWithCursor(isNull(), any());
  }

  // ========== userFeed tests ==========

  @Test
  void userFeed_withFirstParam_returnsArticlesConnection() {
    Profile profile = Profile.newBuilder().username("johnjacob").build();
    DgsDataFetchingEnvironment dfe = createDfeWithSource(profile);
    when(userRepository.findByUsername("johnjacob")).thenReturn(Optional.of(user));

    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(any(User.class), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(10, null, null, null, dfe);

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void userFeed_withLastParam_returnsArticlesConnection() {
    Profile profile = Profile.newBuilder().username("johnjacob").build();
    DgsDataFetchingEnvironment dfe = createDfeWithSource(profile);
    when(userRepository.findByUsername("johnjacob")).thenReturn(Optional.of(user));

    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.PREV, false);
    when(articleQueryService.findUserFeedWithCursor(any(User.class), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(null, null, 10, null, dfe);

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void userFeed_withNullFirstAndLast_throwsIllegalArgumentException() {
    DgsDataFetchingEnvironment dfe = createDfe();
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFeed(null, null, null, null, dfe));
  }

  @Test
  void userFeed_withNonexistentUser_throwsResourceNotFoundException() {
    Profile profile = Profile.newBuilder().username("nonexistent").build();
    DgsDataFetchingEnvironment dfe = createDfeWithSource(profile);
    when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleDatafetcher.userFeed(10, null, null, null, dfe));
  }

  // ========== userFavorites tests ==========

  @Test
  void userFavorites_withFirstParam_returnsArticlesConnection() {
    setAuthenticatedUser(user);
    Profile profile = Profile.newBuilder().username("johnjacob").build();
    DgsDataFetchingEnvironment dfe = createDfeWithSource(profile);

    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("johnjacob"), any(), any()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(10, null, null, null, dfe);

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void userFavorites_withLastParam_returnsArticlesConnection() {
    setAuthenticatedUser(user);
    Profile profile = Profile.newBuilder().username("johnjacob").build();
    DgsDataFetchingEnvironment dfe = createDfeWithSource(profile);

    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.PREV, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("johnjacob"), any(), any()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(null, null, 10, null, dfe);

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void userFavorites_withNullFirstAndLast_throwsIllegalArgumentException() {
    DgsDataFetchingEnvironment dfe = createDfe();
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFavorites(null, null, null, null, dfe));
  }

  // ========== userArticles tests ==========

  @Test
  void userArticles_withFirstParam_returnsArticlesConnection() {
    setAuthenticatedUser(user);
    Profile profile = Profile.newBuilder().username("johnjacob").build();
    DgsDataFetchingEnvironment dfe = createDfeWithSource(profile);

    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("johnjacob"), isNull(), any(), any()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(10, null, null, null, dfe);

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void userArticles_withLastParam_returnsArticlesConnection() {
    setAuthenticatedUser(user);
    Profile profile = Profile.newBuilder().username("johnjacob").build();
    DgsDataFetchingEnvironment dfe = createDfeWithSource(profile);

    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.PREV, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("johnjacob"), isNull(), any(), any()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(null, null, 10, null, dfe);

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void userArticles_withNullFirstAndLast_throwsIllegalArgumentException() {
    DgsDataFetchingEnvironment dfe = createDfe();
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userArticles(null, null, null, null, dfe));
  }

  // ========== getArticles tests ==========

  @Test
  void getArticles_withFirstParam_returnsArticlesConnection() {
    setAuthenticatedUser(user);
    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);

    DgsDataFetchingEnvironment dfe = createDfe();
    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, null, null, null, dfe);

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void getArticles_withLastParam_returnsArticlesConnection() {
    setAuthenticatedUser(user);
    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.PREV, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);

    DgsDataFetchingEnvironment dfe = createDfe();
    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(null, null, 10, null, null, null, null, dfe);

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void getArticles_withNullFirstAndLast_throwsIllegalArgumentException() {
    DgsDataFetchingEnvironment dfe = createDfe();
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getArticles(null, null, null, null, null, null, null, dfe));
  }

  @Test
  void getArticles_withFilterParams_passesFiltersToService() {
    setAuthenticatedUser(user);
    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), eq("johnjacob"), eq("janedoe"), any(), any()))
        .thenReturn(pager);

    DgsDataFetchingEnvironment dfe = createDfe();
    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, "johnjacob", "janedoe", "java", dfe);

    assertNotNull(result);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(eq("java"), eq("johnjacob"), eq("janedoe"), any(), any());
  }

  @Test
  void getArticles_withNoAuth_passesNullUser() {
    SecurityContextHolder.clearContext();
    TestingAuthenticationToken anonAuth = new TestingAuthenticationToken(null, null);
    SecurityContextHolder.getContext().setAuthentication(anonAuth);

    CursorPager<ArticleData> pager = new CursorPager<>(new ArrayList<>(), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), isNull()))
        .thenReturn(pager);

    DgsDataFetchingEnvironment dfe = createDfe();
    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, null, null, null, dfe);

    assertNotNull(result);
    verify(articleQueryService).findRecentArticlesWithCursor(any(), any(), any(), any(), isNull());
  }

  // ========== getArticle tests ==========

  @Test
  void getArticle_returnsArticle() {
    setAuthenticatedUser(user);
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article(
            "Test Article", "desc", "body", asList("java"), user.getId());

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(coreArticle);
    when(articleQueryService.findById(eq(coreArticle.getId()), any()))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.getArticle(dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals("test-article", result.getData().getSlug());
    assertEquals("Test Article", result.getData().getTitle());
  }

  @Test
  void getArticle_articleNotFound_throwsResourceNotFoundException() {
    setAuthenticatedUser(user);
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article(
            "Test Article", "desc", "body", asList("java"), user.getId());

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(coreArticle);
    when(articleQueryService.findById(eq(coreArticle.getId()), any())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleDatafetcher.getArticle(dfe));
  }

  // ========== getCommentArticle tests ==========

  @Test
  void getCommentArticle_returnsArticle() {
    setAuthenticatedUser(user);
    CommentData commentData =
        new CommentData(
            "comment-1", "Nice article!", "article-id-1", new DateTime(), new DateTime(), null);

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(commentData);
    when(articleQueryService.findById(eq("article-id-1"), any()))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.getCommentArticle(dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals("test-article", result.getData().getSlug());
  }

  @Test
  void getCommentArticle_articleNotFound_throwsResourceNotFoundException() {
    setAuthenticatedUser(user);
    CommentData commentData =
        new CommentData(
            "comment-1", "Nice article!", "article-id-1", new DateTime(), new DateTime(), null);

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(commentData);
    when(articleQueryService.findById(eq("article-id-1"), any())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleDatafetcher.getCommentArticle(dfe));
  }

  // ========== findArticleBySlug tests ==========

  @Test
  void findArticleBySlug_returnsArticle() {
    setAuthenticatedUser(user);
    when(articleQueryService.findBySlug(eq("test-article"), any()))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("test-article");

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals("test-article", result.getData().getSlug());
    assertEquals("Test Article", result.getData().getTitle());
    assertEquals("A test description", result.getData().getDescription());
    assertEquals("Article body content", result.getData().getBody());
    assertFalse(result.getData().getFavorited());
    assertEquals(0, result.getData().getFavoritesCount());
    assertEquals(asList("java", "spring"), result.getData().getTagList());
  }

  @Test
  void findArticleBySlug_notFound_throwsResourceNotFoundException() {
    setAuthenticatedUser(user);
    when(articleQueryService.findBySlug(eq("nonexistent"), any())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleDatafetcher.findArticleBySlug("nonexistent"));
  }

  @Test
  void findArticleBySlug_withNoAuth_passesNullUser() {
    SecurityContextHolder.clearContext();
    TestingAuthenticationToken anonAuth = new TestingAuthenticationToken(null, null);
    SecurityContextHolder.getContext().setAuthentication(anonAuth);

    when(articleQueryService.findBySlug(eq("test-article"), isNull()))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("test-article");

    assertNotNull(result);
    verify(articleQueryService).findBySlug(eq("test-article"), isNull());
  }

  // ========== pageInfo / localContext tests ==========

  @Test
  void getFeed_setsLocalContextWithArticleDataMap() {
    setAuthenticatedUser(user);
    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(any(User.class), any())).thenReturn(pager);

    DgsDataFetchingEnvironment dfe = createDfe();
    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, dfe);

    assertNotNull(result.getLocalContext());
    @SuppressWarnings("unchecked")
    java.util.Map<String, ArticleData> contextMap =
        (java.util.Map<String, ArticleData>) result.getLocalContext();
    assertTrue(contextMap.containsKey("test-article"));
    assertEquals(articleData, contextMap.get("test-article"));
  }

  @Test
  void getArticles_pageInfo_hasNextPageWhenHasExtra() {
    setAuthenticatedUser(user);
    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.NEXT, true);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);

    DgsDataFetchingEnvironment dfe = createDfe();
    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, null, null, null, dfe);

    assertTrue(result.getData().getPageInfo().isHasNextPage());
    assertFalse(result.getData().getPageInfo().isHasPreviousPage());
  }

  @Test
  void getArticles_pageInfo_hasPreviousPageWhenHasExtra() {
    setAuthenticatedUser(user);
    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.PREV, true);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);

    DgsDataFetchingEnvironment dfe = createDfe();
    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(null, null, 10, null, null, null, null, dfe);

    assertFalse(result.getData().getPageInfo().isHasNextPage());
    assertTrue(result.getData().getPageInfo().isHasPreviousPage());
  }

  @Test
  void getArticles_emptyResult_pageInfoCursorsAreNull() {
    setAuthenticatedUser(user);
    CursorPager<ArticleData> pager = new CursorPager<>(new ArrayList<>(), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);

    DgsDataFetchingEnvironment dfe = createDfe();
    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, null, null, null, dfe);

    assertNull(result.getData().getPageInfo().getStartCursor());
    assertNull(result.getData().getPageInfo().getEndCursor());
  }

  @Test
  void findArticleBySlug_setsLocalContextMap() {
    setAuthenticatedUser(user);
    when(articleQueryService.findBySlug(eq("test-article"), any()))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("test-article");

    assertNotNull(result.getLocalContext());
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> contextMap =
        (java.util.Map<String, Object>) result.getLocalContext();
    assertTrue(contextMap.containsKey("test-article"));
  }

  @Test
  void getArticle_setsLocalContextMap() {
    setAuthenticatedUser(user);
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article(
            "Test Article", "desc", "body", asList("java"), user.getId());

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(coreArticle);
    when(articleQueryService.findById(eq(coreArticle.getId()), any()))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.getArticle(dfe);

    assertNotNull(result.getLocalContext());
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> contextMap =
        (java.util.Map<String, Object>) result.getLocalContext();
    assertTrue(contextMap.containsKey("test-article"));
  }

  @Test
  void getArticles_articleEdge_hasCursor() {
    setAuthenticatedUser(user);
    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);

    DgsDataFetchingEnvironment dfe = createDfe();
    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, null, null, null, dfe);

    assertNotNull(result.getData().getEdges().get(0).getCursor());
    assertFalse(result.getData().getEdges().get(0).getCursor().isEmpty());
  }

  @Test
  void userFavorites_withAfterCursor_usesNextDirection() {
    setAuthenticatedUser(user);
    Profile profile = Profile.newBuilder().username("johnjacob").build();
    DgsDataFetchingEnvironment dfe = createDfeWithSource(profile);

    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.NEXT, true);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("johnjacob"), any(), any()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(10, "1234567890", null, null, dfe);

    assertNotNull(result);
    assertTrue(result.getData().getPageInfo().isHasNextPage());
  }

  @Test
  void userArticles_withBeforeCursor_usesPrevDirection() {
    setAuthenticatedUser(user);
    Profile profile = Profile.newBuilder().username("johnjacob").build();
    DgsDataFetchingEnvironment dfe = createDfeWithSource(profile);

    CursorPager<ArticleData> pager =
        new CursorPager<>(new ArrayList<>(asList(articleData)), Direction.PREV, true);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("johnjacob"), isNull(), any(), any()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(null, null, 10, "1234567890", dfe);

    assertNotNull(result);
    assertTrue(result.getData().getPageInfo().isHasPreviousPage());
  }

  @Test
  void getCommentArticle_setsLocalContextMap() {
    setAuthenticatedUser(user);
    CommentData commentData =
        new CommentData(
            "comment-1", "Nice article!", "article-id-1", new DateTime(), new DateTime(), null);

    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(commentData);
    when(articleQueryService.findById(eq("article-id-1"), any()))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.getCommentArticle(dfe);

    assertNotNull(result.getLocalContext());
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> contextMap =
        (java.util.Map<String, Object>) result.getLocalContext();
    assertTrue(contextMap.containsKey("test-article"));
  }
}
