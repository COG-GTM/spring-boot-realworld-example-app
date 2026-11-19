package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.article.NewArticleParam;
import io.spring.application.article.UpdateArticleParam;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.ArticlePayload;
import io.spring.graphql.types.CreateArticleInput;
import io.spring.graphql.types.DeletionStatus;
import io.spring.graphql.types.UpdateArticleInput;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class ArticleMutationTest {
  private ArticleCommandService articleCommandService;
  private ArticleFavoriteRepository articleFavoriteRepository;
  private ArticleRepository articleRepository;
  private ArticleMutation articleMutation;
  private User user;
  private User other;
  private Article article;

  @BeforeEach
  public void setUp() {
    articleCommandService = mock(ArticleCommandService.class);
    articleFavoriteRepository = mock(ArticleFavoriteRepository.class);
    articleRepository = mock(ArticleRepository.class);
    articleMutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    user = new User("a@b.com", "alice", "pw", "", "");
    other = new User("o@b.com", "other", "pw", "", "");
    article = new Article("My Title", "desc", "body", Arrays.asList("java"), user.getId());
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
  public void createArticle_should_create_with_current_user() {
    authenticate(user);
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    DataFetcherResult<ArticlePayload> result =
        articleMutation.createArticle(
            CreateArticleInput.newBuilder()
                .title("My Title")
                .description("desc")
                .body("body")
                .tagList(Arrays.asList("java", "spring"))
                .build());

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    assertThat(captor.getValue().getTitle()).isEqualTo("My Title");
    assertThat(captor.getValue().getDescription()).isEqualTo("desc");
    assertThat(captor.getValue().getBody()).isEqualTo("body");
    assertThat(captor.getValue().getTagList()).containsExactly("java", "spring");
    assertThat(result.getData()).isInstanceOf(ArticlePayload.class);
    assertThat((Object) result.getLocalContext()).isSameAs(article);
  }

  @Test
  public void createArticle_should_default_null_tag_list_to_empty() {
    authenticate(user);
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    articleMutation.createArticle(
        CreateArticleInput.newBuilder().title("t").description("d").body("b").build());

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    assertThat(captor.getValue().getTagList()).isEmpty();
  }

  @Test
  public void createArticle_should_require_authentication() {
    anonymous();

    assertThatThrownBy(
            () ->
                articleMutation.createArticle(
                    CreateArticleInput.newBuilder().title("t").description("d").body("b").build()))
        .isInstanceOf(AuthenticationException.class);
    verify(articleCommandService, never()).createArticle(any(), any());
  }

  @Test
  public void updateArticle_should_update_when_owner() {
    authenticate(user);
    Article updated = new Article("New", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(updated);

    DataFetcherResult<ArticlePayload> result =
        articleMutation.updateArticle(
            article.getSlug(),
            UpdateArticleInput.newBuilder().title("New").body("nb").description("nd").build());

    ArgumentCaptor<UpdateArticleParam> captor = ArgumentCaptor.forClass(UpdateArticleParam.class);
    verify(articleCommandService).updateArticle(eq(article), captor.capture());
    assertThat(captor.getValue().getTitle()).isEqualTo("New");
    assertThat(captor.getValue().getBody()).isEqualTo("nb");
    assertThat(captor.getValue().getDescription()).isEqualTo("nd");
    assertThat(result.getData()).isInstanceOf(ArticlePayload.class);
    assertThat((Object) result.getLocalContext()).isSameAs(updated);
  }

  @Test
  public void updateArticle_should_throw_not_found_before_checking_auth() {
    anonymous();
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                articleMutation.updateArticle(
                    "missing", UpdateArticleInput.newBuilder().title("x").build()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  public void updateArticle_should_require_authentication() {
    anonymous();
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    assertThatThrownBy(
            () ->
                articleMutation.updateArticle(
                    article.getSlug(), UpdateArticleInput.newBuilder().title("x").build()))
        .isInstanceOf(AuthenticationException.class);
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  public void updateArticle_should_reject_non_owner() {
    authenticate(other);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    assertThatThrownBy(
            () ->
                articleMutation.updateArticle(
                    article.getSlug(), UpdateArticleInput.newBuilder().title("x").build()))
        .isInstanceOf(NoAuthorizationException.class);
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  public void favoriteArticle_should_save_favorite() {
    authenticate(other);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle(article.getSlug());

    verify(articleFavoriteRepository).save(new ArticleFavorite(article.getId(), other.getId()));
    assertThat(result.getData()).isInstanceOf(ArticlePayload.class);
    assertThat((Object) result.getLocalContext()).isSameAs(article);
  }

  @Test
  public void favoriteArticle_should_throw_not_found() {
    authenticate(user);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> articleMutation.favoriteArticle("missing"))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(articleFavoriteRepository, never()).save(any());
  }

  @Test
  public void favoriteArticle_should_require_authentication() {
    anonymous();

    assertThatThrownBy(() -> articleMutation.favoriteArticle(article.getSlug()))
        .isInstanceOf(AuthenticationException.class);
    verify(articleRepository, never()).findBySlug(any());
  }

  @Test
  public void unfavoriteArticle_should_remove_existing_favorite() {
    authenticate(other);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), other.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), other.getId()))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(article.getSlug());

    verify(articleFavoriteRepository).remove(favorite);
    assertThat(result.getData()).isInstanceOf(ArticlePayload.class);
    assertThat((Object) result.getLocalContext()).isSameAs(article);
  }

  @Test
  public void unfavoriteArticle_should_be_noop_when_not_favorited() {
    authenticate(other);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), other.getId()))
        .thenReturn(Optional.empty());

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(article.getSlug());

    verify(articleFavoriteRepository, never()).remove(any());
    assertThat((Object) result.getLocalContext()).isSameAs(article);
  }

  @Test
  public void unfavoriteArticle_should_throw_not_found() {
    authenticate(user);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> articleMutation.unfavoriteArticle("missing"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  public void unfavoriteArticle_should_require_authentication() {
    anonymous();

    assertThatThrownBy(() -> articleMutation.unfavoriteArticle(article.getSlug()))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  public void deleteArticle_should_remove_when_owner() {
    authenticate(user);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    DeletionStatus status = articleMutation.deleteArticle(article.getSlug());

    verify(articleRepository).remove(article);
    assertThat(status.getSuccess()).isTrue();
  }

  @Test
  public void deleteArticle_should_reject_non_owner() {
    authenticate(other);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    assertThatThrownBy(() -> articleMutation.deleteArticle(article.getSlug()))
        .isInstanceOf(NoAuthorizationException.class);
    verify(articleRepository, never()).remove(any());
  }

  @Test
  public void deleteArticle_should_throw_not_found() {
    authenticate(user);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> articleMutation.deleteArticle("missing"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  public void deleteArticle_should_require_authentication() {
    anonymous();

    assertThatThrownBy(() -> articleMutation.deleteArticle(article.getSlug()))
        .isInstanceOf(AuthenticationException.class);
    verify(articleRepository, never()).remove(any());
  }
}
