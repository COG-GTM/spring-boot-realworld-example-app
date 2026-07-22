package io.spring.graphql;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CommentMutationTest extends GraphQLTestBase {

  @Mock private ArticleRepository articleRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentQueryService commentQueryService;

  private CommentMutation commentMutation;
  private User user;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    commentMutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);
    user = new User("email@test.com", "username", "pass", "", "");
  }

  private Article articleOf(User owner) {
    return new Article("title", "desc", "body", Arrays.asList("java"), owner.getId());
  }

  private CommentData commentData(String id) {
    return new CommentData(
        id, "body", "article-id", null, null, new ProfileData("pid", "author", "", "", false));
  }

  @Test
  public void should_create_comment() {
    setCurrentUser(user);
    Article article = articleOf(user);
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(String.class), eq(user)))
        .thenReturn(Optional.of(commentData("cid")));

    DataFetcherResult<CommentPayload> result = commentMutation.createComment("title", "a comment");

    assertThat(result.getData(), instanceOf(CommentPayload.class));
    assertThat(result.getLocalContext(), is(commentData("cid")));
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  public void should_throw_when_create_comment_anonymous() {
    setAnonymous();
    assertThrows(
        AuthenticationException.class, () -> commentMutation.createComment("title", "a comment"));
  }

  @Test
  public void should_throw_when_create_comment_article_missing() {
    setCurrentUser(user);
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.createComment("missing", "a comment"));
  }

  @Test
  public void should_remove_comment_when_authorized() {
    setCurrentUser(user);
    Article article = articleOf(user);
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment("title", comment.getId());

    assertThat(status.getSuccess(), is(true));
    verify(commentRepository).remove(eq(comment));
  }

  @Test
  public void should_throw_when_remove_comment_not_authorized() {
    User author = new User("author@test.com", "author", "pass", "", "");
    User commenter = new User("commenter@test.com", "commenter", "pass", "", "");
    setCurrentUser(user);
    Article article = articleOf(author);
    Comment comment = new Comment("body", commenter.getId(), article.getId());
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    assertThrows(
        NoAuthorizationException.class,
        () -> commentMutation.removeComment("title", comment.getId()));
  }

  @Test
  public void should_throw_when_remove_comment_missing() {
    setCurrentUser(user);
    Article article = articleOf(user);
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("missing")))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> commentMutation.removeComment("title", "missing"));
  }

  @Test
  public void should_throw_when_remove_comment_anonymous() {
    setAnonymous();
    assertThrows(
        AuthenticationException.class, () -> commentMutation.removeComment("title", "cid"));
  }
}
