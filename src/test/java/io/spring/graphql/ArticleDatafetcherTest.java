package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ArticleDatafetcherTest {

  @Mock private ArticleQueryService articleQueryService;
  @Mock private UserRepository userRepository;
  @Mock private DataFetchingEnvironment dfe;

  private DgsDataFetchingEnvironment dgsDfe;

  @Captor private ArgumentCaptor<CursorPageParameter<DateTime>> pageCaptor;

  private ArticleDatafetcher datafetcher;
  private User currentUser;
  private ArticleData articleData;

  @BeforeEach
  public void setUp() {
    datafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    dgsDfe = new DgsDataFetchingEnvironment(dfe);
    currentUser = new User("john@example.com", "john", "123", "bio", "image");
    articleData =
        new ArticleData(
            "article-id",
            "test-title",
            "Test Title",
            "a description",
            "a body",
            true,
            3,
            new DateTime(2020, 1, 1, 0, 0, org.joda.time.DateTimeZone.UTC),
            new DateTime(2020, 1, 2, 0, 0, org.joda.time.DateTimeZone.UTC),
            Arrays.asList("java", "spring"),
            null);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void loginAs(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
  }

  private void loginAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  private CursorPager<ArticleData> pagerOf(ArticleData... data) {
    return new CursorPager<>(Arrays.asList(data), Direction.NEXT, true);
  }

  private void assertSingleArticleConnection(DataFetcherResult<ArticlesConnection> result) {
    ArticlesConnection connection = result.getData();
    assertThat(connection.getEdges()).hasSize(1);
    assertThat(connection.getEdges().get(0).getCursor())
        .isEqualTo(articleData.getCursor().toString());
    Article node = connection.getEdges().get(0).getNode();
    assertThat(node.getSlug()).isEqualTo("test-title");
    assertThat(node.getTitle()).isEqualTo("Test Title");
    assertThat(node.getBody()).isEqualTo("a body");
    assertThat(node.getDescription()).isEqualTo("a description");
    assertThat(node.getFavorited()).isTrue();
    assertThat(node.getFavoritesCount()).isEqualTo(3);
    assertThat(node.getTagList()).containsExactly("java", "spring");
    assertThat(node.getCreatedAt()).isEqualTo("2020-01-01T00:00:00.000Z");
    assertThat(node.getUpdatedAt()).isEqualTo("2020-01-02T00:00:00.000Z");
    assertThat(connection.getPageInfo().isHasNextPage()).isTrue();
    assertThat(connection.getPageInfo().isHasPreviousPage()).isFalse();
    assertThat(connection.getPageInfo().getStartCursor().getValue())
        .isEqualTo(articleData.getCursor().toString());
    assertThat(connection.getPageInfo().getEndCursor().getValue())
        .isEqualTo(articleData.getCursor().toString());

    @SuppressWarnings("unchecked")
    Map<String, ArticleData> localContext = (Map<String, ArticleData>) result.getLocalContext();
    assertThat(localContext).containsEntry("test-title", articleData);
  }

  @Test
  public void should_get_feed_forward_for_current_user() {
    loginAs(currentUser);
    when(articleQueryService.findUserFeedWithCursor(eq(currentUser), any()))
        .thenReturn(pagerOf(articleData));

    DataFetcherResult<ArticlesConnection> result =
        datafetcher.getFeed(10, "1000", null, null, dgsDfe);

    assertSingleArticleConnection(result);
    org.mockito.Mockito.verify(articleQueryService)
        .findUserFeedWithCursor(eq(currentUser), pageCaptor.capture());
    assertThat(pageCaptor.getValue().getDirection()).isEqualTo(Direction.NEXT);
    assertThat(pageCaptor.getValue().getLimit()).isEqualTo(10);
    assertThat(pageCaptor.getValue().getCursor().getMillis()).isEqualTo(1000L);
  }

  @Test
  public void should_get_feed_backward_for_anonymous_user() {
    loginAnonymous();
    when(articleQueryService.findUserFeedWithCursor(isNull(), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.PREV, true));

    DataFetcherResult<ArticlesConnection> result =
        datafetcher.getFeed(null, null, 5, "2000", dgsDfe);

    assertThat(result.getData().getEdges()).isEmpty();
    assertThat(result.getData().getPageInfo().getStartCursor()).isNull();
    assertThat(result.getData().getPageInfo().getEndCursor()).isNull();
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isTrue();
    assertThat(result.getData().getPageInfo().isHasNextPage()).isFalse();
    org.mockito.Mockito.verify(articleQueryService)
        .findUserFeedWithCursor(isNull(), pageCaptor.capture());
    assertThat(pageCaptor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(pageCaptor.getValue().getLimit()).isEqualTo(5);
    assertThat(pageCaptor.getValue().getCursor().getMillis()).isEqualTo(2000L);
  }

  @Test
  public void should_reject_feed_without_first_and_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> datafetcher.getFeed(null, null, null, null, dgsDfe));
  }

  @Test
  public void should_get_user_feed_forward() {
    when(dfe.<Profile>getSource()).thenReturn(Profile.newBuilder().username("john").build());
    when(userRepository.findByUsername("john")).thenReturn(Optional.of(currentUser));
    when(articleQueryService.findUserFeedWithCursor(eq(currentUser), any()))
        .thenReturn(pagerOf(articleData));

    assertSingleArticleConnection(datafetcher.userFeed(10, null, null, null, dgsDfe));
  }

  @Test
  public void should_get_user_feed_backward() {
    when(dfe.<Profile>getSource()).thenReturn(Profile.newBuilder().username("john").build());
    when(userRepository.findByUsername("john")).thenReturn(Optional.of(currentUser));
    when(articleQueryService.findUserFeedWithCursor(eq(currentUser), any()))
        .thenReturn(pagerOf(articleData));

    assertSingleArticleConnection(datafetcher.userFeed(null, null, 10, "3000", dgsDfe));
    org.mockito.Mockito.verify(articleQueryService)
        .findUserFeedWithCursor(eq(currentUser), pageCaptor.capture());
    assertThat(pageCaptor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(pageCaptor.getValue().getCursor().getMillis()).isEqualTo(3000L);
  }

  @Test
  public void should_throw_not_found_when_user_feed_target_missing() {
    when(dfe.<Profile>getSource()).thenReturn(Profile.newBuilder().username("ghost").build());
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> datafetcher.userFeed(10, null, null, null, dgsDfe));
  }

  @Test
  public void should_reject_user_feed_without_first_and_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> datafetcher.userFeed(null, null, null, null, dgsDfe));
  }

  @Test
  public void should_get_user_favorites_forward() {
    loginAs(currentUser);
    when(dfe.<Profile>getSource()).thenReturn(Profile.newBuilder().username("john").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("john"), any(), eq(currentUser)))
        .thenReturn(pagerOf(articleData));

    assertSingleArticleConnection(datafetcher.userFavorites(10, "500", null, null, dgsDfe));
    org.mockito.Mockito.verify(articleQueryService)
        .findRecentArticlesWithCursor(
            isNull(), isNull(), eq("john"), pageCaptor.capture(), eq(currentUser));
    assertThat(pageCaptor.getValue().getDirection()).isEqualTo(Direction.NEXT);
    assertThat(pageCaptor.getValue().getCursor().getMillis()).isEqualTo(500L);
  }

  @Test
  public void should_get_user_favorites_backward_for_anonymous_user() {
    loginAnonymous();
    when(dfe.<Profile>getSource()).thenReturn(Profile.newBuilder().username("john").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("john"), any(), isNull()))
        .thenReturn(pagerOf(articleData));

    assertSingleArticleConnection(datafetcher.userFavorites(null, null, 10, null, dgsDfe));
    org.mockito.Mockito.verify(articleQueryService)
        .findRecentArticlesWithCursor(
            isNull(), isNull(), eq("john"), pageCaptor.capture(), isNull());
    assertThat(pageCaptor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(pageCaptor.getValue().getCursor()).isNull();
  }

  @Test
  public void should_reject_user_favorites_without_first_and_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> datafetcher.userFavorites(null, null, null, null, dgsDfe));
  }

  @Test
  public void should_get_user_articles_forward() {
    loginAs(currentUser);
    when(dfe.<Profile>getSource()).thenReturn(Profile.newBuilder().username("john").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("john"), isNull(), any(), eq(currentUser)))
        .thenReturn(pagerOf(articleData));

    assertSingleArticleConnection(datafetcher.userArticles(10, null, null, null, dgsDfe));
  }

  @Test
  public void should_get_user_articles_backward() {
    loginAnonymous();
    when(dfe.<Profile>getSource()).thenReturn(Profile.newBuilder().username("john").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("john"), isNull(), any(), isNull()))
        .thenReturn(pagerOf(articleData));

    assertSingleArticleConnection(datafetcher.userArticles(null, null, 7, "900", dgsDfe));
    org.mockito.Mockito.verify(articleQueryService)
        .findRecentArticlesWithCursor(
            isNull(), eq("john"), isNull(), pageCaptor.capture(), isNull());
    assertThat(pageCaptor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(pageCaptor.getValue().getLimit()).isEqualTo(7);
    assertThat(pageCaptor.getValue().getCursor().getMillis()).isEqualTo(900L);
  }

  @Test
  public void should_reject_user_articles_without_first_and_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> datafetcher.userArticles(null, null, null, null, dgsDfe));
  }

  @Test
  public void should_get_articles_forward_with_filters() {
    loginAs(currentUser);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), eq("john"), eq("jane"), any(), eq(currentUser)))
        .thenReturn(pagerOf(articleData));

    assertSingleArticleConnection(
        datafetcher.getArticles(10, "100", null, null, "john", "jane", "java", dgsDfe));
  }

  @Test
  public void should_get_articles_backward_with_filters() {
    loginAnonymous();
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), isNull(), isNull(), any(), isNull()))
        .thenReturn(pagerOf(articleData));

    assertSingleArticleConnection(
        datafetcher.getArticles(null, null, 3, "800", null, null, "java", dgsDfe));
    org.mockito.Mockito.verify(articleQueryService)
        .findRecentArticlesWithCursor(
            eq("java"), isNull(), isNull(), pageCaptor.capture(), isNull());
    assertThat(pageCaptor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(pageCaptor.getValue().getLimit()).isEqualTo(3);
  }

  @Test
  public void should_reject_articles_without_first_and_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> datafetcher.getArticles(null, null, null, null, null, null, null, dgsDfe));
  }

  @Test
  public void should_get_article_from_payload_local_context() {
    loginAs(currentUser);
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "Test Title", "a description", "a body", Arrays.asList("java"), currentUser.getId());
    when(dfe.<io.spring.core.article.Article>getLocalContext()).thenReturn(article);
    when(articleQueryService.findById(article.getId(), currentUser))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = datafetcher.getArticle(dfe);

    assertThat(result.getData().getSlug()).isEqualTo("test-title");
    assertThat(result.getData().getTitle()).isEqualTo("Test Title");
    @SuppressWarnings("unchecked")
    Map<String, ArticleData> localContext = (Map<String, ArticleData>) result.getLocalContext();
    assertThat(localContext).containsEntry("test-title", articleData);
  }

  @Test
  public void should_throw_not_found_when_payload_article_missing() {
    loginAnonymous();
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "Test Title", "a description", "a body", Arrays.asList("java"), "user-id");
    when(dfe.<io.spring.core.article.Article>getLocalContext()).thenReturn(article);
    when(articleQueryService.findById(article.getId(), null)).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> datafetcher.getArticle(dfe));
  }

  @Test
  public void should_get_article_of_comment() {
    loginAs(currentUser);
    CommentData comment =
        new CommentData("comment-id", "body", "article-id", new DateTime(), new DateTime(), null);
    when(dfe.<CommentData>getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById("article-id", currentUser))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = datafetcher.getCommentArticle(dfe);

    assertThat(result.getData().getSlug()).isEqualTo("test-title");
    @SuppressWarnings("unchecked")
    Map<String, ArticleData> localContext = (Map<String, ArticleData>) result.getLocalContext();
    assertThat(localContext).containsEntry("test-title", articleData);
  }

  @Test
  public void should_throw_not_found_when_comment_article_missing() {
    loginAnonymous();
    CommentData comment =
        new CommentData("comment-id", "body", "article-id", new DateTime(), new DateTime(), null);
    when(dfe.<CommentData>getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById("article-id", null)).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> datafetcher.getCommentArticle(dfe));
  }

  @Test
  public void should_find_article_by_slug() {
    loginAs(currentUser);
    when(articleQueryService.findBySlug("test-title", currentUser))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = datafetcher.findArticleBySlug("test-title");

    assertThat(result.getData().getSlug()).isEqualTo("test-title");
    assertThat(result.getData().getFavoritesCount()).isEqualTo(3);
    @SuppressWarnings("unchecked")
    Map<String, ArticleData> localContext = (Map<String, ArticleData>) result.getLocalContext();
    assertThat(localContext).containsEntry("test-title", articleData);
  }

  @Test
  public void should_find_article_by_slug_for_anonymous_user() {
    loginAnonymous();
    when(articleQueryService.findBySlug("test-title", null)).thenReturn(Optional.of(articleData));

    assertThat(datafetcher.findArticleBySlug("test-title").getData().getSlug())
        .isEqualTo("test-title");
  }

  @Test
  public void should_throw_not_found_when_slug_missing() {
    loginAnonymous();
    when(articleQueryService.findBySlug("missing", null)).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> datafetcher.findArticleBySlug("missing"));
  }
}
