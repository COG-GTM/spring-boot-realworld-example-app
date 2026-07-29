package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleDatafetcherTest {

  private static final DateTime CREATED_AT = new DateTime(2021, 1, 1, 0, 0, DateTimeZone.UTC);
  private static final DateTime UPDATED_AT = new DateTime(2021, 2, 2, 0, 0, DateTimeZone.UTC);

  @Mock private ArticleQueryService articleQueryService;
  @Mock private UserRepository userRepository;
  @Mock private DataFetchingEnvironment delegateEnvironment;

  @Captor private ArgumentCaptor<CursorPageParameter<DateTime>> pageParameterCaptor;

  @InjectMocks private ArticleDatafetcher articleDatafetcher;

  private final User currentUser = new User("jake@jake.jake", "jake", "123", "bio", "image");
  private final User targetUser = new User("john@john.com", "john", "123", "", "");

  @AfterEach
  void tearDown() {
    SecurityContextHelper.clear();
  }

  @Test
  public void should_return_feed_forward_page_for_current_user() {
    SecurityContextHelper.authenticate(currentUser);
    when(articleQueryService.findUserFeedWithCursor(eq(currentUser), pageParameterCaptor.capture()))
        .thenReturn(pagerOf(articleData("a-title"), Direction.NEXT, true));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, "1000", null, null, dgsEnvironment());

    CursorPageParameter<DateTime> pageParameter = pageParameterCaptor.getValue();
    assertEquals(10, pageParameter.getLimit());
    assertEquals(Direction.NEXT, pageParameter.getDirection());
    assertEquals(1000L, pageParameter.getCursor().getMillis());

    assertConnectionContainsArticle(result, "a-title");
    assertTrue(result.getData().getPageInfo().isHasNextPage());
    assertFalse(result.getData().getPageInfo().isHasPreviousPage());
  }

  @Test
  public void should_return_feed_backward_page_for_anonymous_user() {
    SecurityContextHelper.anonymous();
    when(articleQueryService.findUserFeedWithCursor(isNull(), pageParameterCaptor.capture()))
        .thenReturn(pagerOf(articleData("a-title"), Direction.PREV, true));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(null, null, 5, null, dgsEnvironment());

    assertEquals(Direction.PREV, pageParameterCaptor.getValue().getDirection());
    assertNull(pageParameterCaptor.getValue().getCursor());
    assertTrue(result.getData().getPageInfo().isHasPreviousPage());
  }

  @Test
  public void should_reject_feed_without_first_or_last() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getFeed(null, null, null, null, dgsEnvironment()));
  }

  @Test
  public void should_return_empty_connection_with_null_cursors() {
    SecurityContextHelper.anonymous();
    when(articleQueryService.findUserFeedWithCursor(isNull(), pageParameterCaptor.capture()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, dgsEnvironment());

    assertTrue(result.getData().getEdges().isEmpty());
    assertNull(result.getData().getPageInfo().getStartCursor());
    assertNull(result.getData().getPageInfo().getEndCursor());
  }

  @Test
  public void should_return_user_feed_of_profile() {
    when(userRepository.findByUsername("john")).thenReturn(Optional.of(targetUser));
    when(articleQueryService.findUserFeedWithCursor(eq(targetUser), pageParameterCaptor.capture()))
        .thenReturn(pagerOf(articleData("a-title"), Direction.NEXT, false));
    when(delegateEnvironment.<Profile>getSource())
        .thenReturn(Profile.newBuilder().username("john").build());

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(10, null, null, null, dgsEnvironment());

    assertEquals(Direction.NEXT, pageParameterCaptor.getValue().getDirection());
    assertConnectionContainsArticle(result, "a-title");
  }

  @Test
  public void should_return_user_feed_backward_page() {
    when(userRepository.findByUsername("john")).thenReturn(Optional.of(targetUser));
    when(articleQueryService.findUserFeedWithCursor(eq(targetUser), pageParameterCaptor.capture()))
        .thenReturn(pagerOf(articleData("a-title"), Direction.PREV, false));
    when(delegateEnvironment.<Profile>getSource())
        .thenReturn(Profile.newBuilder().username("john").build());

    articleDatafetcher.userFeed(null, null, 3, "2000", dgsEnvironment());

    assertEquals(Direction.PREV, pageParameterCaptor.getValue().getDirection());
    assertEquals(2000L, pageParameterCaptor.getValue().getCursor().getMillis());
  }

  @Test
  public void should_throw_when_user_feed_owner_is_missing() {
    when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
    when(delegateEnvironment.<Profile>getSource())
        .thenReturn(Profile.newBuilder().username("john").build());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleDatafetcher.userFeed(10, null, null, null, dgsEnvironment()));
  }

  @Test
  public void should_reject_user_feed_without_first_or_last() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFeed(null, null, null, null, dgsEnvironment()));
  }

  @Test
  public void should_return_articles_favorited_by_profile() {
    SecurityContextHelper.authenticate(currentUser);
    when(delegateEnvironment.<Profile>getSource())
        .thenReturn(Profile.newBuilder().username("john").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("john"), pageParameterCaptor.capture(), eq(currentUser)))
        .thenReturn(pagerOf(articleData("a-title"), Direction.NEXT, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(10, null, null, null, dgsEnvironment());

    assertEquals(Direction.NEXT, pageParameterCaptor.getValue().getDirection());
    assertConnectionContainsArticle(result, "a-title");
  }

  @Test
  public void should_return_articles_favorited_by_profile_backward() {
    SecurityContextHelper.anonymous();
    when(delegateEnvironment.<Profile>getSource())
        .thenReturn(Profile.newBuilder().username("john").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("john"), pageParameterCaptor.capture(), isNull()))
        .thenReturn(pagerOf(articleData("a-title"), Direction.PREV, false));

    articleDatafetcher.userFavorites(null, null, 4, null, dgsEnvironment());

    assertEquals(Direction.PREV, pageParameterCaptor.getValue().getDirection());
  }

  @Test
  public void should_reject_user_favorites_without_first_or_last() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFavorites(null, null, null, null, dgsEnvironment()));
  }

  @Test
  public void should_return_articles_authored_by_profile() {
    SecurityContextHelper.anonymous();
    when(delegateEnvironment.<Profile>getSource())
        .thenReturn(Profile.newBuilder().username("john").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("john"), isNull(), pageParameterCaptor.capture(), isNull()))
        .thenReturn(pagerOf(articleData("a-title"), Direction.NEXT, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(10, null, null, null, dgsEnvironment());

    assertConnectionContainsArticle(result, "a-title");
  }

  @Test
  public void should_return_articles_authored_by_profile_backward() {
    SecurityContextHelper.anonymous();
    when(delegateEnvironment.<Profile>getSource())
        .thenReturn(Profile.newBuilder().username("john").build());
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("john"), isNull(), pageParameterCaptor.capture(), isNull()))
        .thenReturn(pagerOf(articleData("a-title"), Direction.PREV, false));

    articleDatafetcher.userArticles(null, null, 2, null, dgsEnvironment());

    assertEquals(Direction.PREV, pageParameterCaptor.getValue().getDirection());
  }

  @Test
  public void should_reject_user_articles_without_first_or_last() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userArticles(null, null, null, null, dgsEnvironment()));
  }

  @Test
  public void should_return_articles_filtered_by_tag_author_and_favorited_by() {
    SecurityContextHelper.authenticate(currentUser);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), eq("john"), eq("jake"), pageParameterCaptor.capture(), eq(currentUser)))
        .thenReturn(pagerOf(articleData("a-title"), Direction.NEXT, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(
            10, null, null, null, "john", "jake", "java", dgsEnvironment());

    assertConnectionContainsArticle(result, "a-title");
  }

  @Test
  public void should_return_articles_backward_page() {
    SecurityContextHelper.anonymous();
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), pageParameterCaptor.capture(), isNull()))
        .thenReturn(pagerOf(articleData("a-title"), Direction.PREV, true));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(null, null, 7, "3000", null, null, null, dgsEnvironment());

    assertEquals(Direction.PREV, pageParameterCaptor.getValue().getDirection());
    assertEquals(3000L, pageParameterCaptor.getValue().getCursor().getMillis());
    assertTrue(result.getData().getPageInfo().isHasPreviousPage());
  }

  @Test
  public void should_reject_articles_without_first_or_last() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            articleDatafetcher.getArticles(
                null, null, null, null, null, null, null, dgsEnvironment()));
  }

  @Test
  public void should_resolve_article_payload_article_from_local_context() {
    SecurityContextHelper.authenticate(currentUser);
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "a title", "desc", "body", Arrays.asList("java"), currentUser.getId());
    when(delegateEnvironment.<io.spring.core.article.Article>getLocalContext()).thenReturn(article);
    when(articleQueryService.findById(article.getId(), currentUser))
        .thenReturn(Optional.of(articleData("a-title")));

    DataFetcherResult<Article> result = articleDatafetcher.getArticle(delegateEnvironment);

    assertEquals("a-title", result.getData().getSlug());
    Map<String, ArticleData> localContext = articleLocalContext(result);
    assertTrue(localContext.containsKey("a-title"));
  }

  @Test
  public void should_throw_when_article_payload_article_is_missing() {
    SecurityContextHelper.anonymous();
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "a title", "desc", "body", Arrays.asList("java"), "user-id");
    when(delegateEnvironment.<io.spring.core.article.Article>getLocalContext()).thenReturn(article);
    when(articleQueryService.findById(article.getId(), null)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleDatafetcher.getArticle(delegateEnvironment));
  }

  @Test
  public void should_resolve_article_of_a_comment() {
    SecurityContextHelper.anonymous();
    CommentData commentData =
        new CommentData("comment-id", "body", "article-id", CREATED_AT, UPDATED_AT, profileData());
    when(delegateEnvironment.<CommentData>getLocalContext()).thenReturn(commentData);
    when(articleQueryService.findById("article-id", null))
        .thenReturn(Optional.of(articleData("a-title")));

    DataFetcherResult<Article> result = articleDatafetcher.getCommentArticle(delegateEnvironment);

    assertEquals("a-title", result.getData().getSlug());
  }

  @Test
  public void should_throw_when_article_of_a_comment_is_missing() {
    SecurityContextHelper.anonymous();
    CommentData commentData =
        new CommentData("comment-id", "body", "article-id", CREATED_AT, UPDATED_AT, profileData());
    when(delegateEnvironment.<CommentData>getLocalContext()).thenReturn(commentData);
    when(articleQueryService.findById("article-id", null)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleDatafetcher.getCommentArticle(delegateEnvironment));
  }

  @Test
  public void should_find_article_by_slug() {
    SecurityContextHelper.authenticate(currentUser);
    when(articleQueryService.findBySlug("a-title", currentUser))
        .thenReturn(Optional.of(articleData("a-title")));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("a-title");

    Article article = result.getData();
    assertEquals("a-title", article.getSlug());
    assertEquals("a title", article.getTitle());
    assertEquals("body", article.getBody());
    assertEquals("desc", article.getDescription());
    assertTrue(article.getFavorited());
    assertEquals(3, article.getFavoritesCount());
    assertEquals(Collections.singletonList("java"), article.getTagList());
    assertEquals("2021-01-01T00:00:00.000Z", article.getCreatedAt());
    assertEquals("2021-02-02T00:00:00.000Z", article.getUpdatedAt());
  }

  @Test
  public void should_throw_when_article_slug_is_unknown() {
    SecurityContextHelper.anonymous();
    when(articleQueryService.findBySlug("missing", null)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleDatafetcher.findArticleBySlug("missing"));
  }

  private void assertConnectionContainsArticle(
      DataFetcherResult<ArticlesConnection> result, String slug) {
    assertEquals(1, result.getData().getEdges().size());
    assertEquals(slug, result.getData().getEdges().get(0).getNode().getSlug());
    assertEquals(
        String.valueOf(UPDATED_AT.getMillis()), result.getData().getEdges().get(0).getCursor());
    assertEquals(
        String.valueOf(UPDATED_AT.getMillis()),
        result.getData().getPageInfo().getStartCursor().getValue());
    Map<String, ArticleData> localContext = articleLocalContext(result);
    assertTrue(localContext.containsKey(slug));
  }

  @SuppressWarnings("unchecked")
  private Map<String, ArticleData> articleLocalContext(DataFetcherResult<?> result) {
    return (Map<String, ArticleData>) result.getLocalContext();
  }

  private DgsDataFetchingEnvironment dgsEnvironment() {
    return new DgsDataFetchingEnvironment(delegateEnvironment);
  }

  private CursorPager<ArticleData> pagerOf(
      ArticleData articleData, Direction direction, boolean hasExtra) {
    List<ArticleData> data = Collections.singletonList(articleData);
    return new CursorPager<>(data, direction, hasExtra);
  }

  private ProfileData profileData() {
    return new ProfileData("author-id", "john", "bio", "image", false);
  }

  private ArticleData articleData(String slug) {
    return new ArticleData(
        "article-id",
        slug,
        "a title",
        "desc",
        "body",
        true,
        3,
        CREATED_AT,
        UPDATED_AT,
        Collections.singletonList("java"),
        profileData());
  }
}
