package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import io.spring.graphql.types.CommentEdge;
import io.spring.graphql.types.CommentsConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class CommentDatafetcherTest {

  private CommentQueryService commentQueryService;
  private CommentDatafetcher commentDatafetcher;

  @BeforeEach
  public void setUp() {
    commentQueryService = mock(CommentQueryService.class);
    commentDatafetcher = new CommentDatafetcher(commentQueryService);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setCurrentUser() {
    User user = new User("e@e.com", "user", "pass", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  private CommentData buildCommentData(String id, String body, DateTime createdAt) {
    ProfileData author = new ProfileData("author-id", "author", "bio", "image", false);
    return new CommentData(id, body, "article-id", createdAt, createdAt, author);
  }

  @Test
  public void should_get_comment_and_expose_it_in_local_context() {
    DateTime createdAt = new DateTime();
    CommentData commentData = buildCommentData("comment-1", "hello world", createdAt);

    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    when(delegate.<CommentData>getLocalContext()).thenReturn(commentData);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dfe);

    Comment comment = result.getData();
    assertEquals("comment-1", comment.getId());
    assertEquals("hello world", comment.getBody());

    Object localContext = result.getLocalContext();
    assertTrue(localContext instanceof Map);
    @SuppressWarnings("unchecked")
    Map<String, Object> map = (Map<String, Object>) localContext;
    assertTrue(map.containsKey("comment-1"));
    assertEquals(commentData, map.get("comment-1"));
  }

  @Test
  public void should_throw_when_both_first_and_last_are_null() {
    DgsDataFetchingEnvironment dfe =
        new DgsDataFetchingEnvironment(mock(DataFetchingEnvironment.class));
    assertThrows(
        IllegalArgumentException.class,
        () -> commentDatafetcher.articleComments(null, null, null, null, dfe));
  }

  @Test
  public void should_return_comments_connection_for_forward_pagination() {
    String slug = "how-to-train-your-dragon";
    Article article = Article.newBuilder().slug(slug).build();

    ArticleData articleData = new ArticleData();
    articleData.setId("article-1");
    articleData.setSlug(slug);
    Map<String, ArticleData> localContext = Collections.singletonMap(slug, articleData);

    setCurrentUser();
    DateTime createdAt = new DateTime();
    CommentData commentData = buildCommentData("comment-1", "first comment", createdAt);
    CursorPager<CommentData> pager =
        new CursorPager<>(Arrays.asList(commentData), Direction.NEXT, false);

    when(commentQueryService.findByArticleIdWithCursor(any(), any(), any())).thenReturn(pager);

    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    when(delegate.<Article>getSource()).thenReturn(article);
    when(delegate.<Map<String, ArticleData>>getLocalContext()).thenReturn(localContext);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dfe);

    CommentsConnection connection = result.getData();
    List<CommentEdge> edges = connection.getEdges();
    assertEquals(1, edges.size());
    assertEquals(commentData.getCursor().toString(), edges.get(0).getCursor());
    assertEquals("comment-1", edges.get(0).getNode().getId());
    assertEquals("first comment", edges.get(0).getNode().getBody());

    graphql.relay.PageInfo pageInfo = connection.getPageInfo();
    assertFalse(pageInfo.isHasNextPage());
    assertFalse(pageInfo.isHasPreviousPage());
    assertNotNull(pageInfo.getStartCursor());
    assertNotNull(pageInfo.getEndCursor());

    Object resultLocalContext = result.getLocalContext();
    assertTrue(resultLocalContext instanceof Map);
    @SuppressWarnings("unchecked")
    Map<String, CommentData> mapped = (Map<String, CommentData>) resultLocalContext;
    assertEquals(commentData, mapped.get("comment-1"));
  }

  @Test
  public void should_return_comments_connection_for_backward_pagination() {
    String slug = "how-to-train-your-dragon";
    Article article = Article.newBuilder().slug(slug).build();

    ArticleData articleData = new ArticleData();
    articleData.setId("article-1");
    articleData.setSlug(slug);
    Map<String, ArticleData> localContext = Collections.singletonMap(slug, articleData);

    setCurrentUser();
    DateTime createdAt = new DateTime();
    CommentData commentData = buildCommentData("comment-2", "older comment", createdAt);
    CursorPager<CommentData> pager =
        new CursorPager<>(Arrays.asList(commentData), Direction.PREV, true);

    when(commentQueryService.findByArticleIdWithCursor(any(), any(), any())).thenReturn(pager);

    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    when(delegate.<Article>getSource()).thenReturn(article);
    when(delegate.<Map<String, ArticleData>>getLocalContext()).thenReturn(localContext);
    DgsDataFetchingEnvironment dfe = new DgsDataFetchingEnvironment(delegate);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(null, null, 5, null, dfe);

    CommentsConnection connection = result.getData();
    assertEquals(1, connection.getEdges().size());
    assertEquals("comment-2", connection.getEdges().get(0).getNode().getId());

    graphql.relay.PageInfo pageInfo = connection.getPageInfo();
    assertTrue(pageInfo.isHasPreviousPage());
    assertFalse(pageInfo.isHasNextPage());
  }
}
