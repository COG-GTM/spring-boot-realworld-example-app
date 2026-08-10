package io.spring.graphql;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

  @InjectMocks private CommentMutation commentMutation;

  private User user;
  private Article article;

  @BeforeEach
  public void setUp() {
    user = new User("email@example.com", "username", "123", "", "");
    article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void login(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
  }

  private void loginAnonymously() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @Test
  public void should_create_comment() {
    login(user);
    CommentData commentData = new CommentData();
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result = commentMutation.createComment("title", "nice post");

    verify(commentRepository).save(any(Comment.class));
    Assertions.assertEquals(commentData, result.getLocalContext());
  }

  @Test
  public void should_throw_authentication_exception_when_not_logged_in() {
    loginAnonymously();
    Assertions.assertThrows(
        AuthenticationException.class, () -> commentMutation.createComment("title", "body"));
  }

  @Test
  public void should_throw_not_found_when_article_missing() {
    login(user);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.createComment("missing", "body"));
  }

  @Test
  public void should_remove_comment_by_author() {
    login(user);
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment("title", comment.getId());

    verify(commentRepository).remove(comment);
    Assertions.assertTrue(status.getSuccess());
  }

  @Test
  public void should_not_remove_comment_without_authorization() {
    User other = new User("other@example.com", "other", "123", "", "");
    login(other);
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    Assertions.assertThrows(
        NoAuthorizationException.class,
        () -> commentMutation.removeComment("title", comment.getId()));
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void should_throw_not_found_when_comment_missing() {
    login(user);
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), "missing")).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.removeComment("title", "missing"));
  }
}
