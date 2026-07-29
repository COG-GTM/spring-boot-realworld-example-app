package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.CommentQueryService;
import io.spring.application.data.CommentData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.user.User;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.CommentPayload;
import io.spring.graphql.types.DeletionStatus;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentMutationTest extends GraphqlTestBase {

  @Mock private ArticleRepository articleRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentQueryService commentQueryService;

  @InjectMocks private CommentMutation commentMutation;

  private User user;
  private Article article;

  @BeforeEach
  void setUp() {
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
    article = new Article("title", "desc", "body", Arrays.asList("joda"), user.getId());
  }

  @Test
  void should_create_comment() {
    login(user);
    CommentData commentData =
        new CommentData(
            "id", "comment body", article.getId(), new DateTime(), new DateTime(), null);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(String.class), eq(user)))
        .thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result =
        commentMutation.createComment(article.getSlug(), "comment body");

    assertThat((CommentData) result.getLocalContext()).isEqualTo(commentData);
    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).save(captor.capture());
    assertThat(captor.getValue().getBody()).isEqualTo("comment body");
    assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
    assertThat(captor.getValue().getArticleId()).isEqualTo(article.getId());
  }

  @Test
  void should_not_create_comment_without_login() {
    logout();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> commentMutation.createComment("slug", "body"));
  }

  @Test
  void should_not_create_comment_on_missing_article() {
    login(user);
    when(articleRepository.findBySlug("unknown")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> commentMutation.createComment("unknown", "body"));
  }

  @Test
  void should_fail_when_created_comment_cannot_be_read_back() {
    login(user);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(String.class), eq(user))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> commentMutation.createComment(article.getSlug(), "body"));
  }

  @Test
  void should_remove_own_comment() {
    login(user);
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment(article.getSlug(), comment.getId());

    assertThat(status.getSuccess()).isTrue();
    verify(commentRepository).remove(comment);
  }

  @Test
  void should_not_remove_comment_of_other_user() {
    User other = new User("other@test.com", "other", "123", "", "");
    login(other);
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    assertThatExceptionOfType(NoAuthorizationException.class)
        .isThrownBy(() -> commentMutation.removeComment(article.getSlug(), comment.getId()));
    verify(commentRepository, never()).remove(any(Comment.class));
  }

  @Test
  void should_not_remove_missing_comment() {
    login(user);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), "unknown")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> commentMutation.removeComment(article.getSlug(), "unknown"));
  }

  @Test
  void should_not_remove_comment_of_missing_article() {
    login(user);
    when(articleRepository.findBySlug("unknown")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> commentMutation.removeComment("unknown", "comment-id"));
  }

  @Test
  void should_not_remove_comment_without_login() {
    logout();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> commentMutation.removeComment("slug", "comment-id"));
  }
}
