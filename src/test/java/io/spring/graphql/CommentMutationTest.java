package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class CommentMutationTest {

  @Mock private ArticleRepository articleRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private CommentQueryService commentQueryService;
  @InjectMocks private CommentMutation mutation;

  private User currentUser;
  private Article article;

  @BeforeEach
  public void setUp() {
    currentUser = new User("a@b.com", "alice", "secret", "", "");
    article = new Article("Title", "Desc", "Body", Arrays.asList("java"), currentUser.getId());
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(currentUser, null, Collections.emptyList()));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_comment() {
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    CommentData commentData =
        new CommentData(
            "comment-id",
            "body",
            article.getId(),
            new DateTime(),
            new DateTime(),
            new ProfileData(currentUser.getId(), currentUser.getUsername(), "", "", false));
    when(commentQueryService.findById(any(), eq(currentUser))).thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result =
        mutation.createComment(article.getSlug(), "this is a comment");

    assertNotNull(result);
    verify(commentRepository, times(1)).save(any(Comment.class));
  }

  @Test
  public void should_throw_authentication_when_unauthenticated_create() {
    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anon", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANON"))));

    assertThrows(AuthenticationException.class, () -> mutation.createComment("slug", "body"));
    verify(commentRepository, never()).save(any());
  }

  @Test
  public void should_throw_resource_not_found_when_article_missing() {
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> mutation.createComment("missing", "body"));
  }

  @Test
  public void should_remove_comment_when_owner() {
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    Comment comment = new Comment("body", currentUser.getId(), article.getId());
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = mutation.removeComment(article.getSlug(), comment.getId());

    assertTrue(status.getSuccess());
    verify(commentRepository, times(1)).remove(comment);
  }

  @Test
  public void should_throw_no_authorization_when_removing_other_users_comment() {
    User other = new User("b@b.com", "bob", "secret", "", "");
    Article otherArticle = new Article("T", "D", "B", Arrays.asList("a"), other.getId());
    when(articleRepository.findBySlug(otherArticle.getSlug()))
        .thenReturn(Optional.of(otherArticle));
    Comment comment = new Comment("body", other.getId(), otherArticle.getId());
    when(commentRepository.findById(otherArticle.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    assertThrows(
        NoAuthorizationException.class,
        () -> mutation.removeComment(otherArticle.getSlug(), comment.getId()));
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void should_throw_resource_not_found_when_comment_missing() {
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), "missing")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> mutation.removeComment(article.getSlug(), "missing"));
  }
}
