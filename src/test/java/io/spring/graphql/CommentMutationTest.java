package io.spring.graphql;

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
import java.util.Collections;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class CommentMutationTest {

  @Mock private ArticleRepository articleRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentQueryService commentQueryService;

  @InjectMocks private CommentMutation commentMutation;

  private User user;
  private User otherUser;
  private Article article;

  @BeforeEach
  void setUp() {
    user = new User("john@jacob.com", "johnjacob", "123", "", "default-avatar");
    otherUser = new User("other@user.com", "otheruser", "456", "", "");
    article = new Article("Test Title", "desc", "body", Arrays.asList("java"), user.getId());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymous",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  private void logIn(User loginUser) {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(loginUser, null));
  }

  // ── createComment ──

  @Test
  void createComment_success() {
    logIn(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    Comment comment = new Comment("Great article!", user.getId(), article.getId());
    DateTime now = new DateTime();
    CommentData commentData =
        new CommentData(
            comment.getId(),
            comment.getBody(),
            comment.getArticleId(),
            now,
            now,
            new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result =
        commentMutation.createComment(article.getSlug(), "Great article!");

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(commentData, result.getLocalContext());
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  void createComment_unauthenticated() {
    setAnonymous();
    assertThrows(
        AuthenticationException.class,
        () -> commentMutation.createComment(article.getSlug(), "body"));
  }

  @Test
  void createComment_articleNotFound() {
    logIn(user);
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.createComment("missing", "body"));
  }

  @Test
  void createComment_commentDataNotFound() {
    logIn(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.createComment(article.getSlug(), "body"));
  }

  // ── deleteComment ──

  @Test
  void deleteComment_successByArticleAuthor() {
    logIn(user);
    Comment comment = new Comment("body", otherUser.getId(), article.getId());

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    DeletionStatus result =
        commentMutation.removeComment(article.getSlug(), comment.getId());

    assertTrue(result.getSuccess());
    verify(commentRepository).remove(eq(comment));
  }

  @Test
  void deleteComment_successByCommentAuthor() {
    logIn(otherUser);
    Article otherArticle =
        new Article("Other Title", "desc", "body", Arrays.asList(), user.getId());
    Comment comment = new Comment("body", otherUser.getId(), otherArticle.getId());

    when(articleRepository.findBySlug(eq(otherArticle.getSlug())))
        .thenReturn(Optional.of(otherArticle));
    when(commentRepository.findById(eq(otherArticle.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    DeletionStatus result =
        commentMutation.removeComment(otherArticle.getSlug(), comment.getId());

    assertTrue(result.getSuccess());
    verify(commentRepository).remove(eq(comment));
  }

  @Test
  void deleteComment_unauthenticated() {
    setAnonymous();
    assertThrows(
        AuthenticationException.class,
        () -> commentMutation.removeComment(article.getSlug(), "comment-id"));
  }

  @Test
  void deleteComment_articleNotFound() {
    logIn(user);
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.removeComment("missing", "comment-id"));
  }

  @Test
  void deleteComment_commentNotFound() {
    logIn(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("no-such-id")))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.removeComment(article.getSlug(), "no-such-id"));
  }

  @Test
  void deleteComment_notAuthorized() {
    User thirdUser = new User("third@user.com", "thirduser", "789", "", "");
    logIn(thirdUser);

    Article someoneElseArticle =
        new Article("Title", "desc", "body", Arrays.asList(), otherUser.getId());
    Comment comment = new Comment("body", user.getId(), someoneElseArticle.getId());

    when(articleRepository.findBySlug(eq(someoneElseArticle.getSlug())))
        .thenReturn(Optional.of(someoneElseArticle));
    when(commentRepository.findById(eq(someoneElseArticle.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    assertThrows(
        NoAuthorizationException.class,
        () -> commentMutation.removeComment(someoneElseArticle.getSlug(), comment.getId()));
  }

  private static void assertEquals(Object expected, Object actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
  }
}
