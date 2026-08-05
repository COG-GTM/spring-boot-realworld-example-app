package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.TestHelper;
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
import io.spring.graphql.types.CommentEdge;
import io.spring.graphql.types.CommentsConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class CommentDatafetcherTest {

  private CommentQueryService commentQueryService;
  private CommentDatafetcher commentDatafetcher;

  private User user;
  private ArticleData articleData;
  private Article article;

  @BeforeEach
  public void setUp() {
    commentQueryService = mock(CommentQueryService.class);
    commentDatafetcher = new CommentDatafetcher(commentQueryService);

    user = new User("john@example.com", "john", "123", "", "");
    articleData = TestHelper.articleDataFixture("test", user);
    article = Article.newBuilder().slug(articleData.getSlug()).build();
    loginAnonymously();
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void login(User currentUser) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(currentUser, null, Collections.emptyList()));
  }

  private void loginAnonymously() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  private CommentData commentDataFixture(String id, DateTime createdAt) {
    return new CommentData(
        id,
        "body of " + id,
        articleData.getId(),
        createdAt,
        createdAt,
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
  }

  private DgsDataFetchingEnvironment dfeWith(Object source, Object localContext) {
    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    when(delegate.getSource()).thenReturn(source);
    when(delegate.getLocalContext()).thenReturn(localContext);
    return new DgsDataFetchingEnvironment(delegate);
  }

  private Map<String, ArticleData> articleLocalContext() {
    Map<String, ArticleData> map = new HashMap<>();
    map.put(articleData.getSlug(), articleData);
    return map;
  }

  @SuppressWarnings("unchecked")
  @Test
  public void should_get_comment_from_local_context() {
    DateTime createdAt = new DateTime();
    CommentData commentData = commentDataFixture("comment-id", createdAt);

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dfeWith(null, commentData));

    Comment comment = result.getData();
    Assertions.assertEquals("comment-id", comment.getId());
    Assertions.assertEquals("body of comment-id", comment.getBody());
    String expectedTime = ISODateTimeFormat.dateTime().withZoneUTC().print(createdAt);
    Assertions.assertEquals(expectedTime, comment.getCreatedAt());
    Assertions.assertEquals(expectedTime, comment.getUpdatedAt());

    Map<String, Object> localContext = (Map<String, Object>) result.getLocalContext();
    Assertions.assertEquals(commentData, localContext.get("comment-id"));
  }

  @Test
  public void should_throw_illegal_argument_exception_without_first_and_last() {
    DgsDataFetchingEnvironment dfe = dfeWith(article, articleLocalContext());

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> commentDatafetcher.articleComments(null, null, null, null, dfe));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> commentDatafetcher.articleComments(null, "cursor", null, "cursor", dfe));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void should_fetch_article_comments_forward() {
    login(user);
    DateTime first = new DateTime().minusHours(2);
    DateTime second = new DateTime().minusHours(1);
    List<CommentData> comments =
        Arrays.asList(commentDataFixture("one", first), commentDataFixture("two", second));
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), eq(user), any()))
        .thenReturn(new CursorPager<>(comments, Direction.NEXT, true));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(
            2, null, null, null, dfeWith(article, articleLocalContext()));

    CommentsConnection connection = result.getData();
    List<CommentEdge> edges = connection.getEdges();
    Assertions.assertEquals(2, edges.size());
    Assertions.assertEquals("one", edges.get(0).getNode().getId());
    Assertions.assertEquals(String.valueOf(first.getMillis()), edges.get(0).getCursor());
    Assertions.assertEquals("two", edges.get(1).getNode().getId());
    Assertions.assertEquals(String.valueOf(second.getMillis()), edges.get(1).getCursor());

    Assertions.assertTrue(connection.getPageInfo().isHasNextPage());
    Assertions.assertFalse(connection.getPageInfo().isHasPreviousPage());
    Assertions.assertEquals(
        String.valueOf(first.getMillis()), connection.getPageInfo().getStartCursor().getValue());
    Assertions.assertEquals(
        String.valueOf(second.getMillis()), connection.getPageInfo().getEndCursor().getValue());

    Map<String, CommentData> localContext = (Map<String, CommentData>) result.getLocalContext();
    Assertions.assertEquals(comments.get(0), localContext.get("one"));
    Assertions.assertEquals(comments.get(1), localContext.get("two"));
  }

  @Test
  public void should_pass_next_page_parameter_with_after_cursor() {
    login(user);
    DateTime createdAt = new DateTime();
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), eq(user), any()))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(commentDataFixture("one", createdAt)),
                Direction.NEXT,
                false));

    commentDatafetcher.articleComments(
        10,
        String.valueOf(createdAt.getMillis()),
        null,
        null,
        dfeWith(article, articleLocalContext()));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(commentQueryService)
        .findByArticleIdWithCursor(eq(articleData.getId()), eq(user), captor.capture());
    CursorPageParameter<DateTime> parameter = captor.getValue();
    Assertions.assertEquals(Direction.NEXT, parameter.getDirection());
    Assertions.assertEquals(10, parameter.getLimit());
    Assertions.assertEquals(createdAt.getMillis(), parameter.getCursor().getMillis());
  }

  @Test
  public void should_fetch_article_comments_backward_with_before_cursor() {
    login(user);
    DateTime createdAt = new DateTime();
    List<CommentData> comments = Collections.singletonList(commentDataFixture("one", createdAt));
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), eq(user), any()))
        .thenReturn(new CursorPager<>(comments, Direction.PREV, true));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(
            null,
            null,
            5,
            String.valueOf(createdAt.getMillis()),
            dfeWith(article, articleLocalContext()));

    Assertions.assertEquals(1, result.getData().getEdges().size());
    Assertions.assertTrue(result.getData().getPageInfo().isHasPreviousPage());
    Assertions.assertFalse(result.getData().getPageInfo().isHasNextPage());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(commentQueryService)
        .findByArticleIdWithCursor(eq(articleData.getId()), eq(user), captor.capture());
    CursorPageParameter<DateTime> parameter = captor.getValue();
    Assertions.assertEquals(Direction.PREV, parameter.getDirection());
    Assertions.assertEquals(5, parameter.getLimit());
    Assertions.assertEquals(createdAt.getMillis(), parameter.getCursor().getMillis());
  }

  @Test
  public void should_fetch_article_comments_without_current_user() {
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), isNull(), any()))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(commentDataFixture("one", new DateTime())),
                Direction.NEXT,
                false));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(
            1, null, null, null, dfeWith(article, articleLocalContext()));

    Assertions.assertEquals(1, result.getData().getEdges().size());
    verify(commentQueryService).findByArticleIdWithCursor(eq(articleData.getId()), isNull(), any());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void should_return_empty_connection_without_comments() {
    login(user);
    when(commentQueryService.findByArticleIdWithCursor(eq(articleData.getId()), eq(user), any()))
        .thenReturn(new CursorPager<>(new ArrayList<>(), Direction.NEXT, false));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(
            20, null, null, null, dfeWith(article, articleLocalContext()));

    CommentsConnection connection = result.getData();
    Assertions.assertTrue(connection.getEdges().isEmpty());
    Assertions.assertNull(connection.getPageInfo().getStartCursor());
    Assertions.assertNull(connection.getPageInfo().getEndCursor());
    Assertions.assertFalse(connection.getPageInfo().isHasNextPage());
    Assertions.assertFalse(connection.getPageInfo().isHasPreviousPage());
    Map<String, CommentData> localContext = (Map<String, CommentData>) result.getLocalContext();
    Assertions.assertTrue(localContext.isEmpty());
  }

  @Test
  public void should_throw_null_pointer_exception_when_article_is_not_in_local_context() {
    login(user);
    DgsDataFetchingEnvironment dfe = dfeWith(article, new HashMap<String, ArticleData>());

    Assertions.assertThrows(
        NullPointerException.class,
        () -> commentDatafetcher.articleComments(10, null, null, null, dfe));
  }
}
