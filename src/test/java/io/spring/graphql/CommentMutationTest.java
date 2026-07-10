package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.exceptions.QueryException;
import io.spring.application.data.CommentData;
import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CommentMutationTest extends DgsGraphQLTestBase {

  private Article articleOf(User author) {
    return new Article("Title", "desc", "body", Arrays.asList("java"), author.getId());
  }

  @Test
  void should_add_comment() {
    setAuthenticatedUser(user);
    Article article = articleOf(user);
    CommentData commentData = commentData("comment-1", user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    String body =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { addComment(slug: \""
                + article.getSlug()
                + "\", body: \"nice article\") { comment { id body } } }",
            "data.addComment.comment.body");

    assertEquals(commentData.getBody(), body);
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  void should_reject_add_comment_when_not_authenticated() {
    setAnonymous();

    QueryException error =
        assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "mutation { addComment(slug: \"s\", body: \"b\") { comment { id } } }",
                    "data.addComment"));

    assertFalse(error.getErrors().isEmpty());
  }

  @Test
  void should_reject_add_comment_when_article_not_found() {
    setAuthenticatedUser(user);
    when(articleRepository.findBySlug(any())).thenReturn(Optional.empty());

    QueryException error =
        assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "mutation { addComment(slug: \"missing\", body: \"b\") { comment { id } } }",
                    "data.addComment"));

    assertFalse(error.getErrors().isEmpty());
  }

  @Test
  void should_delete_comment() {
    setAuthenticatedUser(user);
    Article article = articleOf(user);
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    Boolean success =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { deleteComment(slug: \""
                + article.getSlug()
                + "\", id: \""
                + comment.getId()
                + "\") { success } }",
            "data.deleteComment.success");

    assertTrue(success);
    verify(commentRepository).remove(eq(comment));
  }

  @Test
  void should_reject_delete_comment_when_not_authorized() {
    setAuthenticatedUser(user);
    User other = new User("other@test.com", "other", "123", "", "");
    Article article = articleOf(other);
    Comment comment = new Comment("body", other.getId(), article.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    QueryException error =
        assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "mutation { deleteComment(slug: \""
                        + article.getSlug()
                        + "\", id: \""
                        + comment.getId()
                        + "\") { success } }",
                    "data.deleteComment"));

    assertFalse(error.getErrors().isEmpty());
  }

  @Test
  void should_reject_delete_comment_when_comment_not_found() {
    setAuthenticatedUser(user);
    Article article = articleOf(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(any(), any())).thenReturn(Optional.empty());

    QueryException error =
        assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "mutation { deleteComment(slug: \""
                        + article.getSlug()
                        + "\", id: \"missing\") { success } }",
                    "data.deleteComment"));

    assertFalse(error.getErrors().isEmpty());
  }

  @Test
  void should_reject_delete_comment_when_not_authenticated() {
    setAnonymous();

    QueryException error =
        assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "mutation { deleteComment(slug: \"s\", id: \"c\") { success } }",
                    "data.deleteComment"));

    assertFalse(error.getErrors().isEmpty());
  }
}
