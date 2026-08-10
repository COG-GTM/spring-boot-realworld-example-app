package io.spring.graphql.mutation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
import io.spring.graphql.ArticleMutation;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.ArticlePayload;
import io.spring.graphql.types.CreateArticleInput;
import io.spring.graphql.types.DeletionStatus;
import io.spring.graphql.types.UpdateArticleInput;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class ArticleMutationTest {

  private final ArticleCommandService articleCommandService = mock(ArticleCommandService.class);
  private final ArticleFavoriteRepository articleFavoriteRepository =
      mock(ArticleFavoriteRepository.class);
  private final ArticleRepository articleRepository = mock(ArticleRepository.class);
  private final ArticleMutation mutation =
      new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);

  private final User user = new User("jake@jake.jake", "jake", "123", "bio", "image");

  private void login(User currentUser) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null, null));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_create_article_with_tag_list() {
    login(user);
    Article article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(article);

    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java"))
            .build();
    DataFetcherResult<ArticlePayload> result = mutation.createArticle(input);

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    assertThat(captor.getValue().getTitle()).isEqualTo("title");
    assertThat(captor.getValue().getTagList()).containsExactly("java");
    assertThat(result.getLocalContext()).isSameAs(article);
    assertThat(result.getData()).isNotNull();
  }

  @Test
  void should_default_tag_list_to_empty_when_null() {
    login(user);
    Article article = new Article("title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(article);

    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("title").description("desc").body("body").build();
    mutation.createArticle(input);

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    assertThat(captor.getValue().getTagList()).isEmpty();
  }

  @Test
  void should_reject_create_article_for_anonymous_user() {
    anonymous();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> mutation.createArticle(CreateArticleInput.newBuilder().build()));
    verify(articleCommandService, never()).createArticle(any(), any());
  }

  @Test
  void should_update_own_article() {
    login(user);
    Article article = new Article("title", "desc", "body", Collections.emptyList(), user.getId());
    Article updated =
        new Article("new title", "new desc", "new body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any(UpdateArticleParam.class)))
        .thenReturn(updated);

    DataFetcherResult<ArticlePayload> result =
        mutation.updateArticle(
            "title",
            UpdateArticleInput.newBuilder()
                .title("new title")
                .body("new body")
                .description("new desc")
                .build());

    assertThat(result.getLocalContext()).isSameAs(updated);
  }

  @Test
  void should_throw_not_found_when_updating_missing_article() {
    login(user);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(
            () -> mutation.updateArticle("missing", UpdateArticleInput.newBuilder().build()));
  }

  @Test
  void should_reject_updating_others_article() {
    User other = new User("other@other.com", "other", "123", "", "");
    login(other);
    Article article = new Article("title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    assertThatExceptionOfType(NoAuthorizationException.class)
        .isThrownBy(() -> mutation.updateArticle("title", UpdateArticleInput.newBuilder().build()));
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  void should_favorite_article() {
    login(user);
    Article article = new Article("title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = mutation.favoriteArticle("title");

    ArgumentCaptor<ArticleFavorite> captor = ArgumentCaptor.forClass(ArticleFavorite.class);
    verify(articleFavoriteRepository).save(captor.capture());
    assertThat(captor.getValue().getArticleId()).isEqualTo(article.getId());
    assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
    assertThat(result.getLocalContext()).isSameAs(article);
  }

  @Test
  void should_throw_not_found_when_favoriting_missing_article() {
    login(user);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> mutation.favoriteArticle("missing"));
  }

  @Test
  void should_remove_favorite_when_present() {
    login(user);
    Article article = new Article("title", "desc", "body", Collections.emptyList(), user.getId());
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = mutation.unfavoriteArticle("title");

    verify(articleFavoriteRepository).remove(favorite);
    assertThat(result.getLocalContext()).isSameAs(article);
  }

  @Test
  void should_ignore_unfavorite_when_favorite_absent() {
    login(user);
    Article article = new Article("title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.empty());

    DataFetcherResult<ArticlePayload> result = mutation.unfavoriteArticle("title");

    verify(articleFavoriteRepository, never()).remove(any());
    assertThat(result.getLocalContext()).isSameAs(article);
  }

  @Test
  void should_delete_own_article() {
    login(user);
    Article article = new Article("title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    DeletionStatus status = mutation.deleteArticle("title");

    verify(articleRepository).remove(article);
    assertThat(status.getSuccess()).isTrue();
  }

  @Test
  void should_reject_deleting_others_article() {
    User other = new User("other@other.com", "other", "123", "", "");
    login(other);
    Article article = new Article("title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    assertThatExceptionOfType(NoAuthorizationException.class)
        .isThrownBy(() -> mutation.deleteArticle("title"));
    verify(articleRepository, never()).remove(any());
  }

  @Test
  void should_throw_not_found_when_deleting_missing_article() {
    login(user);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> mutation.deleteArticle("missing"));
  }
}
