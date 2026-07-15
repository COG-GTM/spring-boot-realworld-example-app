package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import io.spring.graphql.types.ArticlesConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

  @BeforeEach
  void setUp() {
    articleQueryService = mock(ArticleQueryService.class);
    userRepository = mock(UserRepository.class);
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private User authenticate() {
    User user = new User("user@example.com", "user", "password", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
    return user;
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  private ArticleData articleData(String slug) {
    return new ArticleData(
        UUID.randomUUID().toString(),
        slug,
        "title",
        "description",
        "body",
        false,
        0,
        new DateTime(),
        new DateTime(),
        Arrays.asList("java"),
        new ProfileData("pid", "author", "bio", "image", false));
  }

  private CursorPager<ArticleData> pagerWith(String slug, Direction direction) {
    return new CursorPager<>(Arrays.asList(articleData(slug)), direction, false);
  }

  private CursorPager<ArticleData> emptyPager(Direction direction) {
    return new CursorPager<>(new ArrayList<>(), direction, false);
  }

  // ---------------------------------------------------------------------------
  // getFeed
  // ---------------------------------------------------------------------------

  @Test
  void getFeed_throws_when_first_and_last_null() {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            articleDatafetcher.getFeed(
                null, null, null, null, new DgsDataFetchingEnvironment(dfe)));
  }

  @Test
  void getFeed_pages_next_when_first_present() {
    User current = authenticate();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(articleQueryService.findUserFeedWithCursor(eq(current), any()))
        .thenReturn(pagerWith("slug-1", Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, new DgsDataFetchingEnvironment(dfe));

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService).findUserFeedWithCursor(eq(current), captor.capture());
    assertEquals(Direction.NEXT, captor.getValue().getDirection());
    assertNull(captor.getValue().getCursor());
    assertEquals(1, result.getData().getEdges().size());
    assertEquals("slug-1", result.getData().getEdges().get(0).getNode().getSlug());
    assertTrue(((Map<?, ?>) result.getLocalContext()).containsKey("slug-1"));
  }

  @Test
  void getFeed_pages_prev_when_last_present() {
    User current = authenticate();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(articleQueryService.findUserFeedWithCursor(eq(current), any()))
        .thenReturn(pagerWith("slug-2", Direction.PREV));

    articleDatafetcher.getFeed(null, null, 10, "1000", new DgsDataFetchingEnvironment(dfe));

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService).findUserFeedWithCursor(eq(current), captor.capture());
    assertEquals(Direction.PREV, captor.getValue().getDirection());
    assertEquals(1000L, captor.getValue().getCursor().getMillis());
  }

  @Test
  void getFeed_uses_null_user_when_anonymous_and_handles_empty_page() {
    anonymous();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(articleQueryService.findUserFeedWithCursor(isNull(), any()))
        .thenReturn(emptyPager(Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, new DgsDataFetchingEnvironment(dfe));

    assertTrue(result.getData().getEdges().isEmpty());
    assertNull(result.getData().getPageInfo().getStartCursor());
    assertNull(result.getData().getPageInfo().getEndCursor());
    verify(articleQueryService).findUserFeedWithCursor(isNull(), any());
  }

  // ---------------------------------------------------------------------------
  // userFeed
  // ---------------------------------------------------------------------------

  @Test
  void userFeed_throws_when_first_and_last_null() {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            articleDatafetcher.userFeed(
                null, null, null, null, new DgsDataFetchingEnvironment(dfe)));
  }

  @Test
  void userFeed_throws_not_found_when_user_missing() {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getSource())
        .thenReturn(io.spring.graphql.types.Profile.newBuilder().username("ghost").build());
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () ->
            articleDatafetcher.userFeed(10, null, null, null, new DgsDataFetchingEnvironment(dfe)));
  }

  @Test
  void userFeed_pages_next_when_first_present() {
    User target = new User("t@example.com", "target", "pw", "", "");
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getSource())
        .thenReturn(io.spring.graphql.types.Profile.newBuilder().username("target").build());
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(articleQueryService.findUserFeedWithCursor(eq(target), any()))
        .thenReturn(pagerWith("slug-3", Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(10, null, null, null, new DgsDataFetchingEnvironment(dfe));

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService).findUserFeedWithCursor(eq(target), captor.capture());
    assertEquals(Direction.NEXT, captor.getValue().getDirection());
    assertEquals("slug-3", result.getData().getEdges().get(0).getNode().getSlug());
  }

  @Test
  void userFeed_pages_prev_when_last_present() {
    User target = new User("t@example.com", "target", "pw", "", "");
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getSource())
        .thenReturn(io.spring.graphql.types.Profile.newBuilder().username("target").build());
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(articleQueryService.findUserFeedWithCursor(eq(target), any()))
        .thenReturn(pagerWith("slug-4", Direction.PREV));

    articleDatafetcher.userFeed(null, null, 10, "2000", new DgsDataFetchingEnvironment(dfe));

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService).findUserFeedWithCursor(eq(target), captor.capture());
    assertEquals(Direction.PREV, captor.getValue().getDirection());
    assertEquals(2000L, captor.getValue().getCursor().getMillis());
  }

  // ---------------------------------------------------------------------------
  // userFavorites
  // ---------------------------------------------------------------------------

  @Test
  void userFavorites_throws_when_first_and_last_null() {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            articleDatafetcher.userFavorites(
                null, null, null, null, new DgsDataFetchingEnvironment(dfe)));
  }

  @Test
  void userFavorites_pages_next_when_first_present() {
    User current = authenticate();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getSource())
        .thenReturn(io.spring.graphql.types.Profile.newBuilder().username("bob").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("bob"), any(), eq(current)))
        .thenReturn(pagerWith("slug-5", Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(10, null, null, null, new DgsDataFetchingEnvironment(dfe));

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), isNull(), eq("bob"), captor.capture(), eq(current));
    assertEquals(Direction.NEXT, captor.getValue().getDirection());
    assertEquals("slug-5", result.getData().getEdges().get(0).getNode().getSlug());
  }

  @Test
  void userFavorites_pages_prev_when_last_present() {
    User current = authenticate();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getSource())
        .thenReturn(io.spring.graphql.types.Profile.newBuilder().username("bob").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("bob"), any(), eq(current)))
        .thenReturn(pagerWith("slug-6", Direction.PREV));

    articleDatafetcher.userFavorites(null, null, 10, "3000", new DgsDataFetchingEnvironment(dfe));

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), isNull(), eq("bob"), captor.capture(), eq(current));
    assertEquals(Direction.PREV, captor.getValue().getDirection());
    assertEquals(3000L, captor.getValue().getCursor().getMillis());
  }

  // ---------------------------------------------------------------------------
  // userArticles
  // ---------------------------------------------------------------------------

  @Test
  void userArticles_throws_when_first_and_last_null() {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            articleDatafetcher.userArticles(
                null, null, null, null, new DgsDataFetchingEnvironment(dfe)));
  }

  @Test
  void userArticles_pages_next_when_first_present() {
    User current = authenticate();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getSource())
        .thenReturn(io.spring.graphql.types.Profile.newBuilder().username("bob").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("bob"), isNull(), any(), eq(current)))
        .thenReturn(pagerWith("slug-7", Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(10, null, null, null, new DgsDataFetchingEnvironment(dfe));

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), eq("bob"), isNull(), captor.capture(), eq(current));
    assertEquals(Direction.NEXT, captor.getValue().getDirection());
    assertEquals("slug-7", result.getData().getEdges().get(0).getNode().getSlug());
  }

  @Test
  void userArticles_pages_prev_when_last_present() {
    User current = authenticate();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.getSource())
        .thenReturn(io.spring.graphql.types.Profile.newBuilder().username("bob").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("bob"), isNull(), any(), eq(current)))
        .thenReturn(pagerWith("slug-8", Direction.PREV));

    articleDatafetcher.userArticles(null, null, 10, "4000", new DgsDataFetchingEnvironment(dfe));

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), eq("bob"), isNull(), captor.capture(), eq(current));
    assertEquals(Direction.PREV, captor.getValue().getDirection());
    assertEquals(4000L, captor.getValue().getCursor().getMillis());
  }

  // ---------------------------------------------------------------------------
  // getArticles
  // ---------------------------------------------------------------------------

  @Test
  void getArticles_throws_when_first_and_last_null() {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            articleDatafetcher.getArticles(
                null, null, null, null, null, null, null, new DgsDataFetchingEnvironment(dfe)));
  }

  @Test
  void getArticles_pages_next_when_first_present() {
    User current = authenticate();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("tag"), eq("author"), eq("fav"), any(), eq(current)))
        .thenReturn(pagerWith("slug-9", Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(
            10, null, null, null, "author", "fav", "tag", new DgsDataFetchingEnvironment(dfe));

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(
            eq("tag"), eq("author"), eq("fav"), captor.capture(), eq(current));
    assertEquals(Direction.NEXT, captor.getValue().getDirection());
    assertEquals("slug-9", result.getData().getEdges().get(0).getNode().getSlug());
  }

  @Test
  void getArticles_pages_prev_when_last_present() {
    User current = authenticate();
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("tag"), eq("author"), eq("fav"), any(), eq(current)))
        .thenReturn(pagerWith("slug-10", Direction.PREV));

    articleDatafetcher.getArticles(
        null, null, 10, "5000", "author", "fav", "tag", new DgsDataFetchingEnvironment(dfe));

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(
            eq("tag"), eq("author"), eq("fav"), captor.capture(), eq(current));
    assertEquals(Direction.PREV, captor.getValue().getDirection());
    assertEquals(5000L, captor.getValue().getCursor().getMillis());
  }

  // ---------------------------------------------------------------------------
  // getArticle (from ARTICLEPAYLOAD local context)
  // ---------------------------------------------------------------------------

  @Test
  void getArticle_returns_data_and_local_context() {
    User current = authenticate();
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "title", "desc", "body", Arrays.asList("java"), "author-id");
    ArticleData data = articleData("resolved-slug");
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<io.spring.core.article.Article>getLocalContext()).thenReturn(article);
    when(articleQueryService.findById(article.getId(), current)).thenReturn(Optional.of(data));

    DataFetcherResult<io.spring.graphql.types.Article> result = articleDatafetcher.getArticle(dfe);

    assertEquals("resolved-slug", result.getData().getSlug());
    assertSame(data, ((Map<?, ?>) result.getLocalContext()).get("resolved-slug"));
  }

  @Test
  void getArticle_throws_not_found_when_missing() {
    authenticate();
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "title", "desc", "body", Arrays.asList("java"), "author-id");
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<io.spring.core.article.Article>getLocalContext()).thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleDatafetcher.getArticle(dfe));
  }

  // ---------------------------------------------------------------------------
  // getCommentArticle (from COMMENT local context)
  // ---------------------------------------------------------------------------

  @Test
  void getCommentArticle_returns_data_and_local_context() {
    User current = authenticate();
    CommentData comment =
        new CommentData(
            "comment-id",
            "body",
            "article-id",
            new DateTime(),
            new DateTime(),
            new ProfileData("pid", "author", "bio", "image", false));
    ArticleData data = articleData("comment-article-slug");
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<CommentData>getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById("article-id", current)).thenReturn(Optional.of(data));

    DataFetcherResult<io.spring.graphql.types.Article> result =
        articleDatafetcher.getCommentArticle(dfe);

    assertEquals("comment-article-slug", result.getData().getSlug());
    assertSame(data, ((Map<?, ?>) result.getLocalContext()).get("comment-article-slug"));
  }

  @Test
  void getCommentArticle_throws_not_found_when_missing() {
    authenticate();
    CommentData comment =
        new CommentData(
            "comment-id",
            "body",
            "article-id",
            new DateTime(),
            new DateTime(),
            new ProfileData("pid", "author", "bio", "image", false));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<CommentData>getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById(eq("article-id"), any())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleDatafetcher.getCommentArticle(dfe));
  }

  // ---------------------------------------------------------------------------
  // findArticleBySlug
  // ---------------------------------------------------------------------------

  @Test
  void findArticleBySlug_returns_data_and_local_context() {
    User current = authenticate();
    ArticleData data = articleData("by-slug");
    when(articleQueryService.findBySlug("by-slug", current)).thenReturn(Optional.of(data));

    DataFetcherResult<io.spring.graphql.types.Article> result =
        articleDatafetcher.findArticleBySlug("by-slug");

    assertEquals("by-slug", result.getData().getSlug());
    assertSame(data, ((Map<?, ?>) result.getLocalContext()).get("by-slug"));
  }

  @Test
  void findArticleBySlug_uses_null_user_when_anonymous() {
    anonymous();
    ArticleData data = articleData("anon-slug");
    when(articleQueryService.findBySlug(eq("anon-slug"), isNull())).thenReturn(Optional.of(data));

    DataFetcherResult<io.spring.graphql.types.Article> result =
        articleDatafetcher.findArticleBySlug("anon-slug");

    assertEquals("anon-slug", result.getData().getSlug());
    verify(articleQueryService).findBySlug(eq("anon-slug"), isNull());
  }

  @Test
  void findArticleBySlug_throws_not_found_when_missing() {
    authenticate();
    when(articleQueryService.findBySlug(eq("missing"), any())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleDatafetcher.findArticleBySlug("missing"));
  }
}
