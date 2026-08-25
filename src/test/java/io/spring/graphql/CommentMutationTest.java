package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class CommentMutationTest {

  @Mock private ArticleRepository articleRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentQueryService commentQueryService;

  private CommentMutation commentMutation;
  private User user;
  private Article article;

  @BeforeEach
  public void setUp() {
    commentMutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);
    user = new User("a@test.com", "aisensiy", "123", "bio", "image");
    article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void login() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @Test
  public void should_create_comment() {
    login();
    CommentData commentData =
        new CommentData(
            "comment-id",
            "comment body",
            article.getId(),
            new DateTime(),
            new DateTime(),
            new ProfileData("user-id", "aisensiy", "bio", "image", false));
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), any())).thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result =
        commentMutation.createComment(article.getSlug(), "comment body");

    verify(commentRepository).save(any(Comment.class));
    assertThat(result.getData()).isNotNull();
    assertThat(result.getLocalContext()).isEqualTo(commentData);
  }

  @Test
  public void should_not_create_comment_without_login() {
    anonymous();
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> commentMutation.createComment("slug", "body"));
    verify(commentRepository, never()).save(any());
  }

  @Test
  public void should_not_create_comment_for_missing_article() {
    login();
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> commentMutation.createComment("slug", "body"));
    verify(commentRepository, never()).save(any());
  }

  @Test
  public void should_throw_when_created_comment_can_not_be_read() {
    login();
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), any())).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> commentMutation.createComment(article.getSlug(), "body"));
  }

  @Test
  public void should_remove_comment() {
    login();
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment(article.getSlug(), comment.getId());

    verify(commentRepository).remove(comment);
    assertThat(status.getSuccess()).isTrue();
  }

  @Test
  public void should_not_remove_comment_without_login() {
    anonymous();
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> commentMutation.removeComment("slug", "comment-id"));
  }

  @Test
  public void should_not_remove_comment_of_other_users() {
    User other = new User("b@test.com", "other", "123", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(other, null));
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    assertThatExceptionOfType(NoAuthorizationException.class)
        .isThrownBy(() -> commentMutation.removeComment(article.getSlug(), comment.getId()));
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void should_throw_when_removing_missing_comment() {
    login();
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), "missing")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> commentMutation.removeComment(article.getSlug(), "missing"));
  }

  @Test
  public void should_throw_when_removing_comment_of_missing_article() {
    login();
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> commentMutation.removeComment("slug", "comment-id"));
  }
}
