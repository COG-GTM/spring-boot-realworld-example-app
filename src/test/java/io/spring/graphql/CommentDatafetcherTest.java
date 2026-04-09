package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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

  private CommentDatafetcher commentDatafetcher;
  private User user;
  private CommentData commentData;
  private ArticleData articleData;

  @BeforeEach
  public void setUp() {
    commentDatafetcher = new CommentDatafetcher(commentQueryService);
    user = new User("john@jacob.com", "johnjacob", "123", "bio", "image");

    ProfileData profileData =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    commentData =
        new CommentData(
            "comment-id-1",
            "comment body",
            "article-id-1",
            new DateTime(),
            new DateTime(),
            profileData);
    articleData =
        new ArticleData(
            "article-id-1",
            "test-title",
            "Test Title",
            "desc",
            "body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            new ArrayList<>(),
            profileData);

    TestingAuthenticationToken auth = new TestingAuthenticationToken(user, null);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private DgsDataFetchingEnvironment createDfe(DataFetchingEnvironment delegate) {
    return new DgsDataFetchingEnvironment(delegate);
  }

  @Test
  public void should_get_comment_from_local_context() {
    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    when(delegate.getLocalContext()).thenReturn(commentData);
    DgsDataFetchingEnvironment dfe = createDfe(delegate);

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals("comment-id-1", result.getData().getId());
    assertEquals("comment body", result.getData().getBody());
    assertNotNull(result.getLocalContext());
  }

  @Test
  public void should_get_article_comments_with_first_parameter() {
    Article article = Article.newBuilder().slug("test-title").build();
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put("test-title", articleData);

    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    when(delegate.getSource()).thenReturn(article);
    when(delegate.getLocalContext()).thenReturn(localContext);
    DgsDataFetchingEnvironment dfe = createDfe(delegate);

    CursorPager<CommentData> cursorPager =
        new CursorPager<>(Arrays.asList(commentData), Direction.NEXT, false);
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id-1"), eq(user), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(1, result.getData().getEdges().size());
    assertEquals("comment-id-1", result.getData().getEdges().get(0).getNode().getId());
    assertEquals("comment body", result.getData().getEdges().get(0).getNode().getBody());
  }

  @Test
  public void should_get_article_comments_with_last_parameter() {
    Article article = Article.newBuilder().slug("test-title").build();
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put("test-title", articleData);

    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    when(delegate.getSource()).thenReturn(article);
    when(delegate.getLocalContext()).thenReturn(localContext);
    DgsDataFetchingEnvironment dfe = createDfe(delegate);

    CursorPager<CommentData> cursorPager =
        new CursorPager<>(Arrays.asList(commentData), Direction.PREV, false);
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id-1"), eq(user), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(null, null, 5, null, dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  public void should_throw_exception_when_both_first_and_last_are_null() {
    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    DgsDataFetchingEnvironment dfe = createDfe(delegate);

    assertThrows(
        IllegalArgumentException.class,
        () -> commentDatafetcher.articleComments(null, null, null, null, dfe));
  }

  @Test
  public void should_get_article_comments_with_empty_result() {
    Article article = Article.newBuilder().slug("test-title").build();
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put("test-title", articleData);

    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    when(delegate.getSource()).thenReturn(article);
    when(delegate.getLocalContext()).thenReturn(localContext);
    DgsDataFetchingEnvironment dfe = createDfe(delegate);

    CursorPager<CommentData> cursorPager =
        new CursorPager<>(new ArrayList<>(), Direction.NEXT, false);
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id-1"), eq(user), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(0, result.getData().getEdges().size());
  }

  @Test
  public void should_get_article_comments_with_cursor_parameters() {
    Article article = Article.newBuilder().slug("test-title").build();
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put("test-title", articleData);

    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    when(delegate.getSource()).thenReturn(article);
    when(delegate.getLocalContext()).thenReturn(localContext);
    DgsDataFetchingEnvironment dfe = createDfe(delegate);

    CursorPager<CommentData> cursorPager =
        new CursorPager<>(Arrays.asList(commentData), Direction.NEXT, true);
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id-1"), eq(user), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, "12345", null, null, dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertNotNull(result.getData().getPageInfo());
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  public void should_get_article_comments_without_authenticated_user() {
    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new org.springframework.security.authentication.AnonymousAuthenticationToken(
                "key",
                "anonymous",
                Arrays.asList(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_ANONYMOUS"))));

    Article article = Article.newBuilder().slug("test-title").build();
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put("test-title", articleData);

    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    when(delegate.getSource()).thenReturn(article);
    when(delegate.getLocalContext()).thenReturn(localContext);
    DgsDataFetchingEnvironment dfe = createDfe(delegate);

    CursorPager<CommentData> cursorPager =
        new CursorPager<>(Arrays.asList(commentData), Direction.NEXT, false);
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id-1"), eq(null), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dfe);

    assertNotNull(result);
    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  public void should_get_article_comments_with_last_and_before_cursor() {
    Article article = Article.newBuilder().slug("test-title").build();
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put("test-title", articleData);

    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    when(delegate.getSource()).thenReturn(article);
    when(delegate.getLocalContext()).thenReturn(localContext);
    DgsDataFetchingEnvironment dfe = createDfe(delegate);

    CursorPager<CommentData> cursorPager =
        new CursorPager<>(Arrays.asList(commentData), Direction.PREV, true);
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id-1"), eq(user), any()))
        .thenReturn(cursorPager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(null, null, 5, "12345", dfe);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertNotNull(result.getData().getPageInfo());
    assertEquals(1, result.getData().getEdges().size());
  }
}
