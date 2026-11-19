package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    articleAuthor = new User("author@b.com", "author", "pw", "", "");
    commenter = new User("c@b.com", "commenter", "pw", "", "");
    stranger = new User("s@b.com", "stranger", "pw", "", "");
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

  @Test
  public void createComment_should_save_and_return_comment_data_as_local_context() {
    authenticate(commenter);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    CommentData commentData =
        new CommentData(
            "cid",
            "nice",
            article.getId(),
            new DateTime(),
            new DateTime(),
            new ProfileData(commenter.getId(), "commenter", "", "", false));
    when(commentQueryService.findById(anyString(), eq(commenter)))
        .thenReturn(Optional.of(commentData));

    DataFetcherResult<CommentPayload> result =
        commentMutation.createComment(article.getSlug(), "nice");

    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).save(captor.capture());
    Comment saved = captor.getValue();
    assertThat(saved.getBody()).isEqualTo("nice");
    assertThat(saved.getUserId()).isEqualTo(commenter.getId());
    assertThat(saved.getArticleId()).isEqualTo(article.getId());
    verify(commentQueryService).findById(saved.getId(), commenter);
    assertThat(result.getData()).isInstanceOf(CommentPayload.class);
    assertThat((Object) result.getLocalContext()).isSameAs(commentData);
  }

  @Test
  public void createComment_should_throw_not_found_when_article_missing() {
    authenticate(commenter);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> commentMutation.createComment("missing", "body"))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(commentRepository, never()).save(any());
  }

  @Test
  public void createComment_should_throw_not_found_when_saved_comment_cannot_be_read_back() {
    authenticate(commenter);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentQueryService.findById(anyString(), eq(commenter))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> commentMutation.createComment(article.getSlug(), "body"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  public void createComment_should_require_authentication() {
    anonymous();

    assertThatThrownBy(() -> commentMutation.createComment(article.getSlug(), "body"))
        .isInstanceOf(AuthenticationException.class);
    verify(commentRepository, never()).save(any());
  }

  @Test
  public void removeComment_should_allow_comment_author() {
    authenticate(commenter);
    Comment comment = new Comment("body", commenter.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment(article.getSlug(), comment.getId());

    verify(commentRepository).remove(comment);
    assertThat(status.getSuccess()).isTrue();
  }

  @Test
  public void removeComment_should_allow_article_author() {
    authenticate(articleAuthor);
    Comment comment = new Comment("body", commenter.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    DeletionStatus status = commentMutation.removeComment(article.getSlug(), comment.getId());

    verify(commentRepository).remove(comment);
    assertThat(status.getSuccess()).isTrue();
  }

  @Test
  public void removeComment_should_reject_other_users() {
    authenticate(stranger);
    Comment comment = new Comment("body", commenter.getId(), article.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), comment.getId()))
        .thenReturn(Optional.of(comment));

    assertThatThrownBy(() -> commentMutation.removeComment(article.getSlug(), comment.getId()))
        .isInstanceOf(NoAuthorizationException.class);
    verify(commentRepository, never()).remove(any());
  }

  @Test
  public void removeComment_should_throw_not_found_when_comment_missing() {
    authenticate(commenter);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(commentRepository.findById(article.getId(), "missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> commentMutation.removeComment(article.getSlug(), "missing"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  public void removeComment_should_throw_not_found_when_article_missing() {
    authenticate(commenter);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> commentMutation.removeComment("missing", "cid"))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(commentRepository, never()).findById(any(), any());
  }

  @Test
  public void removeComment_should_require_authentication() {
    anonymous();

    assertThatThrownBy(() -> commentMutation.removeComment(article.getSlug(), "cid"))
        .isInstanceOf(AuthenticationException.class);
    verify(articleRepository, never()).findBySlug(any());
  }
}
