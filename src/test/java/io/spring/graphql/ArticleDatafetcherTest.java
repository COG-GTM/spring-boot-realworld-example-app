package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.ArticlesConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

  private ArticleDatafetcher articleDatafetcher;

  private User currentUser;

  @BeforeEach
  void setUp() {
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    currentUser = new User("current@example.com", "current", "123", "bio", "image");
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAs(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, AuthorityUtils.NO_AUTHORITIES));
  }

  private void authenticateAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  private ArticleData articleData(String id, String slug) {
    DateTime now = new DateTime();
    return new ArticleData(
        id,
        slug,
        "title-" + slug,
        "description",
        "body",
        true,
        3,
        now,
        now,
        Arrays.asList("java", "spring"),
        null);
  }

  private CursorPager<ArticleData> pagerWith(ArticleData... data) {
    return new CursorPager<>(Arrays.asList(data), Direction.NEXT, true);
  }

  private CursorPager<ArticleData> emptyPager() {
    return new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);
  }

  // ---------------------------------------------------------------------------
  // getFeed (QUERY.Feed)
  // ---------------------------------------------------------------------------

  @Test
  void getFeed_withFirst_resolvesNextPage() {
    authenticateAs(currentUser);
    DataFetchingEnvironment delegate = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);
    when(articleQueryService.findUserFeedWithCursor(eq(currentUser), any()))
        .thenReturn(pagerWith(articleData("1", "slug-1"), articleData("2", "slug-2")));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, dfe);

    ArticlesConnection connection = result.getData();
    assertThat(connection.getEdges()).hasSize(2);
    assertThat(connection.getEdges().get(0).getNode().getSlug()).isEqualTo("slug-1");
    assertThat(connection.getPageInfo().isHasNextPage()).isTrue();

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService).findUserFeedWithCursor(eq(currentUser), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.NEXT);
    assertThat(captor.getValue().getLimit()).isEqualTo(10);
  }

  @Test
  void getFeed_withLast_resolvesPrevPage() {
    authenticateAnonymous();
    DataFetchingEnvironment delegate = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);
    when(articleQueryService.findUserFeedWithCursor(isNull(), any()))
        .thenReturn(
            new CursorPager<>(Arrays.asList(articleData("1", "slug-1")), Direction.PREV, true));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(null, null, 5, "1000", dfe);

    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isTrue();

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService).findUserFeedWithCursor(isNull(), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getCursor()).isNotNull();
  }

  @Test
  void getFeed_withNeitherFirstNorLast_throws() {
    authenticateAnonymous();
    DataFetchingEnvironment delegate = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);
    assertThatThrownBy(() -> articleDatafetcher.getFeed(null, null, null, null, dfe))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ---------------------------------------------------------------------------
  // userFeed (PROFILE.Feed)
  // ---------------------------------------------------------------------------

  @Test
  void userFeed_withFirst_resolvesForProfileUser() {
    io.spring.graphql.types.Profile profile =
        io.spring.graphql.types.Profile.newBuilder().username("target").build();
    User target = new User("target@example.com", "target", "123", "", "");
    DataFetchingEnvironment delegate = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);
    when(delegate.getSource()).thenReturn(profile);
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(articleQueryService.findUserFeedWithCursor(eq(target), any()))
        .thenReturn(pagerWith(articleData("1", "slug-1")));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(10, null, null, null, dfe);

    assertThat(result.getData().getEdges()).hasSize(1);
    verify(articleQueryService).findUserFeedWithCursor(eq(target), any());
  }

  @Test
  void userFeed_withLast_resolvesPrevPage() {
    io.spring.graphql.types.Profile profile =
        io.spring.graphql.types.Profile.newBuilder().username("target").build();
    User target = new User("target@example.com", "target", "123", "", "");
    DataFetchingEnvironment delegate = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);
    when(delegate.getSource()).thenReturn(profile);
    when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
    when(articleQueryService.findUserFeedWithCursor(eq(target), any())).thenReturn(emptyPager());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(null, null, 5, "2000", dfe);

    assertThat(result.getData().getEdges()).isEmpty();
  }

  @Test
  void userFeed_whenProfileUserMissing_throwsNotFound() {
    io.spring.graphql.types.Profile profile =
        io.spring.graphql.types.Profile.newBuilder().username("ghost").build();
    DataFetchingEnvironment delegate = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);
    when(delegate.getSource()).thenReturn(profile);
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> articleDatafetcher.userFeed(10, null, null, null, dfe))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void userFeed_withNeitherFirstNorLast_throws() {
    authenticateAnonymous();
    DataFetchingEnvironment delegate = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);
    assertThatThrownBy(() -> articleDatafetcher.userFeed(null, null, null, null, dfe))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ---------------------------------------------------------------------------
  // userFavorites (PROFILE.Favorites)
  // ---------------------------------------------------------------------------

  @Test
  void userFavorites_withFirst_queriesByFavoritedBy() {
    authenticateAs(currentUser);
    io.spring.graphql.types.Profile profile =
        io.spring.graphql.types.Profile.newBuilder().username("target").build();
    DataFetchingEnvironment delegate = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);
    when(delegate.getSource()).thenReturn(profile);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("target"), any(), eq(currentUser)))
        .thenReturn(pagerWith(articleData("1", "slug-1")));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(10, null, null, null, dfe);

    assertThat(result.getData().getEdges()).hasSize(1);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), isNull(), eq("target"), any(), eq(currentUser));
  }

  @Test
  void userFavorites_withLast_resolvesPrevPage() {
    authenticateAnonymous();
    io.spring.graphql.types.Profile profile =
        io.spring.graphql.types.Profile.newBuilder().username("target").build();
    DataFetchingEnvironment delegate = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);
    when(delegate.getSource()).thenReturn(profile);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("target"), any(), isNull()))
        .thenReturn(emptyPager());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(null, null, 5, "1000", dfe);

    assertThat(result.getData().getEdges()).isEmpty();
  }

  @Test
  void userFavorites_withNeitherFirstNorLast_throws() {
    authenticateAnonymous();
    DataFetchingEnvironment delegate = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);
    assertThatThrownBy(() -> articleDatafetcher.userFavorites(null, null, null, null, dfe))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ---------------------------------------------------------------------------
  // userArticles (PROFILE.Articles)
  // ---------------------------------------------------------------------------

  @Test
  void userArticles_withFirst_queriesByAuthoredBy() {
    authenticateAs(currentUser);
    io.spring.graphql.types.Profile profile =
        io.spring.graphql.types.Profile.newBuilder().username("target").build();
    DataFetchingEnvironment delegate = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);
    when(delegate.getSource()).thenReturn(profile);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("target"), isNull(), any(), eq(currentUser)))
        .thenReturn(pagerWith(articleData("1", "slug-1"), articleData("2", "slug-2")));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(10, null, null, null, dfe);

    assertThat(result.getData().getEdges()).hasSize(2);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), eq("target"), isNull(), any(), eq(currentUser));
  }

  @Test
  void userArticles_withLast_resolvesPrevPage() {
    authenticateAnonymous();
    io.spring.graphql.types.Profile profile =
        io.spring.graphql.types.Profile.newBuilder().username("target").build();
    DataFetchingEnvironment delegate = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);
    when(delegate.getSource()).thenReturn(profile);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("target"), isNull(), any(), isNull()))
        .thenReturn(emptyPager());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(null, null, 5, "1000", dfe);

    assertThat(result.getData().getEdges()).isEmpty();
  }

  @Test
  void userArticles_withNeitherFirstNorLast_throws() {
    authenticateAnonymous();
    DataFetchingEnvironment delegate = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);
    assertThatThrownBy(() -> articleDatafetcher.userArticles(null, null, null, null, dfe))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ---------------------------------------------------------------------------
  // getArticles (QUERY.Articles)
  // ---------------------------------------------------------------------------

  @Test
  void getArticles_withFirst_passesAllFilters() {
    authenticateAs(currentUser);
    DataFetchingEnvironment delegate = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("tag"), eq("author"), eq("favoriter"), any(), eq(currentUser)))
        .thenReturn(pagerWith(articleData("1", "slug-1")));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, "author", "favoriter", "tag", dfe);

    assertThat(result.getData().getEdges()).hasSize(1);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(
            eq("tag"), eq("author"), eq("favoriter"), any(), eq(currentUser));
  }

  @Test
  void getArticles_withLast_resolvesPrevPage() {
    authenticateAnonymous();
    DataFetchingEnvironment delegate = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), isNull()))
        .thenReturn(emptyPager());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(null, null, 5, "1000", null, null, null, dfe);

    assertThat(result.getData().getEdges()).isEmpty();
  }

  @Test
  void getArticles_withNeitherFirstNorLast_throws() {
    authenticateAnonymous();
    DataFetchingEnvironment delegate = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);
    assertThatThrownBy(
            () -> articleDatafetcher.getArticles(null, null, null, null, null, null, null, dfe))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ---------------------------------------------------------------------------
  // getArticle (ARTICLEPAYLOAD.Article)
  // ---------------------------------------------------------------------------

  @Test
  void getArticle_resolvesFromLocalContext() {
    authenticateAs(currentUser);
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article(
            "Title", "desc", "body", Arrays.asList("java"), currentUser.getId());
    ArticleData data = articleData(coreArticle.getId(), "title");
    DataFetchingEnvironment dfe = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    when(dfe.<io.spring.core.article.Article>getLocalContext()).thenReturn(coreArticle);
    when(articleQueryService.findById(coreArticle.getId(), currentUser))
        .thenReturn(Optional.of(data));

    DataFetcherResult<Article> result = articleDatafetcher.getArticle(dfe);

    assertThat(result.getData().getSlug()).isEqualTo("title");
    assertThat(result.getData().getFavoritesCount()).isEqualTo(3);
  }

  @Test
  void getArticle_whenMissing_throwsNotFound() {
    authenticateAs(currentUser);
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article(
            "Title", "desc", "body", Arrays.asList("java"), currentUser.getId());
    DataFetchingEnvironment dfe = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    when(dfe.<io.spring.core.article.Article>getLocalContext()).thenReturn(coreArticle);
    when(articleQueryService.findById(coreArticle.getId(), currentUser))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> articleDatafetcher.getArticle(dfe))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  // ---------------------------------------------------------------------------
  // getCommentArticle (COMMENT.Article)
  // ---------------------------------------------------------------------------

  @Test
  void getCommentArticle_resolvesFromCommentLocalContext() {
    authenticateAnonymous();
    CommentData comment =
        new CommentData("c1", "body", "article-1", new DateTime(), new DateTime(), null);
    ArticleData data = articleData("article-1", "the-slug");
    DataFetchingEnvironment dfe = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    when(dfe.<CommentData>getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById(eq("article-1"), isNull())).thenReturn(Optional.of(data));

    DataFetcherResult<Article> result = articleDatafetcher.getCommentArticle(dfe);

    assertThat(result.getData().getSlug()).isEqualTo("the-slug");
  }

  @Test
  void getCommentArticle_whenMissing_throwsNotFound() {
    authenticateAnonymous();
    CommentData comment =
        new CommentData("c1", "body", "article-1", new DateTime(), new DateTime(), null);
    DataFetchingEnvironment dfe = org.mockito.Mockito.mock(DataFetchingEnvironment.class);
    when(dfe.<CommentData>getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById(eq("article-1"), isNull())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> articleDatafetcher.getCommentArticle(dfe))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  // ---------------------------------------------------------------------------
  // findArticleBySlug (QUERY.Article)
  // ---------------------------------------------------------------------------

  @Test
  void findArticleBySlug_resolvesArticle() {
    authenticateAs(currentUser);
    ArticleData data = articleData("1", "my-slug");
    when(articleQueryService.findBySlug("my-slug", currentUser)).thenReturn(Optional.of(data));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("my-slug");

    assertThat(result.getData().getSlug()).isEqualTo("my-slug");
    assertThat(result.getData().getTitle()).isEqualTo("title-my-slug");
    assertThat(result.getData().getFavorited()).isTrue();
  }

  @Test
  void findArticleBySlug_whenMissing_throwsNotFound() {
    authenticateAnonymous();
    when(articleQueryService.findBySlug(eq("missing"), isNull())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> articleDatafetcher.findArticleBySlug("missing"))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
