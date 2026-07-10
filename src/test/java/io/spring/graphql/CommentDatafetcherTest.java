package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.core.user.User;
import io.spring.graphql.types.Article;
import io.spring.graphql.types.Comment;
import io.spring.graphql.types.CommentsConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentDatafetcherTest extends GraphQLTestBase {

  @Mock private CommentQueryService commentQueryService;

  private CommentDatafetcher commentDatafetcher;
  private User user;

  @BeforeEach
  void setUp() {
    commentDatafetcher = new CommentDatafetcher(commentQueryService);
    user = newUser();
  }

  @Test
  void should_get_comment_from_local_context() {
    DataFetchingEnvironment dfe = mockEnv();
    when(dfe.getLocalContext()).thenReturn(commentData("c1", "art1", "johnjacob"));

    DataFetcherResult<Comment> result = commentDatafetcher.getComment(dgs(dfe));

    assertThat(result.getData().getId()).isEqualTo("c1");
    assertThat(result.getData().getBody()).isEqualTo("a comment body");
  }

  @Test
  void should_get_article_comments_forward() {
    setCurrentUser(user);
    DataFetchingEnvironment dfe = mockEnv();
    Article article = Article.newBuilder().slug("art-slug").build();
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put("art-slug", articleData("art1", "art-slug", "johnjacob"));
    when(dfe.getSource()).thenReturn(article);
    when(dfe.getLocalContext()).thenReturn(localContext);
    when(commentQueryService.findByArticleIdWithCursor(
            eq("art1"), any(), any(CursorPageParameter.class)))
        .thenReturn(
            new CursorPager<>(
                Collections.singletonList(commentData("c1", "art1", "johnjacob")),
                Direction.NEXT,
                false));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(10, null, null, null, dgs(dfe));

    assertThat(result.getData().getEdges()).hasSize(1);
    assertThat(result.getData().getEdges().get(0).getNode().getId()).isEqualTo("c1");
  }

  @Test
  void should_get_article_comments_backward() {
    setCurrentUser(user);
    DataFetchingEnvironment dfe = mockEnv();
    Article article = Article.newBuilder().slug("art-slug").build();
    Map<String, ArticleData> localContext = new HashMap<>();
    localContext.put("art-slug", articleData("art1", "art-slug", "johnjacob"));
    when(dfe.getSource()).thenReturn(article);
    when(dfe.getLocalContext()).thenReturn(localContext);
    when(commentQueryService.findByArticleIdWithCursor(
            eq("art1"), any(), any(CursorPageParameter.class)))
        .thenReturn(new CursorPager<>(Collections.emptyList(), Direction.PREV, false));

    DataFetcherResult<CommentsConnection> result =
        commentDatafetcher.articleComments(null, null, 10, "123", dgs(dfe));

    assertThat(result.getData().getEdges()).isEmpty();
  }

  @Test
  void should_throw_when_neither_first_nor_last_provided() {
    DataFetchingEnvironment dfe = mockEnv();
    assertThatThrownBy(
            () -> commentDatafetcher.articleComments(null, null, null, null, dgs(dfe)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
