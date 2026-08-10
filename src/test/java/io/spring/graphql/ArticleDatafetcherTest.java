package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import io.spring.graphql.types.Article;
import io.spring.graphql.types.ArticlesConnection;
import io.spring.graphql.types.Profile;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class ArticleDatafetcherTest {

  private static final DateTime UPDATED_AT = new DateTime(1600000000000L);

  private final ArticleQueryService articleQueryService = mock(ArticleQueryService.class);
  private final UserRepository userRepository = mock(UserRepository.class);
  private final ArticleDatafetcher datafetcher =
      new ArticleDatafetcher(articleQueryService, userRepository);

  @BeforeEach
  void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  private User authenticate() {
    User user = new User("jake@jake.jake", "jake", "123", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, null));
    return user;
  }

  private ArticleData articleData(String slug) {
    return new ArticleData(
        "id-" + slug,
        slug,
        "title of " + slug,
        "description",
        "body",
        true,
        3,
        UPDATED_AT,
        UPDATED_AT,
        Collections.singletonList("java"),
        new ProfileData("profile-id", "jake", "bio", "image", false));
  }

  private DgsDataFetchingEnvironment profileEnvironment(String username) {
    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    when(delegate.<Profile>getSource()).thenReturn(Profile.newBuilder().username(username).build());
    return new DgsDataFetchingEnvironment(delegate);
  }

  @SuppressWarnings("unchecked")
  private ArgumentCaptor<CursorPageParameter<DateTime>> pageCaptor() {
    return ArgumentCaptor.forClass(CursorPageParameter.class);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> localContextOf(DataFetcherResult<?> result) {
    return (Map<String, Object>) result.getLocalContext();
  }

  @Test
  void should_get_feed_of_current_user_forward() {
    User current = authenticate();
    when(articleQueryService.findUserFeedWithCursor(eq(current), any()))
        .thenReturn(
            new CursorPager<>(
                Arrays.asList(articleData("first"), articleData("second")), Direction.NEXT, true));

    DataFetcherResult<ArticlesConnection> result =
        datafetcher.getFeed(10, "1600000000000", null, null, profileEnvironment("jake"));

    assertThat(result.getData().getEdges()).hasSize(2);
    assertThat(result.getData().getEdges().get(0).getNode().getSlug()).isEqualTo("first");
    assertThat(result.getData().getEdges().get(0).getNode().getTitle()).isEqualTo("title of first");
    assertThat(result.getData().getEdges().get(0).getNode().getFavorited()).isTrue();
    assertThat(result.getData().getEdges().get(0).getNode().getFavoritesCount()).isEqualTo(3);
    assertThat(result.getData().getEdges().get(0).getNode().getUpdatedAt())
        .isEqualTo("2020-09-13T12:26:40.000Z");
    assertThat(result.getData().getPageInfo().isHasNextPage()).isTrue();
    assertThat(localContextOf(result)).containsKeys("first", "second");

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService).findUserFeedWithCursor(eq(current), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.NEXT);
    assertThat(captor.getValue().getCursor().getMillis()).isEqualTo(1600000000000L);
  }

  @Test
  void should_get_feed_backward_with_null_user_when_anonymous() {
    when(articleQueryService.findUserFeedWithCursor(isNull(), any()))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(articleData("first")), Direction.PREV, true));

    DataFetcherResult<ArticlesConnection> result =
        datafetcher.getFeed(null, null, 5, "1600000000000", profileEnvironment("jake"));

    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isTrue();
    assertThat(result.getData().getPageInfo().isHasNextPage()).isFalse();

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService).findUserFeedWithCursor(isNull(), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getLimit()).isEqualTo(5);
  }

  @Test
  void should_reject_feed_without_first_or_last() {
    DgsDataFetchingEnvironment dfe = profileEnvironment("jake");

    assertThatThrownBy(() -> datafetcher.getFeed(null, null, null, null, dfe))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_get_user_feed_of_profile_owner() {
    User target = new User("john@john.com", "john", "123", "bio", "image");
    when(userRepository.findByUsername("john")).thenReturn(Optional.of(target));
    when(articleQueryService.findUserFeedWithCursor(eq(target), any()))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(articleData("first")), Direction.NEXT, false));

    DataFetcherResult<ArticlesConnection> result =
        datafetcher.userFeed(10, null, null, null, profileEnvironment("john"));

    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getPageInfo().isHasNextPage()).isFalse();
    assertThat(result.getData().getPageInfo().getEndCursor().getValue()).isEqualTo("1600000000000");
  }

  @Test
  void should_get_user_feed_backward() {
    User target = new User("john@john.com", "john", "123", "bio", "image");
    when(userRepository.findByUsername("john")).thenReturn(Optional.of(target));
    when(articleQueryService.findUserFeedWithCursor(eq(target), any()))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(articleData("first")), Direction.PREV, true));

    DataFetcherResult<ArticlesConnection> result =
        datafetcher.userFeed(null, null, 3, "1600000000000", profileEnvironment("john"));

    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isTrue();

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService).findUserFeedWithCursor(eq(target), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getLimit()).isEqualTo(3);
  }

  @Test
  void should_throw_not_found_when_profile_owner_is_unknown() {
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
    DgsDataFetchingEnvironment dfe = profileEnvironment("ghost");

    assertThatThrownBy(() -> datafetcher.userFeed(10, null, null, null, dfe))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_reject_user_feed_without_first_or_last() {
    DgsDataFetchingEnvironment dfe = profileEnvironment("john");

    assertThatThrownBy(() -> datafetcher.userFeed(null, null, null, null, dfe))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_get_favorites_of_profile_forward() {
    User current = authenticate();
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("john"), any(), eq(current)))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(articleData("liked")), Direction.NEXT, true));

    DataFetcherResult<ArticlesConnection> result =
        datafetcher.userFavorites(10, "1600000000000", null, null, profileEnvironment("john"));

    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getEdges().get(0).getNode().getSlug()).isEqualTo("liked");
    assertThat(result.getData().getPageInfo().isHasNextPage()).isTrue();
  }

  @Test
  void should_get_favorites_of_profile_backward() {
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("john"), any(), isNull()))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(articleData("liked")), Direction.PREV, false));

    DataFetcherResult<ArticlesConnection> result =
        datafetcher.userFavorites(null, null, 4, "1600000000000", profileEnvironment("john"));

    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isFalse();

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), isNull(), eq("john"), captor.capture(), isNull());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getLimit()).isEqualTo(4);
  }

  @Test
  void should_reject_favorites_without_first_or_last() {
    DgsDataFetchingEnvironment dfe = profileEnvironment("john");

    assertThatThrownBy(() -> datafetcher.userFavorites(null, null, null, null, dfe))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_get_articles_authored_by_profile_forward() {
    User current = authenticate();
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("john"), isNull(), any(), eq(current)))
        .thenReturn(
            new CursorPager<>(
                Arrays.asList(articleData("a"), articleData("b")), Direction.NEXT, false));

    DataFetcherResult<ArticlesConnection> result =
        datafetcher.userArticles(10, null, null, null, profileEnvironment("john"));

    assertThat(result.getData().getEdges()).hasSize(2);
    assertThat(localContextOf(result)).containsKeys("a", "b");
  }

  @Test
  void should_get_articles_authored_by_profile_backward() {
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("john"), isNull(), any(), isNull()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.PREV, false));

    DataFetcherResult<ArticlesConnection> result =
        datafetcher.userArticles(null, null, 2, "1600000000000", profileEnvironment("john"));

    assertThat(result.getData().getEdges()).isEmpty();
    assertThat(result.getData().getPageInfo().getStartCursor()).isNull();
  }

  @Test
  void should_reject_user_articles_without_first_or_last() {
    DgsDataFetchingEnvironment dfe = profileEnvironment("john");

    assertThatThrownBy(() -> datafetcher.userArticles(null, null, null, null, dfe))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_query_articles_with_filters_forward() {
    User current = authenticate();
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), eq("john"), eq("jake"), any(), eq(current)))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(articleData("filtered")), Direction.NEXT, true));

    DataFetcherResult<ArticlesConnection> result =
        datafetcher.getArticles(
            10, "1600000000000", null, null, "john", "jake", "java", profileEnvironment("jake"));

    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getEdges().get(0).getNode().getSlug()).isEqualTo("filtered");
    assertThat(result.getData().getEdges().get(0).getNode().getTagList()).containsExactly("java");
    assertThat(result.getData().getPageInfo().isHasNextPage()).isTrue();
  }

  @Test
  void should_query_articles_backward() {
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), isNull()))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(articleData("filtered")), Direction.PREV, true));

    DataFetcherResult<ArticlesConnection> result =
        datafetcher.getArticles(
            null, null, 7, "1600000000000", null, null, null, profileEnvironment("jake"));

    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isTrue();

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), isNull(), isNull(), captor.capture(), isNull());
    assertThat(captor.getValue().getLimit()).isEqualTo(7);
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
  }

  @Test
  void should_reject_articles_query_without_first_or_last() {
    DgsDataFetchingEnvironment dfe = profileEnvironment("jake");

    assertThatThrownBy(() -> datafetcher.getArticles(null, null, null, null, null, null, null, dfe))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_get_article_from_payload_local_context() {
    User current = authenticate();
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "a title", "description", "body", Collections.singletonList("java"), current.getId());
    when(articleQueryService.findById(article.getId(), current))
        .thenReturn(Optional.of(articleData("a-title")));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<io.spring.core.article.Article>getLocalContext()).thenReturn(article);

    DataFetcherResult<Article> result = datafetcher.getArticle(dfe);

    assertThat(result.getData().getSlug()).isEqualTo("a-title");
    assertThat(result.getData().getBody()).isEqualTo("body");
    assertThat(localContextOf(result)).containsKey("a-title");
  }

  @Test
  void should_throw_not_found_when_payload_article_is_missing() {
    User current = authenticate();
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "a title", "description", "body", Collections.singletonList("java"), current.getId());
    when(articleQueryService.findById(article.getId(), current)).thenReturn(Optional.empty());
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<io.spring.core.article.Article>getLocalContext()).thenReturn(article);

    assertThatThrownBy(() -> datafetcher.getArticle(dfe))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_get_article_of_a_comment() {
    CommentData comment =
        new CommentData(
            "comment-id",
            "body",
            "article-id",
            UPDATED_AT,
            UPDATED_AT,
            new ProfileData("profile-id", "jake", "bio", "image", false));
    when(articleQueryService.findById(eq("article-id"), isNull()))
        .thenReturn(Optional.of(articleData("commented")));
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<CommentData>getLocalContext()).thenReturn(comment);

    DataFetcherResult<Article> result = datafetcher.getCommentArticle(dfe);

    assertThat(result.getData().getSlug()).isEqualTo("commented");
    assertThat(localContextOf(result)).containsKey("commented");
  }

  @Test
  void should_throw_not_found_when_comment_article_is_missing() {
    CommentData comment =
        new CommentData(
            "comment-id",
            "body",
            "article-id",
            UPDATED_AT,
            UPDATED_AT,
            new ProfileData("profile-id", "jake", "bio", "image", false));
    when(articleQueryService.findById(eq("article-id"), isNull())).thenReturn(Optional.empty());
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    when(dfe.<CommentData>getLocalContext()).thenReturn(comment);

    assertThatThrownBy(() -> datafetcher.getCommentArticle(dfe))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_find_article_by_slug() {
    User current = authenticate();
    when(articleQueryService.findBySlug("a-slug", current))
        .thenReturn(Optional.of(articleData("a-slug")));

    DataFetcherResult<Article> result = datafetcher.findArticleBySlug("a-slug");

    assertThat(result.getData().getSlug()).isEqualTo("a-slug");
    assertThat(result.getData().getDescription()).isEqualTo("description");
    assertThat(result.getData().getCreatedAt()).isEqualTo("2020-09-13T12:26:40.000Z");
    assertThat(localContextOf(result)).containsKey("a-slug");
  }

  @Test
  void should_throw_not_found_when_slug_is_unknown() {
    when(articleQueryService.findBySlug(eq("ghost"), isNull())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> datafetcher.findArticleBySlug("ghost"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_expose_all_edge_cursors_from_pager() {
    authenticate();
    List<ArticleData> data = Arrays.asList(articleData("a"), articleData("b"));
    when(articleQueryService.findUserFeedWithCursor(any(), any()))
        .thenReturn(new CursorPager<>(data, Direction.NEXT, false));

    DataFetcherResult<ArticlesConnection> result =
        datafetcher.getFeed(10, null, null, null, profileEnvironment("jake"));

    assertThat(result.getData().getEdges())
        .extracting(edge -> edge.getCursor())
        .containsExactly("1600000000000", "1600000000000");
  }
}
