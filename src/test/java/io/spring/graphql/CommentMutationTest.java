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
  private Article article;

  @BeforeEach
  void setUp() {
    commentMutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);
    user = new User("user@example.com", "testuser", "password", "bio", "image");
    article = new Article("Test Title", "desc", "body", Arrays.asList("tag1"), user.getId());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createComment_shouldCreateAndReturnComment() {
    setUpSecurityContext(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    ProfileData profileData = new ProfileData(user.getId(), "testuser", "bio", "image", false);
    CommentData commentData =
        new CommentData(
            "comment-id",
            "comment body",
            article.getId(),
            new DateTime(),
            new DateTime(),
            profileData);
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result =
        commentMutation.createComment(article.getSlug(), "comment body");

    assertNotNull(result);
    assertNotNull(result.getData());
    assertNotNull(result.getLocalContext());
    assertEquals(commentData, result.getLocalContext());

    verify(commentRepository).save(any(Comment.class));
    verify(commentQueryService).findById(any(), eq(user));
  }

  @Test
  void createComment_withoutAuthentication_shouldThrowAuthenticationException() {
    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new org.springframework.security.authentication.AnonymousAuthenticationToken(
                "key",
                "anonymous",
                java.util.Collections.singletonList(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_ANONYMOUS"))));

    assertThrows(
        AuthenticationException.class,
        () -> commentMutation.createComment("some-slug", "comment body"));
  }

  @Test
  void createComment_withNonExistentArticle_shouldThrowResourceNotFoundException() {
    setUpSecurityContext(user);
    when(articleRepository.findBySlug(eq("non-existent-slug"))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.createComment("non-existent-slug", "comment body"));
  }

  @Test
  void createComment_whenCommentDataNotFound_shouldThrowResourceNotFoundException() {
    setUpSecurityContext(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.createComment(article.getSlug(), "comment body"));

    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  void removeComment_shouldDeleteAndReturnSuccess() {
    setUpSecurityContext(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    Comment comment = new Comment("comment body", user.getId(), article.getId());
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    DeletionStatus result = commentMutation.removeComment(article.getSlug(), comment.getId());

    assertNotNull(result);
    assertTrue(result.getSuccess());
    verify(commentRepository).remove(comment);
  }

  @Test
  void removeComment_withoutAuthentication_shouldThrowAuthenticationException() {
    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new org.springframework.security.authentication.AnonymousAuthenticationToken(
                "key",
                "anonymous",
                java.util.Collections.singletonList(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_ANONYMOUS"))));

    assertThrows(
        AuthenticationException.class, () -> commentMutation.removeComment("some-slug", "some-id"));
  }

  @Test
  void removeComment_withNonExistentArticle_shouldThrowResourceNotFoundException() {
    setUpSecurityContext(user);
    when(articleRepository.findBySlug(eq("non-existent-slug"))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.removeComment("non-existent-slug", "some-id"));
  }

  @Test
  void removeComment_withNonExistentComment_shouldThrowResourceNotFoundException() {
    setUpSecurityContext(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("non-existent-id")))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.removeComment(article.getSlug(), "non-existent-id"));
  }

  @Test
  void removeComment_withUnauthorizedUser_shouldThrowNoAuthorizationException() {
    User otherUser = new User("other@example.com", "otheruser", "password", "bio", "image");
    setUpSecurityContext(otherUser);

    User articleAuthor = new User("author@example.com", "author", "password", "bio", "image");
    Article otherArticle =
        new Article("Other Title", "desc", "body", Arrays.asList("tag1"), articleAuthor.getId());

    when(articleRepository.findBySlug(eq(otherArticle.getSlug())))
        .thenReturn(Optional.of(otherArticle));

    Comment comment = new Comment("comment body", articleAuthor.getId(), otherArticle.getId());
    when(commentRepository.findById(eq(otherArticle.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    assertThrows(
        NoAuthorizationException.class,
        () -> commentMutation.removeComment(otherArticle.getSlug(), comment.getId()));

    verify(commentRepository, never()).remove(any());
  }

  @Test
  void removeComment_byArticleAuthor_shouldSucceed() {
    User articleAuthor = new User("author@example.com", "author", "password", "bio", "image");
    setUpSecurityContext(articleAuthor);

    Article authorArticle =
        new Article("Title", "desc", "body", Arrays.asList("tag1"), articleAuthor.getId());

    User commentAuthor = new User("commenter@example.com", "commenter", "password", "bio", "image");
    Comment comment = new Comment("comment body", commentAuthor.getId(), authorArticle.getId());

    when(articleRepository.findBySlug(eq(authorArticle.getSlug())))
        .thenReturn(Optional.of(authorArticle));
    when(commentRepository.findById(eq(authorArticle.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    DeletionStatus result = commentMutation.removeComment(authorArticle.getSlug(), comment.getId());

    assertNotNull(result);
    assertTrue(result.getSuccess());
    verify(commentRepository).remove(comment);
  }

  @Test
  void removeComment_byCommentAuthor_shouldSucceed() {
    User commentAuthor = new User("commenter@example.com", "commenter", "password", "bio", "image");
    setUpSecurityContext(commentAuthor);

    User articleAuthor = new User("author@example.com", "author", "password", "bio", "image");
    Article otherArticle =
        new Article("Title", "desc", "body", Arrays.asList("tag1"), articleAuthor.getId());

    Comment comment = new Comment("comment body", commentAuthor.getId(), otherArticle.getId());

    when(articleRepository.findBySlug(eq(otherArticle.getSlug())))
        .thenReturn(Optional.of(otherArticle));
    when(commentRepository.findById(eq(otherArticle.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    DeletionStatus result = commentMutation.removeComment(otherArticle.getSlug(), comment.getId());

    assertNotNull(result);
    assertTrue(result.getSuccess());
    verify(commentRepository).remove(comment);
  }

  private void setUpSecurityContext(User user) {
    TestingAuthenticationToken authentication = new TestingAuthenticationToken(user, null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
