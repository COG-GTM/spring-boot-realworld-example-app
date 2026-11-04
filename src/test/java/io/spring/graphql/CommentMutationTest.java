package io.spring.graphql;

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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommentMutationTest {
  @Mock private ArticleRepository articleRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentQueryService commentQueryService;

  private CommentMutation commentMutation;
  private User author;
  private User commenter;
  private User stranger;
  private Article article;

  @BeforeEach
  public void setUp() {
    commentMutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);
    author = new User("author@test.com", "author", "123", "", "");
    commenter = new User("commenter@test.com", "commenter", "123", "", "");
    stranger = new User("stranger@test.com", "stranger", "123", "", "");
    article = new Article("title", "desc", "body", Arrays.asList("java"), author.getId());
  }

  @AfterEach
  public void tearDown() {
    GraphQLTestSupport.clear();
  }

  @Test
  public void should_create_comment() {
    GraphQLTestSupport.authenticate(commenter);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    DateTime now = new DateTime();
    CommentData commentData =
        new CommentData(
            "comment-id",
            "content",
            article.getId(),
            now,
            now,
            new ProfileData(commenter.getId(), "commenter", "", "", false));
    when(commentQueryService.findById(anyString(), eq(commenter)))
        .thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result =
        commentMutation.createComment(article.getSlug(), "content");

    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).save(captor.capture());
    Comment saved = captor.getValue();
    Assertions.assertEquals("content", saved.getBody());
    Assertions.assertEquals(commenter.getId(), saved.getUserId());
    Assertions.assertEquals(article.getId(), saved.getArticleId());
    verify(commentQueryService).findById(saved.getId(), commenter);
    Assertions.assertNotNull(result.getData());
    Assertions.assertSame(commentData, result.getLocalContext());
  }

  @Test
  public void should_reject_anonymous_comment() {
    GraphQLTestSupport.anonymous();

    Assertions.assertThrows(
        AuthenticationException.class,
        () -> commentMutation.createComment(article.getSlug(), "content"));
    verifyNoInteractions(commentRepository);
  }

  @Test
  public void should_fail_comment_on_unknown_article() {
    GraphQLTestSupport.authenticate(commenter);
    when(articleRepository.findBySlug("unknown")).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.createComment("unknown", "c"));
    verifyNoInteractions(commentRepository);
  }

  @Test
  public void should_allow_comment_author_to_delete_comment() {
    GraphQLTestSupport.authenticate(commenter);
    Comment comment = new Comment("content", commenter.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment(article.getSlug(), comment.getId());

    verify(commentRepository).remove(comment);
    Assertions.assertTrue(status.getSuccess());
  }

  @Test
  public void should_allow_article_author_to_delete_comment() {
    GraphQLTestSupport.authenticate(author);
    Comment comment = new Comment("content", commenter.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    Assertions.assertTrue(
        commentMutation.removeComment(article.getSlug(), comment.getId()).getSuccess());
    verify(commentRepository).remove(comment);
  }

  @Test
  public void should_reject_delete_by_other_user() {
    GraphQLTestSupport.authenticate(stranger);
    Comment comment = new Comment("content", commenter.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    Assertions.assertThrows(
        NoAuthorizationException.class,
        () -> commentMutation.removeComment(article.getSlug(), comment.getId()));
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void should_fail_delete_for_unknown_comment() {
    GraphQLTestSupport.authenticate(commenter);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), "unknown")).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.removeComment(article.getSlug(), "unknown"));
  }
}
