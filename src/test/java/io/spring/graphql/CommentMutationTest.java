package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.CommentQueryService;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentMutationTest extends GraphQLTestBase {

  @Mock private ArticleRepository articleRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentQueryService commentQueryService;

  private CommentMutation commentMutation;
  private User user;

  @BeforeEach
  void setUp() {
    commentMutation = new CommentMutation(articleRepository, commentRepository, commentQueryService);
    user = newUser();
  }

  private Article ownedArticle() {
    return new Article("title", "desc", "body", Arrays.asList("t"), user.getId());
  }

  @Test
  void should_create_comment() {
    setCurrentUser(user);
    Article article = ownedArticle();
    when(articleRepository.findBySlug(eq("slug"))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user)))
        .thenReturn(Optional.of(commentData("c1", article.getId(), "johnjacob")));

    DataFetcherResult<CommentPayload> result = commentMutation.createComment("slug", "hi");

    assertThat(((io.spring.application.data.CommentData) result.getLocalContext()).getId())
        .isEqualTo("c1");
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  void should_throw_when_create_comment_unauthenticated() {
    setAnonymous();
    assertThatThrownBy(() -> commentMutation.createComment("slug", "hi"))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  void should_throw_when_create_comment_article_missing() {
    setCurrentUser(user);
    when(articleRepository.findBySlug(eq("slug"))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> commentMutation.createComment("slug", "hi"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_throw_when_created_comment_not_found_afterwards() {
    setCurrentUser(user);
    Article article = ownedArticle();
    when(articleRepository.findBySlug(eq("slug"))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> commentMutation.createComment("slug", "hi"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_remove_comment() {
    setCurrentUser(user);
    Article article = ownedArticle();
    Comment comment = new Comment("body", user.getId(), article.getId());
    when(articleRepository.findBySlug(eq("slug"))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment("slug", comment.getId());

    assertThat(status.getSuccess()).isTrue();
    verify(commentRepository).remove(comment);
  }

  @Test
  void should_throw_when_remove_comment_unauthenticated() {
    setAnonymous();
    assertThatThrownBy(() -> commentMutation.removeComment("slug", "c1"))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  void should_throw_when_remove_comment_article_missing() {
    setCurrentUser(user);
    when(articleRepository.findBySlug(eq("slug"))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> commentMutation.removeComment("slug", "c1"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_throw_when_remove_comment_missing() {
    setCurrentUser(user);
    Article article = ownedArticle();
    when(articleRepository.findBySlug(eq("slug"))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("c1"))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> commentMutation.removeComment("slug", "c1"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_throw_when_remove_comment_not_authorized() {
    setCurrentUser(user);
    Article article = new Article("title", "desc", "body", Arrays.asList("t"), "another-user");
    Comment comment = new Comment("body", "someone-else", article.getId());
    when(articleRepository.findBySlug(eq("slug"))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    assertThatThrownBy(() -> commentMutation.removeComment("slug", comment.getId()))
        .isInstanceOf(NoAuthorizationException.class);
    verify(commentRepository, never()).remove(any());
  }
}
