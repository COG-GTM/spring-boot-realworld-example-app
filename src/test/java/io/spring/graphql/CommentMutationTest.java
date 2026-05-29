package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import io.spring.graphql.types.DeletionStatus;
import java.util.Collections;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class CommentMutationTest {

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
    commentMutation = new CommentMutation(articleRepository, commentRepository, commentQueryService);
    user = new User("test@test.com", "testuser", "password", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_create_comment() {
    Article article =
        new Article("Title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    CommentData commentData =
        new CommentData(
            "commentId",
            "comment body",
            article.getId(),
            new DateTime(),
            new DateTime(),
            new ProfileData(user.getId(), user.getUsername(), "", "", false));
    when(commentQueryService.findById(any(), any())).thenReturn(Optional.of(commentData));

    var result = commentMutation.createComment("title", "comment body");

    assertNotNull(result);
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  void should_throw_when_not_authenticated() {
    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new org.springframework.security.authentication.AnonymousAuthenticationToken(
                "key",
                "anon",
                Collections.singletonList(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_ANONYMOUS"))));
    assertThrows(
        AuthenticationException.class, () -> commentMutation.createComment("slug", "body"));
  }

  @Test
  void should_throw_when_article_not_found() {
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.createComment("missing", "body"));
  }

  @Test
  void should_delete_comment() {
    Article article =
        new Article("Title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(commentRepository.findById(article.getId(), "commentId"))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment("title", "commentId");

    assertTrue(status.getSuccess());
    verify(commentRepository).remove(comment);
  }
}
