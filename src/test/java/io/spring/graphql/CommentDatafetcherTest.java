package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import io.spring.graphql.types.Article;
import io.spring.graphql.types.Comment;
import io.spring.graphql.types.CommentsConnection;
import java.util.Collections;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class CommentDatafetcherTest {

  private CommentQueryService commentQueryService;
  private CommentDatafetcher commentDatafetcher;
  private DataFetchingEnvironment inner;
  private DgsDataFetchingEnvironment dfe;

  @BeforeEach
  void setUp() {
    commentQueryService = mock(CommentQueryService.class);
    commentDatafetcher = new CommentDatafetcher(commentQueryService);
    inner = mock(DataFetchingEnvironment.class);
    dfe = new DgsDataFetchingEnvironment(inner);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private CommentData commentData(String id) {
    return new CommentData(id, "body", "articleId", new DateTime(), new DateTime(), null);
  }

  @Test
  void should_build_comment_from_local_context() {
    when(inner.getLocalContext()).thenReturn(commentData("cid"));

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dfe);

    assertEquals("cid", result.getData().getId());
    assertEquals("body", result.getData().getBody());
  }

  @Test
  void should_throw_when_both_first_and_last_null() {
    assertThrows(
        IllegalArgumentException.class,
        () -> commentDatafetcher.articleComments(null, null, null, null, dfe));
  }

  @Test
  void should_fetch_article_comments_forward() {
    Article article = Article.newBuilder().slug("a-slug").build();
    ArticleData articleData = new ArticleData();
    articleData.setId("article-id");
    Map<String, ArticleData> map = Collections.singletonMap("a-slug", articleData);
    when(inner.getSource()).thenReturn(article);
    when(inner.getLocalContext()).thenReturn(map);
    CursorPager<CommentData> pager =
        new CursorPager<>(Collections.singletonList(commentData("cid")), Direction.NEXT, false);
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), any(), any()))
        .thenReturn(pager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dfe);

    assertEquals(1, result.getData().getEdges().size());
  }

  @Test
  void should_fetch_article_comments_backward() {
    Article article = Article.newBuilder().slug("a-slug").build();
    ArticleData articleData = new ArticleData();
    articleData.setId("article-id");
    Map<String, ArticleData> map = Collections.singletonMap("a-slug", articleData);
    when(inner.getSource()).thenReturn(article);
    when(inner.getLocalContext()).thenReturn(map);
    CursorPager<CommentData> pager =
        new CursorPager<>(Collections.emptyList(), Direction.PREV, false);
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), any(), any()))
        .thenReturn(pager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(null, null, 10, null, dfe);

    assertEquals(0, result.getData().getEdges().size());
  }
}
