package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class CommentDatafetcherTest {

  private static final DateTime CREATED_AT = new DateTime(1600000000000L);

  private final CommentQueryService commentQueryService = mock(CommentQueryService.class);
  private final CommentDatafetcher datafetcher = new CommentDatafetcher(commentQueryService);

  @BeforeEach
  void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  private User authenticate() {
    User user = new User("jake@jake.jake", "jake", "123", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, null));
    return user;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> localContextOf(DataFetcherResult<?> result) {
    return (Map<String, Object>) result.getLocalContext();
  }

  private CommentData commentData(String id) {
    return new CommentData(
        id,
        "comment body " + id,
        "article-id",
        CREATED_AT,
        CREATED_AT,
        new ProfileData("profile-id", "jake", "bio", "image", false));
  }

  private DgsDataFetchingEnvironment articleEnvironment() {
    ArticleData articleData = new ArticleData();
    articleData.setId("article-id");
    articleData.setSlug("a-slug");
    Map<String, ArticleData> localContext = Collections.singletonMap("a-slug", articleData);
    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    when(delegate.<Map<String, ArticleData>>getLocalContext()).thenReturn(localContext);
    when(delegate.<Article>getSource()).thenReturn(Article.newBuilder().slug("a-slug").build());
    return new DgsDataFetchingEnvironment(delegate);
  }

  @Test
  void should_build_comment_from_local_context() {
    CommentData comment = commentData("comment-id");
    DataFetchingEnvironment delegate = mock(DataFetchingEnvironment.class);
    when(delegate.<CommentData>getLocalContext()).thenReturn(comment);

    DataFetcherResult<Comment> result =
        datafetcher.getComment(new DgsDataFetchingEnvironment(delegate));

    assertThat(result.getData().getId()).isEqualTo("comment-id");
    assertThat(result.getData().getBody()).isEqualTo("comment body comment-id");
    assertThat(result.getData().getCreatedAt()).isEqualTo("2020-09-13T12:26:40.000Z");
    assertThat(localContextOf(result)).containsEntry("comment-id", comment);
  }

  @Test
  void should_page_article_comments_forward() {
    User current = authenticate();
    List<CommentData> data = Arrays.asList(commentData("c1"), commentData("c2"));
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), eq(current), any()))
        .thenReturn(new CursorPager<>(data, Direction.NEXT, true));

    DataFetcherResult<CommentsConnection> result =
        datafetcher.articleComments(10, "1600000000000", null, null, articleEnvironment());

    assertThat(result.getData().getEdges()).hasSize(2);
    assertThat(result.getData().getEdges().get(0).getNode().getId()).isEqualTo("c1");
    assertThat(result.getData().getEdges().get(0).getCursor()).isEqualTo("1600000000000");
    assertThat(result.getData().getPageInfo().isHasNextPage()).isTrue();
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isFalse();
    assertThat(result.getData().getPageInfo().getStartCursor().getValue())
        .isEqualTo("1600000000000");

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(commentQueryService)
        .findByArticleIdWithCursor(eq("article-id"), eq(current), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.NEXT);
    assertThat(captor.getValue().getLimit()).isEqualTo(10);
    assertThat(captor.getValue().getCursor().getMillis()).isEqualTo(1600000000000L);
  }

  @Test
  void should_page_article_comments_backward_for_anonymous_user() {
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), isNull(), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(commentData("c1")), Direction.PREV, true));

    DataFetcherResult<CommentsConnection> result =
        datafetcher.articleComments(null, null, 5, "1600000000000", articleEnvironment());

    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isTrue();
    assertThat(result.getData().getPageInfo().isHasNextPage()).isFalse();

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    verify(commentQueryService)
        .findByArticleIdWithCursor(eq("article-id"), isNull(), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getLimit()).isEqualTo(5);
  }

  @Test
  void should_return_empty_connection_without_cursors_when_no_comments() {
    authenticate();
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), any(), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    DataFetcherResult<CommentsConnection> result =
        datafetcher.articleComments(10, null, null, null, articleEnvironment());

    assertThat(result.getData().getEdges()).isEmpty();
    assertThat(result.getData().getPageInfo().getStartCursor()).isNull();
    assertThat(result.getData().getPageInfo().getEndCursor()).isNull();
  }

  @Test
  void should_reject_paging_without_first_or_last() {
    DgsDataFetchingEnvironment dfe = articleEnvironment();

    assertThatThrownBy(() -> datafetcher.articleComments(null, null, null, null, dfe))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
