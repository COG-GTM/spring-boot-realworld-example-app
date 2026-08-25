package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
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
import org.joda.time.DateTimeZone;
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
@SuppressWarnings("unchecked")
public class CommentDatafetcherTest {

  @Mock private CommentQueryService commentQueryService;
  @Mock private graphql.schema.DataFetchingEnvironment delegate;

  private CommentDatafetcher commentDatafetcher;
  private DgsDataFetchingEnvironment dfe;
  private CommentData commentData;

  @BeforeEach
  public void setUp() {
    commentDatafetcher = new CommentDatafetcher(commentQueryService);
    dfe = new DgsDataFetchingEnvironment(delegate);
    commentData =
        new CommentData(
            "comment-id",
            "comment body",
            "article-id",
            new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC),
            new DateTime(2020, 1, 2, 0, 0, DateTimeZone.UTC),
            new ProfileData("user-id", "aisensiy", "bio", "image", false));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_build_comment_from_local_context() {
    when(delegate.<CommentData>getLocalContext()).thenReturn(commentData);

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dfe);

    assertThat(result.getData().getId()).isEqualTo("comment-id");
    assertThat(result.getData().getBody()).isEqualTo("comment body");
    assertThat(result.getData().getCreatedAt()).isEqualTo("2020-01-01T00:00:00.000Z");
    assertThat(result.getData().getUpdatedAt()).isEqualTo("2020-01-01T00:00:00.000Z");
    assertThat((Map<String, Object>) result.getLocalContext())
        .containsEntry("comment-id", commentData);
  }

  @Test
  public void should_throw_when_neither_first_nor_last_is_provided() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> commentDatafetcher.articleComments(null, null, null, null, dfe));
  }

  @Test
  public void should_fetch_article_comments_forward() {
    prepareArticleContext();
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), eq(null), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(commentData), Direction.NEXT, true));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, "100", null, null, dfe);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    org.mockito.Mockito.verify(commentQueryService)
        .findByArticleIdWithCursor(eq("article-id"), eq(null), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.NEXT);
    assertThat(captor.getValue().getLimit()).isEqualTo(10);
    assertThat(captor.getValue().getCursor().getMillis()).isEqualTo(100L);

    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getEdges().get(0).getNode().getId()).isEqualTo("comment-id");
    assertThat(result.getData().getEdges().get(0).getCursor())
        .isEqualTo(String.valueOf(commentData.getCreatedAt().getMillis()));
    assertThat(result.getData().getPageInfo().isHasNextPage()).isTrue();
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isFalse();
    assertThat((Map<String, CommentData>) result.getLocalContext())
        .containsEntry("comment-id", commentData);
  }

  @Test
  public void should_fetch_article_comments_backward() {
    prepareArticleContext();
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), eq(null), any()))
        .thenReturn(
            new CursorPager<>(Collections.singletonList(commentData), Direction.PREV, true));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(null, null, 5, "200", dfe);

    ArgumentCaptor<CursorPageParameter<DateTime>> captor =
        ArgumentCaptor.forClass(CursorPageParameter.class);
    org.mockito.Mockito.verify(commentQueryService)
        .findByArticleIdWithCursor(eq("article-id"), eq(null), captor.capture());
    assertThat(captor.getValue().getDirection()).isEqualTo(Direction.PREV);
    assertThat(captor.getValue().getLimit()).isEqualTo(5);
    assertThat(captor.getValue().getCursor().getMillis()).isEqualTo(200L);

    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isTrue();
    assertThat(result.getData().getPageInfo().isHasNextPage()).isFalse();
    assertThat(result.getData().getEdges()).hasSize(1);
  }

  @Test
  public void should_handle_empty_comments_page() {
    prepareArticleContext();
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), eq(null), any()))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.NEXT, false));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dfe);

    assertThat(result.getData().getEdges()).isEmpty();
    assertThat(result.getData().getPageInfo().getStartCursor()).isNull();
    assertThat(result.getData().getPageInfo().getEndCursor()).isNull();
  }

  @Test
  public void should_pass_current_user_to_query_service() {
    User user = new User("a@test.com", "aisensiy", "123", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    prepareArticleContext();
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), eq(user), any()))
        .thenReturn(new CursorPager<>(Arrays.asList(commentData), Direction.NEXT, false));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dfe);

    assertThat(result.getData().getEdges()).hasSize(1);
  }

  private void prepareArticleContext() {
    Article article = Article.newBuilder().slug("a-slug").build();
    ArticleData articleData = new ArticleData();
    articleData.setId("article-id");
    articleData.setSlug("a-slug");
    when(delegate.<Article>getSource()).thenReturn(article);
    when(delegate.<Map<String, ArticleData>>getLocalContext())
        .thenReturn(Collections.singletonMap("a-slug", articleData));
  }
}
