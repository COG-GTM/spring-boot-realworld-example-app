package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class CommentMutationTest {

  private ArticleRepository articleRepository;
  private CommentRepository commentRepository;
  private CommentQueryService commentQueryService;
  private CommentMutation commentMutation;

  private User user;

  @BeforeEach
  public void setUp() {
    articleRepository = mock(ArticleRepository.class);
    commentRepository = mock(CommentRepository.class);
    commentQueryService = mock(CommentQueryService.class);
    commentMutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);

    user = new User("e@e.com", "user", "pass", "", "");
    setAuthenticatedUser(user);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAuthenticatedUser(User currentUser) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(currentUser, null, Collections.emptyList()));
  }

  private void setAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  private Article articleBy(String authorId) {
    return new Article("Test Title", "desc", "body", Arrays.asList("java"), authorId);
  }

  @Test
  public void should_create_comment_success() {
    String slug = "test-title";
    String body = "nice article";
    Article article = articleBy("other-author");
    CommentData commentData = new CommentData();

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), any())).thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result = commentMutation.createComment(slug, body);

    assertNotNull(result.getData());
    assertSame(commentData, result.getLocalContext());
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  public void should_throw_authentication_exception_when_creating_comment_unauthenticated() {
    setAnonymous();

    assertThrows(
        AuthenticationException.class, () -> commentMutation.createComment("test-title", "body"));
    verify(commentRepository, never()).save(any(Comment.class));
  }

  @Test
  public void should_throw_not_found_when_article_missing_on_create() {
    when(articleRepository.findBySlug(any())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.createComment("missing", "body"));
    verify(commentRepository, never()).save(any(Comment.class));
  }

  @Test
  public void should_throw_not_found_when_comment_data_missing_on_create() {
    String slug = "test-title";
    Article article = articleBy("other-author");

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), any())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.createComment(slug, "body"));
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  public void should_remove_comment_success_when_current_user_is_comment_author() {
    String slug = "test-title";
    Article article = articleBy("other-author");
    Comment comment = new Comment("body", user.getId(), article.getId());

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment(slug, comment.getId());

    assertTrue(status.getSuccess());
    verify(commentRepository).remove(comment);
  }

  @Test
  public void should_remove_comment_success_when_current_user_is_article_author() {
    String slug = "test-title";
    Article article = articleBy(user.getId());
    Comment comment = new Comment("body", "other-author", article.getId());

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment(slug, comment.getId());

    assertTrue(status.getSuccess());
    verify(commentRepository).remove(comment);
  }

  @Test
  public void should_throw_no_authorization_when_user_is_neither_author() {
    String slug = "test-title";
    Article article = articleBy("other-author");
    Comment comment = new Comment("body", "someone-else", article.getId());

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    assertThrows(
        NoAuthorizationException.class, () -> commentMutation.removeComment(slug, comment.getId()));
    verify(commentRepository, never()).remove(any(Comment.class));
  }

  @Test
  public void should_throw_authentication_exception_when_removing_comment_unauthenticated() {
    setAnonymous();

    assertThrows(
        AuthenticationException.class,
        () -> commentMutation.removeComment("test-title", "comment-id"));
  }

  @Test
  public void should_throw_not_found_when_article_missing_on_remove() {
    when(articleRepository.findBySlug(any())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.removeComment("missing", "comment-id"));
  }

  @Test
  public void should_throw_not_found_when_comment_missing_on_remove() {
    String slug = "test-title";
    Article article = articleBy("other-author");

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), any())).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.removeComment(slug, "comment-id"));
  }
}
