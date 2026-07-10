package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.exceptions.QueryException;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CommentDatafetcherTest extends DgsGraphQLTestBase {

  private CursorPager<CommentData> onePage() {
    return new CursorPager<>(
        Collections.singletonList(commentData("comment-1", user)), Direction.NEXT, false);
  }

  @Test
  void should_resolve_article_comments() {
    setAuthenticatedUser(user);
    ArticleData articleData = articleData("hello-world", user);
    when(articleQueryService.findBySlug(eq("hello-world"), any()))
        .thenReturn(Optional.of(articleData));
    when(commentQueryService.findByArticleIdWithCursor(any(), any(), any())).thenReturn(onePage());

    List<String> ids =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ article(slug: \"hello-world\") { slug comments(first: 5) { edges { cursor node { id"
                + " body createdAt updatedAt } } pageInfo { hasNextPage } } } }",
            "data.article.comments.edges[*].node.id");

    assertEquals(Collections.singletonList("comment-1"), ids);
  }

  @Test
  void should_resolve_article_comments_backwards() {
    setAnonymous();
    ArticleData articleData = articleData("hello-world", user);
    when(articleQueryService.findBySlug(eq("hello-world"), any()))
        .thenReturn(Optional.of(articleData));
    when(commentQueryService.findByArticleIdWithCursor(any(), any(), any())).thenReturn(onePage());

    Boolean hasNext =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ article(slug: \"hello-world\") { comments(last: 5) { edges { node { id } } pageInfo {"
                + " hasNextPage } } } }",
            "data.article.comments.pageInfo.hasNextPage");

    assertFalse(hasNext);
  }

  @Test
  void should_error_when_article_comments_missing_pagination() {
    setAnonymous();
    ArticleData articleData = articleData("hello-world", user);
    when(articleQueryService.findBySlug(eq("hello-world"), any()))
        .thenReturn(Optional.of(articleData));

    QueryException error =
        org.junit.jupiter.api.Assertions.assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "{ article(slug: \"hello-world\") { comments { edges { node { id } } } } }",
                    "data.article.comments"));

    assertFalse(error.getErrors().isEmpty());
  }
}
