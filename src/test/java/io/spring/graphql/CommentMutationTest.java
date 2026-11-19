package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.CommentQueryService;
import io.spring.application.data.CommentData;
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
  private User articleAuthor;
  private User commenter;
  private User stranger;
  private Article article;

  @BeforeEach
  public void setUp() {
    articleRepository = mock(ArticleRepository.class);
    commentRepository = mock(CommentRepository.class);
    commentQueryService = mock(CommentQueryService.class);
    commentMutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);
    articleAuthor = new User("author@example.com", "author", "pw", "", "");
    commenter = new User("commenter@example.com", "commenter", "pw", "", "");
    stranger = new User("stranger@example.com", "stranger", "pw", "", "");
    article = new Article("Title", "desc", "body", Collections.emptyList(), articleAuthor.getId());
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  // ---- addComment ----

  @Test
  public void add_comment_should_save_and_return_comment_data_as_local_context() {
    authenticate(commenter);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    CommentData commentData =
        new CommentData("cid", "hello", article.getId(), new DateTime(), new DateTime(), null);
    when(commentQueryService.findById(anyString(), eq(commenter)))
        .thenReturn(Optional.of(commentData));
    ArgumentCaptor<Comment> saved = ArgumentCaptor.forClass(Comment.class);

    DataFetcherResult<CommentPayload> result =
        commentMutation.createComment(article.getSlug(), "hello");

    verify(commentRepository).save(saved.capture());
    assertEquals("hello", saved.getValue().getBody());
    assertEquals(commenter.getId(), saved.getValue().getUserId());
    assertEquals(article.getId(), saved.getValue().getArticleId());
    verify(commentQueryService).findById(saved.getValue().getId(), commenter);
    assertNotNull(result.getData());
    assertSame(commentData, result.getLocalContext());
  }

  @Test
  public void add_comment_should_throw_not_found_for_unknown_article() {
    authenticate(commenter);
    when(articleRepository.findBySlug("nope")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.createComment("nope", "hello"));
    verifyNoInteractions(commentRepository, commentQueryService);
  }

  @Test
  public void add_comment_should_throw_not_found_when_saved_comment_cannot_be_read_back() {
    authenticate(commenter);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentQueryService.findById(anyString(), eq(commenter))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.createComment(article.getSlug(), "hello"));
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  public void add_comment_should_require_authentication() {
    anonymous();

    assertThrows(
        AuthenticationException.class,
        () -> commentMutation.createComment(article.getSlug(), "hello"));
    verifyNoInteractions(articleRepository, commentRepository, commentQueryService);
  }

  // ---- deleteComment ----

  @Test
  public void delete_comment_should_allow_comment_author() {
    authenticate(commenter);
    Comment comment = new Comment("hi", commenter.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment(article.getSlug(), comment.getId());

    verify(commentRepository).remove(comment);
    assertTrue(status.getSuccess());
  }

  @Test
  public void delete_comment_should_allow_article_author() {
    authenticate(articleAuthor);
    Comment comment = new Comment("hi", commenter.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment(article.getSlug(), comment.getId());

    verify(commentRepository).remove(comment);
    assertTrue(status.getSuccess());
  }

  @Test
  public void delete_comment_should_reject_user_who_is_neither_comment_nor_article_author() {
    authenticate(stranger);
    Comment comment = new Comment("hi", commenter.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    assertThrows(
        NoAuthorizationException.class,
        () -> commentMutation.removeComment(article.getSlug(), comment.getId()));
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void delete_comment_should_throw_not_found_for_unknown_comment() {
    authenticate(commenter);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), "missing")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.removeComment(article.getSlug(), "missing"));
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void delete_comment_should_throw_not_found_for_unknown_article() {
    authenticate(commenter);
    when(articleRepository.findBySlug("nope")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.removeComment("nope", "cid"));
    verifyNoInteractions(commentRepository);
  }

  @Test
  public void delete_comment_should_require_authentication() {
    anonymous();

    assertThrows(
        AuthenticationException.class,
        () -> commentMutation.removeComment(article.getSlug(), "cid"));
    verifyNoInteractions(articleRepository, commentRepository);
  }
}
