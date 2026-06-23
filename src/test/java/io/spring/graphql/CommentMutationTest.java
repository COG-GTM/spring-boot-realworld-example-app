package io.spring.graphql;

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
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommentMutationTest {

  private ArticleRepository articleRepository;
  private CommentRepository commentRepository;
  private CommentQueryService commentQueryService;
  private CommentMutation mutation;
  private User user;

  @BeforeEach
  void setUp() {
    articleRepository = mock(ArticleRepository.class);
    commentRepository = mock(CommentRepository.class);
    commentQueryService = mock(CommentQueryService.class);
    mutation = new CommentMutation(articleRepository, commentRepository, commentQueryService);
    user = new User("user@test.com", "user", "123", "bio", "image");
  }

  @AfterEach
  void tearDown() {
    GraphQLTestSecurity.clear();
  }

  private Article article(String ownerId) {
    return new Article("title", "desc", "body", Collections.singletonList("java"), ownerId);
  }

  @Test
  void createComment_saves_and_returns_payload() {
    GraphQLTestSecurity.login(user);
    Article article = article(user.getId());
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    CommentData commentData =
        new CommentData("cid", "body", article.getId(), new DateTime(), new DateTime(), null);
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result = mutation.createComment("slug", "body");

    assertNotNull(result.getData());
    org.junit.jupiter.api.Assertions.assertEquals(commentData, result.getLocalContext());
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  void createComment_when_anonymous_throws_authentication() {
    GraphQLTestSecurity.anonymous();
    assertThrows(AuthenticationException.class, () -> mutation.createComment("slug", "body"));
  }

  @Test
  void createComment_when_article_missing_throws_not_found() {
    GraphQLTestSecurity.login(user);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> mutation.createComment("slug", "body"));
  }

  @Test
  void createComment_when_saved_comment_not_found_throws_not_found() {
    GraphQLTestSecurity.login(user);
    Article article = article(user.getId());
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> mutation.createComment("slug", "body"));
  }

  @Test
  void removeComment_when_author_removes_comment() {
    GraphQLTestSecurity.login(user);
    Article article = article(user.getId());
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = mutation.removeComment("slug", comment.getId());

    assertTrue(status.getSuccess());
    verify(commentRepository).remove(comment);
  }

  @Test
  void removeComment_when_anonymous_throws_authentication() {
    GraphQLTestSecurity.anonymous();
    assertThrows(AuthenticationException.class, () -> mutation.removeComment("slug", "cid"));
  }

  @Test
  void removeComment_when_article_missing_throws_not_found() {
    GraphQLTestSecurity.login(user);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> mutation.removeComment("slug", "cid"));
  }

  @Test
  void removeComment_when_comment_missing_throws_not_found() {
    GraphQLTestSecurity.login(user);
    Article article = article(user.getId());
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("cid"))).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> mutation.removeComment("slug", "cid"));
  }

  @Test
  void removeComment_when_not_authorized_throws_no_authorization() {
    User other = new User("other@test.com", "other", "123", "", "");
    GraphQLTestSecurity.login(other);
    Article article = article(user.getId());
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    assertThrows(
        NoAuthorizationException.class, () -> mutation.removeComment("slug", comment.getId()));
    verify(commentRepository, never()).remove(any());
  }
}
