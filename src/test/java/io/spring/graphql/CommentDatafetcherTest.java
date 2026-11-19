package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
  private static final DateTime T1 = new DateTime(2021, 1, 1, 0, 0, DateTimeZone.UTC);
  private static final DateTime T2 = new DateTime(2021, 1, 2, 0, 0, DateTimeZone.UTC);

  private CommentQueryService commentQueryService;
  private CommentDatafetcher commentDatafetcher;
  private DataFetchingEnvironment env;
  private DgsDataFetchingEnvironment dfe;
  private User user;

  @BeforeEach
  public void setUp() {
    commentQueryService = mock(CommentQueryService.class);
    commentDatafetcher = new CommentDatafetcher(commentQueryService);
    env = mock(DataFetchingEnvironment.class);
    dfe = new DgsDataFetchingEnvironment(env);
    user = new User("a@b.com", "alice", "pw", "", "");
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

  private static CommentData commentData(String id, DateTime createdAt) {
    return new CommentData(
        id,
        "body " + id,
        "article-id",
        createdAt,
        createdAt,
        new ProfileData("p", "bob", "", "", false));
  }

  private void givenArticleSource() {
    ArticleData articleData = new ArticleData();
    articleData.setId("article-id");
    articleData.setSlug("hello");
    Map<String, ArticleData> map = new HashMap<>();
    map.put("hello", articleData);
    when(env.getSource()).thenReturn(Article.newBuilder().slug("hello").build());
    when(env.getLocalContext()).thenReturn(map);
  }

  @SuppressWarnings("unchecked")
  private static ArgumentCaptor<CursorPageParameter<DateTime>> pageCaptor() {
    return ArgumentCaptor.forClass(CursorPageParameter.class);
  }

  @Test
  public void getComment_should_build_comment_and_key_local_context_by_id() {
    CommentData data = commentData("c1", T1);
    when(env.getLocalContext()).thenReturn(data);

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dfe);

    Comment comment = result.getData();
    assertThat(comment.getId()).isEqualTo("c1");
    assertThat(comment.getBody()).isEqualTo("body c1");
    assertThat(comment.getCreatedAt()).isEqualTo("2021-01-01T00:00:00.000Z");
    assertThat(comment.getUpdatedAt()).isEqualTo("2021-01-01T00:00:00.000Z");
    @SuppressWarnings("unchecked")
    Map<String, Object> localContext = (Map<String, Object>) result.getLocalContext();
    assertThat(localContext).containsOnlyKeys("c1").containsEntry("c1", data);
  }

  @Test
  public void articleComments_should_reject_missing_first_and_last() {
    assertThatThrownBy(() -> commentDatafetcher.articleComments(null, null, null, null, dfe))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(commentQueryService);
  }

  @Test
  public void articleComments_should_page_forward_for_authenticated_user() {
    authenticate(user);
    givenArticleSource();
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), eq(user), any()))
        .thenReturn(
            new CursorPager<>(
                Arrays.asList(commentData("c1", T1), commentData("c2", T2)), Direction.NEXT, true));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(2, String.valueOf(T1.getMillis()), null, null, dfe);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(commentQueryService)
        .findByArticleIdWithCursor(eq("article-id"), eq(user), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.NEXT);
    assertThat(captor.getValue().getLimit()).isEqualTo(2);
    assertThat(captor.getValue().getCursor().getMillis()).isEqualTo(T1.getMillis());

    CommentsConnection connection = result.getData();
    assertThat(connection.getEdges()).hasSize(2);
    assertThat(connection.getEdges().get(0).getCursor()).isEqualTo(String.valueOf(T1.getMillis()));
    assertThat(connection.getEdges().get(1).getNode().getId()).isEqualTo("c2");
    assertThat(connection.getPageInfo().isHasNextPage()).isTrue();
    assertThat(connection.getPageInfo().isHasPreviousPage()).isFalse();
    assertThat(connection.getPageInfo().getStartCursor().getValue())
        .isEqualTo(String.valueOf(T1.getMillis()));
    assertThat(connection.getPageInfo().getEndCursor().getValue())
        .isEqualTo(String.valueOf(T2.getMillis()));
    @SuppressWarnings("unchecked")
    Map<String, CommentData> localContext = (Map<String, CommentData>) result.getLocalContext();
    assertThat(localContext).containsOnlyKeys("c1", "c2");
  }

  @Test
  public void articleComments_should_page_backward_for_anonymous_user() {
    anonymous();
    givenArticleSource();
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), isNull(), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(commentData("c1", T1)), Direction.PREV, true));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(null, null, 1, String.valueOf(T2.getMillis()), dfe);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor = pageCaptor();
    verify(commentQueryService)
        .findByArticleIdWithCursor(eq("article-id"), isNull(), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getCursor().getMillis()).isEqualTo(T2.getMillis());
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isTrue();
    assertThat(result.getData().getPageInfo().isHasNextPage()).isFalse();
  }

  @Test
  public void articleComments_should_handle_empty_page() {
    authenticate(user);
    givenArticleSource();
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), eq(user), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dfe);

    assertThat(result.getData().getEdges()).isEmpty();
    assertThat(result.getData().getPageInfo().getStartCursor()).isNull();
    assertThat(result.getData().getPageInfo().getEndCursor()).isNull();
    assertThat((Map<?, ?>) result.getLocalContext()).isEmpty();
  }
}
