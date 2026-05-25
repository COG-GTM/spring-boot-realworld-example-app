package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
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
import java.util.*;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ArticleDatafetcherTest {

  @Mock private ArticleQueryService articleQueryService;
  @Mock private UserRepository userRepository;
  @Mock private DataFetchingEnvironment dfe;

  private ArticleDatafetcher articleDatafetcher;
  private User user;
  private ProfileData profileData;

  @BeforeEach
  void setUp() {
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    profileData =
        new ProfileData(
            user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private DgsDataFetchingEnvironment dgsDfe() {
    return new DgsDataFetchingEnvironment(dfe);
  }

  private void setAuthenticatedUser(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(u, null, AuthorityUtils.NO_AUTHORITIES));
  }

  private void setAnonymousUser() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  private ArticleData createArticleData(String seed) {
    DateTime now = new DateTime();
    return new ArticleData(
        seed + "-id",
        "slug-" + seed,
        "title-" + seed,
        "desc-" + seed,
        "body-" + seed,
        false,
        0,
        now,
        now,
        new ArrayList<>(Arrays.asList("java", "spring")),
        profileData);
  }

  private CursorPager<ArticleData> createCursorPager(
      List<ArticleData> data, Direction direction, boolean hasExtra) {
    return new CursorPager<>(data, direction, hasExtra);
  }

  // --- getFeed tests ---

  @Test
  void getFeed_withFirstParam_returnsArticlesConnection() {
    setAuthenticatedUser(user);
    List<ArticleData> articles = Arrays.asList(createArticleData("1"), createArticleData("2"));
    CursorPager<ArticleData> pager = createCursorPager(articles, Direction.NEXT, true);

    when(articleQueryService.findUserFeedWithCursor(eq(user), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, dgsDfe());

    assertNotNull(result);
    ArticlesConnection conn = result.getData();
    assertEquals(2, conn.getEdges().size());
    assertEquals("slug-1", conn.getEdges().get(0).getNode().getSlug());
    assertEquals("slug-2", conn.getEdges().get(1).getNode().getSlug());
    assertTrue(conn.getPageInfo().isHasNextPage());
    assertFalse(conn.getPageInfo().isHasPreviousPage());
  }

  @Test
  void getFeed_withLastParam_returnsArticlesConnection() {
    setAuthenticatedUser(user);
    List<ArticleData> articles = Arrays.asList(createArticleData("1"));
    CursorPager<ArticleData> pager = createCursorPager(articles, Direction.PREV, false);

    when(articleQueryService.findUserFeedWithCursor(eq(user), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(null, null, 5, null, dgsDfe());

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
    assertFalse(result.getData().getPageInfo().isHasNextPage());
  }

  @Test
  void getFeed_withNoFirstOrLast_throwsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getFeed(null, null, null, null, dgsDfe()));
  }

  @Test
  void getFeed_emptyResult_returnsEmptyConnection() {
    setAnonymousUser();
    CursorPager<ArticleData> pager =
        createCursorPager(Collections.emptyList(), Direction.NEXT, false);

    when(articleQueryService.findUserFeedWithCursor(isNull(), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, dgsDfe());

    assertNotNull(result);
    assertTrue(result.getData().getEdges().isEmpty());
    assertNull(result.getData().getPageInfo().getStartCursor());
    assertNull(result.getData().getPageInfo().getEndCursor());
  }

  @Test
  void getFeed_withAfterCursor_passesToService() {
    setAuthenticatedUser(user);
    DateTime cursorTime = new DateTime();
    String cursorStr = String.valueOf(cursorTime.getMillis());
    CursorPager<ArticleData> pager =
        createCursorPager(Collections.emptyList(), Direction.NEXT, false);

    when(articleQueryService.findUserFeedWithCursor(eq(user), any(CursorPageParameter.class)))
        .thenReturn(pager);

    articleDatafetcher.getFeed(10, cursorStr, null, null, dgsDfe());

    verify(articleQueryService).findUserFeedWithCursor(eq(user), any(CursorPageParameter.class));
  }

  @Test
  void getFeed_setsLocalContext() {
    setAuthenticatedUser(user);
    ArticleData article = createArticleData("ctx");
    CursorPager<ArticleData> pager =
        createCursorPager(Arrays.asList(article), Direction.NEXT, false);

    when(articleQueryService.findUserFeedWithCursor(eq(user), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, dgsDfe());

    @SuppressWarnings("unchecked")
    Map<String, ArticleData> localCtx = (Map<String, ArticleData>) result.getLocalContext();
    assertNotNull(localCtx);
    assertEquals(article, localCtx.get("slug-ctx"));
  }

  // --- userFeed tests ---

  @Test
  void userFeed_withFirstParam_returnsArticlesForProfile() {
    Profile profile = Profile.newBuilder().username("testuser").build();
    when(dfe.getSource()).thenReturn(profile);
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

    List<ArticleData> articles = Arrays.asList(createArticleData("uf1"));
    CursorPager<ArticleData> pager = createCursorPager(articles, Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(5, null, null, null, dgsDfe());

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void userFeed_withLastParam_usesPrevDirection() {
    Profile profile = Profile.newBuilder().username("testuser").build();
    when(dfe.getSource()).thenReturn(profile);
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

    CursorPager<ArticleData> pager =
        createCursorPager(Collections.emptyList(), Direction.PREV, false);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(null, null, 5, null, dgsDfe());

    assertNotNull(result);
    assertTrue(result.getData().getEdges().isEmpty());
  }

  @Test
  void userFeed_userNotFound_throwsResourceNotFound() {
    Profile profile = Profile.newBuilder().username("nonexistent").build();
    when(dfe.getSource()).thenReturn(profile);
    when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleDatafetcher.userFeed(5, null, null, null, dgsDfe()));
  }

  @Test
  void userFeed_noFirstOrLast_throwsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFeed(null, null, null, null, dgsDfe()));
  }

  // --- getArticles tests ---

  @Test
  void getArticles_withFirstParam_returnsArticles() {
    setAuthenticatedUser(user);
    List<ArticleData> articles = Arrays.asList(createArticleData("a1"), createArticleData("a2"));
    CursorPager<ArticleData> pager = createCursorPager(articles, Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(CursorPageParameter.class), eq(user)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, null, null, null, dgsDfe());

    assertNotNull(result);
    assertEquals(2, result.getData().getEdges().size());
  }

  @Test
  void getArticles_withFilterParams_passesToService() {
    setAnonymousUser();
    CursorPager<ArticleData> pager =
        createCursorPager(Collections.emptyList(), Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), eq("author1"), eq("fav1"), any(CursorPageParameter.class), isNull()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(
            10, null, null, null, "author1", "fav1", "java", dgsDfe());

    assertNotNull(result);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(
            eq("java"), eq("author1"), eq("fav1"), any(CursorPageParameter.class), isNull());
  }

  @Test
  void getArticles_withLastParam_usesPrevDirection() {
    setAnonymousUser();
    CursorPager<ArticleData> pager =
        createCursorPager(Collections.emptyList(), Direction.PREV, false);

    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(CursorPageParameter.class), isNull()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(null, null, 5, null, null, null, null, dgsDfe());

    assertNotNull(result);
  }

  @Test
  void getArticles_noFirstOrLast_throwsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            articleDatafetcher.getArticles(null, null, null, null, null, null, null, dgsDfe()));
  }

  // --- getArticle (ArticlePayload) tests ---

  @Test
  void getArticle_returnsArticleFromLocalContext() {
    setAuthenticatedUser(user);
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article(
            "Title", "Desc", "Body", Arrays.asList("java"), user.getId());
    ArticleData articleData = createArticleData("payload");

    when(dfe.getLocalContext()).thenReturn(coreArticle);
    when(articleQueryService.findById(coreArticle.getId(), user))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.getArticle(dfe);

    assertNotNull(result);
    assertEquals("slug-payload", result.getData().getSlug());
  }

  @Test
  void getArticle_notFound_throwsResourceNotFound() {
    setAnonymousUser();
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article(
            "Title", "Desc", "Body", Arrays.asList("java"), user.getId());

    when(dfe.getLocalContext()).thenReturn(coreArticle);
    when(articleQueryService.findById(coreArticle.getId(), null)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleDatafetcher.getArticle(dfe));
  }

  // --- findArticleBySlug tests ---

  @Test
  void findArticleBySlug_returnsArticle() {
    setAuthenticatedUser(user);
    ArticleData articleData = createArticleData("byslug");

    when(articleQueryService.findBySlug("slug-byslug", user))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("slug-byslug");

    assertNotNull(result);
    assertEquals("slug-byslug", result.getData().getSlug());
    assertEquals("title-byslug", result.getData().getTitle());
    assertEquals("body-byslug", result.getData().getBody());
  }

  @Test
  void findArticleBySlug_notFound_throwsResourceNotFound() {
    setAnonymousUser();
    when(articleQueryService.findBySlug("nonexistent", null)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleDatafetcher.findArticleBySlug("nonexistent"));
  }

  @Test
  void findArticleBySlug_setsLocalContext() {
    setAuthenticatedUser(user);
    ArticleData articleData = createArticleData("ctxslug");

    when(articleQueryService.findBySlug("slug-ctxslug", user))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("slug-ctxslug");

    @SuppressWarnings("unchecked")
    Map<String, Object> localCtx = (Map<String, Object>) result.getLocalContext();
    assertEquals(articleData, localCtx.get("slug-ctxslug"));
  }

  // --- getCommentArticle tests ---

  @Test
  void getCommentArticle_returnsArticle() {
    setAnonymousUser();
    CommentData commentData =
        new CommentData(
            "c1", "body", "article-id-1", new DateTime(), new DateTime(), profileData);
    ArticleData articleData = createArticleData("comment-art");

    when(dfe.getLocalContext()).thenReturn(commentData);
    when(articleQueryService.findById("article-id-1", null))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.getCommentArticle(dfe);

    assertNotNull(result);
    assertEquals("slug-comment-art", result.getData().getSlug());
  }

  @Test
  void getCommentArticle_articleNotFound_throwsResourceNotFound() {
    setAnonymousUser();
    CommentData commentData =
        new CommentData(
            "c1", "body", "nonexistent-id", new DateTime(), new DateTime(), profileData);

    when(dfe.getLocalContext()).thenReturn(commentData);
    when(articleQueryService.findById("nonexistent-id", null)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleDatafetcher.getCommentArticle(dfe));
  }

  // --- userFavorites tests ---

  @Test
  void userFavorites_withFirstParam_returnsArticles() {
    setAuthenticatedUser(user);
    Profile profile = Profile.newBuilder().username("testuser").build();
    when(dfe.getSource()).thenReturn(profile);

    List<ArticleData> articles = Arrays.asList(createArticleData("fav1"));
    CursorPager<ArticleData> pager = createCursorPager(articles, Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("testuser"), any(CursorPageParameter.class), eq(user)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(5, null, null, null, dgsDfe());

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void userFavorites_noFirstOrLast_throwsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFavorites(null, null, null, null, dgsDfe()));
  }

  // --- userArticles tests ---

  @Test
  void userArticles_withFirstParam_returnsArticles() {
    setAuthenticatedUser(user);
    Profile profile = Profile.newBuilder().username("testuser").build();
    when(dfe.getSource()).thenReturn(profile);

    List<ArticleData> articles = Arrays.asList(createArticleData("ua1"));
    CursorPager<ArticleData> pager = createCursorPager(articles, Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("testuser"), isNull(), any(CursorPageParameter.class), eq(user)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(5, null, null, null, dgsDfe());

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void userArticles_noFirstOrLast_throwsIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userArticles(null, null, null, null, dgsDfe()));
  }

  // --- Article result mapping tests ---

  @Test
  void buildArticleResult_mapsAllFields() {
    setAnonymousUser();
    ArticleData articleData = createArticleData("map");

    when(articleQueryService.findBySlug("slug-map", null)).thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("slug-map");

    Article article = result.getData();
    assertEquals("slug-map", article.getSlug());
    assertEquals("title-map", article.getTitle());
    assertEquals("desc-map", article.getDescription());
    assertEquals("body-map", article.getBody());
    assertFalse(article.getFavorited());
    assertEquals(0, article.getFavoritesCount());
    assertNotNull(article.getCreatedAt());
    assertNotNull(article.getUpdatedAt());
    assertEquals(Arrays.asList("java", "spring"), article.getTagList());
  }

  // --- Pagination with pageInfo tests ---

  @Test
  void getFeed_paginationPageInfo_hasNext() {
    setAuthenticatedUser(user);
    List<ArticleData> articles = Arrays.asList(createArticleData("p1"));
    CursorPager<ArticleData> pager = createCursorPager(articles, Direction.NEXT, true);

    when(articleQueryService.findUserFeedWithCursor(eq(user), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(1, null, null, null, dgsDfe());

    assertTrue(result.getData().getPageInfo().isHasNextPage());
    assertFalse(result.getData().getPageInfo().isHasPreviousPage());
    assertNotNull(result.getData().getPageInfo().getStartCursor());
    assertNotNull(result.getData().getPageInfo().getEndCursor());
  }

  @Test
  void getFeed_cursorValues_areStringifiedTimestamps() {
    setAuthenticatedUser(user);
    ArticleData article = createArticleData("cursor");
    CursorPager<ArticleData> pager =
        createCursorPager(Arrays.asList(article), Direction.NEXT, false);

    when(articleQueryService.findUserFeedWithCursor(eq(user), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, dgsDfe());

    String edgeCursor = result.getData().getEdges().get(0).getCursor();
    assertDoesNotThrow(() -> Long.parseLong(edgeCursor));
  }
}
