package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import io.spring.application.data.ProfileData;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.ArticlesConnection;
import io.spring.graphql.types.Profile;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleDatafetcherTest extends GraphQLTestBase {

  @Mock private ArticleQueryService articleQueryService;
  @Mock private UserRepository userRepository;

  @InjectMocks private ArticleDatafetcher articleDatafetcher;

  @Captor private ArgumentCaptor<CursorPageParameter<DateTime>> pageParameterCaptor;

  private ArticleData articleData() {
    return TestHelper.articleDataFixture("test", user);
  }

  private CursorPager<ArticleData> pager(ArticleData articleData, Direction direction) {
    return new CursorPager<>(Collections.singletonList(articleData), direction, true);
  }

  private void assertSingleArticleConnection(
      DataFetcherResult<ArticlesConnection> result, ArticleData expected) {
    ArticlesConnection connection = result.getData();
    assertThat(connection.getEdges()).hasSize(1);
    assertThat(connection.getEdges().get(0).getCursor()).isEqualTo(expected.getCursor().toString());
    assertThat(connection.getEdges().get(0).getNode().getSlug()).isEqualTo(expected.getSlug());
    assertThat(connection.getPageInfo().getStartCursor().getValue())
        .isEqualTo(expected.getCursor().toString());
    assertThat(connection.getPageInfo().getEndCursor().getValue())
        .isEqualTo(expected.getCursor().toString());
    @SuppressWarnings("unchecked")
    Map<String, ArticleData> localContext = (Map<String, ArticleData>) result.getLocalContext();
    assertThat(localContext).containsKey(expected.getSlug());
  }

  @Test
  void should_get_feed_forward() {
    ArticleData articleData = articleData();
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pager(articleData, Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, dgsDfe(null, null));

    assertSingleArticleConnection(result, articleData);
    assertThat(result.getData().getPageInfo().isHasNextPage()).isTrue();
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isFalse();
  }

  @Test
  void should_get_feed_backward_with_cursor() {
    ArticleData articleData = articleData();
    DateTime before = new DateTime();
    when(articleQueryService.findUserFeedWithCursor(eq(user), pageParameterCaptor.capture()))
        .thenReturn(pager(articleData, Direction.PREV));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(
            null, null, 5, String.valueOf(before.getMillis()), dgsDfe(null, null));

    assertSingleArticleConnection(result, articleData);
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isTrue();
    assertThat(pageParameterCaptor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(pageParameterCaptor.getValue().getLimit()).isEqualTo(5);
    assertThat(pageParameterCaptor.getValue().getCursor().getMillis())
        .isEqualTo(before.getMillis());
  }

  @Test
  void should_reject_feed_without_first_and_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> articleDatafetcher.getFeed(null, null, null, null, dgsDfe(null, null)));
  }

  @Test
  void should_get_user_feed_of_target_user() {
    ArticleData articleData = articleData();
    when(userRepository.findByUsername(eq(user.getUsername()))).thenReturn(Optional.of(user));
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pager(articleData, Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(
            10, null, null, null, dgsDfe(profileOf(user.getUsername()), null));

    assertSingleArticleConnection(result, articleData);
  }

  @Test
  void should_get_user_feed_backward() {
    ArticleData articleData = articleData();
    when(userRepository.findByUsername(eq(user.getUsername()))).thenReturn(Optional.of(user));
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pager(articleData, Direction.PREV));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(
            null, null, 10, null, dgsDfe(profileOf(user.getUsername()), null));

    assertSingleArticleConnection(result, articleData);
  }

  @Test
  void should_reject_user_feed_without_first_and_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                articleDatafetcher.userFeed(
                    null, null, null, null, dgsDfe(profileOf(user.getUsername()), null)));
  }

  @Test
  void should_fail_user_feed_when_target_user_not_found() {
    when(userRepository.findByUsername(eq("unknown"))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(
            () ->
                articleDatafetcher.userFeed(
                    10, null, null, null, dgsDfe(profileOf("unknown"), null)));
  }

  @Test
  void should_get_user_favorites() {
    ArticleData articleData = articleData();
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq(user.getUsername()), any(), eq(user)))
        .thenReturn(pager(articleData, Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(
            10, null, null, null, dgsDfe(profileOf(user.getUsername()), null));

    assertSingleArticleConnection(result, articleData);
  }

  @Test
  void should_get_user_favorites_backward() {
    ArticleData articleData = articleData();
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq(user.getUsername()), any(), eq(user)))
        .thenReturn(pager(articleData, Direction.PREV));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(
            null, null, 10, null, dgsDfe(profileOf(user.getUsername()), null));

    assertSingleArticleConnection(result, articleData);
  }

  @Test
  void should_reject_user_favorites_without_first_and_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                articleDatafetcher.userFavorites(
                    null, null, null, null, dgsDfe(profileOf(user.getUsername()), null)));
  }

  @Test
  void should_get_user_articles() {
    ArticleData articleData = articleData();
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq(user.getUsername()), isNull(), any(), eq(user)))
        .thenReturn(pager(articleData, Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(
            10, null, null, null, dgsDfe(profileOf(user.getUsername()), null));

    assertSingleArticleConnection(result, articleData);
  }

  @Test
  void should_get_user_articles_backward() {
    ArticleData articleData = articleData();
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq(user.getUsername()), isNull(), any(), eq(user)))
        .thenReturn(pager(articleData, Direction.PREV));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(
            null, null, 10, null, dgsDfe(profileOf(user.getUsername()), null));

    assertSingleArticleConnection(result, articleData);
  }

  @Test
  void should_reject_user_articles_without_first_and_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                articleDatafetcher.userArticles(
                    null, null, null, null, dgsDfe(profileOf(user.getUsername()), null)));
  }

  @Test
  void should_get_articles_with_filters() {
    ArticleData articleData = articleData();
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("joda"), eq("author"), eq("fan"), any(), eq(user)))
        .thenReturn(pager(articleData, Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(
            10, null, null, null, "author", "fan", "joda", dgsDfe(null, null));

    assertSingleArticleConnection(result, articleData);
  }

  @Test
  void should_get_articles_backward() {
    ArticleData articleData = articleData();
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), eq(user)))
        .thenReturn(pager(articleData, Direction.PREV));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(null, null, 10, null, null, null, null, dgsDfe(null, null));

    assertSingleArticleConnection(result, articleData);
  }

  @Test
  void should_reject_articles_without_first_and_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                articleDatafetcher.getArticles(
                    null, null, null, null, null, null, null, dgsDfe(null, null)));
  }

  @Test
  void should_get_article_from_local_context() {
    ArticleData articleData = articleData();
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "title", "desc", "body", Collections.singletonList("joda"), user.getId());
    when(articleQueryService.findById(eq(article.getId()), eq(user)))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.getArticle(dfe(null, article));

    assertThat(result.getData().getSlug()).isEqualTo(articleData.getSlug());
    assertThat(result.getData().getTitle()).isEqualTo(articleData.getTitle());
    @SuppressWarnings("unchecked")
    Map<String, Object> localContext = (Map<String, Object>) result.getLocalContext();
    assertThat(localContext).containsKey(articleData.getSlug());
  }

  @Test
  void should_fail_get_article_when_not_found() {
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "title", "desc", "body", Collections.singletonList("joda"), user.getId());
    when(articleQueryService.findById(eq(article.getId()), eq(user))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleDatafetcher.getArticle(dfe(null, article)));
  }

  @Test
  void should_get_comment_article() {
    ArticleData articleData = articleData();
    CommentData commentData =
        new CommentData(
            "comment-id",
            "comment body",
            articleData.getId(),
            new DateTime(),
            new DateTime(),
            new ProfileData(
                user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
    when(articleQueryService.findById(eq(articleData.getId()), eq(user)))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result =
        articleDatafetcher.getCommentArticle(dfe(null, commentData));

    assertThat(result.getData().getSlug()).isEqualTo(articleData.getSlug());
  }

  @Test
  void should_find_article_by_slug() {
    ArticleData articleData = articleData();
    when(articleQueryService.findBySlug(eq(articleData.getSlug()), eq(user)))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug(articleData.getSlug());

    assertThat(result.getData().getBody()).isEqualTo(articleData.getBody());
    assertThat(result.getData().getFavoritesCount()).isEqualTo(articleData.getFavoritesCount());
  }

  @Test
  void should_fail_find_article_by_slug_when_not_found() {
    when(articleQueryService.findBySlug(eq("unknown"), eq(user))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleDatafetcher.findArticleBySlug("unknown"));
  }

  @Test
  void should_get_articles_for_anonymous_user() {
    anonymous();
    ArticleData articleData = articleData();
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), isNull()))
        .thenReturn(pager(articleData, Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, null, null, null, dgsDfe(null, null));

    assertSingleArticleConnection(result, articleData);
  }

  private Profile profileOf(String username) {
    return Profile.newBuilder().username(username).build();
  }
}
