package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.util.Collections;
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
    user = new User("a@b.com", "alice", "123", "", "");
    article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void login(User current) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(current, null, Collections.emptyList()));
  }

  @Test
  public void should_throw_authentication_exception_when_creating_comment_anonymously() {
    assertThatThrownBy(() -> commentMutation.createComment("a-slug", "body"))
        .isInstanceOf(AuthenticationException.class);
    verify(commentRepository, never()).save(any());
  }

  @Test
  public void should_throw_not_found_when_article_missing_on_create() {
    login(user);
    when(articleRepository.findBySlug("a-slug")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> commentMutation.createComment("a-slug", "body"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  public void should_create_comment_and_return_payload() {
    login(user);
    when(articleRepository.findBySlug("a-slug")).thenReturn(Optional.of(article));
    CommentData commentData =
        new CommentData(
            "comment-id",
            "the body",
            article.getId(),
            new DateTime(1000L),
            new DateTime(1000L),
            new ProfileData(user.getId(), "alice", "bio", "image", false));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result = commentMutation.createComment("a-slug", "the body");

    verify(commentRepository).save(any(Comment.class));
    assertThat(result.getData()).isNotNull();
    assertThat(result.getLocalContext()).isSameAs(commentData);
  }

  @Test
  public void should_throw_not_found_when_created_comment_cannot_be_read_back() {
    login(user);
    when(articleRepository.findBySlug("a-slug")).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> commentMutation.createComment("a-slug", "the body"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  public void should_throw_authentication_exception_when_removing_comment_anonymously() {
    assertThatThrownBy(() -> commentMutation.removeComment("a-slug", "comment-id"))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  public void should_throw_not_found_when_article_missing_on_remove() {
    login(user);
    when(articleRepository.findBySlug("a-slug")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> commentMutation.removeComment("a-slug", "comment-id"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  public void should_throw_not_found_when_comment_missing_on_remove() {
    login(user);
    when(articleRepository.findBySlug("a-slug")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), "comment-id")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> commentMutation.removeComment("a-slug", "comment-id"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  public void should_throw_no_authorization_when_user_cannot_write_comment() {
    User other = new User("o@b.com", "other", "123", "", "");
    login(other);
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(articleRepository.findBySlug("a-slug")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    assertThatThrownBy(() -> commentMutation.removeComment("a-slug", comment.getId()))
        .isInstanceOf(NoAuthorizationException.class);
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void should_remove_comment_of_author() {
    login(user);
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(articleRepository.findBySlug("a-slug")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment("a-slug", comment.getId());

    verify(commentRepository).remove(comment);
    assertThat(status.getSuccess()).isTrue();
  }
}
