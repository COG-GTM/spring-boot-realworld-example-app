package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.ExecutionResult;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CommentMutationTest extends GraphQLTestBase {

  private Article article;

  @BeforeEach
  public void setUpFixtures() {
    article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
  }

  @Test
  public void should_add_comment() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    CommentData commentData =
        new CommentData(
            "comment-1",
            "nice post",
            article.getId(),
            new DateTime(),
            new DateTime(),
            new ProfileData(user.getId(), username, "bio", defaultAvatar, false));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    String body =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "mutation { addComment(slug: \"%s\", body: \"nice post\") { comment { body } } }",
                article.getSlug()),
            "data.addComment.comment.body");

    Assertions.assertEquals("nice post", body);
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  public void should_reject_add_comment_when_anonymous() {
    logout();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { addComment(slug: \"%s\", body: \"hi\") { comment { body } } }",
                article.getSlug()));

    Assertions.assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_return_error_when_article_not_found_on_add_comment() {
    when(articleRepository.findBySlug(any())).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { addComment(slug: \"missing\", body: \"hi\") { comment { body } } }");

    Assertions.assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_remove_comment_when_author() {
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    Boolean success =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "mutation { deleteComment(slug: \"%s\", id: \"%s\") { success } }",
                article.getSlug(), comment.getId()),
            "data.deleteComment.success");

    Assertions.assertTrue(success);
    verify(commentRepository).remove(comment);
  }

  @Test
  public void should_reject_remove_comment_when_not_authorized() {
    Article othersArticle =
        new Article("other", "d", "b", Arrays.asList("java"), "another-user-id");
    Comment comment = new Comment("body", "yet-another-user", othersArticle.getId());
    when(articleRepository.findBySlug(eq(othersArticle.getSlug())))
        .thenReturn(Optional.of(othersArticle));
    when(commentRepository.findById(eq(othersArticle.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { deleteComment(slug: \"%s\", id: \"%s\") { success } }",
                othersArticle.getSlug(), comment.getId()));

    Assertions.assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_return_error_when_comment_not_found() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(any(), any())).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { deleteComment(slug: \"%s\", id: \"nope\") { success } }",
                article.getSlug()));

    Assertions.assertFalse(result.getErrors().isEmpty());
  }
}
