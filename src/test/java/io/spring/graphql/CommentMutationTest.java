package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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
    SecurityContextHolder.clearContext();
    user = new User("test@example.com", "testuser", "password", "bio", "image");
    article =
        new Article(
            "Test Title",
            "Test Description",
            "Test Body",
            Arrays.asList("tag1", "tag2"),
            user.getId(),
            new DateTime());
    comment = new Comment("Test comment body", user.getId(), article.getId());
    ProfileData profileData =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    commentData =
        new CommentData(
            comment.getId(),
            "Test comment body",
            article.getId(),
            new DateTime(),
            new DateTime(),
            profileData);
  }

  @Test
  public void should_create_comment_successfully() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    String slug = "test-title";
    String body = "Test comment body";

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result = commentMutation.createComment(slug, body);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(commentData, result.getLocalContext());
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  public void should_fail_to_create_comment_without_authentication() {
    String slug = "test-title";
    String body = "Test comment body";

    assertThrows(
        NullPointerException.class, () -> commentMutation.createComment(slug, body));
  }

  @Test
  public void should_fail_to_create_comment_article_not_found() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    String slug = "non-existent-slug";
    String body = "Test comment body";

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.createComment(slug, body));
  }

  @Test
  public void should_fail_to_create_comment_when_comment_data_not_found() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    String slug = "test-title";
    String body = "Test comment body";

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.createComment(slug, body));
  }

  @Test
  public void should_delete_comment_successfully() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    String slug = "test-title";
    String commentId = comment.getId();

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(commentId)))
        .thenReturn(Optional.of(comment));

    DeletionStatus result = commentMutation.removeComment(slug, commentId);

    assertNotNull(result);
    assertTrue(result.getSuccess());
    verify(commentRepository).remove(eq(comment));
  }

  @Test
  public void should_fail_to_delete_comment_without_authentication() {
    String slug = "test-title";
    String commentId = comment.getId();

    assertThrows(
        NullPointerException.class, () -> commentMutation.removeComment(slug, commentId));
  }

  @Test
  public void should_fail_to_delete_comment_article_not_found() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    String slug = "non-existent-slug";
    String commentId = comment.getId();

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.removeComment(slug, commentId));
  }

  @Test
  public void should_fail_to_delete_comment_not_found() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    String slug = "test-title";
    String commentId = "non-existent-comment-id";

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(commentId)))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.removeComment(slug, commentId));
  }

  @Test
  public void should_fail_to_delete_comment_without_authorization() {
    User otherUser =
        new User("other@example.com", "otheruser", "password", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(otherUser, null));

    String slug = "test-title";
    String commentId = comment.getId();

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(commentId)))
        .thenReturn(Optional.of(comment));

    assertThrows(
        NoAuthorizationException.class, () -> commentMutation.removeComment(slug, commentId));
  }
}
