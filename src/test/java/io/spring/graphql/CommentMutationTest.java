package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import io.spring.graphql.types.CommentPayload;
import io.spring.graphql.types.DeletionStatus;
import java.util.Collections;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class CommentMutationTest {

  @Mock private ArticleRepository articleRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentQueryService commentQueryService;

  private CommentMutation commentMutation;
  private User user;

  @BeforeEach
  void setUp() {
    commentMutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);
    user = new User("test@example.com", "testuser", "password", "", "");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(user, null);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_create_comment() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    ProfileData profileData = new ProfileData(user.getId(), "testuser", "", "", false);
    CommentData commentData =
        new CommentData(
            "commentId", "body", article.getId(), new DateTime(), new DateTime(), profileData);
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result = commentMutation.createComment("title", "body");

    assertNotNull(result);
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  void should_throw_when_article_not_found_for_comment() {
    when(articleRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.createComment("nonexistent", "body"));
  }

  @Test
  void should_delete_own_comment() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "otherUserId");
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus result = commentMutation.removeComment("title", comment.getId());

    assertTrue(result.getSuccess());
    verify(commentRepository).remove(comment);
  }

  @Test
  void should_throw_when_deleting_others_comment_without_article_ownership() {
    User otherUser = new User("other@example.com", "other", "pass", "", "");
    Article article =
        new Article("Title", "desc", "body", Collections.emptyList(), otherUser.getId());
    Comment comment = new Comment("body", otherUser.getId(), article.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    assertThrows(
        NoAuthorizationException.class,
        () -> commentMutation.removeComment("title", comment.getId()));
  }

  @Test
  void should_throw_when_comment_not_found() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), "nonexistent")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.removeComment("title", "nonexistent"));
  }
}
