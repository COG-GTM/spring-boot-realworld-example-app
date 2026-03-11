package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.Optional;
import org.joda.time.DateTime;
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

  private User user;
  private Article article;
  private Comment comment;
  private CommentData commentData;

  @BeforeEach
  public void setUp() {
    user = new User("test@example.com", "testuser", "password", "bio", "image");
    article = new Article("test-title", "test-description", "test-body", java.util.Collections.emptyList(), user.getId(), new DateTime());
    comment = new Comment("test comment body", user.getId(), article.getId());
    ProfileData profileData = new ProfileData("id", "testuser", "bio", "image", false);
    commentData =
        new CommentData(
            comment.getId(),
            "test comment body",
            article.getId(),
            new DateTime(),
            new DateTime(),
            profileData);
  }

  @Test
  public void should_add_comment_successfully() {
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), any())).thenReturn(Optional.of(commentData));

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      DataFetcherResult<CommentPayload> result =
          commentMutation.createComment("test-slug", "test comment body");

      assertThat(result).isNotNull();
      assertThat(result.getData()).isNotNull();
      assertThat(result.getLocalContext()).isEqualTo(commentData);
      verify(commentRepository).save(any(Comment.class));
    }
  }

  @Test
  public void should_fail_add_comment_without_authentication() {
    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      assertThrows(
          AuthenticationException.class,
          () -> commentMutation.createComment("test-slug", "test comment body"));
    }
  }

  @Test
  public void should_fail_add_comment_with_nonexistent_article() {
    when(articleRepository.findBySlug(eq("nonexistent-slug"))).thenReturn(Optional.empty());

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      assertThrows(
          ResourceNotFoundException.class,
          () -> commentMutation.createComment("nonexistent-slug", "test comment body"));
    }
  }

  @Test
  public void should_delete_comment_successfully() {
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("comment-id")))
        .thenReturn(Optional.of(comment));

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      DeletionStatus result = commentMutation.removeComment("test-slug", "comment-id");

      assertThat(result).isNotNull();
      assertThat(result.getSuccess()).isTrue();
      verify(commentRepository).remove(any(Comment.class));
    }
  }

  @Test
  public void should_fail_delete_comment_with_nonexistent_comment() {
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("nonexistent-id")))
        .thenReturn(Optional.empty());

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      assertThrows(
          ResourceNotFoundException.class,
          () -> commentMutation.removeComment("test-slug", "nonexistent-id"));
    }
  }

  @Test
  public void should_fail_delete_comment_without_authorization() {
    User otherUser = new User("other@example.com", "otheruser", "password", "bio", "image");
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("comment-id")))
        .thenReturn(Optional.of(comment));

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(otherUser));

      assertThrows(
          NoAuthorizationException.class,
          () -> commentMutation.removeComment("test-slug", "comment-id"));
    }
  }
}
