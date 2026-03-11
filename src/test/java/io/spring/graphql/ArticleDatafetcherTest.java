package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import io.spring.core.article.Article;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.ArticlesConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleDatafetcherTest {

  @Mock private ArticleQueryService articleQueryService;
  @Mock private UserRepository userRepository;
  @Mock private DgsDataFetchingEnvironment dfe;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private ArticleDatafetcher articleDatafetcher;
  private User user;
  private ArticleData articleData;

  @BeforeEach
  void setUp() {
    articleDatafetcher = new ArticleDatafetcher(articleQueryService, userRepository);
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    DateTime now = DateTime.now();
    articleData =
        new ArticleData(
            "article-id",
            "test-slug",
            "Test Title",
            "Test Description",
            "Test Body",
            false,
            0,
            now,
            now,
            Arrays.asList("tag1", "tag2"),
            new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
  }

  @Test
  void getFeed_withFirstParameter_returnsArticlesConnection() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      CursorPager<ArticleData> cursorPager =
          new CursorPager<>(Arrays.asList(articleData), Direction.NEXT, true);
      when(articleQueryService.findUserFeedWithCursor(eq(user), any(CursorPageParameter.class)))
          .thenReturn(cursorPager);

      DataFetcherResult<ArticlesConnection> result =
          articleDatafetcher.getFeed(10, null, null, null, dfe);

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(1, result.getData().getEdges().size());
      verify(articleQueryService).findUserFeedWithCursor(eq(user), any(CursorPageParameter.class));
    }
  }

  @Test
  void getFeed_withLastParameter_returnsArticlesConnection() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      CursorPager<ArticleData> cursorPager =
          new CursorPager<>(Arrays.asList(articleData), Direction.PREV, true);
      when(articleQueryService.findUserFeedWithCursor(eq(user), any(CursorPageParameter.class)))
          .thenReturn(cursorPager);

      DataFetcherResult<ArticlesConnection> result =
          articleDatafetcher.getFeed(null, null, 10, null, dfe);

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(1, result.getData().getEdges().size());
    }
  }

  @Test
  void getFeed_withoutFirstOrLast_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getFeed(null, null, null, null, dfe));
  }

  @Test
  void getFeed_withoutAuthenticatedUser_returnsArticlesConnection() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      CursorPager<ArticleData> cursorPager =
          new CursorPager<>(Arrays.asList(articleData), Direction.NEXT, true);
      when(articleQueryService.findUserFeedWithCursor(eq(null), any(CursorPageParameter.class)))
          .thenReturn(cursorPager);

      DataFetcherResult<ArticlesConnection> result =
          articleDatafetcher.getFeed(10, null, null, null, dfe);

      assertNotNull(result);
      verify(articleQueryService).findUserFeedWithCursor(eq(null), any(CursorPageParameter.class));
    }
  }

  @Test
  void userFeed_withFirstParameter_returnsArticlesConnection() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      io.spring.graphql.types.Profile profile =
          io.spring.graphql.types.Profile.newBuilder().username(user.getUsername()).build();
      when(dfe.getSource()).thenReturn(profile);
      when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

      CursorPager<ArticleData> cursorPager =
          new CursorPager<>(Arrays.asList(articleData), Direction.NEXT, true);
      when(articleQueryService.findUserFeedWithCursor(eq(user), any(CursorPageParameter.class)))
          .thenReturn(cursorPager);

      DataFetcherResult<ArticlesConnection> result =
          articleDatafetcher.userFeed(10, null, null, null, dfe);

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(1, result.getData().getEdges().size());
    }
  }

  @Test
  void userFeed_withLastParameter_returnsArticlesConnection() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      io.spring.graphql.types.Profile profile =
          io.spring.graphql.types.Profile.newBuilder().username(user.getUsername()).build();
      when(dfe.getSource()).thenReturn(profile);
      when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

      CursorPager<ArticleData> cursorPager =
          new CursorPager<>(Arrays.asList(articleData), Direction.PREV, true);
      when(articleQueryService.findUserFeedWithCursor(eq(user), any(CursorPageParameter.class)))
          .thenReturn(cursorPager);

      DataFetcherResult<ArticlesConnection> result =
          articleDatafetcher.userFeed(null, null, 10, null, dfe);

      assertNotNull(result);
      assertNotNull(result.getData());
    }
  }

  @Test
  void userFeed_withoutFirstOrLast_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFeed(null, null, null, null, dfe));
  }

  @Test
  void userFeed_userNotFound_throwsResourceNotFoundException() {
    io.spring.graphql.types.Profile profile =
        io.spring.graphql.types.Profile.newBuilder().username("nonexistent").build();
    when(dfe.getSource()).thenReturn(profile);
    when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleDatafetcher.userFeed(10, null, null, null, dfe));
  }

  @Test
  void userFavorites_withFirstParameter_returnsArticlesConnection() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      io.spring.graphql.types.Profile profile =
          io.spring.graphql.types.Profile.newBuilder().username(user.getUsername()).build();
      when(dfe.getSource()).thenReturn(profile);

      CursorPager<ArticleData> cursorPager =
          new CursorPager<>(Arrays.asList(articleData), Direction.NEXT, true);
      when(articleQueryService.findRecentArticlesWithCursor(
              eq(null), eq(null), eq(user.getUsername()), any(CursorPageParameter.class), eq(user)))
          .thenReturn(cursorPager);

      DataFetcherResult<ArticlesConnection> result =
          articleDatafetcher.userFavorites(10, null, null, null, dfe);

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(1, result.getData().getEdges().size());
    }
  }

  @Test
  void userFavorites_withLastParameter_returnsArticlesConnection() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      io.spring.graphql.types.Profile profile =
          io.spring.graphql.types.Profile.newBuilder().username(user.getUsername()).build();
      when(dfe.getSource()).thenReturn(profile);

      CursorPager<ArticleData> cursorPager =
          new CursorPager<>(Arrays.asList(articleData), Direction.PREV, true);
      when(articleQueryService.findRecentArticlesWithCursor(
              eq(null), eq(null), eq(user.getUsername()), any(CursorPageParameter.class), eq(user)))
          .thenReturn(cursorPager);

      DataFetcherResult<ArticlesConnection> result =
          articleDatafetcher.userFavorites(null, null, 10, null, dfe);

      assertNotNull(result);
      assertNotNull(result.getData());
    }
  }

  @Test
  void userFavorites_withoutFirstOrLast_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userFavorites(null, null, null, null, dfe));
  }

  @Test
  void userArticles_withFirstParameter_returnsArticlesConnection() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      io.spring.graphql.types.Profile profile =
          io.spring.graphql.types.Profile.newBuilder().username(user.getUsername()).build();
      when(dfe.getSource()).thenReturn(profile);

      CursorPager<ArticleData> cursorPager =
          new CursorPager<>(Arrays.asList(articleData), Direction.NEXT, true);
      when(articleQueryService.findRecentArticlesWithCursor(
              eq(null), eq(user.getUsername()), eq(null), any(CursorPageParameter.class), eq(user)))
          .thenReturn(cursorPager);

      DataFetcherResult<ArticlesConnection> result =
          articleDatafetcher.userArticles(10, null, null, null, dfe);

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(1, result.getData().getEdges().size());
    }
  }

  @Test
  void userArticles_withLastParameter_returnsArticlesConnection() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      io.spring.graphql.types.Profile profile =
          io.spring.graphql.types.Profile.newBuilder().username(user.getUsername()).build();
      when(dfe.getSource()).thenReturn(profile);

      CursorPager<ArticleData> cursorPager =
          new CursorPager<>(Arrays.asList(articleData), Direction.PREV, true);
      when(articleQueryService.findRecentArticlesWithCursor(
              eq(null), eq(user.getUsername()), eq(null), any(CursorPageParameter.class), eq(user)))
          .thenReturn(cursorPager);

      DataFetcherResult<ArticlesConnection> result =
          articleDatafetcher.userArticles(null, null, 10, null, dfe);

      assertNotNull(result);
      assertNotNull(result.getData());
    }
  }

  @Test
  void userArticles_withoutFirstOrLast_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.userArticles(null, null, null, null, dfe));
  }

  @Test
  void getArticles_withFirstParameter_returnsArticlesConnection() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      CursorPager<ArticleData> cursorPager =
          new CursorPager<>(Arrays.asList(articleData), Direction.NEXT, true);
      when(articleQueryService.findRecentArticlesWithCursor(
              any(), any(), any(), any(CursorPageParameter.class), eq(user)))
          .thenReturn(cursorPager);

      DataFetcherResult<ArticlesConnection> result =
          articleDatafetcher.getArticles(10, null, null, null, "author", "favorited", "tag", dfe);

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(1, result.getData().getEdges().size());
    }
  }

  @Test
  void getArticles_withLastParameter_returnsArticlesConnection() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      CursorPager<ArticleData> cursorPager =
          new CursorPager<>(Arrays.asList(articleData), Direction.PREV, true);
      when(articleQueryService.findRecentArticlesWithCursor(
              any(), any(), any(), any(CursorPageParameter.class), eq(user)))
          .thenReturn(cursorPager);

      DataFetcherResult<ArticlesConnection> result =
          articleDatafetcher.getArticles(null, null, 10, null, null, null, null, dfe);

      assertNotNull(result);
      assertNotNull(result.getData());
    }
  }

  @Test
  void getArticles_withoutFirstOrLast_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> articleDatafetcher.getArticles(null, null, null, null, null, null, null, dfe));
  }

  @Test
  void getArticle_returnsArticle() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      Article article = new Article("title", "description", "body", Arrays.asList("tag"), user.getId());
      when(dataFetchingEnvironment.getLocalContext()).thenReturn(article);
      when(articleQueryService.findById(article.getId(), user)).thenReturn(Optional.of(articleData));

      DataFetcherResult<io.spring.graphql.types.Article> result =
          articleDatafetcher.getArticle(dataFetchingEnvironment);

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(articleData.getSlug(), result.getData().getSlug());
    }
  }

  @Test
  void getArticle_articleNotFound_throwsResourceNotFoundException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      Article article = new Article("title", "description", "body", Arrays.asList("tag"), user.getId());
      when(dataFetchingEnvironment.getLocalContext()).thenReturn(article);
      when(articleQueryService.findById(article.getId(), user)).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class,
          () -> articleDatafetcher.getArticle(dataFetchingEnvironment));
    }
  }

  @Test
  void getCommentArticle_returnsArticle() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      DateTime now = DateTime.now();
      CommentData commentData =
          new CommentData(
              "comment-id",
              "comment body",
              articleData.getId(),
              now,
              now,
              new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
      when(dataFetchingEnvironment.getLocalContext()).thenReturn(commentData);
      when(articleQueryService.findById(commentData.getArticleId(), user))
          .thenReturn(Optional.of(articleData));

      DataFetcherResult<io.spring.graphql.types.Article> result =
          articleDatafetcher.getCommentArticle(dataFetchingEnvironment);

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(articleData.getSlug(), result.getData().getSlug());
    }
  }

  @Test
  void getCommentArticle_articleNotFound_throwsResourceNotFoundException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      DateTime now = DateTime.now();
      CommentData commentData =
          new CommentData(
              "comment-id",
              "comment body",
              "nonexistent-article-id",
              now,
              now,
              new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
      when(dataFetchingEnvironment.getLocalContext()).thenReturn(commentData);
      when(articleQueryService.findById("nonexistent-article-id", user)).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class,
          () -> articleDatafetcher.getCommentArticle(dataFetchingEnvironment));
    }
  }

  @Test
  void findArticleBySlug_returnsArticle() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleQueryService.findBySlug("test-slug", user)).thenReturn(Optional.of(articleData));

      DataFetcherResult<io.spring.graphql.types.Article> result =
          articleDatafetcher.findArticleBySlug("test-slug");

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals("test-slug", result.getData().getSlug());
      assertEquals("Test Title", result.getData().getTitle());
    }
  }

  @Test
  void findArticleBySlug_articleNotFound_throwsResourceNotFoundException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleQueryService.findBySlug("nonexistent", user)).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class,
          () -> articleDatafetcher.findArticleBySlug("nonexistent"));
    }
  }

  @Test
  void findArticleBySlug_withoutAuthenticatedUser_returnsArticle() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      when(articleQueryService.findBySlug("test-slug", null)).thenReturn(Optional.of(articleData));

      DataFetcherResult<io.spring.graphql.types.Article> result =
          articleDatafetcher.findArticleBySlug("test-slug");

      assertNotNull(result);
      assertNotNull(result.getData());
    }
  }

  @Test
  void getFeed_withEmptyResults_returnsEmptyConnection() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      CursorPager<ArticleData> cursorPager = new CursorPager<>(Arrays.asList(), Direction.NEXT, false);
      when(articleQueryService.findUserFeedWithCursor(eq(user), any(CursorPageParameter.class)))
          .thenReturn(cursorPager);

      DataFetcherResult<ArticlesConnection> result =
          articleDatafetcher.getFeed(10, null, null, null, dfe);

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(0, result.getData().getEdges().size());
    }
  }

  @Test
  void getArticles_withAllFilters_returnsFilteredArticles() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      CursorPager<ArticleData> cursorPager =
          new CursorPager<>(Arrays.asList(articleData), Direction.NEXT, true);
      when(articleQueryService.findRecentArticlesWithCursor(
              eq("java"), eq("author"), eq("favorited"), any(CursorPageParameter.class), eq(user)))
          .thenReturn(cursorPager);

      DataFetcherResult<ArticlesConnection> result =
          articleDatafetcher.getArticles(10, null, null, null, "author", "favorited", "java", dfe);

      assertNotNull(result);
      verify(articleQueryService)
          .findRecentArticlesWithCursor(
              eq("java"), eq("author"), eq("favorited"), any(CursorPageParameter.class), eq(user));
    }
  }
}
