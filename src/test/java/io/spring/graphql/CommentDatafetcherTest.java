package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class CommentDatafetcherTest {

  private static final DateTime T1 = new DateTime(2024, 1, 1, 0, 0, DateTimeZone.UTC);
  private static final DateTime T2 = new DateTime(2024, 1, 2, 0, 0, DateTimeZone.UTC);

  private CommentQueryService commentQueryService;
  private CommentDatafetcher commentDatafetcher;
  private DataFetchingEnvironment env;
  private DgsDataFetchingEnvironment dfe;
  private User current;
  private CommentData c1;
  private CommentData c2;

  @BeforeEach
  public void setUp() {
    commentQueryService = mock(CommentQueryService.class);
    commentDatafetcher = new CommentDatafetcher(commentQueryService);
    env = mock(DataFetchingEnvironment.class);
    dfe = new DgsDataFetchingEnvironment(env);
    current = new User("alice@example.com", "alice", "pw", "", "");
    ProfileData author = new ProfileData("bob-id", "bob", "", "", false);
    c1 = new CommentData("c1", "first comment", "a1", T2, T2, author);
    c2 = new CommentData("c2", "second comment", "a1", T1, T1, author);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  private void givenArticleSource() {
    ArticleData articleData =
        new ArticleData(
            "a1", "my-slug", "t", "d", "b", false, 0, T1, T1, Collections.emptyList(), null);
    Map<String, ArticleData> map = new HashMap<>();
    map.put("my-slug", articleData);
    when(env.getSource()).thenReturn(Article.newBuilder().slug("my-slug").build());
    when(env.getLocalContext()).thenReturn(map);
  }

  @SuppressWarnings("unchecked")
  private static ArgumentCaptor<CursorPageParameter<DateTime>> pageCaptor() {
    return ArgumentCaptor.forClass(CursorPageParameter.class);
  }

  private void assertConnection(
      DataFetcherResult<CommentsConnection> result, boolean hasNext, boolean hasPrevious) {
    CommentsConnection connection = result.getData();
    assertEquals(2, connection.getEdges().size());
    assertEquals(String.valueOf(T2.getMillis()), connection.getEdges().get(0).getCursor());
    Comment node = connection.getEdges().get(0).getNode();
    assertEquals("c1", node.getId());
    assertEquals("first comment", node.getBody());
    assertEquals("2024-01-02T00:00:00.000Z", node.getCreatedAt());
    assertEquals("2024-01-02T00:00:00.000Z", node.getUpdatedAt());
    assertEquals(
        String.valueOf(T2.getMillis()), connection.getPageInfo().getStartCursor().getValue());
    assertEquals(
        String.valueOf(T1.getMillis()), connection.getPageInfo().getEndCursor().getValue());
    assertEquals(hasNext, connection.getPageInfo().isHasNextPage());
    assertEquals(hasPrevious, connection.getPageInfo().isHasPreviousPage());
    Map<?, ?> localContext = (Map<?, ?>) result.getLocalContext();
    assertSame(c1, localContext.get("c1"));
    assertSame(c2, localContext.get("c2"));
  }

  @Test
  public void comment_payload_should_build_comment_and_keyed_local_context() {
    when(env.getLocalContext()).thenReturn(c1);

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dfe);

    assertEquals("c1", result.getData().getId());
    assertEquals("first comment", result.getData().getBody());
    assertEquals("2024-01-02T00:00:00.000Z", result.getData().getCreatedAt());
    Map<?, ?> localContext = (Map<?, ?>) result.getLocalContext();
    assertEquals(1, localContext.size());
    assertSame(c1, localContext.get("c1"));
  }

  @Test
  public void article_comments_should_reject_missing_first_and_last() {
    authenticate(current);
    assertThrows(
        IllegalArgumentException.class,
        () -> commentDatafetcher.articleComments(null, null, null, null, dfe));
    verifyNoInteractions(commentQueryService);
  }

  @Test
  public void article_comments_should_page_forward_for_authenticated_user() {
    authenticate(current);
    givenArticleSource();
    when(commentQueryService.findByArticleIdWithCursor(eq("a1"), eq(current), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(c1, c2), Direction.NEXT, true));
    ArgumentCaptor<CursorPageParameter<DateTime>> page = pageCaptor();

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, String.valueOf(T1.getMillis()), null, null, dfe);

    verify(commentQueryService).findByArticleIdWithCursor(eq("a1"), eq(current), page.capture());
    assertEquals(Direction.NEXT, page.getValue().getDirection());
    assertEquals(10, page.getValue().getLimit());
    assertEquals(T1.getMillis(), page.getValue().getCursor().getMillis());
    assertConnection(result, true, false);
  }

  @Test
  public void article_comments_should_page_backward_for_anonymous_user() {
    anonymous();
    givenArticleSource();
    when(commentQueryService.findByArticleIdWithCursor(eq("a1"), isNull(), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(c1, c2), Direction.PREV, true));
    ArgumentCaptor<CursorPageParameter<DateTime>> page = pageCaptor();

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(null, null, 5, String.valueOf(T2.getMillis()), dfe);

    verify(commentQueryService).findByArticleIdWithCursor(eq("a1"), isNull(), page.capture());
    assertEquals(Direction.PREV, page.getValue().getDirection());
    assertEquals(5, page.getValue().getLimit());
    assertEquals(T2.getMillis(), page.getValue().getCursor().getMillis());
    assertConnection(result, false, true);
  }

  @Test
  public void article_comments_should_return_empty_connection_with_null_cursors() {
    authenticate(current);
    givenArticleSource();
    when(commentQueryService.findByArticleIdWithCursor(eq("a1"), eq(current), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dfe);

    assertTrue(result.getData().getEdges().isEmpty());
    assertNull(result.getData().getPageInfo().getStartCursor());
    assertNull(result.getData().getPageInfo().getEndCursor());
    assertFalse(result.getData().getPageInfo().isHasNextPage());
    assertTrue(((Map<?, ?>) result.getLocalContext()).isEmpty());
  }
}
