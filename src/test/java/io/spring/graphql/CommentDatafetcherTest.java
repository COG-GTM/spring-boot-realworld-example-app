package io.spring.graphql;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.Collections;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CommentDatafetcherTest extends GraphQLTestBase {

  @Mock private CommentQueryService commentQueryService;
  @Mock private DataFetchingEnvironment env;

  private CommentDatafetcher commentDatafetcher;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    commentDatafetcher = new CommentDatafetcher(commentQueryService);
    setAnonymous();
  }

  private CommentData commentData(String id) {
    return new CommentData(
        id,
        "body",
        "article-id",
        new DateTime(),
        new DateTime(),
        new ProfileData("pid", "author", "bio", "image", false));
  }

  @Test
  public void should_get_comment_from_local_context() {
    when(env.<CommentData>getLocalContext()).thenReturn(commentData("cid"));

    DataFetcherResult<Comment> result =
        commentDatafetcher.getComment(new DgsDataFetchingEnvironment(env));

    assertThat(result.getData().getId(), is("cid"));
    assertThat(result.getData().getBody(), is("body"));
  }

  @Test
  public void should_get_article_comments_with_first() {
    Article article = Article.newBuilder().slug("a-slug").build();
    ArticleData articleData =
        new ArticleData(
            "article-id",
            "a-slug",
            "title",
            "desc",
            "body",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Collections.emptyList(),
            new ProfileData("pid", "author", "bio", "image", false));
    Map<String, ArticleData> map = Collections.singletonMap("a-slug", articleData);
    when(env.<Article>getSource()).thenReturn(article);
    when(env.<Map<String, ArticleData>>getLocalContext()).thenReturn(map);
    CursorPager<CommentData> pager =
        new CursorPager<>(Collections.singletonList(commentData("cid")), Direction.NEXT, false);
    when(commentQueryService.findByArticleIdWithCursor(eq("article-id"), any(), any()))
        .thenReturn(pager);

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(
            10, null, null, null, new DgsDataFetchingEnvironment(env));

    assertThat(result.getData().getEdges().size(), is(1));
    assertThat(result.getData().getEdges().get(0).getNode().getId(), is("cid"));
  }

  @Test
  public void should_throw_when_article_comments_missing_first_and_last() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            commentDatafetcher.articleComments(
                null, null, null, null, new DgsDataFetchingEnvironment(env)));
  }
}
