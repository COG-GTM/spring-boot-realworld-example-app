package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class CommentMutationTest {
  @Mock private ArticleRepository articleRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentQueryService commentQueryService;

  private CommentMutation commentMutation;
  private User author;
  private User commenter;
  private Article article;

  @BeforeEach
  public void setUp() {
    commentMutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);
    author = new User("author@example.com", "author", "123", "", "");
    commenter = new User("commenter@example.com", "commenter", "123", "", "");
    article = new Article("title", "desc", "body", Arrays.asList("java"), author.getId());
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_add_comment_for_authenticated_user() {
    authenticate(commenter);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    CommentData commentData = new CommentData();
    when(commentQueryService.findById(anyString(), eq(commenter)))
        .thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result =
        commentMutation.createComment(article.getSlug(), "nice article");

    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).save(captor.capture());
    Comment saved = captor.getValue();
    assertEquals("nice article", saved.getBody());
    assertEquals(commenter.getId(), saved.getUserId());
    assertEquals(article.getId(), saved.getArticleId());
    verify(commentQueryService).findById(saved.getId(), commenter);
    assertSame(commentData, result.getLocalContext());
  }

  @Test
  public void should_reject_add_comment_for_anonymous_user() {
    anonymous();

    assertThrows(
        AuthenticationException.class,
        () -> commentMutation.createComment(article.getSlug(), "body"));
    verifyNoInteractions(commentRepository);
  }

  @Test
  public void should_throw_not_found_when_adding_comment_to_missing_article() {
    authenticate(commenter);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.createComment("missing", "body"));
    verifyNoInteractions(commentRepository);
  }

  @Test
  public void comment_owner_can_delete_comment() {
    authenticate(commenter);
    Comment comment = new Comment("body", commenter.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment(article.getSlug(), comment.getId());

    assertTrue(status.getSuccess());
    verify(commentRepository).remove(comment);
  }

  @Test
  public void article_author_can_delete_others_comment() {
    authenticate(author);
    Comment comment = new Comment("body", commenter.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment(article.getSlug(), comment.getId());

    assertTrue(status.getSuccess());
    verify(commentRepository).remove(comment);
  }

  @Test
  public void other_user_cannot_delete_comment() {
    User stranger = new User("stranger@example.com", "stranger", "123", "", "");
    authenticate(stranger);
    Comment comment = new Comment("body", commenter.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    assertThrows(
        NoAuthorizationException.class,
        () -> commentMutation.removeComment(article.getSlug(), comment.getId()));
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void should_throw_not_found_when_deleting_missing_comment() {
    authenticate(author);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), "missing")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.removeComment(article.getSlug(), "missing"));
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void should_reject_delete_comment_for_anonymous_user() {
    anonymous();

    assertThrows(
        AuthenticationException.class,
        () -> commentMutation.removeComment(article.getSlug(), "id"));
    verifyNoInteractions(articleRepository, commentRepository);
  }

  private static void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  private static void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }
}
