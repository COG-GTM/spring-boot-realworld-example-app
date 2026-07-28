package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
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
import io.spring.graphql.types.CommentPayload;
import io.spring.graphql.types.DeletionStatus;
import java.util.Collections;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentMutationTest extends GraphQLTestBase {

  @Mock private ArticleRepository articleRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentQueryService commentQueryService;

  @InjectMocks private CommentMutation commentMutation;

  private Article article(User author) {
    return new Article("title", "desc", "body", Collections.singletonList("joda"), author.getId());
  }

  private CommentData commentData(String articleId) {
    return new CommentData(
        "comment-id",
        "comment body",
        articleId,
        new DateTime(),
        new DateTime(),
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
  }

  @Test
  void should_create_comment() {
    Article article = article(user);
    CommentData commentData = commentData(article.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(anyString(), eq(user))).thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result =
        commentMutation.createComment(article.getSlug(), "comment body");

    assertThat(result.getData()).isNotNull();
    assertThat((Object) result.getLocalContext()).isEqualTo(commentData);
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  void should_fail_create_comment_when_article_not_found() {
    when(articleRepository.findBySlug(eq("unknown"))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> commentMutation.createComment("unknown", "body"));
  }

  @Test
  void should_fail_create_comment_when_comment_not_readable() {
    Article article = article(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(anyString(), eq(user))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> commentMutation.createComment(article.getSlug(), "body"));
  }

  @Test
  void should_fail_create_comment_for_anonymous_user() {
    anonymous();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> commentMutation.createComment("slug", "body"));
  }

  @Test
  void should_remove_own_comment() {
    Article article = article(user);
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment(article.getSlug(), comment.getId());

    assertThat(status.getSuccess()).isTrue();
    verify(commentRepository).remove(eq(comment));
  }

  @Test
  void should_fail_remove_comment_of_other_user_on_other_article() {
    User other = new User("other@test.com", "other", "123", "", "");
    Article article = article(other);
    Comment comment = new Comment("body", other.getId(), article.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    assertThatExceptionOfType(NoAuthorizationException.class)
        .isThrownBy(() -> commentMutation.removeComment(article.getSlug(), comment.getId()));
    verify(commentRepository, never()).remove(any());
  }

  @Test
  void should_fail_remove_comment_when_comment_not_found() {
    Article article = article(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("unknown")))
        .thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> commentMutation.removeComment(article.getSlug(), "unknown"));
  }

  @Test
  void should_fail_remove_comment_when_article_not_found() {
    when(articleRepository.findBySlug(eq("unknown"))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> commentMutation.removeComment("unknown", "comment-id"));
  }

  @Test
  void should_fail_remove_comment_for_anonymous_user() {
    anonymous();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> commentMutation.removeComment("slug", "comment-id"));
  }
}
