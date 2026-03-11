package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.CommentPayload;
import io.spring.graphql.types.DeletionStatus;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommentMutationTest {

  @Mock private ArticleRepository articleRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentQueryService commentQueryService;

  private CommentMutation commentMutation;
  private User user;
  private Article article;
  private Comment comment;
  private CommentData commentData;

  @BeforeEach
  void setUp() {
    commentMutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    article = new Article("Test Title", "description", "body", Arrays.asList("tag1"), user.getId());
    comment = new Comment("comment body", user.getId(), article.getId());
    DateTime now = DateTime.now();
    ProfileData profileData =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    commentData =
        new CommentData(comment.getId(), "comment body", article.getId(), now, now, profileData);
  }

  @Test
  void createComment_withValidInput_returnsCommentPayload() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));
      when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

      DataFetcherResult<CommentPayload> result =
          commentMutation.createComment("test-slug", "comment body");

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(commentData, result.getLocalContext());
      verify(commentRepository).save(any(Comment.class));
    }
  }

  @Test
  void createComment_articleNotFound_throwsResourceNotFoundException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class,
          () -> commentMutation.createComment("nonexistent", "comment body"));
    }
  }

  @Test
  void createComment_withoutAuthentication_throwsAuthenticationException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      assertThrows(
          AuthenticationException.class,
          () -> commentMutation.createComment("test-slug", "comment body"));
    }
  }

  @Test
  void createComment_commentDataNotFound_throwsResourceNotFoundException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));
      when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class,
          () -> commentMutation.createComment("test-slug", "comment body"));
    }
  }

  @Test
  void removeComment_withValidInput_returnsDeletionStatus() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));
      when(commentRepository.findById(article.getId(), comment.getId()))
          .thenReturn(Optional.of(comment));

      DeletionStatus result = commentMutation.removeComment("test-slug", comment.getId());

      assertNotNull(result);
      assertTrue(result.getSuccess());
      verify(commentRepository).remove(comment);
    }
  }

  @Test
  void removeComment_articleNotFound_throwsResourceNotFoundException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class,
          () -> commentMutation.removeComment("nonexistent", "comment-id"));
    }
  }

  @Test
  void removeComment_commentNotFound_throwsResourceNotFoundException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));
      when(commentRepository.findById(article.getId(), "nonexistent-comment"))
          .thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class,
          () -> commentMutation.removeComment("test-slug", "nonexistent-comment"));
    }
  }

  @Test
  void removeComment_withoutAuthentication_throwsAuthenticationException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      assertThrows(
          AuthenticationException.class,
          () -> commentMutation.removeComment("test-slug", "comment-id"));
    }
  }

  @Test
  void removeComment_withoutAuthorization_throwsNoAuthorizationException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      User anotherUser = new User("other@test.com", "otheruser", "password", "bio", "image");
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(anotherUser));

      when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));
      when(commentRepository.findById(article.getId(), comment.getId()))
          .thenReturn(Optional.of(comment));

      assertThrows(
          NoAuthorizationException.class,
          () -> commentMutation.removeComment("test-slug", comment.getId()));
    }
  }

  @Test
  void removeComment_articleAuthorCanDelete_returnsDeletionStatus() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      User commentAuthor = new User("commenter@test.com", "commenter", "password", "bio", "image");
      Comment otherComment = new Comment("other comment", commentAuthor.getId(), article.getId());

      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));
      when(commentRepository.findById(article.getId(), otherComment.getId()))
          .thenReturn(Optional.of(otherComment));

      DeletionStatus result = commentMutation.removeComment("test-slug", otherComment.getId());

      assertNotNull(result);
      assertTrue(result.getSuccess());
      verify(commentRepository).remove(otherComment);
    }
  }

  @Test
  void createComment_withEmptyBody_savesComment() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));
      when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

      DataFetcherResult<CommentPayload> result = commentMutation.createComment("test-slug", "");

      assertNotNull(result);
      verify(commentRepository).save(any(Comment.class));
    }
  }

  @Test
  void createComment_withLongBody_savesComment() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      String longBody = "a".repeat(1000);
      when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));
      when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

      DataFetcherResult<CommentPayload> result =
          commentMutation.createComment("test-slug", longBody);

      assertNotNull(result);
      verify(commentRepository).save(any(Comment.class));
    }
  }
}
