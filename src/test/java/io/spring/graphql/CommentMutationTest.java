package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
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

  @BeforeEach
  public void setUp() {
    testUser = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    testArticle = new Article("Test Title", "Test Description", "Test Body", Arrays.asList("tag1"), testUser.getId());
    testComment = new Comment("Test comment body", testUser.getId(), testArticle.getId());
  }

  @Test
  public void should_create_comment_successfully() {
    String slug = "test-title";
    String body = "This is a test comment";
    
    CommentData commentData = new CommentData(
        "comment-id",
        body,
        new DateTime(),
        new DateTime(),
        new ProfileData(testUser.getId(), testUser.getUsername(), testUser.getBio(), testUser.getImage(), false)
    );

    when(articleRepository.findBySlug(slug)).thenReturn(Optional.of(testArticle));
    when(commentQueryService.findById(any(), eq(testUser))).thenReturn(Optional.of(commentData));

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      DataFetcherResult<CommentPayload> result = commentMutation.createComment(slug, body);

      Assertions.assertNotNull(result);
      Assertions.assertNotNull(result.getData());
      Assertions.assertEquals(commentData, result.getLocalContext());
      verify(commentRepository).save(any(Comment.class));
    }
  }

  @Test
  public void should_throw_exception_when_creating_comment_without_authentication() {
    String slug = "test-title";
    String body = "Test comment";

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      Assertions.assertThrows(AuthenticationException.class, () -> {
        commentMutation.createComment(slug, body);
      });
    }
  }

  @Test
  public void should_throw_exception_when_creating_comment_on_nonexistent_article() {
    String slug = "nonexistent-slug";
    String body = "Test comment";

    when(articleRepository.findBySlug(slug)).thenReturn(Optional.empty());

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      Assertions.assertThrows(ResourceNotFoundException.class, () -> {
        commentMutation.createComment(slug, body);
      });
    }
  }

  @Test
  public void should_delete_comment_successfully() {
    String slug = "test-title";
    String commentId = "comment-id";

    when(articleRepository.findBySlug(slug)).thenReturn(Optional.of(testArticle));
    when(commentRepository.findById(testArticle.getId(), commentId)).thenReturn(Optional.of(testComment));

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      DeletionStatus result = commentMutation.removeComment(slug, commentId);

      Assertions.assertNotNull(result);
      Assertions.assertTrue(result.getSuccess());
      verify(commentRepository).remove(testComment);
    }
  }

  @Test
  public void should_throw_exception_when_deleting_comment_without_authentication() {
    String slug = "test-title";
    String commentId = "comment-id";

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      Assertions.assertThrows(AuthenticationException.class, () -> {
        commentMutation.removeComment(slug, commentId);
      });
    }
  }

  @Test
  public void should_throw_exception_when_deleting_comment_on_nonexistent_article() {
    String slug = "nonexistent-slug";
    String commentId = "comment-id";

    when(articleRepository.findBySlug(slug)).thenReturn(Optional.empty());

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      Assertions.assertThrows(ResourceNotFoundException.class, () -> {
        commentMutation.removeComment(slug, commentId);
      });
    }
  }

  @Test
  public void should_throw_exception_when_deleting_nonexistent_comment() {
    String slug = "test-title";
    String commentId = "nonexistent-comment-id";

    when(articleRepository.findBySlug(slug)).thenReturn(Optional.of(testArticle));
    when(commentRepository.findById(testArticle.getId(), commentId)).thenReturn(Optional.empty());

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      Assertions.assertThrows(ResourceNotFoundException.class, () -> {
        commentMutation.removeComment(slug, commentId);
      });
    }
  }
}
