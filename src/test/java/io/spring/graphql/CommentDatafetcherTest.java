package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Collections;
import java.util.Map;
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
public class CommentDatafetcherTest {
  @Mock private CommentQueryService commentQueryService;

  private CommentDatafetcher commentDatafetcher;
  private User user;
  private ArticleData articleData;
  private CommentData comment1;
  private CommentData comment2;

  @BeforeEach
  public void setUp() {
    commentDatafetcher = new CommentDatafetcher(commentQueryService);
    user = new User("a@test.com", "a", "123", "", "");
    articleData = io.spring.TestHelper.articleDataFixture("1", user);
    ProfileData profile = new ProfileData(user.getId(), "a", "", "", false);
    comment1 =
        new CommentData(
            "c1", "first", articleData.getId(), new DateTime(2000L), new DateTime(2000L), profile);
    comment2 =
        new CommentData(
            "c2", "second", articleData.getId(), new DateTime(1000L), new DateTime(1000L), profile);
    GraphQLTestSupport.authenticate(user);
  }

  @AfterEach
  public void tearDown() {
    GraphQLTestSupport.clear();
  }

  private Map<String, ArticleData> articleContext() {
    return Collections.singletonMap(articleData.getSlug(), articleData);
  }

  @Test
  public void should_build_comment_from_payload() {
    DataFetcherResult<Comment> result =
        commentDatafetcher.getComment(GraphQLTestSupport.dgsEnv(null, comment1));

    Comment comment = result.getData();
    Assertions.assertEquals("c1", comment.getId());
    Assertions.assertEquals("first", comment.getBody());
    Assertions.assertEquals(
        ISODateTimeFormat.dateTime().withZoneUTC().print(comment1.getCreatedAt()),
        comment.getCreatedAt());
    Assertions.assertSame(comment1, ((Map<?, ?>) result.getLocalContext()).get("c1"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void should_fetch_article_comments_forward() {
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), eq(user), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(comment1, comment2), Direction.NEXT, true));
    Article source = Article.newBuilder().slug(articleData.getSlug()).build();

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(
            10, "5000", null, null, GraphQLTestSupport.dgsEnv(source, articleContext()));

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(commentQueryService)
        .findByArticleIdWithCursor(eq(articleData.getId()), eq(user), captor.capture());
    Assertions.assertEquals(Direction.NEXT, captor.getValue().getDirection());
    Assertions.assertEquals(5000L, captor.getValue().getCursor().getMillis());

    CommentsConnection connection = result.getData();
    Assertions.assertEquals(2, connection.getEdges().size());
    Assertions.assertEquals("2000", connection.getEdges().get(0).getCursor());
    Assertions.assertEquals("first", connection.getEdges().get(0).getNode().getBody());
    Assertions.assertTrue(connection.getPageInfo().isHasNextPage());
    Assertions.assertEquals("2000", connection.getPageInfo().getStartCursor().getValue());
    Assertions.assertEquals("1000", connection.getPageInfo().getEndCursor().getValue());
    Map<String, CommentData> localContext = (Map<String, CommentData>) result.getLocalContext();
    Assertions.assertSame(comment2, localContext.get("c2"));
  }

  @Test
  public void should_fetch_article_comments_backward() {
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), eq(user), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.PREV, false));
    Article source = Article.newBuilder().slug(articleData.getSlug()).build();

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(
            null, null, 10, null, GraphQLTestSupport.dgsEnv(source, articleContext()));

    Assertions.assertTrue(result.getData().getEdges().isEmpty());
    Assertions.assertNull(result.getData().getPageInfo().getStartCursor());
    Assertions.assertNull(result.getData().getPageInfo().getEndCursor());
    Assertions.assertFalse(result.getData().getPageInfo().isHasPreviousPage());
  }

  @Test
  public void should_require_first_or_last() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> commentDatafetcher.articleComments(null, null, null, null, null));
  }
}
