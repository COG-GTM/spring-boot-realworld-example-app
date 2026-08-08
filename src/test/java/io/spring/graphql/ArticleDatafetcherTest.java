package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
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
import io.spring.graphql.types.Profile;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ArticleDatafetcherTest {

  @Mock private ArticleQueryService articleQueryService;
  @Mock private UserRepository userRepository;
  @Mock private DataFetchingEnvironment delegate;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private ArticleDatafetcher articleDatafetcher;
  private DgsDataFetchingEnvironment dgsDataFetchingEnvironment;
  private User user;

  @BeforeEach
  public void setUp() {
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    dgsDataFetchingEnvironment = new DgsDataFetchingEnvironment(delegate);
    user = new User("john@example.com", "john", "123", "bio", "image");
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void loginAs(User loggedIn) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(loggedIn, null, Collections.emptyList()));
  }

  private void loginAsAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  private ArticleData articleData(String slug, long updatedAtMillis) {
    return new ArticleData(
        "article-id-" + slug,
        slug,
        "Title of " + slug,
        "description of " + slug,
        "body of " + slug,
        true,
        3,
        new DateTime(updatedAtMillis, DateTimeZone.UTC),
        new DateTime(updatedAtMillis, DateTimeZone.UTC),
        Arrays.asList("java", "spring"),
        null);
  }

  private CursorPager<ArticleData> pagerOf(Direction direction, ArticleData... data) {
    return new CursorPager<>(Arrays.asList(data), direction, true);
  }

  private CursorPageParameter<DateTime> capturedFeedPageParameter(User expectedUser) {
    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService).findUserFeedWithCursor(eq(expectedUser), captor.capture());
    return captor.getValue();
  }

  private CursorPageParameter<DateTime> capturedRecentPageParameter() {
    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(articleQueryService)
        .findRecentArticlesWithCursor(any(), any(), any(), captor.capture(), any());
    return captor.getValue();
  }

  @Test
  public void should_throw_when_neither_first_nor_last_given_for_feed() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> articleDatafetcher.getFeed(null, null, null, null, dgsDataFetchingEnvironment));
  }

  @Test
  public void should_get_feed_forward_for_current_user() {
    loginAs(user);
    ArticleData article = articleData("slug-one", 1000L);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pagerOf(Direction.NEXT, article));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, "500", null, null, dgsDataFetchingEnvironment);

    CursorPageParameter<DateTime> pageParameter = capturedFeedPageParameter(user);
    assertThat(pageParameter.getDirection()).isEqualTo(Direction.NEXT);
    assertThat(pageParameter.getLimit()).isEqualTo(10);
    assertThat(pageParameter.getCursor().getMillis()).isEqualTo(500L);

    ArticlesConnection connection = result.getData();
    assertThat(connection.getEdges()).hasSize(1);
    assertThat(connection.getEdges().get(0).getCursor()).isEqualTo("1000");
    assertThat(connection.getEdges().get(0).getNode().getSlug()).isEqualTo("slug-one");
    assertThat(connection.getEdges().get(0).getNode().getTitle()).isEqualTo("Title of slug-one");
    assertThat(connection.getEdges().get(0).getNode().getFavorited()).isTrue();
    assertThat(connection.getEdges().get(0).getNode().getFavoritesCount()).isEqualTo(3);
    assertThat(connection.getEdges().get(0).getNode().getTagList())
        .containsExactly("java", "spring");
    assertThat(connection.getPageInfo().isHasNextPage()).isTrue();
    assertThat(connection.getPageInfo().isHasPreviousPage()).isFalse();
    assertThat(connection.getPageInfo().getStartCursor().getValue()).isEqualTo("1000");

    Map<String, ArticleData> localContext = (Map<String, ArticleData>) result.getLocalContext();
    assertThat(localContext).containsOnlyKeys("slug-one");
    assertThat(localContext.get("slug-one")).isSameAs(article);
  }

  @Test
  public void should_get_feed_backward_for_anonymous_user() {
    loginAsAnonymous();
    when(articleQueryService.findUserFeedWithCursor(isNull(), any()))
        .thenReturn(pagerOf(Direction.PREV, articleData("slug-two", 2000L)));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(null, null, 5, "3000", dgsDataFetchingEnvironment);

    CursorPageParameter<DateTime> pageParameter = capturedFeedPageParameter(null);
    assertThat(pageParameter.getDirection()).isEqualTo(Direction.PREV);
    assertThat(pageParameter.getLimit()).isEqualTo(5);
    assertThat(pageParameter.getCursor().getMillis()).isEqualTo(3000L);

    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getPageInfo().isHasNextPage()).isFalse();
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isTrue();
    assertThat(result.getData().getPageInfo().getEndCursor().getValue()).isEqualTo("2000");
  }

  @Test
  public void should_throw_when_neither_first_nor_last_given_for_user_feed() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> articleDatafetcher.userFeed(null, null, null, null, dgsDataFetchingEnvironment));
  }

  @Test
  public void should_get_user_feed_forward() {
    Profile profile = Profile.newBuilder().username("john").build();
    when(delegate.getSource()).thenReturn(profile);
    when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pagerOf(Direction.NEXT, articleData("slug-three", 4000L)));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(2, null, null, null, dgsDataFetchingEnvironment);

    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getEdges().get(0).getNode().getSlug()).isEqualTo("slug-three");
    assertThat((Map<String, ArticleData>) result.getLocalContext()).containsOnlyKeys("slug-three");
  }

  @Test
  public void should_get_user_feed_backward() {
    Profile profile = Profile.newBuilder().username("john").build();
    when(delegate.getSource()).thenReturn(profile);
    when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pagerOf(Direction.PREV, articleData("slug-four", 5000L)));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(null, null, 2, "6000", dgsDataFetchingEnvironment);

    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isTrue();
    assertThat(result.getData().getEdges().get(0).getCursor()).isEqualTo("5000");
  }

  @Test
  public void should_throw_not_found_when_user_feed_owner_missing() {
    Profile profile = Profile.newBuilder().username("ghost").build();
    when(delegate.getSource()).thenReturn(profile);
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(
            () -> articleDatafetcher.userFeed(1, null, null, null, dgsDataFetchingEnvironment));
  }

  @Test
  public void should_throw_when_neither_first_nor_last_given_for_user_favorites() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                articleDatafetcher.userFavorites(
                    null, null, null, null, dgsDataFetchingEnvironment));
  }

  @Test
  public void should_get_user_favorites_forward() {
    loginAs(user);
    Profile profile = Profile.newBuilder().username("john").build();
    when(delegate.getSource()).thenReturn(profile);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("john"), any(), eq(user)))
        .thenReturn(pagerOf(Direction.NEXT, articleData("fav-one", 7000L)));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(3, "100", null, null, dgsDataFetchingEnvironment);

    CursorPageParameter<DateTime> pageParameter = capturedRecentPageParameter();
    assertThat(pageParameter.getDirection()).isEqualTo(Direction.NEXT);
    assertThat(pageParameter.getLimit()).isEqualTo(3);
    assertThat(pageParameter.getCursor().getMillis()).isEqualTo(100L);
    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getEdges().get(0).getNode().getSlug()).isEqualTo("fav-one");
    assertThat(result.getData().getPageInfo().isHasNextPage()).isTrue();
  }

  @Test
  public void should_get_user_favorites_backward_for_anonymous_user() {
    loginAsAnonymous();
    Profile profile = Profile.newBuilder().username("john").build();
    when(delegate.getSource()).thenReturn(profile);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("john"), any(), isNull()))
        .thenReturn(pagerOf(Direction.PREV, articleData("fav-two", 8000L)));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(null, null, 3, "9000", dgsDataFetchingEnvironment);

    CursorPageParameter<DateTime> pageParameter = capturedRecentPageParameter();
    assertThat(pageParameter.getDirection()).isEqualTo(Direction.PREV);
    assertThat(pageParameter.getCursor().getMillis()).isEqualTo(9000L);
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isTrue();
    assertThat((Map<String, ArticleData>) result.getLocalContext()).containsOnlyKeys("fav-two");
  }

  @Test
  public void should_throw_when_neither_first_nor_last_given_for_user_articles() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                articleDatafetcher.userArticles(
                    null, null, null, null, dgsDataFetchingEnvironment));
  }

  @Test
  public void should_get_user_articles_forward() {
    loginAs(user);
    Profile profile = Profile.newBuilder().username("john").build();
    when(delegate.getSource()).thenReturn(profile);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("john"), isNull(), any(), eq(user)))
        .thenReturn(pagerOf(Direction.NEXT, articleData("own-one", 10000L)));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(4, null, null, null, dgsDataFetchingEnvironment);

    CursorPageParameter<DateTime> pageParameter = capturedRecentPageParameter();
    assertThat(pageParameter.getDirection()).isEqualTo(Direction.NEXT);
    assertThat(pageParameter.getLimit()).isEqualTo(4);
    assertThat(pageParameter.getCursor()).isNull();
    assertThat(result.getData().getEdges().get(0).getNode().getSlug()).isEqualTo("own-one");
    assertThat(result.getData().getEdges().get(0).getCursor()).isEqualTo("10000");
  }

  @Test
  public void should_get_user_articles_backward() {
    loginAs(user);
    Profile profile = Profile.newBuilder().username("john").build();
    when(delegate.getSource()).thenReturn(profile);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("john"), isNull(), any(), eq(user)))
        .thenReturn(pagerOf(Direction.PREV, articleData("own-two", 11000L)));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(null, null, 4, "12000", dgsDataFetchingEnvironment);

    CursorPageParameter<DateTime> pageParameter = capturedRecentPageParameter();
    assertThat(pageParameter.getDirection()).isEqualTo(Direction.PREV);
    assertThat(pageParameter.getCursor().getMillis()).isEqualTo(12000L);
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isTrue();
    assertThat(result.getData().getPageInfo().isHasNextPage()).isFalse();
  }

  @Test
  public void should_throw_when_neither_first_nor_last_given_for_articles() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                articleDatafetcher.getArticles(
                    null, null, null, null, null, null, null, dgsDataFetchingEnvironment));
  }

  @Test
  public void should_get_articles_forward_with_filters() {
    loginAs(user);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), eq("john"), eq("jane"), any(), eq(user)))
        .thenReturn(
            pagerOf(
                Direction.NEXT, articleData("filtered-one", 13000L), articleData("f2", 14000L)));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(
            2, "12000", null, null, "john", "jane", "java", dgsDataFetchingEnvironment);

    CursorPageParameter<DateTime> pageParameter = capturedRecentPageParameter();
    assertThat(pageParameter.getDirection()).isEqualTo(Direction.NEXT);
    assertThat(pageParameter.getLimit()).isEqualTo(2);
    assertThat(pageParameter.getCursor().getMillis()).isEqualTo(12000L);
    assertThat(result.getData().getEdges()).hasSize(2);
    assertThat(result.getData().getPageInfo().getStartCursor().getValue()).isEqualTo("13000");
    assertThat(result.getData().getPageInfo().getEndCursor().getValue()).isEqualTo("14000");
    assertThat((Map<String, ArticleData>) result.getLocalContext())
        .containsOnlyKeys("filtered-one", "f2");
  }

  @Test
  public void should_get_articles_backward_for_anonymous_user() {
    loginAsAnonymous();
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), isNull()))
        .thenReturn(pagerOf(Direction.PREV, articleData("back-one", 15000L)));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(
            null, null, 1, "16000", null, null, null, dgsDataFetchingEnvironment);

    CursorPageParameter<DateTime> pageParameter = capturedRecentPageParameter();
    assertThat(pageParameter.getDirection()).isEqualTo(Direction.PREV);
    assertThat(pageParameter.getLimit()).isEqualTo(1);
    assertThat(pageParameter.getCursor().getMillis()).isEqualTo(16000L);
    assertThat(result.getData().getEdges().get(0).getNode().getSlug()).isEqualTo("back-one");
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isTrue();
  }

  @Test
  public void should_report_no_next_page_when_pager_has_no_extra() {
    loginAs(user);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), eq(user)))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(articleData("last-page", 20000L)),
                Direction.NEXT,
                false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(
            5, null, null, null, null, null, null, dgsDataFetchingEnvironment);

    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getPageInfo().isHasNextPage()).isFalse();
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isFalse();
    assertThat(result.getData().getPageInfo().getStartCursor().getValue()).isEqualTo("20000");
  }

  @Test
  public void should_return_empty_connection_without_cursors() {
    loginAs(user);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), eq(user)))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(
            5, null, null, null, null, null, null, dgsDataFetchingEnvironment);

    assertThat(result.getData().getEdges()).isEmpty();
    assertThat(result.getData().getPageInfo().getStartCursor()).isNull();
    assertThat(result.getData().getPageInfo().getEndCursor()).isNull();
    assertThat((Map<String, ArticleData>) result.getLocalContext()).isEmpty();
  }

  @Test
  public void should_get_article_from_payload_local_context() {
    loginAs(user);
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "Title", "desc", "body", List.of("java"), user.getId(), new DateTime(1000L));
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(article);
    ArticleData articleData = articleData("payload-slug", 17000L);
    when(articleQueryService.findById(article.getId(), user)).thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.getArticle(dataFetchingEnvironment);

    assertThat(result.getData().getSlug()).isEqualTo("payload-slug");
    assertThat(result.getData().getBody()).isEqualTo("body of payload-slug");
    assertThat(result.getData().getDescription()).isEqualTo("description of payload-slug");
    assertThat((Map<String, Object>) result.getLocalContext())
        .containsEntry("payload-slug", articleData);
  }

  @Test
  public void should_throw_not_found_when_payload_article_missing() {
    loginAsAnonymous();
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "Title", "desc", "body", List.of("java"), "user-id", new DateTime(1000L));
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(article);
    when(articleQueryService.findById(article.getId(), null)).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleDatafetcher.getArticle(dataFetchingEnvironment));
  }

  @Test
  public void should_get_article_of_comment() {
    loginAs(user);
    CommentData comment =
        new CommentData(
            "comment-id",
            "comment body",
            "article-id",
            new DateTime(1000L),
            new DateTime(1000L),
            null);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(comment);
    ArticleData articleData = articleData("comment-article", 18000L);
    when(articleQueryService.findById("article-id", user)).thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result =
        articleDatafetcher.getCommentArticle(dataFetchingEnvironment);

    assertThat(result.getData().getSlug()).isEqualTo("comment-article");
    assertThat(result.getData().getTitle()).isEqualTo("Title of comment-article");
    assertThat((Map<String, Object>) result.getLocalContext())
        .containsEntry("comment-article", articleData);
  }

  @Test
  public void should_throw_not_found_when_comment_article_missing() {
    loginAsAnonymous();
    CommentData comment =
        new CommentData(
            "comment-id",
            "comment body",
            "missing-article",
            new DateTime(1000L),
            new DateTime(1000L),
            null);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById("missing-article", null)).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleDatafetcher.getCommentArticle(dataFetchingEnvironment));
  }

  @Test
  public void should_find_article_by_slug() {
    loginAs(user);
    ArticleData articleData = articleData("by-slug", 19000L);
    when(articleQueryService.findBySlug("by-slug", user)).thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("by-slug");

    assertThat(result.getData().getSlug()).isEqualTo("by-slug");
    assertThat(result.getData().getCreatedAt()).isEqualTo("1970-01-01T00:00:19.000Z");
    assertThat(result.getData().getUpdatedAt()).isEqualTo("1970-01-01T00:00:19.000Z");
    assertThat((Map<String, Object>) result.getLocalContext())
        .containsEntry("by-slug", articleData);
  }

  @Test
  public void should_throw_not_found_when_slug_missing() {
    loginAsAnonymous();
    when(articleQueryService.findBySlug("missing", null)).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleDatafetcher.findArticleBySlug("missing"));
  }
}
