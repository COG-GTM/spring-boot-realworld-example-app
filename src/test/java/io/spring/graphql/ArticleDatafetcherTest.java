package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import io.spring.graphql.types.Comment;
import io.spring.graphql.types.Profile;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class ArticleDatafetcherTest {
  private static final DateTime T1 = new DateTime(2021, 1, 1, 0, 0, DateTimeZone.UTC);
  private static final DateTime T2 = new DateTime(2021, 1, 2, 0, 0, DateTimeZone.UTC);

  private ArticleQueryService articleQueryService;
  private UserRepository userRepository;
  private ArticleDatafetcher articleDatafetcher;
  private DataFetchingEnvironment env;
  private DgsDataFetchingEnvironment dfe;
  private User user;

  @BeforeEach
  public void setUp() {
    articleQueryService = mock(ArticleQueryService.class);
    userRepository = mock(UserRepository.class);
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    env = mock(DataFetchingEnvironment.class);
    dfe = new DgsDataFetchingEnvironment(env);
    user = new User("a@b.com", "alice", "pw", "", "");
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

  private static ArticleData articleData(String slug, DateTime updatedAt) {
    return new ArticleData(
        slug + "-id",
        slug,
        "Title " + slug,
        "desc",
        "body",
        true,
        3,
        T1,
        updatedAt,
        Arrays.asList("java"),
        new ProfileData("author-id", "author", "", "", false));
  }

  private static CursorPager<ArticleData> pager(Direction direction, boolean hasExtra) {
    return new CursorPager<>(
        Arrays.asList(articleData("first", T1), articleData("second", T2)), direction, hasExtra);
  }

  @SuppressWarnings("unchecked")
  private static ArgumentCaptor<CursorPageParameter<DateTime>> pageCaptor() {
    return ArgumentCaptor.forClass(CursorPageParameter.class);
  }

  private static void assertConnection(
      DataFetcherResult<ArticlesConnection> result, boolean hasNext, boolean hasPrevious) {
    ArticlesConnection connection = result.getData();
    assertThat(connection.getEdges()).hasSize(2);
    assertThat(connection.getEdges().get(0).getCursor()).isEqualTo(String.valueOf(T1.getMillis()));
    Article node = connection.getEdges().get(0).getNode();
    assertThat(node.getSlug()).isEqualTo("first");
    assertThat(node.getTitle()).isEqualTo("Title first");
    assertThat(node.getFavorited()).isTrue();
    assertThat(node.getFavoritesCount()).isEqualTo(3);
    assertThat(node.getTagList()).containsExactly("java");
    assertThat(node.getCreatedAt()).isEqualTo("2021-01-01T00:00:00.000Z");
    assertThat(connection.getPageInfo().getStartCursor().getValue())
        .isEqualTo(String.valueOf(T1.getMillis()));
    assertThat(connection.getPageInfo().getEndCursor().getValue())
        .isEqualTo(String.valueOf(T2.getMillis()));
    assertThat(connection.getPageInfo().isHasNextPage()).isEqualTo(hasNext);
    assertThat(connection.getPageInfo().isHasPreviousPage()).isEqualTo(hasPrevious);
    @SuppressWarnings("unchecked")
    Map<String, ArticleData> localContext = (Map<String, ArticleData>) result.getLocalContext();
    assertThat(localContext).containsOnlyKeys("first", "second");
    assertThat(localContext.get("first").getId()).isEqualTo("first-id");
  }

  // ---- Query.feed ----

  @Test
  public void getFeed_should_reject_missing_first_and_last() {
    authenticate(user);
    assertThatThrownBy(() -> articleDatafetcher.getFeed(null, null, null, null, dfe))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(articleQueryService);
  }

  @Test
  public void getFeed_should_page_forward_with_first_and_after() {
    authenticate(user);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pager(Direction.NEXT, true));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, String.valueOf(T1.getMillis()), null, null, dfe);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService).findUserFeedWithCursor(eq(user), captor.capture());
    assertThat(captor.getValue().getLimit()).isEqualTo(10);
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.NEXT);
    assertThat(captor.getValue().getCursor().getMillis()).isEqualTo(T1.getMillis());
    assertConnection(result, true, false);
  }

  @Test
  public void getFeed_should_page_backward_with_last_and_before() {
    authenticate(user);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pager(Direction.PREV, true));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(null, null, 5, String.valueOf(T2.getMillis()), dfe);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService).findUserFeedWithCursor(eq(user), captor.capture());
    assertThat(captor.getValue().getLimit()).isEqualTo(5);
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getCursor().getMillis()).isEqualTo(T2.getMillis());
    assertConnection(result, false, true);
  }

  @Test
  public void getFeed_should_pass_null_user_when_anonymous_and_handle_empty_page() {
    anonymous();
    when(articleQueryService.findUserFeedWithCursor(isNull(), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, dfe);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService).findUserFeedWithCursor(isNull(), captor.capture());
    assertThat(captor.getValue().getCursor()).isNull();
    assertThat(result.getData().getEdges()).isEmpty();
    assertThat(result.getData().getPageInfo().getStartCursor()).isNull();
    assertThat(result.getData().getPageInfo().getEndCursor()).isNull();
    assertThat((Map<?, ?>) result.getLocalContext()).isEmpty();
  }

  // ---- Profile.feed ----

  @Test
  public void userFeed_should_reject_missing_first_and_last() {
    assertThatThrownBy(() -> articleDatafetcher.userFeed(null, null, null, null, dfe))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void userFeed_should_throw_not_found_for_unknown_profile() {
    when(env.getSource()).thenReturn(Profile.newBuilder().username("ghost").build());
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> articleDatafetcher.userFeed(10, null, null, null, dfe))
        .isInstanceOf(ResourceNotFoundException.class);
    verifyNoInteractions(articleQueryService);
  }

  @Test
  public void userFeed_should_page_forward_for_profile_user() {
    when(env.getSource()).thenReturn(Profile.newBuilder().username("alice").build());
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pager(Direction.NEXT, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(2, null, null, null, dfe);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService).findUserFeedWithCursor(eq(user), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.NEXT);
    assertConnection(result, false, false);
  }

  @Test
  public void userFeed_should_page_backward_for_profile_user() {
    when(env.getSource()).thenReturn(Profile.newBuilder().username("alice").build());
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pager(Direction.PREV, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(null, null, 2, String.valueOf(T2.getMillis()), dfe);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService).findUserFeedWithCursor(eq(user), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getCursor().getMillis()).isEqualTo(T2.getMillis());
    assertConnection(result, false, false);
  }

  // ---- Profile.favorites ----

  @Test
  public void userFavorites_should_reject_missing_first_and_last() {
    assertThatThrownBy(() -> articleDatafetcher.userFavorites(null, null, null, null, dfe))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void userFavorites_should_query_by_favorited_username_forward() {
    authenticate(user);
    when(env.getSource()).thenReturn(Profile.newBuilder().username("bob").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("bob"), any(), eq(user)))
        .thenReturn(pager(Direction.NEXT, true));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(10, null, null, null, dfe);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), isNull(), eq("bob"), captor.capture(), eq(user));
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.NEXT);
    assertConnection(result, true, false);
  }

  @Test
  public void userFavorites_should_query_backward_for_anonymous() {
    anonymous();
    when(env.getSource()).thenReturn(Profile.newBuilder().username("bob").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("bob"), any(), isNull()))
        .thenReturn(pager(Direction.PREV, true));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(null, null, 10, null, dfe);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), isNull(), eq("bob"), captor.capture(), isNull());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertConnection(result, false, true);
  }

  // ---- Profile.articles ----

  @Test
  public void userArticles_should_reject_missing_first_and_last() {
    assertThatThrownBy(() -> articleDatafetcher.userArticles(null, null, null, null, dfe))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void userArticles_should_query_by_author_username_forward() {
    authenticate(user);
    when(env.getSource()).thenReturn(Profile.newBuilder().username("bob").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("bob"), isNull(), any(), eq(user)))
        .thenReturn(pager(Direction.NEXT, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(10, null, null, null, dfe);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), eq("bob"), isNull(), captor.capture(), eq(user));
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.NEXT);
    assertConnection(result, false, false);
  }

  @Test
  public void userArticles_should_query_backward_for_anonymous() {
    anonymous();
    when(env.getSource()).thenReturn(Profile.newBuilder().username("bob").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("bob"), isNull(), any(), isNull()))
        .thenReturn(pager(Direction.PREV, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(null, null, 10, null, dfe);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), eq("bob"), isNull(), captor.capture(), isNull());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertConnection(result, false, false);
  }

  // ---- Query.articles ----

  @Test
  public void getArticles_should_reject_missing_first_and_last() {
    assertThatThrownBy(
            () -> articleDatafetcher.getArticles(null, null, null, null, null, null, null, dfe))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void getArticles_should_pass_filters_forward_for_authenticated_user() {
    authenticate(user);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), eq("bob"), eq("carol"), any(), eq(user)))
        .thenReturn(pager(Direction.NEXT, true));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(
            10, String.valueOf(T1.getMillis()), null, null, "bob", "carol", "java", dfe);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService)
        .findRecentArticlesWithCursor(
            eq("java"), eq("bob"), eq("carol"), captor.capture(), eq(user));
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.NEXT);
    assertThat(captor.getValue().getCursor().getMillis()).isEqualTo(T1.getMillis());
    assertConnection(result, true, false);
  }

  @Test
  public void getArticles_should_page_backward_for_anonymous() {
    anonymous();
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), isNull()))
        .thenReturn(pager(Direction.PREV, true));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(null, null, 3, null, null, null, null, dfe);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), isNull(), isNull(), captor.capture(), isNull());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getLimit()).isEqualTo(3);
    assertConnection(result, false, true);
  }

  // ---- ArticlePayload.article ----

  @Test
  public void getArticle_should_resolve_article_from_local_context() {
    authenticate(user);
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "Hello", "desc", "body", Collections.emptyList(), user.getId());
    ArticleData data = articleData("hello", T2);
    when(env.getLocalContext()).thenReturn(article);
    when(articleQueryService.findById(article.getId(), user)).thenReturn(Optional.of(data));

    DataFetcherResult<Article> result = articleDatafetcher.getArticle(env);

    verify(articleQueryService).findById(article.getId(), user);
    assertThat(result.getData().getSlug()).isEqualTo("hello");
    assertThat(result.getData().getUpdatedAt()).isEqualTo("2021-01-02T00:00:00.000Z");
    @SuppressWarnings("unchecked")
    Map<String, Object> localContext = (Map<String, Object>) result.getLocalContext();
    assertThat(localContext).containsEntry("hello", data);
  }

  @Test
  public void getArticle_should_pass_null_user_when_anonymous() {
    anonymous();
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "Hello", "desc", "body", Collections.emptyList(), user.getId());
    when(env.getLocalContext()).thenReturn(article);
    when(articleQueryService.findById(article.getId(), null))
        .thenReturn(Optional.of(articleData("hello", T2)));

    assertThat(articleDatafetcher.getArticle(env).getData().getSlug()).isEqualTo("hello");
    verify(articleQueryService).findById(article.getId(), null);
  }

  @Test
  public void getArticle_should_throw_not_found() {
    authenticate(user);
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "Hello", "desc", "body", Collections.emptyList(), user.getId());
    when(env.getLocalContext()).thenReturn(article);
    when(articleQueryService.findById(article.getId(), user)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> articleDatafetcher.getArticle(env))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  // ---- Comment.article ----

  @Test
  public void getCommentArticle_should_resolve_article_of_comment_data() {
    authenticate(user);
    CommentData comment = new CommentData("c1", "body", "article-id", T1, T1, null);
    ArticleData data = articleData("hello", T2);
    when(env.getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById("article-id", user)).thenReturn(Optional.of(data));

    DataFetcherResult<Article> result = articleDatafetcher.getCommentArticle(env);

    verify(articleQueryService).findById("article-id", user);
    assertThat(result.getData().getSlug()).isEqualTo("hello");
    @SuppressWarnings("unchecked")
    Map<String, Object> localContext = (Map<String, Object>) result.getLocalContext();
    assertThat(localContext).containsEntry("hello", data);
  }

  @Test
  @Disabled(
      "Known production bug (RP-66): CommentDatafetcher passes a Map<commentId, CommentData> as"
          + " local context, but getCommentArticle casts it to CommentData, so Comment.article"
          + " throws ClassCastException at runtime. Enable once the datafetcher is fixed.")
  public void getCommentArticle_should_resolve_article_from_comment_fetcher_local_context() {
    authenticate(user);
    CommentData comment = new CommentData("c1", "body", "article-id", T1, T1, null);
    ArticleData data = articleData("hello", T2);
    Map<String, CommentData> localContext = new HashMap<>();
    localContext.put("c1", comment);
    when(env.getLocalContext()).thenReturn(localContext);
    when(env.getSource()).thenReturn(Comment.newBuilder().id("c1").build());
    when(articleQueryService.findById("article-id", user)).thenReturn(Optional.of(data));

    DataFetcherResult<Article> result = articleDatafetcher.getCommentArticle(env);

    assertThat(result.getData().getSlug()).isEqualTo("hello");
  }

  @Test
  public void getCommentArticle_should_throw_not_found() {
    anonymous();
    CommentData comment = new CommentData("c1", "body", "article-id", T1, T1, null);
    when(env.getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById("article-id", null)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> articleDatafetcher.getCommentArticle(env))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  // ---- Query.article ----

  @Test
  public void findArticleBySlug_should_return_article() {
    authenticate(user);
    ArticleData data = articleData("hello", T2);
    when(articleQueryService.findBySlug("hello", user)).thenReturn(Optional.of(data));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("hello");

    verify(articleQueryService).findBySlug("hello", user);
    assertThat(result.getData().getSlug()).isEqualTo("hello");
    assertThat(result.getData().getBody()).isEqualTo("body");
    assertThat(result.getData().getDescription()).isEqualTo("desc");
    @SuppressWarnings("unchecked")
    Map<String, Object> localContext = (Map<String, Object>) result.getLocalContext();
    assertThat(localContext).containsEntry("hello", data);
  }

  @Test
  public void findArticleBySlug_should_pass_null_user_when_anonymous() {
    anonymous();
    when(articleQueryService.findBySlug("hello", null))
        .thenReturn(Optional.of(articleData("hello", T2)));

    assertThat(articleDatafetcher.findArticleBySlug("hello").getData().getSlug())
        .isEqualTo("hello");
    verify(articleQueryService).findBySlug("hello", null);
  }

  @Test
  public void findArticleBySlug_should_throw_not_found() {
    anonymous();
    when(articleQueryService.findBySlug("missing", null)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> articleDatafetcher.findArticleBySlug("missing"))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
