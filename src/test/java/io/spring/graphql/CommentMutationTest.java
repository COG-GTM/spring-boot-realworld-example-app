package io.spring.graphql;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class CommentMutationTest {

  @Mock private ArticleRepository articleRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentQueryService commentQueryService;
  @Mock private CommentData commentData;

  private CommentMutation commentMutation;
  private final User user = new User("email@test.com", "username", "123", "", "");
  private final Article article =
      new Article("a title", "desc", "body", singletonList("java"), user.getId());

  @BeforeEach
  public void setUp() {
    commentMutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void login(User currentUser) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null, emptyList()));
  }

  private void loginAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @Test
  public void should_create_comment() {
    login(user);
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    assertEquals(
        commentData, commentMutation.createComment("a-title", "content").getLocalContext());
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  public void should_not_create_comment_for_anonymous_user() {
    loginAnonymous();

    assertThrows(
        AuthenticationException.class, () -> commentMutation.createComment("a-title", "content"));
  }

  @Test
  public void should_not_create_comment_on_missing_article() {
    login(user);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.createComment("missing", "content"));
  }

  @Test
  public void should_remove_own_comment() {
    login(user);
    Comment comment = new Comment("content", user.getId(), article.getId());
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    assertTrue(commentMutation.removeComment("a-title", comment.getId()).getSuccess());
    verify(commentRepository).remove(comment);
  }

  @Test
  public void should_not_remove_comment_of_other_user() {
    User other = new User("other@test.com", "other", "123", "", "");
    login(other);
    Comment comment = new Comment("content", user.getId(), article.getId());
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    assertThrows(
        NoAuthorizationException.class,
        () -> commentMutation.removeComment("a-title", comment.getId()));
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void should_not_remove_missing_comment() {
    login(user);
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), "123")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.removeComment("a-title", "123"));
  }
}
