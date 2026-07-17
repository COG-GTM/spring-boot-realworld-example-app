package io.spring.graphql;

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
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.CommentPayload;
import io.spring.graphql.types.DeletionStatus;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class CommentMutationTest {

  private ArticleRepository articleRepository;
  private CommentRepository commentRepository;
  private CommentQueryService commentQueryService;
  private CommentMutation commentMutation;

  private User user;

  @BeforeEach
  void setUp() {
    articleRepository = mock(ArticleRepository.class);
    commentRepository = mock(CommentRepository.class);
    commentQueryService = mock(CommentQueryService.class);
    commentMutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);
    user = new User("john@example.com", "john", "123", "", "");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAs(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(u, null));
  }

  private void authenticateAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  private Article articleBy(User author) {
    return new Article("Title", "desc", "body", Arrays.asList("java"), author.getId());
  }

  @Test
  void should_create_comment_and_expose_data_via_local_context() {
    authenticateAs(user);
    Article article = articleBy(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    CommentData data = mock(CommentData.class);
    when(commentQueryService.findById(any(String.class), eq(user))).thenReturn(Optional.of(data));

    DataFetcherResult<CommentPayload> result =
        commentMutation.createComment(article.getSlug(), "nice post");

    assertThat(result.getData()).isNotNull();
    assertThat(result.getLocalContext()).isEqualTo(data);

    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).save(captor.capture());
    assertThat(captor.getValue().getBody()).isEqualTo("nice post");
    assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
    assertThat(captor.getValue().getArticleId()).isEqualTo(article.getId());
  }

  @Test
  void should_reject_create_comment_when_not_authenticated() {
    authenticateAnonymous();
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> commentMutation.createComment("slug", "body"));
    verify(commentRepository, never()).save(any());
  }

  @Test
  void should_reject_create_comment_when_article_missing() {
    authenticateAs(user);
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> commentMutation.createComment("missing", "body"));
    verify(commentRepository, never()).save(any());
  }

  @Test
  void should_reject_create_comment_when_saved_comment_not_readable() {
    authenticateAs(user);
    Article article = articleBy(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(String.class), eq(user))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> commentMutation.createComment(article.getSlug(), "body"));
  }

  @Test
  void should_remove_comment_when_author() {
    authenticateAs(user);
    Article article = articleBy(user);
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment(article.getSlug(), comment.getId());

    assertThat(status.getSuccess()).isTrue();
    verify(commentRepository).remove(comment);
  }

  @Test
  void should_reject_remove_comment_when_not_authorized() {
    authenticateAs(user);
    User another = new User("a@a.com", "another", "123", "", "");
    Article article = articleBy(another);
    Comment comment = new Comment("body", another.getId(), article.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    assertThatExceptionOfType(NoAuthorizationException.class)
        .isThrownBy(() -> commentMutation.removeComment(article.getSlug(), comment.getId()));
    verify(commentRepository, never()).remove(any());
  }

  @Test
  void should_reject_remove_comment_when_article_missing() {
    authenticateAs(user);
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> commentMutation.removeComment("missing", "cid"));
  }

  @Test
  void should_reject_remove_comment_when_comment_missing() {
    authenticateAs(user);
    Article article = articleBy(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("cid"))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> commentMutation.removeComment(article.getSlug(), "cid"));
    verify(commentRepository, never()).remove(any());
  }

  @Test
  void should_reject_remove_comment_when_not_authenticated() {
    authenticateAnonymous();
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> commentMutation.removeComment("slug", "cid"));
  }
}
