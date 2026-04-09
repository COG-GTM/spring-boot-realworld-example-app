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
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class CommentMutationTest {

  @Mock private ArticleRepository articleRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentQueryService commentQueryService;

  private CommentMutation commentMutation;
  private User user;
  private Article article;

  @BeforeEach
  public void setUp() {
    commentMutation =
        new CommentMutation(articleRepository, commentRepository, commentQueryService);
    user = new User("john@jacob.com", "johnjacob", "123", "bio", "image");
    article = new Article("Test Title", "desc", "body", Arrays.asList("java"), user.getId());

    TestingAuthenticationToken auth = new TestingAuthenticationToken(user, null);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_comment_successfully() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    ProfileData profileData =
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    CommentData commentData =
        new CommentData(
            "comment-id",
            "comment body",
            article.getId(),
            new DateTime(),
            new DateTime(),
            profileData);
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result =
        commentMutation.createComment(article.getSlug(), "comment body");

    assertNotNull(result);
    assertNotNull(result.getData());
    assertNotNull(result.getLocalContext());
    verify(commentRepository).save(any(Comment.class));
  }

  @Test
  public void should_throw_authentication_exception_when_not_logged_in_for_create() {
    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new org.springframework.security.authentication.AnonymousAuthenticationToken(
                "key",
                "anonymous",
                Arrays.asList(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_ANONYMOUS"))));

    assertThrows(
        AuthenticationException.class,
        () -> commentMutation.createComment("some-slug", "comment body"));
  }

  @Test
  public void should_throw_resource_not_found_when_article_not_exists_for_create() {
    when(articleRepository.findBySlug(eq("non-existent-slug"))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.createComment("non-existent-slug", "comment body"));
  }

  @Test
  public void should_throw_resource_not_found_when_comment_data_not_found_after_save() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentQueryService.findById(any(), eq(user))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.createComment(article.getSlug(), "comment body"));
  }

  @Test
  public void should_remove_comment_successfully_by_article_author() {
    User commentAuthor = new User("other@test.com", "other", "123", "", "");
    Comment comment = new Comment("comment body", commentAuthor.getId(), article.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    DeletionStatus result = commentMutation.removeComment(article.getSlug(), comment.getId());

    assertNotNull(result);
    assertTrue(result.getSuccess());
    verify(commentRepository).remove(comment);
  }

  @Test
  public void should_remove_comment_successfully_by_comment_author() {
    User articleAuthor = new User("author@test.com", "author", "123", "", "");
    Article otherArticle =
        new Article("Other Title", "desc", "body", Arrays.asList("java"), articleAuthor.getId());
    Comment comment = new Comment("comment body", user.getId(), otherArticle.getId());

    when(articleRepository.findBySlug(eq(otherArticle.getSlug())))
        .thenReturn(Optional.of(otherArticle));
    when(commentRepository.findById(eq(otherArticle.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    DeletionStatus result = commentMutation.removeComment(otherArticle.getSlug(), comment.getId());

    assertNotNull(result);
    assertTrue(result.getSuccess());
    verify(commentRepository).remove(comment);
  }

  @Test
  public void should_throw_authentication_exception_when_not_logged_in_for_remove() {
    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new org.springframework.security.authentication.AnonymousAuthenticationToken(
                "key",
                "anonymous",
                Arrays.asList(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_ANONYMOUS"))));

    assertThrows(
        AuthenticationException.class, () -> commentMutation.removeComment("some-slug", "some-id"));
  }

  @Test
  public void should_throw_resource_not_found_when_article_not_exists_for_remove() {
    when(articleRepository.findBySlug(eq("non-existent-slug"))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.removeComment("non-existent-slug", "some-id"));
  }

  @Test
  public void should_throw_resource_not_found_when_comment_not_exists_for_remove() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(commentRepository.findById(eq(article.getId()), eq("non-existent-id")))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> commentMutation.removeComment(article.getSlug(), "non-existent-id"));
  }

  @Test
  public void should_throw_no_authorization_when_user_cannot_write_comment() {
    User articleAuthor = new User("author@test.com", "author", "123", "", "");
    Article otherArticle =
        new Article("Other Title", "desc", "body", Arrays.asList("java"), articleAuthor.getId());
    User commentAuthor = new User("commentauthor@test.com", "commentauthor", "123", "", "");
    Comment comment = new Comment("comment body", commentAuthor.getId(), otherArticle.getId());

    when(articleRepository.findBySlug(eq(otherArticle.getSlug())))
        .thenReturn(Optional.of(otherArticle));
    when(commentRepository.findById(eq(otherArticle.getId()), eq(comment.getId())))
        .thenReturn(Optional.of(comment));

    assertThrows(
        NoAuthorizationException.class,
        () -> commentMutation.removeComment(otherArticle.getSlug(), comment.getId()));
  }
}
