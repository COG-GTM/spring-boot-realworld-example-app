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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class CommentMutationTest {

  private ArticleRepository articleRepository;
  private CommentRepository commentRepository;
  private CommentQueryService commentQueryService;
  private CommentMutation commentMutation;
  private User user;

  @BeforeEach
  public void setUp() {
    articleRepository = mock(ArticleRepository.class);
    commentRepository = mock(CommentRepository.class);
    commentQueryService = mock(CommentQueryService.class);
    commentMutation = new CommentMutation(articleRepository, commentRepository, commentQueryService);

    user = new User("test@test.com", "testuser", "password", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(user, null));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_comment() {
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList(), user.getId());
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));

    ProfileData profileData =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    CommentData commentData =
        new CommentData(
            "comment-id", "Test comment body", article.getId(), new DateTime(), new DateTime(), profileData);
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result =
        commentMutation.createComment("title", "Test comment body");
    assertNotNull(result);
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  public void should_throw_when_not_authenticated_for_create() {
    SecurityContextHolder.clearContext();
    assertThrows(
        Exception.class,
        () -> commentMutation.createComment("title", "body"));
  }

  @Test
  public void should_throw_when_article_not_found_for_create() {
    when(articleRepository.findBySlug(eq("not-found"))).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.createComment("not-found", "body"));
  }

  @Test
  public void should_delete_comment() {
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList(), user.getId());
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));

    Comment comment = new Comment("body", user.getId(), article.getId());
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    DeletionStatus result = commentMutation.removeComment("title", comment.getId());
    assertTrue(result.getSuccess());
    verify(commentRepository).remove(comment);
  }

  @Test
  public void should_throw_when_not_authorized_to_delete() {
    User otherUser = new User("other@test.com", "other", "pass", "", "");
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList(), otherUser.getId());
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));

    Comment comment = new Comment("body", otherUser.getId(), article.getId());
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    assertThrows(
        NoAuthorizationException.class,
        () -> commentMutation.removeComment("title", comment.getId()));
  }

  @Test
  public void should_throw_when_comment_not_found_for_delete() {
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList(), user.getId());
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("no-such-comment")))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.removeComment("title", "no-such-comment"));
  }
}
