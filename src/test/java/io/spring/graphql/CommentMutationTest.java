package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.Collections;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommentMutationTest {

  @Mock private ArticleRepository articleRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentQueryService commentQueryService;

  @Captor private ArgumentCaptor<Comment> commentCaptor;

  @InjectMocks private CommentMutation commentMutation;

  private final User currentUser = new User("jake@jake.jake", "jake", "123", "bio", "image");

  @AfterEach
  void tearDown() {
    SecurityContextHelper.clear();
  }

  @Test
  public void should_create_comment_on_article() {
    SecurityContextHelper.authenticate(currentUser);
    Article article = ownedArticle();
    CommentData commentData = commentData();
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(currentUser))).thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result = commentMutation.createComment("a-title", "body");

    verify(commentRepository).save(commentCaptor.capture());
    assertSame(article.getId(), commentCaptor.getValue().getArticleId());
    assertSame(currentUser.getId(), commentCaptor.getValue().getUserId());
    assertSame(commentData, result.getLocalContext());
  }

  @Test
  public void should_reject_comment_creation_for_anonymous_user() {
    SecurityContextHelper.anonymous();

    assertThrows(
        AuthenticationException.class, () -> commentMutation.createComment("a-title", "body"));
  }

  @Test
  public void should_throw_when_commenting_unknown_article() {
    SecurityContextHelper.authenticate(currentUser);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.createComment("missing", "body"));
  }

  @Test
  public void should_throw_when_created_comment_cannot_be_read_back() {
    SecurityContextHelper.authenticate(currentUser);
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(ownedArticle()));
    when(commentQueryService.findById(any(), eq(currentUser))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.createComment("a-title", "body"));
  }

  @Test
  public void should_remove_own_comment() {
    SecurityContextHelper.authenticate(currentUser);
    Article article = ownedArticle();
    Comment comment = new Comment("body", currentUser.getId(), article.getId());
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment("a-title", comment.getId());

    verify(commentRepository).remove(comment);
    assertTrue(status.getSuccess());
  }

  @Test
  public void should_reject_removing_comment_without_authorization() {
    SecurityContextHelper.authenticate(currentUser);
    Article article = otherUsersArticle();
    Comment comment = new Comment("body", "another-user-id", article.getId());
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    assertThrows(
        NoAuthorizationException.class,
        () -> commentMutation.removeComment("a-title", comment.getId()));
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void should_throw_when_removing_unknown_comment() {
    SecurityContextHelper.authenticate(currentUser);
    Article article = ownedArticle();
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), "missing")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.removeComment("a-title", "missing"));
  }

  @Test
  public void should_throw_when_removing_comment_of_unknown_article() {
    SecurityContextHelper.authenticate(currentUser);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.removeComment("missing", "comment-id"));
  }

  @Test
  public void should_reject_comment_removal_for_anonymous_user() {
    SecurityContextHelper.anonymous();

    assertThrows(
        AuthenticationException.class,
        () -> commentMutation.removeComment("a-title", "comment-id"));
  }

  private CommentData commentData() {
    return new CommentData(
        "comment-id",
        "body",
        "article-id",
        new DateTime(),
        new DateTime(),
        new ProfileData("author-id", "jake", "bio", "image", false));
  }

  private Article ownedArticle() {
    return new Article(
        "a title", "desc", "body", Collections.singletonList("java"), currentUser.getId());
  }

  private Article otherUsersArticle() {
    return new Article("a title", "desc", "body", Collections.singletonList("java"), "other-id");
  }
}
