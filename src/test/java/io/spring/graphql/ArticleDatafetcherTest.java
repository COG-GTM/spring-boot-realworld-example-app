package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import java.util.*;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ArticleDatafetcherTest {

  @Mock private ArticleQueryService articleQueryService;
  @Mock private UserRepository userRepository;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private DgsDataFetchingEnvironment dfe;

  private ArticleDatafetcher articleDatafetcher;
  private User currentUser;

  @BeforeEach
  void setUp() {
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    currentUser = new User("test@example.com", "testuser", "password", "", "");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(currentUser, null);
    SecurityContextHolder.getContext().setAuthentication(auth);
    dfe = new DgsDataFetchingEnvironment(dataFetchingEnvironment);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private ArticleData createArticleData(String id, String slug, String title) {
    ProfileData profileData = new ProfileData("authorId", "author", "bio", "img", false);
    return new ArticleData(
        id,
        slug,
        title,
        "desc",
        "body",
        false,
        0,
        new DateTime(),
        new DateTime(),
        Collections.emptyList(),
        profileData);
  }

  @Test
  void should_get_feed_with_first_param() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);
    when(articleQueryService.findUserFeedWithCursor(any(), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertTrue(result.getData().getEdges().isEmpty());
  }

  @Test
  void should_get_feed_with_last_param() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.PREV, false);
    when(articleQueryService.findUserFeedWithCursor(any(), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(null, null, 10, null, dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
  }

  @Test
  void should_throw_when_feed_has_no_first_or_last() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getFeed(null, null, null, null, dfe));
  }

  @Test
  void should_get_feed_with_articles() {
    ArticleData article = createArticleData("id1", "slug1", "Title 1");
    CursorPager<ArticleData> pager =
        new CursorPager<>(Arrays.asList(article), Direction.NEXT, true);
    when(articleQueryService.findUserFeedWithCursor(any(), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getFeed(10, null, null, null, dfe);

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
    assertEquals("slug1", result.getData().getEdges().get(0).getNode().getSlug());
  }

  @Test
  void should_get_articles_with_first_param() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            any(), any(), any(), any(CursorPageParameter.class), any()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, null, null, null, dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
  }

  @Test
  void should_get_articles_with_last_param() {
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.PREV, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            any(), any(), any(), any(CursorPageParameter.class), any()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(null, null, 5, null, null, null, null, dfe);

    assertNotNull(result);
  }

  @Test
  void should_throw_when_articles_has_no_first_or_last() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getArticles(null, null, null, null, null, null, null, dfe));
  }

  @Test
  void should_get_articles_with_filters() {
    ArticleData article = createArticleData("id1", "slug1", "Title 1");
    CursorPager<ArticleData> pager =
        new CursorPager<>(Arrays.asList(article), Direction.NEXT, false);
    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), eq("author1"), eq("user1"), any(CursorPageParameter.class), any()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.getArticles(10, null, null, null, "author1", "user1", "java", dfe);

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void should_find_article_by_slug() {
    ArticleData articleData = createArticleData("id1", "test-slug", "Test Title");
    when(articleQueryService.findBySlug(eq("test-slug"), any()))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.findArticleBySlug("test-slug");

    assertNotNull(result);
    assertEquals("test-slug", result.getData().getSlug());
    assertEquals("Test Title", result.getData().getTitle());
  }

  @Test
  void should_throw_when_article_not_found_by_slug() {
    when(articleQueryService.findBySlug(eq("nonexistent"), any())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleDatafetcher.findArticleBySlug("nonexistent"));
  }

  @Test
  void should_get_article_from_payload() {
    io.spring.core.article.Article coreArticle =
        new io.spring.core.article.Article(
            "Test Title", "desc", "body", Collections.emptyList(), "userId");
    ArticleData articleData =
        createArticleData(coreArticle.getId(), coreArticle.getSlug(), "Test Title");

    when(dataFetchingEnvironment.getLocalContext()).thenReturn(coreArticle);
    when(articleQueryService.findById(eq(coreArticle.getId()), any()))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result = articleDatafetcher.getArticle(dataFetchingEnvironment);

    assertNotNull(result);
    assertNotNull(result.getData());
  }

  @Test
  void should_get_comment_article() {
    CommentData commentData =
        new CommentData(
            "commentId",
            "comment body",
            "articleId1",
            new DateTime(),
            new DateTime(),
            new ProfileData("authorId", "author", "bio", "img", false));
    ArticleData articleData = createArticleData("articleId1", "slug1", "Title");

    when(dataFetchingEnvironment.getLocalContext()).thenReturn(commentData);
    when(articleQueryService.findById(eq("articleId1"), any()))
        .thenReturn(Optional.of(articleData));

    DataFetcherResult<Article> result =
        articleDatafetcher.getCommentArticle(dataFetchingEnvironment);

    assertNotNull(result);
    assertNotNull(result.getData());
  }

  @Test
  void should_get_user_feed_from_profile() {
    Profile profile = Profile.newBuilder().username("targetuser").build();
    User targetUser = new User("target@example.com", "targetuser", "pass", "", "");
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    when(dataFetchingEnvironment.getSource()).thenReturn(profile);
    when(userRepository.findByUsername("targetuser")).thenReturn(Optional.of(targetUser));
    when(articleQueryService.findUserFeedWithCursor(any(), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(10, null, null, null, dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
  }

  @Test
  void should_get_user_feed_with_last_param() {
    Profile profile = Profile.newBuilder().username("targetuser").build();
    User targetUser = new User("target@example.com", "targetuser", "pass", "", "");
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.PREV, false);

    when(dataFetchingEnvironment.getSource()).thenReturn(profile);
    when(userRepository.findByUsername("targetuser")).thenReturn(Optional.of(targetUser));
    when(articleQueryService.findUserFeedWithCursor(any(), any(CursorPageParameter.class)))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFeed(null, null, 5, null, dfe);

    assertNotNull(result);
  }

  @Test
  void should_throw_when_user_feed_has_no_first_or_last() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFeed(null, null, null, null, dfe));
  }

  @Test
  void should_get_user_favorites() {
    Profile profile = Profile.newBuilder().username("favuser").build();
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    when(dataFetchingEnvironment.getSource()).thenReturn(profile);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("favuser"), any(CursorPageParameter.class), any()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(10, null, null, null, dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
  }

  @Test
  void should_get_user_favorites_with_last_param() {
    Profile profile = Profile.newBuilder().username("favuser").build();
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.PREV, false);

    when(dataFetchingEnvironment.getSource()).thenReturn(profile);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), isNull(), eq("favuser"), any(CursorPageParameter.class), any()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userFavorites(null, null, 5, null, dfe);

    assertNotNull(result);
  }

  @Test
  void should_throw_when_user_favorites_has_no_first_or_last() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFavorites(null, null, null, null, dfe));
  }

  @Test
  void should_get_user_articles() {
    Profile profile = Profile.newBuilder().username("authoruser").build();
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.NEXT, false);

    when(dataFetchingEnvironment.getSource()).thenReturn(profile);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("authoruser"), isNull(), any(CursorPageParameter.class), any()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(10, null, null, null, dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
  }

  @Test
  void should_get_user_articles_with_last_param() {
    Profile profile = Profile.newBuilder().username("authoruser").build();
    CursorPager<ArticleData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.PREV, false);

    when(dataFetchingEnvironment.getSource()).thenReturn(profile);
    when(articleQueryService.findRecentArticlesWithCursor(
            isNull(), eq("authoruser"), isNull(), any(CursorPageParameter.class), any()))
        .thenReturn(pager);

    DataFetcherResult<ArticlesConnection> result =
        articleDatafetcher.userArticles(null, null, 5, null, dfe);

    assertNotNull(result);
  }

  @Test
  void should_throw_when_user_articles_has_no_first_or_last() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userArticles(null, null, null, null, dfe));
  }
}
