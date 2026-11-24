package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import graphql.ExecutionResult;
import io.spring.TestHelper;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class CommentMutationTest extends GraphQLTestBase {
  private Article article;
  private ProfileData profileData;

  @BeforeEach
  public void setUp() {
    article = new Article("title", "desc", "body", Arrays.asList("test"), user.getId());
    profileData =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleQueryService.findById(eq(article.getId()), any()))
        .thenReturn(Optional.of(TestHelper.getArticleDataFromArticleAndUser(article, user)));
    when(profileQueryService.findByUsername(eq(user.getUsername()), any()))
        .thenReturn(Optional.of(profileData));
  }

  @Test
  public void should_add_comment() {
    loginAs(user);
    when(commentQueryService.findById(any(), eq(user)))
        .thenAnswer(
            invocation ->
                Optional.of(
                    new CommentData(
                        invocation.getArgument(0),
                        "a comment",
                        article.getId(),
                        new DateTime(),
                        new DateTime(),
                        profileData)));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { addComment(slug: \""
                + article.getSlug()
                + "\", body: \"a comment\") { comment { id body createdAt author { username } } } }");

    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).save(captor.capture());
    Comment saved = captor.getValue();
    assertThat(saved.getBody()).isEqualTo("a comment");
    assertThat(saved.getArticleId()).isEqualTo(article.getId());
    assertThat(saved.getUserId()).isEqualTo(user.getId());

    assertThat(context.<String>read("data.addComment.comment.id")).isEqualTo(saved.getId());
    assertThat(context.<String>read("data.addComment.comment.body")).isEqualTo("a comment");
    assertThat(context.<String>read("data.addComment.comment.author.username"))
        .isEqualTo(user.getUsername());
  }

  @Test
  public void should_reject_add_comment_without_authentication() {
    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { addComment(slug: \""
                + article.getSlug()
                + "\", body: \"a comment\") { comment { id } } }");

    assertThat(result.getErrors()).hasSize(1);
    verify(commentRepository, never()).save(any());
  }

  @Test
  public void should_delete_own_comment() {
    User commenter = new User("commenter@test.com", "commenter", "123", "", "");
    loginAs(commenter);
    Comment comment = new Comment("a comment", commenter.getId(), article.getId());
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

    assertThat(success).isTrue();
    verify(commentRepository).remove(eq(comment));
  }

  @Test
  public void should_not_delete_comment_of_other_user() {
    User commenter = new User("commenter@test.com", "commenter", "123", "", "");
    loginAs(new User("other@test.com", "other", "123", "", ""));
    Comment comment = new Comment("a comment", commenter.getId(), article.getId());
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { deleteComment(slug: \""
                + article.getSlug()
                + "\", id: \""
                + comment.getId()
                + "\") { success } }");

    assertThat(result.getErrors()).hasSize(1);
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void should_return_error_when_deleting_unknown_comment() {
    loginAs(user);
    when(commentRepository.findById(eq(article.getId()), eq("missing")))
        .thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { deleteComment(slug: \""
                + article.getSlug()
                + "\", id: \"missing\") { success } }");

    assertThat(result.getErrors()).hasSize(1);
    verify(commentRepository, never()).remove(any());
  }
}
