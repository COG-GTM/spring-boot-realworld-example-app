package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class CommentMutationTest {

  @Mock
  private ArticleRepository articleRepository;

  @Mock
  private CommentRepository commentRepository;

  @Mock
  private CommentQueryService commentQueryService;

  private CommentMutation commentMutation;

  private User user;
  private Article article;
  private Comment comment;
  private CommentData commentData;
  private ProfileData profileData;

  @BeforeEach
  void setUp() {
    commentMutation = new CommentMutation(articleRepository, commentRepository, commentQueryService);
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    
    DateTime now = new DateTime();
    article = new Article("Test Article", "Test Description", "Test Body", Arrays.asList("java"), user.getId(), now);
    comment = new Comment("This is a test comment", user.getId(), article.getId());
    
    commentData = new CommentData(
        comment.getId(),
        comment.getBody(),
        article.getId(),
        now,
        now,
        profileData
    );
    
    setAnonymousAuthentication();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAnonymousAuthentication() {
    AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
        "anonymous", "anonymousUser", 
        java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);
  }

  @Test
  void shouldAddCommentWhenAuthenticated() {
    setAuthenticatedUser(user);
    
    when(articleRepository.findBySlug(eq(article.getSlug())))
        .thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user)))
        .thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result = commentMutation.createComment(
        article.getSlug(), "This is a test comment");
    
    assertThat(result.getData()).isNotNull();
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  void shouldFailToAddCommentWhenNotAuthenticated() {
    assertThatThrownBy(() -> commentMutation.createComment("test-article", "Test comment"))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  void shouldFailToAddCommentWhenArticleNotFound() {
    setAuthenticatedUser(user);
    
    when(articleRepository.findBySlug(eq("non-existent")))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> commentMutation.createComment("non-existent", "Test comment"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void shouldDeleteCommentWhenAuthorized() {
    setAuthenticatedUser(user);
    
    when(articleRepository.findBySlug(eq(article.getSlug())))
        .thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    DeletionStatus result = commentMutation.removeComment(article.getSlug(), comment.getId());
    
    assertThat(result.getSuccess()).isTrue();
    verify(commentRepository).remove(eq(comment));
  }

  @Test
  void shouldFailToDeleteCommentWhenNotAuthenticated() {
    assertThatThrownBy(() -> commentMutation.removeComment("test-article", "comment-id"))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  void shouldFailToDeleteCommentWhenArticleNotFound() {
    setAuthenticatedUser(user);
    
    when(articleRepository.findBySlug(eq("non-existent")))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> commentMutation.removeComment("non-existent", "comment-id"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void shouldFailToDeleteCommentWhenCommentNotFound() {
    setAuthenticatedUser(user);
    
    when(articleRepository.findBySlug(eq(article.getSlug())))
        .thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("non-existent")))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> commentMutation.removeComment(article.getSlug(), "non-existent"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void shouldFailToDeleteCommentWhenNotAuthor() {
    User anotherUser = new User("another@test.com", "anotheruser", "password", "bio", "image");
    setAuthenticatedUser(anotherUser);
    
    when(articleRepository.findBySlug(eq(article.getSlug())))
        .thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    assertThatThrownBy(() -> commentMutation.removeComment(article.getSlug(), comment.getId()))
        .isInstanceOf(NoAuthorizationException.class);
  }

  private void setAuthenticatedUser(User user) {
    UsernamePasswordAuthenticationToken authentication = 
        new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
