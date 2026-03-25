package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
import io.spring.graphql.types.CommentPayload;
import io.spring.graphql.types.DeletionStatus;
import java.util.Collections;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class CommentMutationTest {

  private ArticleRepository articleRepository;
  private CommentRepository commentRepository;
  private CommentQueryService commentQueryService;
  private CommentMutation commentMutation;
  private User user;

  @BeforeEach
  void setUp() {
    articleRepository = mock(ArticleRepository.class);
    commentRepository = mock(CommentRepository.class);
    commentQueryService = mock(CommentQueryService.class);
    commentMutation = new CommentMutation(articleRepository, commentRepository, commentQueryService);
    user = new User("a@b.com", "testuser", "pass", "", "");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_comment() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "other");
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    ProfileData profile = new ProfileData(user.getId(), "testuser", "", "", false);
    DateTime now = new DateTime();
    CommentData commentData = new CommentData("c1", "body", article.getId(), now, now, profile);
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result = commentMutation.createComment("title", "body");
    assertNotNull(result);
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  public void should_throw_when_article_not_found_for_comment() {
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> commentMutation.createComment("missing", "body"));
  }

  @Test
  public void should_delete_comment() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(commentRepository.findById(article.getId(), comment.getId())).thenReturn(Optional.of(comment));

    DeletionStatus result = commentMutation.removeComment("title", comment.getId());
    assertTrue(result.getSuccess());
    verify(commentRepository).remove(comment);
  }

  @Test
  public void should_throw_when_delete_comment_not_authorized() {
    User other = new User("c@d.com", "other", "pass", "", "");
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), other.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    Comment comment = new Comment("body", other.getId(), article.getId());
    when(commentRepository.findById(article.getId(), comment.getId())).thenReturn(Optional.of(comment));

    assertThrows(NoAuthorizationException.class, () -> commentMutation.removeComment("title", comment.getId()));
  }

  @Test
  public void should_throw_when_comment_not_found_for_delete() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), "missing")).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> commentMutation.removeComment("title", "missing"));
  }
}
