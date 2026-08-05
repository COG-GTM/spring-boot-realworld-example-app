package io.spring.graphql;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class ArticleDatafetcherTest {

  private static final DateTime BASE_TIME = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);

  private ArticleQueryService articleQueryService;
  private UserRepository userRepository;
  private ArticleDatafetcher articleDatafetcher;

  private User currentUser;
  private int fixtureCount;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    fixtureCount = 0;
    articleQueryService = mock(ArticleQueryService.class);
    userRepository = mock(UserRepository.class);
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    currentUser = new User("john@example.com", "john", "123", "bio", "avatar");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setCurrentUser(User user) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private void setAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  // DgsDataFetchingEnvironment is final and cannot be mocked with the inline mock maker
  // unavailable; wrap a mocked DataFetchingEnvironment which it delegates to.
  private DgsDataFetchingEnvironment dgsEnv(DataFetchingEnvironment inner) {
    return new DgsDataFetchingEnvironment(inner);
  }

  private DgsDataFetchingEnvironment dgsEnvWithSource(Object source) {
    DataFetchingEnvironment inner = mock(DataFetchingEnvironment.class);
    when(inner.<Object>getSource()).thenReturn(source);
    return new DgsDataFetchingEnvironment(inner);
  }

  // Each fixture gets its own timestamps so that cursors, which derive from updatedAt, differ.
  private ArticleData articleData(String seed) {
    DateTime createdAt = BASE_TIME.plusMinutes(fixtureCount);
    DateTime updatedAt = createdAt.plusDays(1);
    fixtureCount++;
    return new ArticleData(
        seed + "-id",
        seed + "-slug",
        "title " + seed,
        "desc " + seed,
        "body " + seed,
        false,
        3,
        createdAt,
        updatedAt,
        Arrays.asList("java", seed),
        new ProfileData("author-id", "author", "author bio", "author image", false));
  }

  private CursorPager<ArticleData> pagerWith(List<ArticleData> data, Direction direction) {
    return new CursorPager<>(data, direction, false);
  }

  private CursorPager<ArticleData> pagerWith(
      List<ArticleData> data, Direction direction, boolean hasExtra) {
    return new CursorPager<>(data, direction, hasExtra);
  }

  @SuppressWarnings("unchecked")
  private CursorPageParameter<DateTime> captureFeedPageParameter(User expectedUser) {
    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService).findUserFeedWithCursor(eq(expectedUser), captor.capture());
    return captor.getValue();
  }

  @SuppressWarnings("unchecked")
  private CursorPageParameter<DateTime> captureRecentPageParameter(
      String withTag, String authoredBy, String favoritedBy, User expectedUser) {
    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(
            eq(withTag), eq(authoredBy), eq(favoritedBy), captor.capture(), eq(expectedUser));
    return captor.getValue();
  }

  // ----- getFeed -----

  @Test
  void getFeed_should_throw_when_first_and_last_both_null() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            articleDatafetcher.getFeed(
                null, null, null, null, dgsEnv(mock(DataFetchingEnvironment.class))));
  }

  @Test
  void getFeed_forward_returns_connection_with_edges() {
    setCurrentUser(currentUser);
    ArticleData a1 = articleData("a1");
    ArticleData a2 = articleData("a2");
    CursorPager<ArticleData> pager = pagerWith(Arrays.asList(a1, a2), Direction.NEXT);
    when(articleQueryService.findUserFeedWithCursor(eq(currentUser), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(
            10, null, null, null, dgsEnv(mock(DataFetchingEnvironment.class)));

    CursorPageParameter<DateTime> page = captureFeedPageParameter(currentUser);
    assertThat(page.getDirection(), is(Direction.NEXT));
    assertThat(page.getCursor(), is(nullValue()));
    assertThat(page.getLimit(), is(10));

    ArticlesConnection connection = result.getData();
    assertThat(connection.getEdges(), hasSize(2));
    assertThat(connection.getEdges().get(0).getNode().getTitle(), is("title a1"));
    assertThat(connection.getEdges().get(0).getCursor(), is(a1.getCursor().toString()));
    assertThat(connection.getEdges().get(1).getNode().getTitle(), is("title a2"));
    assertThat(connection.getEdges().get(1).getCursor(), is(a2.getCursor().toString()));
    assertThat(a2.getCursor().toString(), is(not(a1.getCursor().toString())));
    assertThat(connection.getPageInfo().getStartCursor().getValue(), is(a1.getCursor().toString()));
    assertThat(connection.getPageInfo().getEndCursor().getValue(), is(a2.getCursor().toString()));
    assertThat(connection.getPageInfo().isHasNextPage(), is(false));

    @SuppressWarnings("unchecked")
    Map<String, ArticleData> localContext = (Map<String, ArticleData>) result.getLocalContext();
    assertThat(localContext.keySet(), containsInAnyOrder("a1-slug", "a2-slug"));
  }

  @Test
  void getFeed_forward_parses_after_cursor() {
    setCurrentUser(currentUser);
    CursorPager<ArticleData> pager =
        pagerWith(Collections.singletonList(articleData("a1")), Direction.NEXT, true);
    when(articleQueryService.findUserFeedWithCursor(eq(currentUser), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(
            7, "300", null, null, dgsEnv(mock(DataFetchingEnvironment.class)));

    CursorPageParameter<DateTime> page = captureFeedPageParameter(currentUser);
    assertThat(page.getDirection(), is(Direction.NEXT));
    assertThat(page.getLimit(), is(7));
    assertThat(page.getCursor().getMillis(), is(300L));
    assertThat(result.getData().getPageInfo().isHasNextPage(), is(true));
    assertThat(result.getData().getPageInfo().isHasPreviousPage(), is(false));
  }

  @Test
  void getFeed_backward_uses_prev_direction_and_anonymous_user() {
    setAnonymous();
    ArticleData a1 = articleData("a1");
    CursorPager<ArticleData> pager = pagerWith(Collections.singletonList(a1), Direction.PREV, true);
    when(articleQueryService.findUserFeedWithCursor(eq(null), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(
            null, null, 5, "100", dgsEnv(mock(DataFetchingEnvironment.class)));

    CursorPageParameter<DateTime> page = captureFeedPageParameter(null);
    assertThat(page.getDirection(), is(Direction.PREV));
    assertThat(page.getLimit(), is(5));
    assertThat(page.getCursor().getMillis(), is(100L));

    assertThat(result.getData().getEdges(), hasSize(1));
    assertThat(result.getData().getPageInfo().isHasPreviousPage(), is(true));
    assertThat(result.getData().getPageInfo().isHasNextPage(), is(false));
  }

  // ----- userFeed -----

  @Test
  void userFeed_should_throw_when_first_and_last_both_null() {
    DgsDataFetchingEnvironment dfe = dgsEnv(mock(DataFetchingEnvironment.class));
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFeed(null, null, null, null, dfe));
  }

  @Test
  void userFeed_should_throw_not_found_when_profile_user_missing() {
    Profile profile = Profile.newBuilder().username("ghost").build();
    DgsDataFetchingEnvironment dfe = dgsEnvWithSource(profile);
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleDatafetcher.userFeed(10, null, null, null, dfe));
  }

  @Test
  void userFeed_forward_returns_connection() {
    Profile profile = Profile.newBuilder().username("target").build();
    DgsDataFetchingEnvironment dfe = dgsEnvWithSource(profile);
    User target = new User("t@example.com", "target", "123", "", "");
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    CursorPager<ArticleData> pager =
        pagerWith(Collections.singletonList(articleData("f1")), Direction.NEXT);
    when(articleQueryService.findUserFeedWithCursor(eq(target), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(10, null, null, null, dfe);

    assertThat(result.getData().getEdges(), hasSize(1));
    assertThat(result.getData().getEdges().get(0).getNode().getSlug(), is("f1-slug"));
  }

  @Test
  void userFeed_backward_uses_prev_direction() {
    Profile profile = Profile.newBuilder().username("target").build();
    DgsDataFetchingEnvironment dfe = dgsEnvWithSource(profile);
    User target = new User("t@example.com", "target", "123", "", "");
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    CursorPager<ArticleData> pager =
        pagerWith(Collections.singletonList(articleData("f1")), Direction.PREV, true);
    when(articleQueryService.findUserFeedWithCursor(eq(target), any())).thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(null, null, 5, "200", dfe);

    CursorPageParameter<DateTime> page = captureFeedPageParameter(target);
    assertThat(page.getDirection(), is(Direction.PREV));
    assertThat(page.getLimit(), is(5));
    assertThat(page.getCursor().getMillis(), is(200L));
    assertThat(result.getData().getPageInfo().isHasPreviousPage(), is(true));
  }

  // ----- userFavorites -----

  @Test
  void userFavorites_forward_passes_username_as_favoritedBy() {
    setCurrentUser(currentUser);
    Profile profile = Profile.newBuilder().username("bob").build();
    DgsDataFetchingEnvironment dfe = dgsEnvWithSource(profile);
    CursorPager<ArticleData> pager =
        pagerWith(Collections.singletonList(articleData("fav")), Direction.NEXT);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq(null), eq(null), eq("bob"), any(), eq(currentUser)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(10, null, null, null, dfe);

    assertThat(result.getData().getEdges(), hasSize(1));
    assertThat(result.getData().getEdges().get(0).getNode().getSlug(), is("fav-slug"));
  }

  @Test
  void userFavorites_backward_passes_username_as_favoritedBy() {
    setCurrentUser(currentUser);
    Profile profile = Profile.newBuilder().username("bob").build();
    DgsDataFetchingEnvironment dfe = dgsEnvWithSource(profile);
    CursorPager<ArticleData> pager =
        pagerWith(Collections.singletonList(articleData("fav")), Direction.PREV, true);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq(null), eq(null), eq("bob"), any(), eq(currentUser)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(null, null, 5, "200", dfe);

    CursorPageParameter<DateTime> page = captureRecentPageParameter(null, null, "bob", currentUser);
    assertThat(page.getDirection(), is(Direction.PREV));
    assertThat(page.getLimit(), is(5));
    assertThat(page.getCursor().getMillis(), is(200L));
    assertThat(result.getData().getPageInfo().isHasPreviousPage(), is(true));
  }

  @Test
  void userFavorites_should_throw_when_first_and_last_both_null() {
    DgsDataFetchingEnvironment dfe = dgsEnv(mock(DataFetchingEnvironment.class));
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFavorites(null, null, null, null, dfe));
  }

  // ----- userArticles -----

  @Test
  void userArticles_forward_passes_username_as_author() {
    setCurrentUser(currentUser);
    Profile profile = Profile.newBuilder().username("bob").build();
    DgsDataFetchingEnvironment dfe = dgsEnvWithSource(profile);
    CursorPager<ArticleData> pager =
        pagerWith(Collections.singletonList(articleData("art")), Direction.NEXT);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq(null), eq("bob"), eq(null), any(), eq(currentUser)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(10, null, null, null, dfe);

    assertThat(result.getData().getEdges(), hasSize(1));
  }

  @Test
  void userArticles_backward_passes_username_as_author() {
    setCurrentUser(currentUser);
    Profile profile = Profile.newBuilder().username("bob").build();
    DgsDataFetchingEnvironment dfe = dgsEnvWithSource(profile);
    CursorPager<ArticleData> pager =
        pagerWith(Collections.singletonList(articleData("art")), Direction.PREV, true);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq(null), eq("bob"), eq(null), any(), eq(currentUser)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(null, null, 5, "200", dfe);

    CursorPageParameter<DateTime> page = captureRecentPageParameter(null, "bob", null, currentUser);
    assertThat(page.getDirection(), is(Direction.PREV));
    assertThat(page.getLimit(), is(5));
    assertThat(page.getCursor().getMillis(), is(200L));
    assertThat(result.getData().getPageInfo().isHasPreviousPage(), is(true));
  }

  @Test
  void userArticles_should_throw_when_first_and_last_both_null() {
    DgsDataFetchingEnvironment dfe = dgsEnv(mock(DataFetchingEnvironment.class));
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userArticles(null, null, null, null, dfe));
  }

  // ----- getArticles -----

  @Test
  void getArticles_forward_passes_all_filters() {
    setCurrentUser(currentUser);
    DgsDataFetchingEnvironment dfe = dgsEnv(mock(DataFetchingEnvironment.class));
    CursorPager<ArticleData> pager =
        pagerWith(Arrays.asList(articleData("g1"), articleData("g2")), Direction.NEXT);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("tag"), eq("author"), eq("fav"), any(), eq(currentUser)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, "300", null, null, "author", "fav", "tag", dfe);

    CursorPageParameter<DateTime> page =
        captureRecentPageParameter("tag", "author", "fav", currentUser);
    assertThat(page.getDirection(), is(Direction.NEXT));
    assertThat(page.getLimit(), is(10));
    assertThat(page.getCursor().getMillis(), is(300L));

    List<String> slugs = new ArrayList<>();
    result.getData().getEdges().forEach(e -> slugs.add(e.getNode().getSlug()));
    assertThat(slugs, contains("g1-slug", "g2-slug"));
  }

  @Test
  void getArticles_backward_passes_all_filters() {
    setCurrentUser(currentUser);
    DgsDataFetchingEnvironment dfe = dgsEnv(mock(DataFetchingEnvironment.class));
    CursorPager<ArticleData> pager =
        pagerWith(Collections.singletonList(articleData("g1")), Direction.PREV, true);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("tag"), eq("author"), eq("fav"), any(), eq(currentUser)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(null, null, 5, "200", "author", "fav", "tag", dfe);

    CursorPageParameter<DateTime> page =
        captureRecentPageParameter("tag", "author", "fav", currentUser);
    assertThat(page.getDirection(), is(Direction.PREV));
    assertThat(page.getLimit(), is(5));
    assertThat(page.getCursor().getMillis(), is(200L));
    assertThat(result.getData().getPageInfo().isHasPreviousPage(), is(true));
  }

  @Test
  void getArticles_should_throw_when_first_and_last_both_null() {
    DgsDataFetchingEnvironment dfe = dgsEnv(mock(DataFetchingEnvironment.class));
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getArticles(null, null, null, null, null, null, null, dfe));
  }

  @Test
  void getArticles_empty_result_has_null_cursors() {
    setAnonymous();
    DgsDataFetchingEnvironment dfe = dgsEnv(mock(DataFetchingEnvironment.class));
    CursorPager<ArticleData> pager = pagerWith(new ArrayList<>(), Direction.NEXT);
    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), eq(null)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, null, null, null, dfe);

    assertThat(result.getData().getEdges(), hasSize(0));
    assertThat(result.getData().getPageInfo().getStartCursor(), is(nullValue()));
    assertThat(result.getData().getPageInfo().getEndCursor(), is(nullValue()));
  }

  // ----- getArticle (ARTICLEPAYLOAD) -----

  @Test
  void getArticle_returns_article_when_found() {
    setCurrentUser(currentUser);
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article(
            "t", "d", "b", Collections.singletonList("java"), currentUser.getId());
    when(dfe.getLocalContext()).thenReturn(coreArticle);
    ArticleData data = articleData("payload");
    when(articleQueryService.findById(eq(coreArticle.getId()), eq(currentUser)))
        .thenReturn(Optional.of(data));

    DataFetcherResult<Article> result = articleDatafetcher.getArticle(dfe);

    assertThat(result.getData().getTitle(), is("title payload"));
    @SuppressWarnings("unchecked")
    Map<String, ArticleData> localContext = (Map<String, ArticleData>) result.getLocalContext();
    assertThat(localContext.get("payload-slug"), is(data));
  }

  @Test
  void getArticle_throws_not_found_when_missing() {
    setCurrentUser(currentUser);
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article(
            "t", "d", "b", Collections.singletonList("java"), currentUser.getId());
    when(dfe.getLocalContext()).thenReturn(coreArticle);
    when(articleQueryService.findById(eq(coreArticle.getId()), any())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleDatafetcher.getArticle(dfe));
  }

  // ----- getCommentArticle -----

  @Test
  void getCommentArticle_returns_article_when_found() {
    setAnonymous();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    CommentData comment = new CommentData("comment-id", "hello", "article-id", null, null, null);
    when(dfe.getLocalContext()).thenReturn(comment);
    ArticleData data = articleData("carticle");
    when(articleQueryService.findById(eq("article-id"), eq(null))).thenReturn(Optional.of(data));

    DataFetcherResult<Article> result = articleDatafetcher.getCommentArticle(dfe);

    assertThat(result.getData().getSlug(), is("carticle-slug"));
  }

  @Test
  void getCommentArticle_throws_not_found_when_missing() {
    setAnonymous();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    CommentData comment = new CommentData("comment-id", "hello", "article-id", null, null, null);
    when(dfe.getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById(eq("article-id"), any())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleDatafetcher.getCommentArticle(dfe));
  }

  // ----- findArticleBySlug -----

  @Test
  void findArticleBySlug_returns_article_when_found() {
    setCurrentUser(currentUser);
    ArticleData data = articleData("byslug");
    when(articleQueryService.findBySlug(eq("byslug-slug"), eq(currentUser)))
        .thenReturn(Optional.of(data));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("byslug-slug");

    assertThat(result.getData().getTitle(), is("title byslug"));
    assertThat(result.getData().getFavoritesCount(), is(equalTo(3)));
    @SuppressWarnings("unchecked")
    Map<String, ArticleData> localContext = (Map<String, ArticleData>) result.getLocalContext();
    assertThat(localContext.get("byslug-slug"), is(data));
  }

  @Test
  void findArticleBySlug_throws_not_found_when_missing() {
    setCurrentUser(currentUser);
    when(articleQueryService.findBySlug(eq("nope"), any())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleDatafetcher.findArticleBySlug("nope"));
  }
}
