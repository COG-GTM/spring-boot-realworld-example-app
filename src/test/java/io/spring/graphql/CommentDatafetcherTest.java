package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class CommentDatafetcherTest {

  @Mock private CommentQueryService commentQueryService;
  @Mock private DataFetchingEnvironment delegate;

  private DgsDataFetchingEnvironment dfe;

  @BeforeEach
  public void setUp() {
    dfe = new DgsDataFetchingEnvironment(delegate);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private CommentData commentData(String id, long millis) {
    return commentData(id, millis, millis);
  }

  private CommentData commentData(String id, long createdMillis, long updatedMillis) {
    return new CommentData(
        id,
        "body of " + id,
        "article-id",
        new DateTime(createdMillis),
        new DateTime(updatedMillis),
        new ProfileData("user-id", "alice", "bio", "image", false));
  }

  @Test
  public void should_build_comment_from_local_context() {
    CommentData data = commentData("comment-1", 1000L, 5000L);
    when(delegate.<CommentData>getLocalContext()).thenReturn(data);

    DataFetcherResult<Comment> result = new CommentDatafetcher(commentQueryService).getComment(dfe);

    assertThat(result.getData().getId()).isEqualTo("comment-1");
    assertThat(result.getData().getBody()).isEqualTo("body of comment-1");
    assertThat(result.getData().getCreatedAt()).isEqualTo("1970-01-01T00:00:01.000Z");
    // both timestamps are built from createdAt, so updatedAt mirrors the creation time
    assertThat(result.getData().getUpdatedAt()).isEqualTo("1970-01-01T00:00:01.000Z");
    assertThat(((Map<?, ?>) result.getLocalContext()).get("comment-1")).isSameAs(data);
  }

  @Test
  public void should_throw_when_neither_first_nor_last_provided() {
    CommentDatafetcher datafetcher = new CommentDatafetcher(commentQueryService);

    assertThatThrownBy(() -> datafetcher.articleComments(null, null, null, null, dfe))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void should_fetch_comments_forward_for_authenticated_user() {
    User user = new User("a@b.com", "alice", "123", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));

    Article article = Article.newBuilder().slug("a-slug").build();
    ArticleData articleData = new ArticleData();
    articleData.setId("article-id");
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put("a-slug", articleData);
    when(delegate.<Article>getSource()).thenReturn(article);
    when(delegate.<Map<String, ArticleData>>getLocalContext()).thenReturn(localContext);

    CommentData first = commentData("c1", 1000L);
    CommentData second = commentData("c2", 2000L);
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), eq(user), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(first, second), Direction.NEXT, true));

    DataFetcherResult<CommentsConnection> result =
        new CommentDatafetcher(commentQueryService).articleComments(2, "1000", null, null, dfe);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    org.mockito.Mockito.verify(commentQueryService)
        .findByArticleIdWithCursor(eq("article-id"), eq(user), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.NEXT);
    assertThat(captor.getValue().getLimit()).isEqualTo(2);
    assertThat(captor.getValue().getCursor().getMillis()).isEqualTo(1000L);

    assertThat(result.getData().getEdges()).hasSize(2);
    assertThat(result.getData().getEdges().get(0).getCursor()).isEqualTo("1000");
    assertThat(result.getData().getEdges().get(1).getNode().getId()).isEqualTo("c2");
    assertThat(result.getData().getPageInfo().isHasNextPage()).isTrue();
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isFalse();
    assertThat(result.getData().getPageInfo().getStartCursor().getValue()).isEqualTo("1000");
    assertThat(result.getData().getPageInfo().getEndCursor().getValue()).isEqualTo("2000");
    Map<Object, Object> resultContext = (Map<Object, Object>) result.getLocalContext();
    assertThat(resultContext.keySet()).containsExactlyInAnyOrder("c1", "c2");
    assertThat(resultContext.get("c1")).isSameAs(first);
  }

  @Test
  public void should_fetch_comments_backward_for_anonymous_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    Article article = Article.newBuilder().slug("a-slug").build();
    ArticleData articleData = new ArticleData();
    articleData.setId("article-id");
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put("a-slug", articleData);
    when(delegate.<Article>getSource()).thenReturn(article);
    when(delegate.<Map<String, ArticleData>>getLocalContext()).thenReturn(localContext);

    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), eq(null), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.PREV, false));

    DataFetcherResult<CommentsConnection> result =
        new CommentDatafetcher(commentQueryService).articleComments(null, null, 5, "3000", dfe);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    org.mockito.Mockito.verify(commentQueryService)
        .findByArticleIdWithCursor(eq("article-id"), eq(null), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getCursor().getMillis()).isEqualTo(3000L);

    assertThat(result.getData().getEdges()).isEmpty();
    assertThat(result.getData().getPageInfo().getStartCursor()).isNull();
    assertThat(result.getData().getPageInfo().getEndCursor()).isNull();
    assertThat(result.getData().getPageInfo().isHasNextPage()).isFalse();
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isFalse();
  }
}
