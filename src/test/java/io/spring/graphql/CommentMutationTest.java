package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.application.CommentQueryService;
import io.spring.application.data.CommentData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {DgsAutoConfiguration.class, CommentMutation.class, CommentDatafetcher.class})
public class CommentMutationTest extends GraphQLTestBase {

  private static final DateTime TIME = new DateTime(2022, 2, 2, 10, 0, DateTimeZone.UTC);
  private static final String TIME_ISO = "2022-02-02T10:00:00.000Z";

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private CommentRepository commentRepository;

  @MockBean private CommentQueryService commentQueryService;

  @Test
  void should_add_comment_to_article() {
    Article article = article(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user)))
        .thenReturn(
            Optional.of(
                new CommentData(
                    "comment-id", "a comment", article.getId(), TIME, TIME, profileData)));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { addComment(slug: \""
                + article.getSlug()
                + "\", body: \"a comment\") { comment { id body createdAt updatedAt } } }");

    assertThat(context.read("$.data.addComment.comment.id", String.class)).isEqualTo("comment-id");
    assertThat(context.read("$.data.addComment.comment.body", String.class)).isEqualTo("a comment");
    assertThat(context.read("$.data.addComment.comment.createdAt", String.class))
        .isEqualTo(TIME_ISO);

    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).save(captor.capture());
    assertThat(captor.getValue().getBody()).isEqualTo("a comment");
    assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
    assertThat(captor.getValue().getArticleId()).isEqualTo(article.getId());
  }

  @Test
  void should_not_add_comment_for_anonymous_user() {
    logout();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { addComment(slug: \"a-title\", body: \"a comment\") { __typename } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("AuthenticationException");
    verify(commentRepository, never()).save(any());
  }

  @Test
  void should_not_add_comment_to_unknown_article() {
    when(articleRepository.findBySlug(eq("unknown"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { addComment(slug: \"unknown\", body: \"a comment\") { __typename } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
  }

  @Test
  void should_fail_when_created_comment_cannot_be_read_back() {
    Article article = article(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { addComment(slug: \""
                + article.getSlug()
                + "\", body: \"a comment\") { __typename } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
  }

  @Test
  void should_delete_own_comment() {
    Article article = article(user);
    Comment comment = new Comment("a comment", user.getId(), article.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            "mutation { deleteComment(slug: \""
                + article.getSlug()
                + "\", id: \""
                + comment.getId()
                + "\") { success } }");

    assertThat(context.read("$.data.deleteComment.success", Boolean.class)).isTrue();
    verify(commentRepository).remove(eq(comment));
  }

  @Test
  void should_not_delete_comment_of_another_user() {
    User other = new User("other@jacob.com", "other", "123", "", DEFAULT_AVATAR);
    Article article = article(other);
    Comment comment = new Comment("a comment", other.getId(), article.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
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
    assertThat(result.getErrors().get(0).getMessage()).contains("NoAuthorizationException");
    verify(commentRepository, never()).remove(any());
  }

  @Test
  void should_not_delete_unknown_comment() {
    Article article = article(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("unknown")))
        .thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { deleteComment(slug: \""
                + article.getSlug()
                + "\", id: \"unknown\") { success } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
  }

  @Test
  void should_not_delete_comment_for_anonymous_user() {
    logout();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { deleteComment(slug: \"a-title\", id: \"comment-id\") { success } }");

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("AuthenticationException");
  }

  private Article article(User author) {
    return new Article(
        "a title", "a description", "a body", Arrays.asList("joda"), author.getId(), TIME);
  }
}
