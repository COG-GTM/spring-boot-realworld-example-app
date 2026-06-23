package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
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
import io.spring.core.user.User;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.Comment;
import io.spring.graphql.types.CommentsConnection;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommentDatafetcherTest {

  private CommentQueryService commentQueryService;
  private CommentDatafetcher datafetcher;
  private User user;

  @BeforeEach
  void setUp() {
    commentQueryService = mock(CommentQueryService.class);
    datafetcher = new CommentDatafetcher(commentQueryService);
    user = new User("user@test.com", "user", "123", "bio", "image");
  }

  @AfterEach
  void tearDown() {
    GraphQLTestSecurity.clear();
  }

  private DgsDataFetchingEnvironment dgsEnv(Object source, Object localContext) {
    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    if (source != null) {
      doReturn(source).when(delegate).getSource();
    }
    if (localContext != null) {
      doReturn(localContext).when(delegate).getLocalContext();
    }
    return new DgsDataFetchingEnvironment(delegate);
  }

  private CommentData commentData(String id) {
    return new CommentData(id, "body " + id, "article-id", new DateTime(), new DateTime(), null);
  }

  private ArticleData articleData() {
    ArticleData articleData = new ArticleData();
    articleData.setId("article-id");
    articleData.setSlug("slug");
    return articleData;
  }

  @Test
  void getComment_builds_result_and_local_context() {
    CommentData comment = commentData("c1");
    DgsDataFetchingEnvironment dfe = dgsEnv(null, comment);

    DataFetcherResult<Comment> result = datafetcher.getComment(dfe);

    assertEquals("c1", result.getData().getId());
    assertEquals("body c1", result.getData().getBody());
    Map<String, CommentData> ctx = (Map<String, CommentData>) result.getLocalContext();
    assertTrue(ctx.containsKey("c1"));
  }

  @Test
  void articleComments_with_first_returns_connection() {
    GraphQLTestSecurity.login(user);
    Article article = Article.newBuilder().slug("slug").build();
    DgsDataFetchingEnvironment dfe = dgsEnv(article, Map.of("slug", articleData()));
    CommentData c = commentData("c2");
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), any(), any()))
        .thenReturn(new CursorPager<>(List.of(c), Direction.NEXT, false));

    DataFetcherResult<CommentsConnection> result =
        datafetcher.articleComments(10, null, null, null, dfe);

    assertEquals(1, result.getData().getEdges().size());
    assertEquals("c2", result.getData().getEdges().get(0).getNode().getId());
  }

  @Test
  void articleComments_with_last_uses_prev_direction_and_empty_pager() {
    GraphQLTestSecurity.anonymous();
    Article article = Article.newBuilder().slug("slug").build();
    DgsDataFetchingEnvironment dfe = dgsEnv(article, Map.of("slug", articleData()));
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), any(), any()))
        .thenReturn(new CursorPager<>(List.of(), Direction.PREV, false));

    DataFetcherResult<CommentsConnection> result =
        datafetcher.articleComments(null, null, 10, "100", dfe);

    assertTrue(result.getData().getEdges().isEmpty());
  }

  @Test
  void articleComments_without_first_and_last_throws() {
    DgsDataFetchingEnvironment dfe = dgsEnv(null, null);
    assertThrows(
        IllegalArgumentException.class,
        () -> datafetcher.articleComments(null, null, null, null, dfe));
  }
}
