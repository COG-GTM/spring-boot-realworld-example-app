package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.TestHelper;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.core.article.Article;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.ArticlesConnection;
import io.spring.graphql.types.Profile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleDatafetcherTest extends GraphqlTestBase {

  @Mock private ArticleQueryService articleQueryService;
  @Mock private UserRepository userRepository;

  @Captor private ArgumentCaptor<CursorPageParameter<DateTime>> pageCaptor;

  @InjectMocks private ArticleDatafetcher articleDatafetcher;

  private User user;
  private ArticleData articleData;

  @BeforeEach
  void setUp() {
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
    articleData = TestHelper.articleDataFixture("1", user);
  }

  private CursorPager<ArticleData> pagerOf(ArticleData... articles) {
    return new CursorPager<>(Arrays.asList(articles), Direction.NEXT, true);
  }

  @Test
  void should_get_feed_of_current_user_forward() {
    login(user);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pagerOf(articleData));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, "1000", null, null, dgsEnvironment);

    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getEdges().get(0).getNode().getSlug())
        .isEqualTo(articleData.getSlug());
    assertThat(result.getData().getEdges().get(0).getCursor())
        .isEqualTo(articleData.getCursor().toString());
    assertThat(result.getData().getPageInfo().isHasNextPage()).isTrue();
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isFalse();
    Map<String, ArticleData> localContext = asMap(result.getLocalContext());
    assertThat(localContext).containsEntry(articleData.getSlug(), articleData);

    verify(articleQueryService).findUserFeedWithCursor(eq(user), pageCaptor.capture());
    assertThat(pageCaptor.getValue().getDirection()).isEqualTo(Direction.NEXT);
    assertThat(pageCaptor.getValue().getLimit()).isEqualTo(10);
    assertThat(pageCaptor.getValue().getCursor().getMillis()).isEqualTo(1000L);
  }

  @Test
  void should_get_feed_of_current_user_backward() {
    login(user);
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(articleData), Direction.PREV, true));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(null, null, 5, "2000", dgsEnvironment);

    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isTrue();
    verify(articleQueryService).findUserFeedWithCursor(eq(user), pageCaptor.capture());
    assertThat(pageCaptor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(pageCaptor.getValue().getCursor().getMillis()).isEqualTo(2000L);
  }

  @Test
  void should_read_feed_as_anonymous_user() {
    logout();
    when(articleQueryService.findUserFeedWithCursor(isNull(), any()))
        .thenReturn(pagerOf(articleData));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, dgsEnvironment);

    assertThat(result.getData().getEdges()).hasSize(1);
  }

  @Test
  void should_reject_feed_without_first_or_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> articleDatafetcher.getFeed(null, null, null, null, dgsEnvironment));
  }

  @Test
  void should_get_feed_of_a_profile() {
    when(environment.getSource()).thenReturn(Profile.newBuilder().username("johnjacob").build());
    when(userRepository.findByUsername("johnjacob")).thenReturn(Optional.of(user));
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pagerOf(articleData));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(10, null, null, null, dgsEnvironment);

    assertThat(result.getData().getEdges()).hasSize(1);
  }

  @Test
  void should_get_feed_of_a_profile_backward() {
    when(environment.getSource()).thenReturn(Profile.newBuilder().username("johnjacob").build());
    when(userRepository.findByUsername("johnjacob")).thenReturn(Optional.of(user));
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pagerOf(articleData));

    articleDatafetcher.userFeed(null, null, 5, null, dgsEnvironment);

    verify(articleQueryService).findUserFeedWithCursor(eq(user), pageCaptor.capture());
    assertThat(pageCaptor.getValue().getDirection()).isEqualTo(Direction.PREV);
  }

  @Test
  void should_fail_profile_feed_when_user_not_found() {
    when(environment.getSource()).thenReturn(Profile.newBuilder().username("unknown").build());
    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleDatafetcher.userFeed(10, null, null, null, dgsEnvironment));
  }

  @Test
  void should_reject_profile_feed_without_first_or_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> articleDatafetcher.userFeed(null, null, null, null, dgsEnvironment));
  }

  @Test
  void should_get_favorites_of_a_profile() {
    login(user);
    when(environment.getSource()).thenReturn(Profile.newBuilder().username("johnjacob").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("johnjacob"), any(), eq(user)))
        .thenReturn(pagerOf(articleData));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(10, null, null, null, dgsEnvironment);

    assertThat(result.getData().getEdges()).hasSize(1);
  }

  @Test
  void should_get_favorites_of_a_profile_backward() {
    login(user);
    when(environment.getSource()).thenReturn(Profile.newBuilder().username("johnjacob").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("johnjacob"), any(), eq(user)))
        .thenReturn(pagerOf(articleData));

    articleDatafetcher.userFavorites(null, null, 5, "3000", dgsEnvironment);

    verify(articleQueryService)
        .findRecentArticlesWithCursor(
            isNull(), isNull(), eq("johnjacob"), pageCaptor.capture(), eq(user));
    assertThat(pageCaptor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(pageCaptor.getValue().getCursor().getMillis()).isEqualTo(3000L);
  }

  @Test
  void should_reject_favorites_without_first_or_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> articleDatafetcher.userFavorites(null, null, null, null, dgsEnvironment));
  }

  @Test
  void should_get_articles_of_a_profile() {
    login(user);
    when(environment.getSource()).thenReturn(Profile.newBuilder().username("johnjacob").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("johnjacob"), isNull(), any(), eq(user)))
        .thenReturn(pagerOf(articleData));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(10, null, null, null, dgsEnvironment);

    assertThat(result.getData().getEdges()).hasSize(1);
  }

  @Test
  void should_get_articles_of_a_profile_backward() {
    login(user);
    when(environment.getSource()).thenReturn(Profile.newBuilder().username("johnjacob").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("johnjacob"), isNull(), any(), eq(user)))
        .thenReturn(pagerOf(articleData));

    articleDatafetcher.userArticles(null, null, 5, null, dgsEnvironment);

    verify(articleQueryService)
        .findRecentArticlesWithCursor(
            isNull(), eq("johnjacob"), isNull(), pageCaptor.capture(), eq(user));
    assertThat(pageCaptor.getValue().getDirection()).isEqualTo(Direction.PREV);
  }

  @Test
  void should_reject_profile_articles_without_first_or_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> articleDatafetcher.userArticles(null, null, null, null, dgsEnvironment));
  }

  @Test
  void should_get_articles_with_filters() {
    login(user);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("joda"), eq("johnjacob"), eq("someone"), any(), eq(user)))
        .thenReturn(pagerOf(articleData));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(
            10, null, null, null, "johnjacob", "someone", "joda", dgsEnvironment);

    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getPageInfo().getStartCursor().getValue())
        .isEqualTo(articleData.getCursor().toString());
  }

  @Test
  void should_get_articles_backward() {
    login(user);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), eq(user)))
        .thenReturn(pagerOf(articleData));

    articleDatafetcher.getArticles(null, null, 5, "4000", null, null, null, dgsEnvironment);

    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), isNull(), isNull(), pageCaptor.capture(), eq(user));
    assertThat(pageCaptor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(pageCaptor.getValue().getCursor().getMillis()).isEqualTo(4000L);
  }

  @Test
  void should_get_empty_articles_page() {
    login(user);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), eq(user)))
        .thenReturn(new CursorPager<>(new ArrayList<>(), Direction.NEXT, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, null, null, null, dgsEnvironment);

    assertThat(result.getData().getEdges()).isEmpty();
    assertThat(result.getData().getPageInfo().getStartCursor()).isNull();
    assertThat(result.getData().getPageInfo().getEndCursor()).isNull();
    assertThat(result.getData().getPageInfo().isHasNextPage()).isFalse();
  }

  @Test
  void should_reject_articles_without_first_or_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                articleDatafetcher.getArticles(
                    null, null, null, null, null, null, null, dgsEnvironment));
  }

  @Test
  void should_get_article_of_a_payload() {
    login(user);
    Article article =
        new Article("title", "desc", "body", Arrays.asList("joda"), user.getId(), new DateTime());
    when(environment.<Article>getLocalContext()).thenReturn(article);
    when(articleQueryService.findById(article.getId(), user)).thenReturn(Optional.of(articleData));

    DataFetcherResult<io.spring.graphql.types.Article> result =
        articleDatafetcher.getArticle(environment);

    assertThat(result.getData().getSlug()).isEqualTo(articleData.getSlug());
    assertThat(result.getData().getTitle()).isEqualTo(articleData.getTitle());
    Map<String, ArticleData> localContext = asMap(result.getLocalContext());
    assertThat(localContext).containsEntry(articleData.getSlug(), articleData);
  }

  @Test
  void should_fail_article_of_a_payload_when_not_found() {
    login(user);
    Article article =
        new Article("title", "desc", "body", Arrays.asList("joda"), user.getId(), new DateTime());
    when(environment.<Article>getLocalContext()).thenReturn(article);
    when(articleQueryService.findById(article.getId(), user)).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleDatafetcher.getArticle(environment));
  }

  @Test
  void should_get_article_of_a_comment() {
    login(user);
    CommentData comment =
        new CommentData("comment-id", "body", "article-id", new DateTime(), new DateTime(), null);
    when(environment.<CommentData>getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById("article-id", user)).thenReturn(Optional.of(articleData));

    DataFetcherResult<io.spring.graphql.types.Article> result =
        articleDatafetcher.getCommentArticle(environment);

    assertThat(result.getData().getSlug()).isEqualTo(articleData.getSlug());
  }

  @Test
  void should_fail_article_of_a_comment_when_not_found() {
    login(user);
    CommentData comment =
        new CommentData("comment-id", "body", "article-id", new DateTime(), new DateTime(), null);
    when(environment.<CommentData>getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById("article-id", user)).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleDatafetcher.getCommentArticle(environment));
  }

  @Test
  void should_find_article_by_slug() {
    login(user);
    when(articleQueryService.findBySlug(articleData.getSlug(), user))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<io.spring.graphql.types.Article> result =
        articleDatafetcher.findArticleBySlug(articleData.getSlug());

    assertThat(result.getData().getBody()).isEqualTo(articleData.getBody());
    assertThat(result.getData().getFavoritesCount()).isEqualTo(articleData.getFavoritesCount());
  }

  @Test
  void should_fail_find_article_by_slug_when_not_found() {
    logout();
    when(articleQueryService.findBySlug("unknown", null)).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleDatafetcher.findArticleBySlug("unknown"));
  }
}
