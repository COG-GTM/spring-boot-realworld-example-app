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
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.ArticlesConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ArticleDatafetcherTest {

  @Mock private ArticleQueryService articleQueryService;
  @Mock private UserRepository userRepository;
  @Mock private DataFetchingEnvironment env;
  @Mock private DataFetchingEnvironment plainDfe;

  private ArticleDatafetcher articleDatafetcher;
  private DgsDataFetchingEnvironment dfe;

  private final ProfileData author = new ProfileData("id", "jane", "bio", "image", false);

  @BeforeEach
  void setUp() {
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    dfe = new DgsDataFetchingEnvironment(env);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private ArticleData articleData(String slug) {
    return new ArticleData(
        slug + "-id",
        slug,
        "title",
        "desc",
        "body",
        false,
        0,
        new DateTime(),
        new DateTime(),
        Arrays.asList("java"),
        author);
  }

  private CursorPager<ArticleData> pager(List<ArticleData> data, Direction direction) {
    return new CursorPager<>(data, direction, false);
  }

  @Test
  void feed_should_throw_when_neither_first_nor_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> articleDatafetcher.getFeed(null, null, null, null, dfe));
  }

  @Test
  void feed_should_return_connection_for_first() {
    when(articleQueryService.findUserFeedWithCursor(isNull(), any()))
        .thenReturn(pager(Arrays.asList(articleData("a1"), articleData("a2")), Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, dfe);

    assertThat(result.getData().getEdges()).hasSize(2);
    assertThat(result.getData().getEdges().get(0).getNode().getSlug()).isEqualTo("a1");
    assertThat((Map<String, ?>) result.getLocalContext()).containsOnlyKeys("a1", "a2");
  }

  @Test
  void feed_should_return_connection_for_last() {
    when(articleQueryService.findUserFeedWithCursor(isNull(), any()))
        .thenReturn(pager(Collections.singletonList(articleData("a1")), Direction.PREV));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(null, null, 5, null, dfe);

    assertThat(result.getData().getEdges()).hasSize(1);
  }

  @Test
  void user_feed_should_throw_when_neither_first_nor_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> articleDatafetcher.userFeed(null, null, null, null, dfe));
  }

  @Test
  void user_feed_should_throw_not_found_when_user_missing() {
    io.spring.graphql.types.Profile profile =
        io.spring.graphql.types.Profile.newBuilder().username("ghost").build();
    when(env.<io.spring.graphql.types.Profile>getSource()).thenReturn(profile);
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleDatafetcher.userFeed(10, null, null, null, dfe));
  }

  @Test
  void user_feed_should_return_connection() {
    io.spring.graphql.types.Profile profile =
        io.spring.graphql.types.Profile.newBuilder().username("jane").build();
    User target = new User("jane@example.com", "jane", "123", "bio", "image");
    when(env.<io.spring.graphql.types.Profile>getSource()).thenReturn(profile);
    when(userRepository.findByUsername("jane")).thenReturn(Optional.of(target));
    when(articleQueryService.findUserFeedWithCursor(eq(target), any()))
        .thenReturn(pager(Collections.singletonList(articleData("a1")), Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(10, null, null, null, dfe);

    assertThat(result.getData().getEdges()).hasSize(1);
  }

  @Test
  void user_favorites_should_throw_when_neither_first_nor_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> articleDatafetcher.userFavorites(null, null, null, null, dfe));
  }

  @Test
  void user_favorites_should_return_connection() {
    io.spring.graphql.types.Profile profile =
        io.spring.graphql.types.Profile.newBuilder().username("jane").build();
    when(env.<io.spring.graphql.types.Profile>getSource()).thenReturn(profile);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("jane"), any(), isNull()))
        .thenReturn(pager(Collections.singletonList(articleData("a1")), Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(10, null, null, null, dfe);

    assertThat(result.getData().getEdges()).hasSize(1);
  }

  @Test
  void user_articles_should_return_connection() {
    io.spring.graphql.types.Profile profile =
        io.spring.graphql.types.Profile.newBuilder().username("jane").build();
    when(env.<io.spring.graphql.types.Profile>getSource()).thenReturn(profile);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("jane"), isNull(), any(), isNull()))
        .thenReturn(pager(Collections.singletonList(articleData("a1")), Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(10, null, null, null, dfe);

    assertThat(result.getData().getEdges()).hasSize(1);
  }

  @Test
  void get_articles_should_throw_when_neither_first_nor_last() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> articleDatafetcher.getArticles(null, null, null, null, null, null, null, dfe));
  }

  @Test
  void get_articles_should_filter_by_tag_author_and_favorited() {
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), eq("bob"), eq("alice"), any(), isNull()))
        .thenReturn(pager(Collections.singletonList(articleData("a1")), Direction.NEXT));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, "bob", "alice", "java", dfe);

    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getEdges().get(0).getNode().getSlug()).isEqualTo("a1");
  }

  @Test
  void get_article_should_return_article_from_local_context() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "user-1");
    when(plainDfe.<Article>getLocalContext()).thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), isNull()))
        .thenReturn(Optional.of(articleData("the-slug")));

    DataFetcherResult<io.spring.graphql.types.Article> result =
        articleDatafetcher.getArticle(plainDfe);

    assertThat(result.getData().getSlug()).isEqualTo("the-slug");
    assertThat((Map<String, ?>) result.getLocalContext()).containsKey("the-slug");
  }

  @Test
  void get_article_should_throw_not_found() {
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), "user-1");
    when(plainDfe.<Article>getLocalContext()).thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), isNull())).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleDatafetcher.getArticle(plainDfe));
  }

  @Test
  void get_comment_article_should_return_article() {
    CommentData comment =
        new CommentData("c1", "body", "article-1", new DateTime(), new DateTime(), author);
    when(plainDfe.<CommentData>getLocalContext()).thenReturn(comment);
    when(articleQueryService.findById(eq("article-1"), isNull()))
        .thenReturn(Optional.of(articleData("the-slug")));

    DataFetcherResult<io.spring.graphql.types.Article> result =
        articleDatafetcher.getCommentArticle(plainDfe);

    assertThat(result.getData().getSlug()).isEqualTo("the-slug");
  }

  @Test
  void find_article_by_slug_should_return_article() {
    when(articleQueryService.findBySlug(eq("the-slug"), isNull()))
        .thenReturn(Optional.of(articleData("the-slug")));

    DataFetcherResult<io.spring.graphql.types.Article> result =
        articleDatafetcher.findArticleBySlug("the-slug");

    assertThat(result.getData().getSlug()).isEqualTo("the-slug");
    assertThat(result.getData().getTitle()).isEqualTo("title");
  }

  @Test
  void find_article_by_slug_should_throw_not_found() {
    when(articleQueryService.findBySlug(eq("ghost"), isNull())).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleDatafetcher.findArticleBySlug("ghost"));
  }
}
