package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import graphql.relay.PageInfo;
import io.spring.TestHelper;
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
import org.joda.time.format.ISODateTimeFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleDatafetcherTest {
  @Mock private ArticleQueryService articleQueryService;
  @Mock private UserRepository userRepository;

  private ArticleDatafetcher articleDatafetcher;
  private User user;
  private ArticleData articleData1;
  private ArticleData articleData2;

  @BeforeEach
  public void setUp() {
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    user = new User("a@test.com", "a", "123", "", "");
    articleData1 = TestHelper.articleDataFixture("1", user);
    articleData1.setUpdatedAt(new DateTime(2000L));
    articleData2 = TestHelper.articleDataFixture("2", user);
    articleData2.setUpdatedAt(new DateTime(1000L));
    GraphQLTestSupport.authenticate(user);
  }

  @AfterEach
  public void tearDown() {
    GraphQLTestSupport.clear();
  }

  private CursorPager<ArticleData> pager(Direction direction, boolean hasExtra) {
    return new CursorPager<>(Arrays.asList(articleData1, articleData2), direction, hasExtra);
  }

  @SuppressWarnings("unchecked")
  private ArgumentCaptor<CursorPageParameter<DateTime>> pageCaptor() {
    return ArgumentCaptor.forClass(CursorPageParameter.class);
  }

  @Test
  public void should_fetch_feed_forward() {
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pager(Direction.NEXT, true));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, "1000", null, null, GraphQLTestSupport.dgsEnv(null, null));

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService).findUserFeedWithCursor(eq(user), captor.capture());
    Assertions.assertEquals(10, captor.getValue().getLimit());
    Assertions.assertEquals(Direction.NEXT, captor.getValue().getDirection());
    Assertions.assertEquals(1000L, captor.getValue().getCursor().getMillis());

    ArticlesConnection connection = result.getData();
    Assertions.assertEquals(2, connection.getEdges().size());
    Assertions.assertEquals("2000", connection.getEdges().get(0).getCursor());
    Assertions.assertEquals(
        articleData1.getSlug(), connection.getEdges().get(0).getNode().getSlug());
    PageInfo pageInfo = connection.getPageInfo();
    Assertions.assertTrue(pageInfo.isHasNextPage());
    Assertions.assertFalse(pageInfo.isHasPreviousPage());
    Assertions.assertEquals("2000", pageInfo.getStartCursor().getValue());
    Assertions.assertEquals("1000", pageInfo.getEndCursor().getValue());

    Map<String, ArticleData> localContext = (Map<String, ArticleData>) result.getLocalContext();
    Assertions.assertSame(articleData1, localContext.get(articleData1.getSlug()));
    Assertions.assertSame(articleData2, localContext.get(articleData2.getSlug()));
  }

  @Test
  public void should_fetch_feed_backward() {
    when(articleQueryService.findUserFeedWithCursor(eq(user), any()))
        .thenReturn(pager(Direction.PREV, true));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(null, null, 5, "3000", GraphQLTestSupport.dgsEnv(null, null));

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService).findUserFeedWithCursor(eq(user), captor.capture());
    Assertions.assertEquals(5, captor.getValue().getLimit());
    Assertions.assertEquals(Direction.PREV, captor.getValue().getDirection());
    Assertions.assertEquals(3000L, captor.getValue().getCursor().getMillis());
    Assertions.assertTrue(result.getData().getPageInfo().isHasPreviousPage());
    Assertions.assertFalse(result.getData().getPageInfo().isHasNextPage());
  }

  @Test
  public void should_return_empty_page_info_for_empty_feed() {
    GraphQLTestSupport.anonymous();
    when(articleQueryService.findUserFeedWithCursor(isNull(), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, GraphQLTestSupport.dgsEnv(null, null));

    Assertions.assertTrue(result.getData().getEdges().isEmpty());
    Assertions.assertNull(result.getData().getPageInfo().getStartCursor());
    Assertions.assertNull(result.getData().getPageInfo().getEndCursor());
  }

  @Test
  public void should_require_first_or_last_for_all_connections() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getFeed(null, null, null, null, null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFeed(null, null, null, null, null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFavorites(null, null, null, null, null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userArticles(null, null, null, null, null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getArticles(null, null, null, null, null, null, null, null));
  }

  @Test
  public void should_fetch_articles_with_filters() {
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), eq("author"), eq("fan"), any(), eq(user)))
        .thenReturn(pager(Direction.NEXT, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(
            10, null, null, null, "author", "fan", "java", GraphQLTestSupport.dgsEnv(null, null));

    Assertions.assertEquals(2, result.getData().getEdges().size());
    Assertions.assertFalse(result.getData().getPageInfo().isHasNextPage());
  }

  @Test
  public void should_fetch_articles_backward() {
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), isNull(), any(), eq(user)))
        .thenReturn(pager(Direction.PREV, false));

    articleDatafetcher.getArticles(
        null, null, 3, null, null, null, null, GraphQLTestSupport.dgsEnv(null, null));

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(articleQueryService)
        .findRecentArticlesWithCursor(isNull(), isNull(), isNull(), captor.capture(), eq(user));
    Assertions.assertEquals(Direction.PREV, captor.getValue().getDirection());
    Assertions.assertNull(captor.getValue().getCursor());
  }

  @Test
  public void should_fetch_profile_feed() {
    User target = new User("b@test.com", "b", "123", "", "");
    when(userRepository.findByUsername("b")).thenReturn(Optional.of(target));
    when(articleQueryService.findUserFeedWithCursor(eq(target), any()))
        .thenReturn(pager(Direction.NEXT, false));
    Profile profile = Profile.newBuilder().username("b").build();

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(10, null, null, null, GraphQLTestSupport.dgsEnv(profile, null));

    Assertions.assertEquals(2, result.getData().getEdges().size());
  }

  @Test
  public void should_fetch_profile_feed_backward() {
    User target = new User("b@test.com", "b", "123", "", "");
    when(userRepository.findByUsername("b")).thenReturn(Optional.of(target));
    when(articleQueryService.findUserFeedWithCursor(eq(target), any()))
        .thenReturn(pager(Direction.PREV, false));

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(
            null,
            null,
            10,
            null,
            GraphQLTestSupport.dgsEnv(Profile.newBuilder().username("b").build(), null));

    Assertions.assertEquals(2, result.getData().getEdges().size());
  }

  @Test
  public void should_fail_profile_feed_for_unknown_user() {
    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class,
        () ->
            articleDatafetcher.userFeed(
                10,
                null,
                null,
                null,
                GraphQLTestSupport.dgsEnv(Profile.newBuilder().username("unknown").build(), null)));
  }

  @Test
  public void should_fetch_profile_favorites() {
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("b"), any(), eq(user)))
        .thenReturn(pager(Direction.NEXT, false));
    Profile profile = Profile.newBuilder().username("b").build();

    Assertions.assertEquals(
        2,
        articleDatafetcher
            .userFavorites(10, null, null, null, GraphQLTestSupport.dgsEnv(profile, null))
            .getData()
            .getEdges()
            .size());
    Assertions.assertEquals(
        2,
        articleDatafetcher
            .userFavorites(null, null, 10, null, GraphQLTestSupport.dgsEnv(profile, null))
            .getData()
            .getEdges()
            .size());
  }

  @Test
  public void should_fetch_profile_articles() {
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("b"), isNull(), any(), eq(user)))
        .thenReturn(pager(Direction.NEXT, false));
    Profile profile = Profile.newBuilder().username("b").build();

    Assertions.assertEquals(
        2,
        articleDatafetcher
            .userArticles(10, null, null, null, GraphQLTestSupport.dgsEnv(profile, null))
            .getData()
            .getEdges()
            .size());
    Assertions.assertEquals(
        2,
        articleDatafetcher
            .userArticles(null, null, 10, null, GraphQLTestSupport.dgsEnv(profile, null))
            .getData()
            .getEdges()
            .size());
  }

  @Test
  public void should_fetch_article_from_payload() {
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "title", "desc", "body", Arrays.asList("java"), user.getId());
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);
    when(articleQueryService.findById(article.getId(), user)).thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result =
        articleDatafetcher.getArticle(GraphQLTestSupport.env(null, article));

    Article data = result.getData();
    Assertions.assertEquals(articleData.getSlug(), data.getSlug());
    Assertions.assertEquals(articleData.getTitle(), data.getTitle());
    Assertions.assertEquals(articleData.getDescription(), data.getDescription());
    Assertions.assertEquals(articleData.getBody(), data.getBody());
    Assertions.assertEquals(articleData.getTagList(), data.getTagList());
    Assertions.assertFalse(data.getFavorited());
    Assertions.assertEquals(0, data.getFavoritesCount());
    Assertions.assertEquals(
        ISODateTimeFormat.dateTime().withZoneUTC().print(articleData.getCreatedAt()),
        data.getCreatedAt());
    Assertions.assertSame(
        articleData, ((Map<?, ?>) result.getLocalContext()).get(articleData.getSlug()));
  }

  @Test
  public void should_fail_to_fetch_missing_article_from_payload() {
    io.spring.core.article.Article article =
        new io.spring.core.article.Article(
            "title", "desc", "body", Arrays.asList("java"), user.getId());
    when(articleQueryService.findById(article.getId(), user)).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class,
        () -> articleDatafetcher.getArticle(GraphQLTestSupport.env(null, article)));
  }

  @Test
  public void should_fetch_article_of_comment() {
    CommentData commentData =
        new CommentData("c1", "body", articleData1.getId(), new DateTime(), new DateTime(), null);
    when(articleQueryService.findById(articleData1.getId(), user))
        .thenReturn(Optional.of(articleData1));

    DataFetcherResult<Article> result =
        articleDatafetcher.getCommentArticle(GraphQLTestSupport.env(null, commentData));

    Assertions.assertEquals(articleData1.getSlug(), result.getData().getSlug());
  }

  @Test
  public void should_find_article_by_slug() {
    GraphQLTestSupport.anonymous();
    when(articleQueryService.findBySlug(articleData1.getSlug(), null))
        .thenReturn(Optional.of(articleData1));

    DataFetcherResult<Article> result =
        articleDatafetcher.findArticleBySlug(articleData1.getSlug());

    Assertions.assertEquals(articleData1.getTitle(), result.getData().getTitle());
    Assertions.assertSame(
        articleData1, ((Map<?, ?>) result.getLocalContext()).get(articleData1.getSlug()));
  }

  @Test
  public void should_fail_to_find_unknown_slug() {
    when(articleQueryService.findBySlug("unknown", user)).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class, () -> articleDatafetcher.findArticleBySlug("unknown"));
  }
}
