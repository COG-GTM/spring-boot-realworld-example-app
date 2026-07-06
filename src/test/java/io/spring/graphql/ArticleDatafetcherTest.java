package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class ArticleDatafetcherTest {

  private ArticleQueryService articleQueryService;
  private UserRepository userRepository;
  private ArticleDatafetcher articleDatafetcher;

  private User currentUser;

  @BeforeEach
  public void setUp() {
    articleQueryService = mock(ArticleQueryService.class);
    userRepository = mock(UserRepository.class);
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);

    currentUser = new User("current@example.com", "current", "pass", "", "");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, java.util.Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  // DgsDataFetchingEnvironment is a final class that cannot be mocked without
  // mockito-inline, so wrap a mocked plain DataFetchingEnvironment interface.
  private DgsDataFetchingEnvironment dgsEnv(DataFetchingEnvironment inner) {
    return new DgsDataFetchingEnvironment(inner);
  }

  private DgsDataFetchingEnvironment dgsEnv() {
    return dgsEnv(mock(DataFetchingEnvironment.class));
  }

  private ArticleData buildArticleData(String slug, DateTime updatedAt) {
    return new ArticleData(
        "id-" + slug,
        slug,
        "Title " + slug,
        "Description " + slug,
        "Body " + slug,
        false,
        0,
        new DateTime(),
        updatedAt,
        Arrays.asList("java"),
        new ProfileData("author-id", "author", "bio", "image", false));
  }

  private CursorPager<ArticleData> pagerWith(
      Direction direction, boolean hasExtra, String... slugs) {
    List<ArticleData> data = new ArrayList<>();
    long millis = 1_000_000L;
    for (String slug : slugs) {
      data.add(buildArticleData(slug, new DateTime().withMillis(millis)));
      millis += 1_000L;
    }
    return new CursorPager<>(data, direction, hasExtra);
  }

  private void assertConnection(
      DataFetcherResult<ArticlesConnection> result, String... expectedSlugs) {
    assertNotNull(result);
    ArticlesConnection connection = result.getData();
    assertNotNull(connection);
    assertEquals(expectedSlugs.length, connection.getEdges().size());
    for (int i = 0; i < expectedSlugs.length; i++) {
      Article node = connection.getEdges().get(i).getNode();
      assertEquals(expectedSlugs[i], node.getSlug());
      assertNotNull(connection.getEdges().get(i).getCursor());
    }
    @SuppressWarnings("unchecked")
    Map<String, ArticleData> localContext = (Map<String, ArticleData>) result.getLocalContext();
    assertNotNull(localContext);
    for (String slug : expectedSlugs) {
      assertTrue(localContext.containsKey(slug));
    }
  }

  // getFeed -----------------------------------------------------------------

  @Test
  public void getFeed_should_throw_when_first_and_last_both_null() {
    anonymous();
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getFeed(null, null, null, null, dgsEnv()));
  }

  @Test
  public void getFeed_should_return_connection_for_next_page() {
    authenticate(currentUser);
    when(articleQueryService.findUserFeedWithCursor(any(), any()))
        .thenReturn(pagerWith(Direction.NEXT, true, "a1", "a2"));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, "1000000", null, null, dgsEnv());

    assertConnection(result, "a1", "a2");
    assertTrue(result.getData().getPageInfo().isHasNextPage());
    assertFalse(result.getData().getPageInfo().isHasPreviousPage());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<CursorPageParameter<DateTime>> pageCaptor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService).findUserFeedWithCursor(eq(currentUser), pageCaptor.capture());
    CursorPageParameter<DateTime> page = pageCaptor.getValue();
    assertEquals(Direction.NEXT, page.getDirection());
    assertEquals(10, page.getLimit());
    assertEquals(1_000_000L, page.getCursor().getMillis());
  }

  @Test
  public void getFeed_should_return_connection_for_previous_page() {
    authenticate(currentUser);
    when(articleQueryService.findUserFeedWithCursor(any(), any()))
        .thenReturn(pagerWith(Direction.PREV, true, "b1"));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(null, null, 10, null, dgsEnv());

    assertConnection(result, "b1");
    assertTrue(result.getData().getPageInfo().isHasPreviousPage());
    assertFalse(result.getData().getPageInfo().isHasNextPage());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<CursorPageParameter<DateTime>> pageCaptor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService).findUserFeedWithCursor(eq(currentUser), pageCaptor.capture());
    CursorPageParameter<DateTime> page = pageCaptor.getValue();
    assertEquals(Direction.PREV, page.getDirection());
    assertEquals(10, page.getLimit());
    assertFalse(page.isNext());
  }

  // userFeed ----------------------------------------------------------------

  @Test
  public void userFeed_should_throw_when_first_and_last_both_null() {
    anonymous();
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFeed(null, null, null, null, dgsEnv()));
  }

  @Test
  public void userFeed_should_return_connection_for_target_user() {
    User target = new User("target@example.com", "target", "pass", "", "");
    Profile profile = Profile.newBuilder().username("target").build();
    DataFetchingEnvironment inner = mock(DataFetchingEnvironment.class);
    when(inner.getSource()).thenReturn(profile);
    DgsDataFetchingEnvironment dfe = dgsEnv(inner);
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(articleQueryService.findUserFeedWithCursor(any(), any()))
        .thenReturn(pagerWith(Direction.NEXT, false, "f1"));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(5, null, null, null, dfe);

    assertConnection(result, "f1");
    verify(articleQueryService).findUserFeedWithCursor(eq(target), any());
  }

  @Test
  public void userFeed_should_throw_when_user_not_found() {
    Profile profile = Profile.newBuilder().username("missing").build();
    DataFetchingEnvironment inner = mock(DataFetchingEnvironment.class);
    when(inner.getSource()).thenReturn(profile);
    DgsDataFetchingEnvironment dfe = dgsEnv(inner);
    when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleDatafetcher.userFeed(5, null, null, null, dfe));
  }

  // userFavorites -----------------------------------------------------------

  @Test
  public void userFavorites_should_throw_when_first_and_last_both_null() {
    anonymous();
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFavorites(null, null, null, null, dgsEnv()));
  }

  @Test
  public void userFavorites_should_return_connection() {
    authenticate(currentUser);
    Profile profile = Profile.newBuilder().username("target").build();
    DataFetchingEnvironment inner = mock(DataFetchingEnvironment.class);
    when(inner.getSource()).thenReturn(profile);
    DgsDataFetchingEnvironment dfe = dgsEnv(inner);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("target"), any(), eq(currentUser)))
        .thenReturn(pagerWith(Direction.NEXT, false, "fav1"));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(5, null, null, null, dfe);

    assertConnection(result, "fav1");
    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), isNull(), eq("target"), any(), eq(currentUser));
  }

  // userArticles ------------------------------------------------------------

  @Test
  public void userArticles_should_throw_when_first_and_last_both_null() {
    anonymous();
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userArticles(null, null, null, null, dgsEnv()));
  }

  @Test
  public void userArticles_should_return_connection_for_previous_page() {
    authenticate(currentUser);
    Profile profile = Profile.newBuilder().username("target").build();
    DataFetchingEnvironment inner = mock(DataFetchingEnvironment.class);
    when(inner.getSource()).thenReturn(profile);
    DgsDataFetchingEnvironment dfe = dgsEnv(inner);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("target"), isNull(), any(), eq(currentUser)))
        .thenReturn(pagerWith(Direction.PREV, true, "art1"));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(null, null, 5, null, dfe);

    assertConnection(result, "art1");
    assertTrue(result.getData().getPageInfo().isHasPreviousPage());
    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), eq("target"), isNull(), any(), eq(currentUser));
  }

  // getArticles -------------------------------------------------------------

  @Test
  public void getArticles_should_throw_when_first_and_last_both_null() {
    anonymous();
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getArticles(null, null, null, null, null, null, null, dgsEnv()));
  }

  @Test
  public void getArticles_should_return_connection_for_anonymous_user() {
    anonymous();
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), eq("john"), eq("jane"), any(), isNull()))
        .thenReturn(pagerWith(Direction.NEXT, false, "g1", "g2"));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, "john", "jane", "java", dgsEnv());

    assertConnection(result, "g1", "g2");
    verify(articleQueryService)
        .findRecentArticlesWithCursor(eq("java"), eq("john"), eq("jane"), any(), isNull());
  }

  // getArticle --------------------------------------------------------------

  @Test
  public void getArticle_should_return_article_from_local_context() {
    authenticate(currentUser);
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "Some Title", "desc", "body", Arrays.asList("java"), currentUser.getId());
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(article);
    ArticleData articleData = buildArticleData("some-title", new DateTime());
    when(articleQueryService.findById(eq(article.getId()), eq(currentUser)))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.getArticle(dfe);

    assertNotNull(result.getData());
    assertEquals("some-title", result.getData().getSlug());
    @SuppressWarnings("unchecked")
    Map<String, ArticleData> localContext = (Map<String, ArticleData>) result.getLocalContext();
    assertTrue(localContext.containsKey("some-title"));
  }

  @Test
  public void getArticle_should_throw_when_article_not_found() {
    authenticate(currentUser);
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "Some Title", "desc", "body", Arrays.asList("java"), currentUser.getId());
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleDatafetcher.getArticle(dfe));
  }

  // getCommentArticle -------------------------------------------------------

  @Test
  public void getCommentArticle_should_return_article_from_comment() {
    authenticate(currentUser);
    CommentData comment =
        new CommentData(
            "comment-id",
            "comment body",
            "article-id",
            new DateTime(),
            new DateTime(),
            new ProfileData("pid", "author", "bio", "image", false));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(comment);
    ArticleData articleData = buildArticleData("linked-slug", new DateTime());
    when(articleQueryService.findById(eq("article-id"), eq(currentUser)))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.getCommentArticle(dfe);

    assertNotNull(result.getData());
    assertEquals("linked-slug", result.getData().getSlug());
    @SuppressWarnings("unchecked")
    Map<String, ArticleData> localContext = (Map<String, ArticleData>) result.getLocalContext();
    assertTrue(localContext.containsKey("linked-slug"));
  }

  @Test
  public void getCommentArticle_should_throw_when_article_not_found() {
    authenticate(currentUser);
    CommentData comment =
        new CommentData(
            "comment-id",
            "comment body",
            "article-id",
            new DateTime(),
            new DateTime(),
            new ProfileData("pid", "author", "bio", "image", false));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById(eq("article-id"), any())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleDatafetcher.getCommentArticle(dfe));
  }

  // findArticleBySlug -------------------------------------------------------

  @Test
  public void findArticleBySlug_should_return_article() {
    authenticate(currentUser);
    ArticleData articleData = buildArticleData("my-slug", new DateTime());
    when(articleQueryService.findBySlug(eq("my-slug"), eq(currentUser)))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("my-slug");

    assertNotNull(result.getData());
    assertEquals("my-slug", result.getData().getSlug());
    @SuppressWarnings("unchecked")
    Map<String, ArticleData> localContext = (Map<String, ArticleData>) result.getLocalContext();
    assertTrue(localContext.containsKey("my-slug"));
  }

  @Test
  public void findArticleBySlug_should_throw_when_not_found() {
    anonymous();
    when(articleQueryService.findBySlug(eq("missing"), any())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleDatafetcher.findArticleBySlug("missing"));
  }
}
