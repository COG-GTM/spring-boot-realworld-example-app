package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommentMutationTest {

  @Mock private ArticleRepository articleRepository;

  @Mock private CommentRepository commentRepository;

  @Mock private CommentQueryService commentQueryService;

  @InjectMocks private CommentMutation commentMutation;

  private User testUser;
  private Article testArticle;
  private Comment testComment;
  private CommentData commentData;

  @BeforeEach
  public void setUp() {
    testUser = new User("test@example.com", "testuser", "password", "bio", "image");
    testArticle =
        new Article(
            "Test Title",
            "Test Description",
            "Test Body",
            Arrays.asList("tag1"),
            testUser.getId());
    testComment = new Comment("Test comment body", testUser.getId(), testArticle.getId());
    ProfileData profileData =
        new ProfileData(testUser.getId(), testUser.getUsername(), testUser.getBio(), testUser.getImage(), false);
    commentData =
        new CommentData(
            testComment.getId(),
            "Test comment body",
            testArticle.getId(),
            new DateTime(),
            new DateTime(),
            profileData);
  }

  @Test
  public void should_add_comment_successfully() {
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(testArticle));
    when(commentQueryService.findById(any(), any())).thenReturn(Optional.of(commentData));

    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      DataFetcherResult<CommentPayload> result =
          commentMutation.createComment("test-slug", "Test comment body");

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(commentData, result.getLocalContext());
      verify(commentRepository).save(any());
    }
  }

  @Test
  public void should_fail_add_comment_without_authentication() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      assertThrows(
          AuthenticationException.class,
          () -> commentMutation.createComment("test-slug", "Test comment body"));
    }
  }

  @Test
  public void should_fail_add_comment_to_non_existent_article() {
    when(articleRepository.findBySlug(eq("non-existent"))).thenReturn(Optional.empty());

    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      assertThrows(
          ResourceNotFoundException.class,
          () -> commentMutation.createComment("non-existent", "Test comment body"));
    }
  }

  @Test
  public void should_delete_comment_successfully() {
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(testArticle));
    when(commentRepository.findById(any(), any())).thenReturn(Optional.of(testComment));

    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      DeletionStatus result = commentMutation.removeComment("test-slug", "comment-id");

      assertNotNull(result);
      assertTrue(result.getSuccess());
      verify(commentRepository).remove(any());
    }
  }

  @Test
  public void should_fail_delete_non_existent_comment() {
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(testArticle));
    when(commentRepository.findById(any(), any())).thenReturn(Optional.empty());

    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      assertThrows(
          ResourceNotFoundException.class,
          () -> commentMutation.removeComment("test-slug", "non-existent"));
    }
  }

  @Test
  public void should_fail_delete_comment_without_authentication() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      assertThrows(
          AuthenticationException.class,
          () -> commentMutation.removeComment("test-slug", "comment-id"));
    }
  }
}
