package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.CommentQueryService;
import io.spring.application.ProfileQueryService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      GraphQLCustomizeExceptionHandler.class,
      CommentMutation.class,
      CommentDatafetcher.class,
      ProfileDatafetcher.class
    })
class CommentMutationTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleRepository articleRepository;
  @MockBean private CommentRepository commentRepository;
  @MockBean private CommentQueryService commentQueryService;
  @MockBean private ProfileQueryService profileQueryService;

  private User author;
  private Article article;

  @BeforeEach
  void setUp() {
    author = userFixture("john");
    article =
        new Article("a title", "a description", "a body", Arrays.asList("java"), author.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(profileQueryService.findByUsername(eq(author.getUsername()), any()))
        .thenReturn(
            Optional.of(
                new ProfileData(
                    author.getId(),
                    author.getUsername(),
                    author.getBio(),
                    author.getImage(),
                    false)));
  }

  @Test
  void should_add_comment_to_article() {
    authenticate(author);
    when(commentQueryService.findById(any(), eq(author)))
        .thenAnswer(
            invocation ->
                Optional.of(commentDataFixture(invocation.getArgument(0), "new comment")));

    String body =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "mutation { addComment(slug: \"%s\", body: \"new comment\") { comment { id body author { username } } } }",
                article.getSlug()),
            "data.addComment.comment.body");

    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).save(captor.capture());
    assertThat(captor.getValue().getBody()).isEqualTo("new comment");
    assertThat(captor.getValue().getUserId()).isEqualTo(author.getId());
    assertThat(captor.getValue().getArticleId()).isEqualTo(article.getId());
    assertThat(body).isEqualTo("new comment");
  }

  @Test
  void should_not_add_comment_for_anonymous_user() {
    anonymous();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { addComment(slug: \"%s\", body: \"new comment\") { comment { id } } }",
                article.getSlug()));

    assertFailedWith(result, AuthenticationException.class);
    verify(commentRepository, never()).save(any());
  }

  @Test
  void should_return_error_when_commenting_unknown_article() {
    authenticate(author);
    when(articleRepository.findBySlug(eq("unknown"))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { addComment(slug: \"unknown\", body: \"new comment\") { comment { id } } }");

    assertFailedWith(result, ResourceNotFoundException.class);
    verify(commentRepository, never()).save(any());
  }

  @Test
  void should_delete_own_comment() {
    authenticate(author);
    Comment comment = new Comment("a comment", author.getId(), article.getId());
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    Boolean success =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "mutation { deleteComment(slug: \"%s\", id: \"%s\") { success } }",
                article.getSlug(), comment.getId()),
            "data.deleteComment.success");

    verify(commentRepository).remove(eq(comment));
    assertThat(success).isTrue();
  }

  @Test
  void should_not_delete_comment_of_other_user() {
    User other = userFixture("jane");
    authenticate(other);
    Comment comment = new Comment("a comment", author.getId(), article.getId());
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { deleteComment(slug: \"%s\", id: \"%s\") { success } }",
                article.getSlug(), comment.getId()));

    assertFailedWith(result, NoAuthorizationException.class);
    verify(commentRepository, never()).remove(any());
  }

  @Test
  void should_return_error_when_comment_not_found() {
    authenticate(author);
    when(commentRepository.findById(eq(article.getId()), eq("unknown")))
        .thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { deleteComment(slug: \"%s\", id: \"unknown\") { success } }",
                article.getSlug()));

    assertFailedWith(result, ResourceNotFoundException.class);
  }

  private CommentData commentDataFixture(String id, String body) {
    DateTime now = new DateTime();
    return new CommentData(
        id,
        body,
        article.getId(),
        now,
        now,
        new ProfileData(
            author.getId(), author.getUsername(), author.getBio(), author.getImage(), false));
  }
}
