package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleDatafetcherTest extends GraphQLTestBase {

  @Mock private ArticleQueryService articleQueryService;
  @Mock private UserRepository userRepository;

  private ArticleDatafetcher articleDatafetcher;
  private User user;

  @BeforeEach
  void setUp() {
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    user = newUser();
  }

  private CursorPager<ArticleData> onePage() {
    return new CursorPager<>(
        Collections.singletonList(articleData("a1", "a-slug", "johnjacob")),
        Direction.NEXT,
        false);
  }

  @Test
  void should_get_feed_forward_with_first() {
    setCurrentUser(user);
    when(articleQueryService.findUserFeedWithCursor(
            any(User.class), any(CursorPageParameter.class)))
        .thenReturn(onePage());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, null);

    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getEdges().get(0).getNode().getSlug()).isEqualTo("a-slug");
  }

  @Test
  void should_get_feed_backward_with_last() {
    setCurrentUser(user);
    when(articleQueryService.findUserFeedWithCursor(any(), any(CursorPageParameter.class)))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.PREV, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(null, null, 10, "123", null);

    assertThat(result.getData().getEdges()).isEmpty();
  }

  @Test
  void should_throw_when_neither_first_nor_last_provided() {
    assertThatThrownBy(() -> articleDatafetcher.getFeed(null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_get_user_feed_by_profile() {
    DataFetchingEnvironment dfe = mockEnv();
    when(dfe.getSource()).thenReturn(Profile.newBuilder().username("johnjacob").build());
    when(userRepository.findByUsername(eq("johnjacob"))).thenReturn(Optional.of(user));
    when(articleQueryService.findUserFeedWithCursor(any(), any(CursorPageParameter.class)))
        .thenReturn(onePage());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(5, null, null, null, dgs(dfe));

    assertThat(result.getData().getEdges()).hasSize(1);
  }

  @Test
  void should_throw_when_user_feed_target_missing() {
    DataFetchingEnvironment dfe = mockEnv();
    when(dfe.getSource()).thenReturn(Profile.newBuilder().username("ghost").build());
    when(userRepository.findByUsername(eq("ghost"))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> articleDatafetcher.userFeed(5, null, null, null, dgs(dfe)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_get_user_favorites_forward_and_backward() {
    setCurrentUser(user);
    DataFetchingEnvironment dfe = mockEnv();
    when(dfe.getSource()).thenReturn(Profile.newBuilder().username("johnjacob").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            any(), any(), any(), any(CursorPageParameter.class), any()))
        .thenReturn(onePage());

    DataFetcherResult<ArticlesConnection> forward =
        articleDatafetcher.userFavorites(5, null, null, null, dgs(dfe));
    DataFetcherResult<ArticlesConnection> backward =
        articleDatafetcher.userFavorites(null, null, 5, "123", dgs(dfe));

    assertThat(forward.getData().getEdges()).hasSize(1);
    assertThat(backward.getData().getEdges()).hasSize(1);
  }

  @Test
  void should_get_user_articles() {
    setCurrentUser(user);
    DataFetchingEnvironment dfe = mockEnv();
    when(dfe.getSource()).thenReturn(Profile.newBuilder().username("johnjacob").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            any(), any(), any(), any(CursorPageParameter.class), any()))
        .thenReturn(onePage());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(5, null, null, null, dgs(dfe));

    assertThat(result.getData().getEdges()).hasSize(1);
  }

  @Test
  void should_get_articles_with_filters() {
    setCurrentUser(user);
    when(articleQueryService.findRecentArticlesWithCursor(
            any(), any(), any(), any(CursorPageParameter.class), any()))
        .thenReturn(onePage());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(5, null, null, null, "authored", "favorited", "tag", null);

    assertThat(result.getData().getEdges()).hasSize(1);
  }

  @Test
  void should_get_article_from_local_context() {
    setCurrentUser(user);
    DataFetchingEnvironment dfe = mockEnv();
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article("title", "desc", "body", Arrays.asList("t"), user.getId());
    when(dfe.getLocalContext()).thenReturn(coreArticle);
    when(articleQueryService.findById(eq(coreArticle.getId()), any()))
        .thenReturn(Optional.of(articleData(coreArticle.getId(), "the-slug", "johnjacob")));

    DataFetcherResult<Article> result = articleDatafetcher.getArticle(dfe);

    assertThat(result.getData().getSlug()).isEqualTo("the-slug");
  }

  @Test
  void should_throw_when_article_by_local_context_missing() {
    setCurrentUser(user);
    DataFetchingEnvironment dfe = mockEnv();
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article("title", "desc", "body", Arrays.asList("t"), user.getId());
    when(dfe.getLocalContext()).thenReturn(coreArticle);
    when(articleQueryService.findById(eq(coreArticle.getId()), any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> articleDatafetcher.getArticle(dfe))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_get_comment_article() {
    setCurrentUser(user);
    DataFetchingEnvironment dfe = mockEnv();
    CommentData comment = commentData("c1", "article-42", "johnjacob");
    when(dfe.getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById(eq("article-42"), any()))
        .thenReturn(Optional.of(articleData("article-42", "art-slug", "johnjacob")));

    DataFetcherResult<Article> result = articleDatafetcher.getCommentArticle(dfe);

    assertThat(result.getData().getSlug()).isEqualTo("art-slug");
  }

  @Test
  void should_find_article_by_slug() {
    setCurrentUser(user);
    when(articleQueryService.findBySlug(eq("art-slug"), any()))
        .thenReturn(Optional.of(articleData("a1", "art-slug", "johnjacob")));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("art-slug");

    assertThat(result.getData().getSlug()).isEqualTo("art-slug");
    assertThat(result.getData().getTitle()).isEqualTo("a title");
  }

  @Test
  void should_throw_when_find_article_by_slug_missing() {
    setCurrentUser(user);
    when(articleQueryService.findBySlug(eq("missing"), any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> articleDatafetcher.findArticleBySlug("missing"))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
