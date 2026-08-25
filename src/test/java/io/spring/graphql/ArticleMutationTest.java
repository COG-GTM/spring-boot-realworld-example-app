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
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ArticleMutationTest {

  @Mock private ArticleCommandService articleCommandService;
  @Mock private ArticleFavoriteRepository articleFavoriteRepository;
  @Mock private ArticleRepository articleRepository;

  @Captor private ArgumentCaptor<NewArticleParam> newArticleParamCaptor;
  @Captor private ArgumentCaptor<UpdateArticleParam> updateArticleParamCaptor;

  private ArticleMutation mutation;
  private User currentUser;
  private Article article;

  @BeforeEach
  public void setUp() {
    mutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    currentUser = new User("john@example.com", "john", "123", "bio", "image");
    article =
        new Article(
            "Test Title", "a description", "a body", Arrays.asList("java"), currentUser.getId());
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void login() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null));
  }

  private void loginAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @Test
  public void should_create_article() {
    login();
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Title")
            .description("a description")
            .body("a body")
            .tagList(Arrays.asList("java", "spring"))
            .build();
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(currentUser)))
        .thenReturn(article);

    DataFetcherResult<ArticlePayload> result = mutation.createArticle(input);

    assertThat(result.getData()).isInstanceOf(ArticlePayload.class);
    assertThat(result.getLocalContext()).isSameAs(article);
    verify(articleCommandService).createArticle(newArticleParamCaptor.capture(), eq(currentUser));
    NewArticleParam param = newArticleParamCaptor.getValue();
    assertThat(param.getTitle()).isEqualTo("Test Title");
    assertThat(param.getDescription()).isEqualTo("a description");
    assertThat(param.getBody()).isEqualTo("a body");
    assertThat(param.getTagList()).containsExactly("java", "spring");
  }

  @Test
  public void should_create_article_with_empty_tag_list_when_tags_are_null() {
    login();
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Title")
            .description("a description")
            .body("a body")
            .build();
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(currentUser)))
        .thenReturn(article);

    mutation.createArticle(input);

    verify(articleCommandService).createArticle(newArticleParamCaptor.capture(), eq(currentUser));
    assertThat(newArticleParamCaptor.getValue().getTagList()).isEmpty();
  }

  @Test
  public void should_reject_create_article_for_anonymous_user() {
    loginAnonymous();
    CreateArticleInput input = CreateArticleInput.newBuilder().title("Test Title").build();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> mutation.createArticle(input));
    verify(articleCommandService, never()).createArticle(any(), any());
  }

  @Test
  public void should_update_article() {
    login();
    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder()
            .title("New Title")
            .body("new body")
            .description("new description")
            .build();
    when(articleRepository.findBySlug("test-title")).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any(UpdateArticleParam.class)))
        .thenReturn(article);

    DataFetcherResult<ArticlePayload> result = mutation.updateArticle("test-title", changes);

    assertThat(result.getLocalContext()).isSameAs(article);
    verify(articleCommandService).updateArticle(eq(article), updateArticleParamCaptor.capture());
    UpdateArticleParam param = updateArticleParamCaptor.getValue();
    assertThat(param.getTitle()).isEqualTo("New Title");
    assertThat(param.getBody()).isEqualTo("new body");
    assertThat(param.getDescription()).isEqualTo("new description");
  }

  @Test
  public void should_throw_not_found_when_updating_missing_article() {
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(
            () -> mutation.updateArticle("missing", UpdateArticleInput.newBuilder().build()));
  }

  @Test
  public void should_reject_update_article_for_anonymous_user() {
    loginAnonymous();
    when(articleRepository.findBySlug("test-title")).thenReturn(Optional.of(article));

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(
            () -> mutation.updateArticle("test-title", UpdateArticleInput.newBuilder().build()));
  }

  @Test
  public void should_reject_update_article_of_other_user() {
    login();
    Article othersArticle =
        new Article("Other", "desc", "body", Arrays.asList("java"), "another-user-id");
    when(articleRepository.findBySlug("other")).thenReturn(Optional.of(othersArticle));

    assertThatExceptionOfType(NoAuthorizationException.class)
        .isThrownBy(() -> mutation.updateArticle("other", UpdateArticleInput.newBuilder().build()));
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  public void should_favorite_article() {
    login();
    when(articleRepository.findBySlug("test-title")).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = mutation.favoriteArticle("test-title");

    assertThat(result.getLocalContext()).isSameAs(article);
    ArgumentCaptor<ArticleFavorite> captor = ArgumentCaptor.forClass(ArticleFavorite.class);
    verify(articleFavoriteRepository).save(captor.capture());
    assertThat(captor.getValue().getArticleId()).isEqualTo(article.getId());
    assertThat(captor.getValue().getUserId()).isEqualTo(currentUser.getId());
  }

  @Test
  public void should_throw_not_found_when_favoriting_missing_article() {
    login();
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> mutation.favoriteArticle("missing"));
    verify(articleFavoriteRepository, never()).save(any());
  }

  @Test
  public void should_reject_favorite_article_for_anonymous_user() {
    loginAnonymous();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> mutation.favoriteArticle("test-title"));
  }

  @Test
  public void should_unfavorite_article() {
    login();
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), currentUser.getId());
    when(articleRepository.findBySlug("test-title")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), currentUser.getId()))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = mutation.unfavoriteArticle("test-title");

    assertThat(result.getLocalContext()).isSameAs(article);
    verify(articleFavoriteRepository).remove(favorite);
  }

  @Test
  public void should_ignore_unfavorite_when_favorite_absent() {
    login();
    when(articleRepository.findBySlug("test-title")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), currentUser.getId()))
        .thenReturn(Optional.empty());

    assertThat(mutation.unfavoriteArticle("test-title").getLocalContext()).isSameAs(article);
    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  public void should_throw_not_found_when_unfavoriting_missing_article() {
    login();
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> mutation.unfavoriteArticle("missing"));
  }

  @Test
  public void should_reject_unfavorite_article_for_anonymous_user() {
    loginAnonymous();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> mutation.unfavoriteArticle("test-title"));
  }

  @Test
  public void should_delete_article() {
    login();
    when(articleRepository.findBySlug("test-title")).thenReturn(Optional.of(article));

    DeletionStatus status = mutation.deleteArticle("test-title");

    assertThat(status.getSuccess()).isTrue();
    verify(articleRepository).remove(article);
  }

  @Test
  public void should_throw_not_found_when_deleting_missing_article() {
    login();
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> mutation.deleteArticle("missing"));
    verify(articleRepository, never()).remove(any());
  }

  @Test
  public void should_reject_delete_article_of_other_user() {
    login();
    Article othersArticle =
        new Article("Other", "desc", "body", Arrays.asList("java"), "another-user-id");
    when(articleRepository.findBySlug("other")).thenReturn(Optional.of(othersArticle));

    assertThatExceptionOfType(NoAuthorizationException.class)
        .isThrownBy(() -> mutation.deleteArticle("other"));
    verify(articleRepository, never()).remove(any());
  }

  @Test
  public void should_reject_delete_article_for_anonymous_user() {
    loginAnonymous();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> mutation.deleteArticle("test-title"));
  }
}
