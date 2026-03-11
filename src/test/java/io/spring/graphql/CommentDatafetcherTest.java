package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.execution.DataFetcherResult;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.Comment;
import io.spring.graphql.types.CommentsConnection;
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
public class CommentDatafetcherTest {

  @Mock private CommentQueryService commentQueryService;
  @Mock private DgsDataFetchingEnvironment dfe;

  private CommentDatafetcher commentDatafetcher;
  private User user;
  private CommentData commentData;
  private ArticleData articleData;

  @BeforeEach
  void setUp() {
    commentDatafetcher = new CommentDatafetcher(commentQueryService);
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    DateTime now = DateTime.now();
    ProfileData profileData =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    commentData = new CommentData("comment-id", "comment body", "article-id", now, now, profileData);
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
            profileData);
  }

  @Test
  void getComment_returnsComment() {
    when(dfe.getLocalContext()).thenReturn(commentData);

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(commentData.getId(), result.getData().getId());
    assertEquals(commentData.getBody(), result.getData().getBody());
  }

  @Test
  void getComment_setsLocalContext() {
    when(dfe.getLocalContext()).thenReturn(commentData);

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dfe);

    assertNotNull(result.getLocalContext());
    assertTrue(result.getLocalContext() instanceof Map);
    @SuppressWarnings("unchecked")
    Map<String, Object> context = (Map<String, Object>) result.getLocalContext();
    assertEquals(commentData, context.get(commentData.getId()));
  }

  @Test
  void articleComments_withFirstParameter_returnsCommentsConnection() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      Article article = Article.newBuilder().slug("test-slug").build();
      when(dfe.getSource()).thenReturn(article);

      Map<String, ArticleData> articleMap = new HashMap<>();
      articleMap.put("test-slug", articleData);
      when(dfe.getLocalContext()).thenReturn(articleMap);

      CursorPager<CommentData> cursorPager =
          new CursorPager<>(Arrays.asList(commentData), Direction.NEXT, true);
      when(commentQueryService.findByArticleIdWithCursor(
              eq(articleData.getId()), eq(user), any(CursorPageParameter.class)))
          .thenReturn(cursorPager);

      DataFetcherResult<CommentsConnection> result =
          commentDatafetcher.articleComments(10, null, null, null, dfe);

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(1, result.getData().getEdges().size());
      assertEquals(commentData.getId(), result.getData().getEdges().get(0).getNode().getId());
    }
  }

  @Test
  void articleComments_withLastParameter_returnsCommentsConnection() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      Article article = Article.newBuilder().slug("test-slug").build();
      when(dfe.getSource()).thenReturn(article);

      Map<String, ArticleData> articleMap = new HashMap<>();
      articleMap.put("test-slug", articleData);
      when(dfe.getLocalContext()).thenReturn(articleMap);

      CursorPager<CommentData> cursorPager =
          new CursorPager<>(Arrays.asList(commentData), Direction.PREV, true);
      when(commentQueryService.findByArticleIdWithCursor(
              eq(articleData.getId()), eq(user), any(CursorPageParameter.class)))
          .thenReturn(cursorPager);

      DataFetcherResult<CommentsConnection> result =
          commentDatafetcher.articleComments(null, null, 10, null, dfe);

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(1, result.getData().getEdges().size());
    }
  }

  @Test
  void articleComments_withoutFirstOrLast_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> commentDatafetcher.articleComments(null, null, null, null, dfe));
  }

  @Test
  void articleComments_withoutAuthenticatedUser_returnsCommentsConnection() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      Article article = Article.newBuilder().slug("test-slug").build();
      when(dfe.getSource()).thenReturn(article);

      Map<String, ArticleData> articleMap = new HashMap<>();
      articleMap.put("test-slug", articleData);
      when(dfe.getLocalContext()).thenReturn(articleMap);

      CursorPager<CommentData> cursorPager =
          new CursorPager<>(Arrays.asList(commentData), Direction.NEXT, true);
      when(commentQueryService.findByArticleIdWithCursor(
              eq(articleData.getId()), eq(null), any(CursorPageParameter.class)))
          .thenReturn(cursorPager);

      DataFetcherResult<CommentsConnection> result =
          commentDatafetcher.articleComments(10, null, null, null, dfe);

      assertNotNull(result);
      verify(commentQueryService)
          .findByArticleIdWithCursor(
              eq(articleData.getId()), eq(null), any(CursorPageParameter.class));
    }
  }

  @Test
  void articleComments_withEmptyResults_returnsEmptyConnection() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      Article article = Article.newBuilder().slug("test-slug").build();
      when(dfe.getSource()).thenReturn(article);

      Map<String, ArticleData> articleMap = new HashMap<>();
      articleMap.put("test-slug", articleData);
      when(dfe.getLocalContext()).thenReturn(articleMap);

      CursorPager<CommentData> cursorPager = new CursorPager<>(Arrays.asList(), Direction.NEXT, false);
      when(commentQueryService.findByArticleIdWithCursor(
              eq(articleData.getId()), eq(user), any(CursorPageParameter.class)))
          .thenReturn(cursorPager);

      DataFetcherResult<CommentsConnection> result =
          commentDatafetcher.articleComments(10, null, null, null, dfe);

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(0, result.getData().getEdges().size());
    }
  }

  @Test
  void articleComments_withAfterCursor_passesNullCursorWhenInvalid() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      Article article = Article.newBuilder().slug("test-slug").build();
      when(dfe.getSource()).thenReturn(article);

      Map<String, ArticleData> articleMap = new HashMap<>();
      articleMap.put("test-slug", articleData);
      when(dfe.getLocalContext()).thenReturn(articleMap);

      CursorPager<CommentData> cursorPager =
          new CursorPager<>(Arrays.asList(commentData), Direction.NEXT, true);
      when(commentQueryService.findByArticleIdWithCursor(
              eq(articleData.getId()), eq(user), any(CursorPageParameter.class)))
          .thenReturn(cursorPager);

      DataFetcherResult<CommentsConnection> result =
          commentDatafetcher.articleComments(10, null, null, null, dfe);

      assertNotNull(result);
      assertNotNull(result.getData());
      assertNotNull(result.getData().getPageInfo());
    }
  }

  @Test
  void articleComments_withBeforeCursor_passesNullCursorWhenInvalid() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      Article article = Article.newBuilder().slug("test-slug").build();
      when(dfe.getSource()).thenReturn(article);

      Map<String, ArticleData> articleMap = new HashMap<>();
      articleMap.put("test-slug", articleData);
      when(dfe.getLocalContext()).thenReturn(articleMap);

      CursorPager<CommentData> cursorPager =
          new CursorPager<>(Arrays.asList(commentData), Direction.PREV, true);
      when(commentQueryService.findByArticleIdWithCursor(
              eq(articleData.getId()), eq(user), any(CursorPageParameter.class)))
          .thenReturn(cursorPager);

      DataFetcherResult<CommentsConnection> result =
          commentDatafetcher.articleComments(null, null, 10, null, dfe);

      assertNotNull(result);
      assertNotNull(result.getData());
    }
  }

  @Test
  void articleComments_setsLocalContextWithCommentMap() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      Article article = Article.newBuilder().slug("test-slug").build();
      when(dfe.getSource()).thenReturn(article);

      Map<String, ArticleData> articleMap = new HashMap<>();
      articleMap.put("test-slug", articleData);
      when(dfe.getLocalContext()).thenReturn(articleMap);

      CursorPager<CommentData> cursorPager =
          new CursorPager<>(Arrays.asList(commentData), Direction.NEXT, true);
      when(commentQueryService.findByArticleIdWithCursor(
              eq(articleData.getId()), eq(user), any(CursorPageParameter.class)))
          .thenReturn(cursorPager);

      DataFetcherResult<CommentsConnection> result =
          commentDatafetcher.articleComments(10, null, null, null, dfe);

      assertNotNull(result.getLocalContext());
      assertTrue(result.getLocalContext() instanceof Map);
      @SuppressWarnings("unchecked")
      Map<String, CommentData> context = (Map<String, CommentData>) result.getLocalContext();
      assertEquals(commentData, context.get(commentData.getId()));
    }
  }

  @Test
  void articleComments_withMultipleComments_returnsAllComments() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      Article article = Article.newBuilder().slug("test-slug").build();
      when(dfe.getSource()).thenReturn(article);

      Map<String, ArticleData> articleMap = new HashMap<>();
      articleMap.put("test-slug", articleData);
      when(dfe.getLocalContext()).thenReturn(articleMap);

      DateTime now = DateTime.now();
      ProfileData profileData =
          new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
      CommentData comment2 =
          new CommentData("comment-id-2", "second comment", "article-id", now, now, profileData);

      CursorPager<CommentData> cursorPager =
          new CursorPager<>(Arrays.asList(commentData, comment2), Direction.NEXT, true);
      when(commentQueryService.findByArticleIdWithCursor(
              eq(articleData.getId()), eq(user), any(CursorPageParameter.class)))
          .thenReturn(cursorPager);

      DataFetcherResult<CommentsConnection> result =
          commentDatafetcher.articleComments(10, null, null, null, dfe);

      assertNotNull(result);
      assertEquals(2, result.getData().getEdges().size());
    }
  }
}
