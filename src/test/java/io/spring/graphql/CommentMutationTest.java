package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

  private final User user = new User("me@example.com", "me", "123", "", "");
  private Article article;

  @BeforeEach
  void setUp() {
    articleRepository = mock(ArticleRepository.class);
    commentRepository = mock(CommentRepository.class);
    commentQueryService = mock(CommentQueryService.class);
    commentMutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);
    article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_create_comment() {
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user)))
        .thenReturn(Optional.of(new CommentData("cid", "body", article.getId(), null, null, null)));

    DataFetcherResult<CommentPayload> result =
        commentMutation.createComment(article.getSlug(), "great");

    verify(commentRepository).save(any(Comment.class));
    assertEquals("cid", ((CommentData) result.getLocalContext()).getId());
  }

  @Test
  void should_throw_when_create_comment_without_auth() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    assertThrows(
        AuthenticationException.class, () -> commentMutation.createComment("slug", "body"));
  }

  @Test
  void should_throw_when_create_comment_article_missing() {
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.createComment("missing", "body"));
  }

  @Test
  void should_remove_comment_when_authorized() {
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment(article.getSlug(), comment.getId());

    assertTrue(status.getSuccess());
    verify(commentRepository).remove(comment);
  }

  @Test
  void should_throw_when_remove_comment_not_authorized() {
    Article othersArticle = new Article("t", "d", "b", Arrays.asList("x"), "someone-else");
    Comment comment = new Comment("body", "another-user", othersArticle.getId());
    when(articleRepository.findBySlug(othersArticle.getSlug()))
        .thenReturn(Optional.of(othersArticle));
    when(commentRepository.findById(othersArticle.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    assertThrows(
        NoAuthorizationException.class,
        () -> commentMutation.removeComment(othersArticle.getSlug(), comment.getId()));
  }

  @Test
  void should_throw_when_remove_comment_missing() {
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), any())).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.removeComment(article.getSlug(), "missing"));
  }
}
