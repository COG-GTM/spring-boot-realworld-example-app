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
import java.util.Collections;
import java.util.Map;
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
public class CommentDatafetcherTest {

  private static final DateTime CREATED_AT = new DateTime(2021, 3, 3, 0, 0, DateTimeZone.UTC);

  @Mock private CommentQueryService commentQueryService;
  @Mock private DataFetchingEnvironment delegateEnvironment;

  @Captor private ArgumentCaptor<CursorPageParameter<DateTime>> pageParameterCaptor;

  @InjectMocks private CommentDatafetcher commentDatafetcher;

  private final User currentUser = new User("jake@jake.jake", "jake", "123", "bio", "image");

  @AfterEach
  void tearDown() {
    SecurityContextHelper.clear();
  }

  @Test
  public void should_resolve_comment_payload_comment_from_local_context() {
    when(delegateEnvironment.<CommentData>getLocalContext()).thenReturn(commentData("comment-id"));

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dgsEnvironment());

    assertEquals("comment-id", result.getData().getId());
    assertEquals("comment body", result.getData().getBody());
    assertEquals("2021-03-03T00:00:00.000Z", result.getData().getCreatedAt());
    assertEquals("2021-03-03T00:00:00.000Z", result.getData().getUpdatedAt());
    Map<String, CommentData> localContext = commentLocalContext(result);
    assertTrue(localContext.containsKey("comment-id"));
  }

  @Test
  public void should_return_article_comments_forward_page() {
    SecurityContextHelper.authenticate(currentUser);
    stubArticleSource();
    when(commentQueryService.findByArticleIdWithCursor(
            eq("article-id"), eq(currentUser), pageParameterCaptor.capture()))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(commentData("comment-id")), Direction.NEXT, true));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, "1000", null, null, dgsEnvironment());

    assertEquals(Direction.NEXT, pageParameterCaptor.getValue().getDirection());
    assertEquals(1000L, pageParameterCaptor.getValue().getCursor().getMillis());
    assertEquals(1, result.getData().getEdges().size());
    assertEquals("comment-id", result.getData().getEdges().get(0).getNode().getId());
    assertEquals(
        String.valueOf(CREATED_AT.getMillis()), result.getData().getEdges().get(0).getCursor());
    assertTrue(result.getData().getPageInfo().isHasNextPage());
    assertFalse(result.getData().getPageInfo().isHasPreviousPage());
    Map<String, CommentData> localContext = commentLocalContext(result);
    assertTrue(localContext.containsKey("comment-id"));
  }

  @Test
  public void should_return_article_comments_backward_page_for_anonymous_user() {
    SecurityContextHelper.anonymous();
    stubArticleSource();
    when(commentQueryService.findByArticleIdWithCursor(
            eq("article-id"), isNull(), pageParameterCaptor.capture()))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(commentData("comment-id")), Direction.PREV, true));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(null, null, 5, null, dgsEnvironment());

    assertEquals(Direction.PREV, pageParameterCaptor.getValue().getDirection());
    assertNull(pageParameterCaptor.getValue().getCursor());
    assertTrue(result.getData().getPageInfo().isHasPreviousPage());
  }

  @Test
  public void should_return_empty_comments_connection_with_null_cursors() {
    SecurityContextHelper.anonymous();
    stubArticleSource();
    when(commentQueryService.findByArticleIdWithCursor(
            eq("article-id"), isNull(), pageParameterCaptor.capture()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dgsEnvironment());

    assertTrue(result.getData().getEdges().isEmpty());
    assertNull(result.getData().getPageInfo().getStartCursor());
    assertNull(result.getData().getPageInfo().getEndCursor());
  }

  @Test
  public void should_reject_article_comments_without_first_or_last() {
    assertThrows(
        IllegalArgumentException.class,
        () -> commentDatafetcher.articleComments(null, null, null, null, dgsEnvironment()));
  }

  @SuppressWarnings("unchecked")
  private Map<String, CommentData> commentLocalContext(DataFetcherResult<?> result) {
    return (Map<String, CommentData>) result.getLocalContext();
  }

  private void stubArticleSource() {
    when(delegateEnvironment.<Article>getSource())
        .thenReturn(Article.newBuilder().slug("a-title").build());
    when(delegateEnvironment.<Map<String, ArticleData>>getLocalContext())
        .thenReturn(Collections.singletonMap("a-title", articleData()));
  }

  private DgsDataFetchingEnvironment dgsEnvironment() {
    return new DgsDataFetchingEnvironment(delegateEnvironment);
  }

  private ProfileData profileData() {
    return new ProfileData("author-id", "john", "bio", "image", false);
  }

  private CommentData commentData(String id) {
    return new CommentData(id, "comment body", "article-id", CREATED_AT, CREATED_AT, profileData());
  }

  private ArticleData articleData() {
    return new ArticleData(
        "article-id",
        "a-title",
        "a title",
        "desc",
        "body",
        false,
        0,
        CREATED_AT,
        CREATED_AT,
        Collections.singletonList("java"),
        profileData());
  }
}
