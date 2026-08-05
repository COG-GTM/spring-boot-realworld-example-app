package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class CommentMutationTest {

  private ArticleRepository articleRepository;
  private CommentRepository commentRepository;
  private CommentQueryService commentQueryService;
  private CommentMutation commentMutation;

  private User user;
  private Article article;

  @BeforeEach
  public void setUp() {
    articleRepository = mock(ArticleRepository.class);
    commentRepository = mock(CommentRepository.class);
    commentQueryService = mock(CommentQueryService.class);
    commentMutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);

    user = new User("john@example.com", "john", "123", "", "");
    article =
        new Article("title", "desc", "body", Arrays.asList("java"), user.getId(), new DateTime());
    login(user);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void login(User currentUser) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(currentUser, null, Collections.emptyList()));
  }

  private void loginAnonymously() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  private CommentData commentDataFixture(String id, String body) {
    DateTime now = new DateTime();
    return new CommentData(
        id,
        body,
        article.getId(),
        now,
        now,
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
  }

  @Test
  public void should_create_comment_success() {
    CommentData commentData = commentDataFixture("comment-id", "comment body");
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result =
        commentMutation.createComment(article.getSlug(), "comment body");

    Assertions.assertNotNull(result.getData());
    Assertions.assertEquals(commentData, result.getLocalContext());

    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).save(captor.capture());
    Comment saved = captor.getValue();
    Assertions.assertEquals("comment body", saved.getBody());
    Assertions.assertEquals(user.getId(), saved.getUserId());
    Assertions.assertEquals(article.getId(), saved.getArticleId());
    verify(commentQueryService).findById(saved.getId(), user);
  }

  @Test
  public void should_throw_authentication_exception_when_create_comment_without_login() {
    loginAnonymously();

    Assertions.assertThrows(
        AuthenticationException.class,
        () -> commentMutation.createComment(article.getSlug(), "comment body"));
    verify(articleRepository, never()).findBySlug(any());
    verify(commentRepository, never()).save(any());
  }

  @Test
  public void should_throw_not_found_exception_when_create_comment_for_missing_article() {
    when(articleRepository.findBySlug("not-exists")).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.createComment("not-exists", "comment body"));
    verify(commentRepository, never()).save(any());
  }

  @Test
  public void should_throw_not_found_exception_when_created_comment_can_not_be_read_back() {
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.createComment(article.getSlug(), "comment body"));
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  public void should_create_comment_with_empty_body_without_validation() {
    CommentData commentData = commentDataFixture("comment-id", "");
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result = commentMutation.createComment(article.getSlug(), "");

    Assertions.assertEquals(commentData, result.getLocalContext());
    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).save(captor.capture());
    Assertions.assertEquals("", captor.getValue().getBody());
  }

  @Test
  public void should_remove_own_comment_success() {
    Comment comment = new Comment("comment body", user.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment(article.getSlug(), comment.getId());

    Assertions.assertTrue(status.getSuccess());
    verify(commentRepository).remove(comment);
  }

  @Test
  public void should_remove_others_comment_on_own_article_success() {
    User otherUser = new User("other@example.com", "other", "123", "", "");
    Comment comment = new Comment("comment body", otherUser.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment(article.getSlug(), comment.getId());

    Assertions.assertTrue(status.getSuccess());
    verify(commentRepository).remove(comment);
  }

  @Test
  public void should_throw_no_authorization_exception_when_remove_others_comment() {
    User author = new User("author@example.com", "author", "123", "", "");
    Article othersArticle =
        new Article("other", "desc", "body", Arrays.asList("java"), author.getId(), new DateTime());
    Comment comment = new Comment("comment body", author.getId(), othersArticle.getId());
    when(articleRepository.findBySlug(othersArticle.getSlug()))
        .thenReturn(Optional.of(othersArticle));
    when(commentRepository.findById(othersArticle.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    Assertions.assertThrows(
        NoAuthorizationException.class,
        () -> commentMutation.removeComment(othersArticle.getSlug(), comment.getId()));
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void should_throw_authentication_exception_when_remove_comment_without_login() {
    loginAnonymously();

    Assertions.assertThrows(
        AuthenticationException.class,
        () -> commentMutation.removeComment(article.getSlug(), "comment-id"));
    verify(articleRepository, never()).findBySlug(any());
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void should_throw_not_found_exception_when_remove_comment_of_missing_article() {
    when(articleRepository.findBySlug("not-exists")).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.removeComment("not-exists", "comment-id"));
    verify(commentRepository, never()).findById(any(), any());
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void should_throw_not_found_exception_when_remove_missing_comment() {
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), "not-exists")).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.removeComment(article.getSlug(), "not-exists"));
    verify(commentRepository, never()).remove(any());
  }
}
