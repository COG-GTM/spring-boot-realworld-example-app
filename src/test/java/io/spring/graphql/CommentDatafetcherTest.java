package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import io.spring.graphql.types.Article;
import io.spring.graphql.types.Comment;
import io.spring.graphql.types.CommentsConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CommentDatafetcherTest {

  @Mock private CommentQueryService commentQueryService;
  @Mock private DataFetchingEnvironment env;

  private CommentDatafetcher commentDatafetcher;
  private DgsDataFetchingEnvironment dfe;

  private final ProfileData author = new ProfileData("id", "jane", "bio", "image", false);

  @BeforeEach
  void setUp() {
    commentDatafetcher = new CommentDatafetcher(commentQueryService);
    dfe = new DgsDataFetchingEnvironment(env);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private CommentData commentData(String id) {
    return new CommentData(id, "body-" + id, "article-1", new DateTime(), new DateTime(), author);
  }

  @Test
  void should_build_comment_from_local_context() {
    CommentData data = commentData("c1");
    when(env.<CommentData>getLocalContext()).thenReturn(data);

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dfe);

    assertThat(result.getData().getId()).isEqualTo("c1");
    assertThat(result.getData().getBody()).isEqualTo("body-c1");
    assertThat((Map<String, ?>) result.getLocalContext()).containsKey("c1");
  }

  @Test
  void should_throw_when_neither_first_nor_last_provided() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> commentDatafetcher.articleComments(null, null, null, null, dfe));
  }

  @Test
  void should_return_comments_connection_for_first() {
    Article article = Article.newBuilder().slug("the-slug").build();
    ArticleData articleData =
        new ArticleData(
            "article-1",
            "the-slug",
            "t",
            "d",
            "b",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Collections.emptyList(),
            author);
    when(env.<Article>getSource()).thenReturn(article);
    when(env.<Map<String, ArticleData>>getLocalContext())
        .thenReturn(Collections.singletonMap("the-slug", articleData));
    CursorPager<CommentData> pager =
        new CursorPager<>(
            Arrays.asList(commentData("c1"), commentData("c2")), Direction.NEXT, false);
    when(commentQueryService.findByArticleIdWithCursor(eq("article-1"), any(), any()))
        .thenReturn(pager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dfe);

    assertThat(result.getData().getEdges()).hasSize(2);
    assertThat(result.getData().getEdges().get(0).getNode().getId()).isEqualTo("c1");
    assertThat((Map<String, ?>) result.getLocalContext()).containsKeys("c1", "c2");
  }

  @Test
  void should_return_comments_connection_for_last() {
    Article article = Article.newBuilder().slug("the-slug").build();
    ArticleData articleData =
        new ArticleData(
            "article-1",
            "the-slug",
            "t",
            "d",
            "b",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Collections.emptyList(),
            author);
    when(env.<Article>getSource()).thenReturn(article);
    when(env.<Map<String, ArticleData>>getLocalContext())
        .thenReturn(Collections.singletonMap("the-slug", articleData));
    CursorPager<CommentData> pager =
        new CursorPager<>(Collections.singletonList(commentData("c9")), Direction.PREV, true);
    when(commentQueryService.findByArticleIdWithCursor(eq("article-1"), any(), any()))
        .thenReturn(pager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(null, null, 5, null, dfe);

    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getPageInfo().isHasPreviousPage()).isTrue();
  }
}
