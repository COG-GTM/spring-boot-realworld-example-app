package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.Comment;
import io.spring.graphql.types.CommentsConnection;
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
public class CommentDatafetcherTest {

  @Mock private CommentQueryService commentQueryService;
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private CommentDatafetcher commentDatafetcher;
  private User user;
  private CommentData commentData;
  private ArticleData articleData;

  @BeforeEach
  void setUp() {
    commentDatafetcher = new CommentDatafetcher(commentQueryService);
    user = new User("user@example.com", "testuser", "password", "bio", "image");
    ProfileData profileData = new ProfileData(user.getId(), "testuser", "bio", "image", false);
    commentData =
        new CommentData(
            "comment-id-1",
            "Test comment body",
            "article-id-1",
            new DateTime(),
            new DateTime(),
            profileData);
    articleData =
        new ArticleData(
            "article-id-1",
            "test-slug",
            "Test Title",
            "desc",
            "body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Arrays.asList("tag1"),
            profileData);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private DgsDataFetchingEnvironment createDfe() {
    return new DgsDataFetchingEnvironment(dataFetchingEnvironment);
  }

  @Test
  void getComment_shouldReturnCommentResult() {
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(commentData);

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(createDfe());

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals("comment-id-1", result.getData().getId());
    assertEquals("Test comment body", result.getData().getBody());
    assertNotNull(result.getData().getCreatedAt());
    assertNotNull(result.getData().getUpdatedAt());

    @SuppressWarnings("unchecked")
    Map<String, Object> localContext = (Map<String, Object>) result.getLocalContext();
    assertNotNull(localContext);
    assertTrue(localContext.containsKey("comment-id-1"));
    assertEquals(commentData, localContext.get("comment-id-1"));
  }

  @Test
  void articleComments_withFirst_shouldReturnCommentsConnection() {
    setUpSecurityContext(user);

    Article article = Article.newBuilder().slug("test-slug").build();
    Map<String, ArticleData> articleMap = new HashMap<>();
    articleMap.put("test-slug", articleData);

    when(dataFetchingEnvironment.getSource()).thenReturn(article);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(articleMap);

    List<CommentData> commentList = Arrays.asList(commentData);
    CursorPager<CommentData> cursorPager = new CursorPager<>(commentList, Direction.NEXT, false);

    when(commentQueryService.findByArticleIdWithCursor(eq("article-id-1"), eq(user), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, createDfe());

    assertNotNull(result);
    assertNotNull(result.getData());
    assertNotNull(result.getData().getEdges());
    assertEquals(1, result.getData().getEdges().size());
    assertEquals("comment-id-1", result.getData().getEdges().get(0).getNode().getId());
    assertEquals("Test comment body", result.getData().getEdges().get(0).getNode().getBody());
    assertNotNull(result.getData().getEdges().get(0).getCursor());
    assertNotNull(result.getData().getPageInfo());
  }

  @Test
  void articleComments_withLast_shouldReturnCommentsConnection() {
    setUpSecurityContext(user);

    Article article = Article.newBuilder().slug("test-slug").build();
    Map<String, ArticleData> articleMap = new HashMap<>();
    articleMap.put("test-slug", articleData);

    when(dataFetchingEnvironment.getSource()).thenReturn(article);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(articleMap);

    List<CommentData> commentList = Arrays.asList(commentData);
    CursorPager<CommentData> cursorPager = new CursorPager<>(commentList, Direction.PREV, false);

    when(commentQueryService.findByArticleIdWithCursor(eq("article-id-1"), eq(user), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(null, null, 5, null, createDfe());

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void articleComments_withoutAuthentication_shouldUseNullUser() {
    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new org.springframework.security.authentication.AnonymousAuthenticationToken(
                "key",
                "anonymous",
                Collections.singletonList(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_ANONYMOUS"))));

    Article article = Article.newBuilder().slug("test-slug").build();
    Map<String, ArticleData> articleMap = new HashMap<>();
    articleMap.put("test-slug", articleData);

    when(dataFetchingEnvironment.getSource()).thenReturn(article);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(articleMap);

    List<CommentData> commentList = Arrays.asList(commentData);
    CursorPager<CommentData> cursorPager = new CursorPager<>(commentList, Direction.NEXT, false);

    when(commentQueryService.findByArticleIdWithCursor(eq("article-id-1"), eq(null), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, createDfe());

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void articleComments_withNeitherFirstNorLast_shouldThrowException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> commentDatafetcher.articleComments(null, null, null, null, createDfe()));
  }

  @Test
  void articleComments_withAfterCursor_shouldPassCursorToQuery() {
    setUpSecurityContext(user);

    Article article = Article.newBuilder().slug("test-slug").build();
    Map<String, ArticleData> articleMap = new HashMap<>();
    articleMap.put("test-slug", articleData);

    when(dataFetchingEnvironment.getSource()).thenReturn(article);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(articleMap);

    CursorPager<CommentData> cursorPager =
        new CursorPager<>(new ArrayList<>(), Direction.NEXT, false);

    when(commentQueryService.findByArticleIdWithCursor(eq("article-id-1"), eq(user), any()))
        .thenReturn(cursorPager);

    String afterCursor = String.valueOf(new DateTime().getMillis());
    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, afterCursor, null, null, createDfe());

    assertNotNull(result);
    assertEquals(0, result.getData().getEdges().size());
  }

  @Test
  void articleComments_withBeforeCursor_shouldPassCursorToQuery() {
    setUpSecurityContext(user);

    Article article = Article.newBuilder().slug("test-slug").build();
    Map<String, ArticleData> articleMap = new HashMap<>();
    articleMap.put("test-slug", articleData);

    when(dataFetchingEnvironment.getSource()).thenReturn(article);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(articleMap);

    CursorPager<CommentData> cursorPager =
        new CursorPager<>(new ArrayList<>(), Direction.PREV, false);

    when(commentQueryService.findByArticleIdWithCursor(eq("article-id-1"), eq(user), any()))
        .thenReturn(cursorPager);

    String beforeCursor = String.valueOf(new DateTime().getMillis());
    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(null, null, 5, beforeCursor, createDfe());

    assertNotNull(result);
    assertEquals(0, result.getData().getEdges().size());
  }

  @Test
  void articleComments_withEmptyComments_shouldReturnEmptyEdges() {
    setUpSecurityContext(user);

    Article article = Article.newBuilder().slug("test-slug").build();
    Map<String, ArticleData> articleMap = new HashMap<>();
    articleMap.put("test-slug", articleData);

    when(dataFetchingEnvironment.getSource()).thenReturn(article);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(articleMap);

    CursorPager<CommentData> cursorPager =
        new CursorPager<>(new ArrayList<>(), Direction.NEXT, false);

    when(commentQueryService.findByArticleIdWithCursor(eq("article-id-1"), eq(user), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, createDfe());

    assertNotNull(result);
    assertEquals(0, result.getData().getEdges().size());
    assertNotNull(result.getData().getPageInfo());
  }

  @Test
  void articleComments_withMultipleComments_shouldReturnAllEdges() {
    setUpSecurityContext(user);

    Article article = Article.newBuilder().slug("test-slug").build();
    Map<String, ArticleData> articleMap = new HashMap<>();
    articleMap.put("test-slug", articleData);

    when(dataFetchingEnvironment.getSource()).thenReturn(article);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(articleMap);

    ProfileData profileData = new ProfileData(user.getId(), "testuser", "bio", "image", false);
    CommentData comment2 =
        new CommentData(
            "comment-id-2",
            "Second comment",
            "article-id-1",
            new DateTime(),
            new DateTime(),
            profileData);
    List<CommentData> commentList = Arrays.asList(commentData, comment2);
    CursorPager<CommentData> cursorPager = new CursorPager<>(commentList, Direction.NEXT, true);

    when(commentQueryService.findByArticleIdWithCursor(eq("article-id-1"), eq(user), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, createDfe());

    assertNotNull(result);
    assertEquals(2, result.getData().getEdges().size());
    assertEquals("comment-id-1", result.getData().getEdges().get(0).getNode().getId());
    assertEquals("comment-id-2", result.getData().getEdges().get(1).getNode().getId());

    @SuppressWarnings("unchecked")
    Map<String, CommentData> localContext = (Map<String, CommentData>) result.getLocalContext();
    assertNotNull(localContext);
    assertEquals(2, localContext.size());
    assertTrue(localContext.containsKey("comment-id-1"));
    assertTrue(localContext.containsKey("comment-id-2"));
  }

  @Test
  void articleComments_pageInfo_shouldReflectPaginationState() {
    setUpSecurityContext(user);

    Article article = Article.newBuilder().slug("test-slug").build();
    Map<String, ArticleData> articleMap = new HashMap<>();
    articleMap.put("test-slug", articleData);

    when(dataFetchingEnvironment.getSource()).thenReturn(article);
    when(dataFetchingEnvironment.getLocalContext()).thenReturn(articleMap);

    List<CommentData> commentList = Arrays.asList(commentData);
    CursorPager<CommentData> cursorPager = new CursorPager<>(commentList, Direction.NEXT, true);

    when(commentQueryService.findByArticleIdWithCursor(eq("article-id-1"), eq(user), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(1, null, null, null, createDfe());

    assertNotNull(result);
    assertNotNull(result.getData().getPageInfo());
    assertTrue(result.getData().getPageInfo().isHasNextPage());
    assertFalse(result.getData().getPageInfo().isHasPreviousPage());
    assertNotNull(result.getData().getPageInfo().getStartCursor());
    assertNotNull(result.getData().getPageInfo().getEndCursor());
  }

  private void setUpSecurityContext(User user) {
    TestingAuthenticationToken authentication = new TestingAuthenticationToken(user, null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
