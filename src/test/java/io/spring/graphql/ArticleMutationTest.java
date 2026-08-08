package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ArticleMutationTest {

  @Mock private ArticleCommandService articleCommandService;
  @Mock private ArticleFavoriteRepository articleFavoriteRepository;
  @Mock private ArticleRepository articleRepository;

  private ArticleMutation articleMutation;
  private User user;
  private Article article;

  @BeforeEach
  public void setUp() {
    articleMutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    user = new User("john@example.com", "john", "123", "bio", "image");
    article = new Article("Title", "description", "body", Arrays.asList("java"), user.getId());
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void loginAs(User loggedIn) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(loggedIn, null, Collections.emptyList()));
  }

  private void loginAsAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @Test
  public void should_create_article() {
    loginAs(user);
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Title")
            .description("description")
            .body("body")
            .tagList(Arrays.asList("java", "spring"))
            .build();
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    assertThat(captor.getValue().getTitle()).isEqualTo("Title");
    assertThat(captor.getValue().getDescription()).isEqualTo("description");
    assertThat(captor.getValue().getBody()).isEqualTo("body");
    assertThat(captor.getValue().getTagList()).containsExactly("java", "spring");
    assertThat(result.getData()).isNotNull();
    assertThat((Article) result.getLocalContext()).isSameAs(article);
  }

  @Test
  public void should_default_tag_list_to_empty_when_null() {
    loginAs(user);
    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("Title").description("d").body("b").build();
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(article);

    articleMutation.createArticle(input);

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    assertThat(captor.getValue().getTagList()).isEmpty();
  }

  @Test
  public void should_throw_authentication_exception_when_creating_without_login() {
    loginAsAnonymous();
    CreateArticleInput input = CreateArticleInput.newBuilder().title("Title").build();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> articleMutation.createArticle(input));
    verify(articleCommandService, never()).createArticle(any(), any());
  }

  @Test
  public void should_update_article() {
    loginAs(user);
    UpdateArticleInput input =
        UpdateArticleInput.newBuilder()
            .title("new title")
            .body("new body")
            .description("new description")
            .build();
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any(UpdateArticleParam.class)))
        .thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.updateArticle("title", input);

    ArgumentCaptor<UpdateArticleParam> captor = ArgumentCaptor.forClass(UpdateArticleParam.class);
    verify(articleCommandService).updateArticle(eq(article), captor.capture());
    assertThat(captor.getValue().getTitle()).isEqualTo("new title");
    assertThat(captor.getValue().getBody()).isEqualTo("new body");
    assertThat(captor.getValue().getDescription()).isEqualTo("new description");
    assertThat(result.getData()).isNotNull();
    assertThat((Article) result.getLocalContext()).isSameAs(article);
  }

  @Test
  public void should_throw_not_found_when_updating_missing_article() {
    loginAsAnonymous();
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(
            () ->
                articleMutation.updateArticle(
                    "missing", UpdateArticleInput.newBuilder().title("t").build()));
  }

  @Test
  public void should_throw_authentication_exception_when_updating_without_login() {
    loginAsAnonymous();
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(
            () ->
                articleMutation.updateArticle(
                    "title", UpdateArticleInput.newBuilder().title("t").build()));
  }

  @Test
  public void should_throw_no_authorization_when_updating_others_article() {
    User other = new User("other@example.com", "other", "123", "", "");
    loginAs(other);
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    assertThatExceptionOfType(NoAuthorizationException.class)
        .isThrownBy(
            () ->
                articleMutation.updateArticle(
                    "title", UpdateArticleInput.newBuilder().title("t").build()));
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  public void should_favorite_article() {
    loginAs(user);
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle("title");

    ArgumentCaptor<ArticleFavorite> captor = ArgumentCaptor.forClass(ArticleFavorite.class);
    verify(articleFavoriteRepository).save(captor.capture());
    assertThat(captor.getValue().getArticleId()).isEqualTo(article.getId());
    assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
    assertThat((Article) result.getLocalContext()).isSameAs(article);
  }

  @Test
  public void should_throw_authentication_exception_when_favoriting_without_login() {
    loginAsAnonymous();
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> articleMutation.favoriteArticle("title"));
  }

  @Test
  public void should_throw_not_found_when_favoriting_missing_article() {
    loginAs(user);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleMutation.favoriteArticle("missing"));
  }

  @Test
  public void should_unfavorite_article_when_favorite_exists() {
    loginAs(user);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("title");

    verify(articleFavoriteRepository).remove(favorite);
    assertThat((Article) result.getLocalContext()).isSameAs(article);
  }

  @Test
  public void should_do_nothing_when_unfavoriting_without_existing_favorite() {
    loginAs(user);
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.empty());

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("title");

    verify(articleFavoriteRepository, never()).remove(any());
    assertThat(result.getData()).isNotNull();
  }

  @Test
  public void should_throw_authentication_exception_when_unfavoriting_without_login() {
    loginAsAnonymous();
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> articleMutation.unfavoriteArticle("title"));
  }

  @Test
  public void should_delete_own_article() {
    loginAs(user);
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    DeletionStatus status = articleMutation.deleteArticle("title");

    verify(articleRepository).remove(article);
    assertThat(status.getSuccess()).isTrue();
  }

  @Test
  public void should_throw_authentication_exception_when_deleting_without_login() {
    loginAsAnonymous();
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> articleMutation.deleteArticle("title"));
    verify(articleRepository, never()).remove(any());
  }

  @Test
  public void should_throw_not_found_when_deleting_missing_article() {
    loginAs(user);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleMutation.deleteArticle("missing"));
  }

  @Test
  public void should_throw_no_authorization_when_deleting_others_article() {
    User other = new User("other@example.com", "other", "123", "", "");
    loginAs(other);
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    assertThatExceptionOfType(NoAuthorizationException.class)
        .isThrownBy(() -> articleMutation.deleteArticle("title"));
    verify(articleRepository, never()).remove(any());
  }
}
