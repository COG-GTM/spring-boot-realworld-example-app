package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import java.util.Arrays;
import java.util.Collections;
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

public class ArticleDatafetcherTest {

  private static final DateTime T1 = new DateTime(2024, 1, 1, 0, 0, DateTimeZone.UTC);
  private static final DateTime T2 = new DateTime(2024, 1, 2, 0, 0, DateTimeZone.UTC);

  private ArticleQueryService articleQueryService;
  private UserRepository userRepository;
  private ArticleDatafetcher articleDatafetcher;
  private DataFetchingEnvironment env;
  private DgsDataFetchingEnvironment dfe;
  private User current;
  private ArticleData first;
  private ArticleData second;

  @BeforeEach
  public void setUp() {
    articleQueryService = mock(ArticleQueryService.class);
    userRepository = mock(UserRepository.class);
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    env = mock(DataFetchingEnvironment.class);
    dfe = new DgsDataFetchingEnvironment(env);
    current = new User("alice@example.com", "alice", "pw", "", "");
    ProfileData author = new ProfileData("bob-id", "bob", "", "", false);
    first =
        new ArticleData(
            "a1", "slug-1", "Title 1", "d1", "b1", true, 3, T1, T2, Arrays.asList("x"), author);
    second =
        new ArticleData(
            "a2", "slug-2", "Title 2", "d2", "b2", false, 0, T1, T1, Arrays.asList(), author);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  private CursorPager<ArticleData> twoArticles(Direction direction) {
    return new CursorPager<>(Arrays.asList(first, second), direction, true);
  }

  @SuppressWarnings("unchecked")
  private static ArgumentCaptor<CursorPageParameter<DateTime>> pageCaptor() {
    return ArgumentCaptor.forClass(CursorPageParameter.class);
  }

  private void assertConnection(
      DataFetcherResult<ArticlesConnection> result, boolean hasNext, boolean hasPrevious) {
    ArticlesConnection connection = result.getData();
    assertEquals(2, connection.getEdges().size());
    assertEquals(String.valueOf(T2.getMillis()), connection.getEdges().get(0).getCursor());
    Article node = connection.getEdges().get(0).getNode();
    assertEquals("slug-1", node.getSlug());
    assertEquals("Title 1", node.getTitle());
    assertEquals("d1", node.getDescription());
    assertEquals("b1", node.getBody());
    assertTrue(node.getFavorited());
    assertEquals(3, node.getFavoritesCount());
    assertEquals(Arrays.asList("x"), node.getTagList());
    assertEquals("2024-01-01T00:00:00.000Z", node.getCreatedAt());
    assertEquals("2024-01-02T00:00:00.000Z", node.getUpdatedAt());
    assertEquals(
        String.valueOf(T2.getMillis()), connection.getPageInfo().getStartCursor().getValue());
    assertEquals(
        String.valueOf(T1.getMillis()), connection.getPageInfo().getEndCursor().getValue());
    assertEquals(hasNext, connection.getPageInfo().isHasNextPage());
    assertEquals(hasPrevious, connection.getPageInfo().isHasPreviousPage());

    Map<String, ArticleData> localContext = (Map<String, ArticleData>) result.getLocalContext();
    assertEquals(2, localContext.size());
    assertSame(first, localContext.get("slug-1"));
    assertSame(second, localContext.get("slug-2"));
  }

  // ---- feed ----

  @Test
  public void feed_should_reject_missing_first_and_last() {
    authenticate(current);
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getFeed(null, null, null, null, dfe));
    verifyNoInteractions(articleQueryService);
  }

  @Test
  public void feed_should_page_forward_for_authenticated_user() {
    authenticate(current);
    ArgumentCaptor<CursorPageParameter<DateTime>> page = pageCaptor();
    when(articleQueryService.findUserFeedWithCursor(eq(current), any()))
        .thenReturn(twoArticles(Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, String.valueOf(T1.getMillis()), null, null, dfe);

    verify(articleQueryService).findUserFeedWithCursor(eq(current), page.capture());
    assertEquals(10, page.getValue().getLimit());
    assertEquals(Direction.NEXT, page.getValue().getDirection());
    assertEquals(T1.getMillis(), page.getValue().getCursor().getMillis());
    assertConnection(result, true, false);
  }

  @Test
  public void feed_should_page_backward_for_anonymous_user() {
    anonymous();
    ArgumentCaptor<CursorPageParameter<DateTime>> page = pageCaptor();
    when(articleQueryService.findUserFeedWithCursor(isNull(), any()))
        .thenReturn(twoArticles(Direction.PREV));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(null, null, 5, String.valueOf(T2.getMillis()), dfe);

    verify(articleQueryService).findUserFeedWithCursor(isNull(), page.capture());
    assertEquals(5, page.getValue().getLimit());
    assertEquals(Direction.PREV, page.getValue().getDirection());
    assertEquals(T2.getMillis(), page.getValue().getCursor().getMillis());
    assertConnection(result, false, true);
  }

  @Test
  public void feed_should_return_empty_connection_with_null_cursors() {
    authenticate(current);
    when(articleQueryService.findUserFeedWithCursor(eq(current), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, dfe);

    assertTrue(result.getData().getEdges().isEmpty());
    assertNull(result.getData().getPageInfo().getStartCursor());
    assertNull(result.getData().getPageInfo().getEndCursor());
    assertFalse(result.getData().getPageInfo().isHasNextPage());
    assertTrue(((Map<?, ?>) result.getLocalContext()).isEmpty());
  }

  // ---- Profile.feed ----

  @Test
  public void user_feed_should_reject_missing_first_and_last() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFeed(null, null, null, null, dfe));
  }

  @Test
  public void user_feed_should_throw_not_found_for_unknown_profile() {
    when(env.getSource()).thenReturn(Profile.newBuilder().username("ghost").build());
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleDatafetcher.userFeed(10, null, null, null, dfe));
    verifyNoInteractions(articleQueryService);
  }

  @Test
  public void user_feed_should_page_forward_for_profile_owner() {
    User bob = new User("bob@example.com", "bob", "pw", "", "");
    when(env.getSource()).thenReturn(Profile.newBuilder().username("bob").build());
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
    when(articleQueryService.findUserFeedWithCursor(eq(bob), any()))
        .thenReturn(twoArticles(Direction.NEXT));
    ArgumentCaptor<CursorPageParameter<DateTime>> page = pageCaptor();

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(10, null, null, null, dfe);

    verify(articleQueryService).findUserFeedWithCursor(eq(bob), page.capture());
    assertEquals(Direction.NEXT, page.getValue().getDirection());
    assertNull(page.getValue().getCursor());
    assertConnection(result, true, false);
  }

  @Test
  public void user_feed_should_page_backward() {
    User bob = new User("bob@example.com", "bob", "pw", "", "");
    when(env.getSource()).thenReturn(Profile.newBuilder().username("bob").build());
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
    when(articleQueryService.findUserFeedWithCursor(eq(bob), any()))
        .thenReturn(twoArticles(Direction.PREV));
    ArgumentCaptor<CursorPageParameter<DateTime>> page = pageCaptor();

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(null, null, 3, String.valueOf(T2.getMillis()), dfe);

    verify(articleQueryService).findUserFeedWithCursor(eq(bob), page.capture());
    assertEquals(Direction.PREV, page.getValue().getDirection());
    assertEquals(3, page.getValue().getLimit());
    assertConnection(result, false, true);
  }

  // ---- Profile.favorites ----

  @Test
  public void user_favorites_should_reject_missing_first_and_last() {
    authenticate(current);
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFavorites(null, null, null, null, dfe));
  }

  @Test
  public void user_favorites_should_query_favorited_by_profile_for_authenticated_user() {
    authenticate(current);
    when(env.getSource()).thenReturn(Profile.newBuilder().username("bob").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("bob"), any(), eq(current)))
        .thenReturn(twoArticles(Direction.NEXT));
    ArgumentCaptor<CursorPageParameter<DateTime>> page = pageCaptor();

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(10, null, null, null, dfe);

    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), isNull(), eq("bob"), page.capture(), eq(current));
    assertEquals(Direction.NEXT, page.getValue().getDirection());
    assertConnection(result, true, false);
  }

  @Test
  public void user_favorites_should_page_backward_for_anonymous_user() {
    anonymous();
    when(env.getSource()).thenReturn(Profile.newBuilder().username("bob").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("bob"), any(), isNull()))
        .thenReturn(twoArticles(Direction.PREV));
    ArgumentCaptor<CursorPageParameter<DateTime>> page = pageCaptor();

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(null, null, 2, null, dfe);

    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), isNull(), eq("bob"), page.capture(), isNull());
    assertEquals(Direction.PREV, page.getValue().getDirection());
    assertConnection(result, false, true);
  }

  // ---- Profile.articles ----

  @Test
  public void user_articles_should_reject_missing_first_and_last() {
    authenticate(current);
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userArticles(null, null, null, null, dfe));
  }

  @Test
  public void user_articles_should_query_authored_by_profile_for_authenticated_user() {
    authenticate(current);
    when(env.getSource()).thenReturn(Profile.newBuilder().username("bob").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("bob"), isNull(), any(), eq(current)))
        .thenReturn(twoArticles(Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(10, null, null, null, dfe);

    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), eq("bob"), isNull(), any(), eq(current));
    assertConnection(result, true, false);
  }

  @Test
  public void user_articles_should_page_backward_for_anonymous_user() {
    anonymous();
    when(env.getSource()).thenReturn(Profile.newBuilder().username("bob").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("bob"), isNull(), any(), isNull()))
        .thenReturn(twoArticles(Direction.PREV));
    ArgumentCaptor<CursorPageParameter<DateTime>> page = pageCaptor();

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(null, null, 2, null, dfe);

    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), eq("bob"), isNull(), page.capture(), isNull());
    assertEquals(Direction.PREV, page.getValue().getDirection());
    assertConnection(result, false, true);
  }

  // ---- articles ----

  @Test
  public void articles_should_reject_missing_first_and_last() {
    anonymous();
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getArticles(null, null, null, null, null, null, null, dfe));
  }

  @Test
  public void articles_should_pass_filters_and_page_forward_for_authenticated_user() {
    authenticate(current);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), eq("bob"), eq("carol"), any(), eq(current)))
        .thenReturn(twoArticles(Direction.NEXT));
    ArgumentCaptor<CursorPageParameter<DateTime>> page = pageCaptor();

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(
            10, String.valueOf(T1.getMillis()), null, null, "bob", "carol", "java", dfe);

    verify(articleQueryService)
        .findRecentArticlesWithCursor(
            eq("java"), eq("bob"), eq("carol"), page.capture(), eq(current));
    assertEquals(Direction.NEXT, page.getValue().getDirection());
    assertEquals(T1.getMillis(), page.getValue().getCursor().getMillis());
    assertConnection(result, true, false);
  }

  @Test
  public void articles_should_page_backward_for_anonymous_user() {
    anonymous();
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), isNull()))
        .thenReturn(twoArticles(Direction.PREV));
    ArgumentCaptor<CursorPageParameter<DateTime>> page = pageCaptor();

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(null, null, 4, null, null, null, null, dfe);

    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), isNull(), isNull(), page.capture(), isNull());
    assertEquals(Direction.PREV, page.getValue().getDirection());
    assertEquals(4, page.getValue().getLimit());
    assertConnection(result, false, true);
  }

  // ---- ArticlePayload.article ----

  @Test
  public void article_payload_should_resolve_article_from_local_context_entity() {
    authenticate(current);
    io.spring.core.article.Article entity =
        new io.spring.core.article.Article("Title 1", "d1", "b1", Arrays.asList("x"), "bob-id");
    when(env.getLocalContext()).thenReturn(entity);
    when(articleQueryService.findById(entity.getId(), current)).thenReturn(Optional.of(first));

    DataFetcherResult<Article> result = articleDatafetcher.getArticle(env);

    assertEquals("slug-1", result.getData().getSlug());
    assertSame(first, ((Map<?, ?>) result.getLocalContext()).get("slug-1"));
    verify(articleQueryService).findById(entity.getId(), current);
  }

  @Test
  public void article_payload_should_pass_null_user_for_anonymous() {
    anonymous();
    io.spring.core.article.Article entity =
        new io.spring.core.article.Article("Title 1", "d1", "b1", Arrays.asList("x"), "bob-id");
    when(env.getLocalContext()).thenReturn(entity);
    when(articleQueryService.findById(eq(entity.getId()), isNull())).thenReturn(Optional.of(first));

    DataFetcherResult<Article> result = articleDatafetcher.getArticle(env);

    assertEquals("slug-1", result.getData().getSlug());
  }

  @Test
  public void article_payload_should_throw_not_found_when_article_missing() {
    authenticate(current);
    io.spring.core.article.Article entity =
        new io.spring.core.article.Article("Title 1", "d1", "b1", Arrays.asList("x"), "bob-id");
    when(env.getLocalContext()).thenReturn(entity);
    when(articleQueryService.findById(entity.getId(), current)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleDatafetcher.getArticle(env));
  }

  // ---- Comment.article ----

  @Test
  public void comment_article_should_resolve_article_from_comment_data_local_context() {
    authenticate(current);
    CommentData comment = new CommentData("c1", "body", "a1", T1, T1, null);
    when(env.getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById("a1", current)).thenReturn(Optional.of(first));

    DataFetcherResult<Article> result = articleDatafetcher.getCommentArticle(env);

    assertEquals("slug-1", result.getData().getSlug());
    assertSame(first, ((Map<?, ?>) result.getLocalContext()).get("slug-1"));
    verify(articleQueryService).findById("a1", current);
  }

  @Test
  public void comment_article_should_pass_null_user_for_anonymous() {
    anonymous();
    CommentData comment = new CommentData("c1", "body", "a1", T1, T1, null);
    when(env.getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById(eq("a1"), isNull())).thenReturn(Optional.of(first));

    assertEquals("slug-1", articleDatafetcher.getCommentArticle(env).getData().getSlug());
  }

  @Test
  public void comment_article_should_throw_not_found_when_article_missing() {
    authenticate(current);
    CommentData comment = new CommentData("c1", "body", "a1", T1, T1, null);
    when(env.getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById("a1", current)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleDatafetcher.getCommentArticle(env));
  }

  // ---- article(slug) ----

  @Test
  public void article_by_slug_should_return_article_for_authenticated_user() {
    authenticate(current);
    when(articleQueryService.findBySlug("slug-1", current)).thenReturn(Optional.of(first));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("slug-1");

    assertEquals("slug-1", result.getData().getSlug());
    assertTrue(result.getData().getFavorited());
    assertSame(first, ((Map<?, ?>) result.getLocalContext()).get("slug-1"));
    verify(articleQueryService).findBySlug("slug-1", current);
  }

  @Test
  public void article_by_slug_should_pass_null_user_for_anonymous() {
    anonymous();
    when(articleQueryService.findBySlug(eq("slug-2"), isNull())).thenReturn(Optional.of(second));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("slug-2");

    assertEquals("slug-2", result.getData().getSlug());
    assertFalse(result.getData().getFavorited());
  }

  @Test
  public void article_by_slug_should_throw_not_found_for_unknown_slug() {
    authenticate(current);
    when(articleQueryService.findBySlug("nope", current)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleDatafetcher.findArticleBySlug("nope"));
  }
}
