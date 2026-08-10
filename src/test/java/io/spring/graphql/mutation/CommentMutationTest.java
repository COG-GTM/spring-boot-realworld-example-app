package io.spring.graphql.mutation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import io.spring.graphql.CommentMutation;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.CommentPayload;
import io.spring.graphql.types.DeletionStatus;
import java.util.Collections;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class CommentMutationTest {

  private final ArticleRepository articleRepository = mock(ArticleRepository.class);
  private final CommentRepository commentRepository = mock(CommentRepository.class);
  private final CommentQueryService commentQueryService = mock(CommentQueryService.class);
  private final CommentMutation mutation =
      new CommentMutation(articleRepository, commentRepository, commentQueryService);

  private final User user = new User("jake@jake.jake", "jake", "123", "bio", "image");
  private final Article article =
      new Article("title", "desc", "body", Collections.emptyList(), "author-id");

  private void login(User currentUser) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null, null));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_create_comment() {
    login(user);
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    CommentData commentData =
        new CommentData(
            "comment-id", "hello", article.getId(), new DateTime(), new DateTime(), null);
    when(commentQueryService.findById(any(String.class), eq(user)))
        .thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result = mutation.createComment("title", "hello");

    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).save(captor.capture());
    assertThat(captor.getValue().getBody()).isEqualTo("hello");
    assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
    assertThat(captor.getValue().getArticleId()).isEqualTo(article.getId());
    assertThat(result.getLocalContext()).isSameAs(commentData);
  }

  @Test
  void should_reject_create_comment_for_anonymous_user() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> mutation.createComment("title", "hello"));
    verify(commentRepository, never()).save(any());
  }

  @Test
  void should_throw_not_found_when_commenting_missing_article() {
    login(user);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> mutation.createComment("missing", "hello"));
  }

  @Test
  void should_throw_not_found_when_comment_data_missing_after_save() {
    login(user);
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(String.class), eq(user))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> mutation.createComment("title", "hello"));
  }

  @Test
  void should_remove_own_comment() {
    login(user);
    Comment comment = new Comment("hello", user.getId(), article.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = mutation.removeComment("title", comment.getId());

    verify(commentRepository).remove(comment);
    assertThat(status.getSuccess()).isTrue();
  }

  @Test
  void should_reject_removing_others_comment() {
    login(user);
    Comment comment = new Comment("hello", "someone-else", article.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    assertThatExceptionOfType(NoAuthorizationException.class)
        .isThrownBy(() -> mutation.removeComment("title", comment.getId()));
    verify(commentRepository, never()).remove(any());
  }

  @Test
  void should_throw_not_found_when_comment_missing() {
    login(user);
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), "missing")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> mutation.removeComment("title", "missing"));
  }

  @Test
  void should_throw_not_found_when_removing_comment_of_missing_article() {
    login(user);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> mutation.removeComment("missing", "comment-id"));
  }
}
