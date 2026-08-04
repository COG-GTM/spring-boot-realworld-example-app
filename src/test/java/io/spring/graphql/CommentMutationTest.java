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
import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.CommentQueryService;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.user.User;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      CommentMutation.class,
      CommentDatafetcher.class,
      GraphQLCustomizeExceptionHandler.class
    })
class CommentMutationTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleRepository articleRepository;
  @MockBean private CommentRepository commentRepository;
  @MockBean private CommentQueryService commentQueryService;

  private final User author = new User("a@example.com", "author", "123", "", "");
  private final User other = new User("o@example.com", "other", "123", "", "");

  private Article article() {
    return new Article("Title", "Desc", "Body", Arrays.asList("java"), author.getId());
  }

  @Test
  void should_add_comment_for_authenticated_user() {
    authenticate(author);
    Article article = article();
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    CommentData commentData =
        new CommentData(
            "comment-id",
            "great post",
            article.getId(),
            new DateTime(),
            new DateTime(),
            new ProfileData(author.getId(), "author", "", "", false));
    when(commentQueryService.findById(any(), eq(author))).thenReturn(Optional.of(commentData));

    String query =
        "mutation { addComment(slug: \""
            + article.getSlug()
            + "\", body: \"great post\") { comment { id body } } }";

    DocumentContext context = dgsQueryExecutor.executeAndGetDocumentContext(query);

    assertThat(context.read("data.addComment.comment.body", String.class)).isEqualTo("great post");
    assertThat(context.read("data.addComment.comment.id", String.class)).isEqualTo("comment-id");
    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).save(captor.capture());
    assertThat(captor.getValue().getBody()).isEqualTo("great post");
    assertThat(captor.getValue().getUserId()).isEqualTo(author.getId());
    assertThat(captor.getValue().getArticleId()).isEqualTo(article.getId());
  }

  @Test
  void should_error_adding_comment_when_unauthenticated() {
    anonymous();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { addComment(slug: \"s\", body: \"x\") { comment { id } } }");

    assertSingleErrorFrom(result, AuthenticationException.class);
    verify(commentRepository, never()).save(any());
  }

  @Test
  void should_error_adding_comment_when_article_missing() {
    authenticate(author);
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { addComment(slug: \"missing\", body: \"x\") { comment { id } } }");

    assertSingleErrorFrom(result, ResourceNotFoundException.class);
    verify(commentRepository, never()).save(any());
  }

  @Test
  void should_delete_comment_when_author() {
    authenticate(author);
    Article article = article();
    Comment comment = new Comment("body", author.getId(), article.getId());
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

    assertThat(success).isTrue();
    verify(commentRepository).remove(eq(comment));
  }

  @Test
  void should_reject_delete_comment_when_not_authorized() {
    authenticate(other);
    Article article = article();
    Comment comment = new Comment("body", author.getId(), article.getId());
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

    assertSingleErrorFrom(result, NoAuthorizationException.class);
    verify(commentRepository, never()).remove(any());
  }

  @Test
  void should_error_delete_comment_when_missing() {
    authenticate(author);
    Article article = article();
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("ghost"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { deleteComment(slug: \""
                + article.getSlug()
                + "\", id: \"ghost\") { success } }");

    assertSingleErrorFrom(result, ResourceNotFoundException.class);
    verify(commentRepository, never()).remove(any());
  }
}
