package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.CommentPayload;
import io.spring.graphql.types.DeletionStatus;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class CommentMutationTest {

  private ArticleRepository articleRepository;
  private CommentRepository commentRepository;
  private CommentQueryService commentQueryService;
  private CommentMutation commentMutation;

  private User currentUser;

  @BeforeEach
  void setUp() {
    articleRepository = mock(ArticleRepository.class);
    commentRepository = mock(CommentRepository.class);
    commentQueryService = mock(CommentQueryService.class);
    commentMutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);
    currentUser = new User("user@example.com", "user", "123", "bio", "image");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  private Article articleOwnedBy(User user) {
    return new Article("title", "desc", "body", List.of("java"), user.getId());
  }

  @Test
  void createComment_success_savesAndReturnsPayload() {
    authenticate(currentUser);
    Article article = articleOwnedBy(currentUser);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    CommentData commentData = mock(CommentData.class);
    when(commentQueryService.findById(any(String.class), eq(currentUser)))
        .thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result = commentMutation.createComment("slug", "body");

    verify(commentRepository).save(any(Comment.class));
    assertNotNull(result.getData());
    assertEquals(commentData, result.getLocalContext());
  }

  @Test
  void createComment_articleNotFound_throwsResourceNotFound() {
    authenticate(currentUser);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.createComment("slug", "body"));
  }

  @Test
  void createComment_commentDataNotFound_throwsResourceNotFound() {
    authenticate(currentUser);
    Article article = articleOwnedBy(currentUser);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(String.class), eq(currentUser)))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.createComment("slug", "body"));
  }

  @Test
  void createComment_noCurrentUser_throwsAuthenticationException() {
    anonymous();

    assertThrows(
        AuthenticationException.class, () -> commentMutation.createComment("slug", "body"));
  }

  @Test
  void removeComment_author_removesAndReturnsSuccess() {
    authenticate(currentUser);
    Article article = articleOwnedBy(currentUser);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    Comment comment = new Comment("body", currentUser.getId(), article.getId());
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment("slug", comment.getId());

    verify(commentRepository).remove(comment);
    assertTrue(status.getSuccess());
  }

  @Test
  void removeComment_nonAuthor_throwsNoAuthorization() {
    authenticate(currentUser);
    User otherUser = new User("other@example.com", "other", "123", "bio", "image");
    Article article = articleOwnedBy(otherUser);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    Comment comment = new Comment("body", otherUser.getId(), article.getId());
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    assertThrows(
        NoAuthorizationException.class,
        () -> commentMutation.removeComment("slug", comment.getId()));
    verify(commentRepository, never()).remove(any(Comment.class));
  }

  @Test
  void removeComment_commentNotFound_throwsResourceNotFound() {
    authenticate(currentUser);
    Article article = articleOwnedBy(currentUser);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), any(String.class)))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.removeComment("slug", "comment-id"));
  }

  @Test
  void removeComment_articleNotFound_throwsResourceNotFound() {
    authenticate(currentUser);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.removeComment("slug", "comment-id"));
  }

  @Test
  void removeComment_noCurrentUser_throwsAuthenticationException() {
    anonymous();

    assertThrows(
        AuthenticationException.class, () -> commentMutation.removeComment("slug", "comment-id"));
  }
}
