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
import io.spring.application.article.ArticleCommandService;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleMutationTest extends GraphQLTestBase {

  @Mock private ArticleCommandService articleCommandService;
  @Mock private ArticleFavoriteRepository articleFavoriteRepository;
  @Mock private ArticleRepository articleRepository;

  private ArticleMutation articleMutation;
  private User user;

  @BeforeEach
  void setUp() {
    articleMutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    user = newUser();
  }

  private Article ownedArticle() {
    return new Article("title", "desc", "body", Arrays.asList("t"), user.getId());
  }

  @Test
  void should_create_article() {
    setCurrentUser(user);
    Article article = ownedArticle();
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    DataFetcherResult<ArticlePayload> result =
        articleMutation.createArticle(
            CreateArticleInput.newBuilder()
                .title("title")
                .description("desc")
                .body("body")
                .tagList(Arrays.asList("t"))
                .build());

    assertThat(result.getLocalContext()).isSameAs(article);
  }

  @Test
  void should_create_article_with_null_tag_list() {
    setCurrentUser(user);
    Article article = ownedArticle();
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    DataFetcherResult<ArticlePayload> result =
        articleMutation.createArticle(
            CreateArticleInput.newBuilder().title("title").description("desc").body("body").build());

    assertThat(result.getLocalContext()).isSameAs(article);
  }

  @Test
  void should_throw_when_create_article_unauthenticated() {
    setAnonymous();
    assertThatThrownBy(
            () ->
                articleMutation.createArticle(
                    CreateArticleInput.newBuilder().title("t").description("d").body("b").build()))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  void should_update_article() {
    setCurrentUser(user);
    Article article = ownedArticle();
    when(articleRepository.findBySlug(eq("slug"))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(article);

    DataFetcherResult<ArticlePayload> result =
        articleMutation.updateArticle(
            "slug", UpdateArticleInput.newBuilder().title("new").body("b").description("d").build());

    assertThat(result.getLocalContext()).isSameAs(article);
  }

  @Test
  void should_throw_when_update_article_missing() {
    when(articleRepository.findBySlug(eq("slug"))).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                articleMutation.updateArticle(
                    "slug", UpdateArticleInput.newBuilder().build()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_throw_when_update_article_not_author() {
    setCurrentUser(user);
    Article othersArticle = new Article("t", "d", "b", Arrays.asList("x"), "another-user");
    when(articleRepository.findBySlug(eq("slug"))).thenReturn(Optional.of(othersArticle));

    assertThatThrownBy(
            () ->
                articleMutation.updateArticle(
                    "slug", UpdateArticleInput.newBuilder().build()))
        .isInstanceOf(NoAuthorizationException.class);
  }

  @Test
  void should_favorite_article() {
    setCurrentUser(user);
    Article article = ownedArticle();
    when(articleRepository.findBySlug(eq("slug"))).thenReturn(Optional.of(article));

    articleMutation.favoriteArticle("slug");

    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  void should_throw_when_favorite_article_missing() {
    setCurrentUser(user);
    when(articleRepository.findBySlug(eq("slug"))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> articleMutation.favoriteArticle("slug"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_unfavorite_article_when_favorite_present() {
    setCurrentUser(user);
    Article article = ownedArticle();
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug(eq("slug"))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(favorite));

    articleMutation.unfavoriteArticle("slug");

    verify(articleFavoriteRepository).remove(favorite);
  }

  @Test
  void should_not_remove_when_unfavorite_without_existing_favorite() {
    setCurrentUser(user);
    Article article = ownedArticle();
    when(articleRepository.findBySlug(eq("slug"))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.empty());

    articleMutation.unfavoriteArticle("slug");

    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  void should_delete_article() {
    setCurrentUser(user);
    Article article = ownedArticle();
    when(articleRepository.findBySlug(eq("slug"))).thenReturn(Optional.of(article));

    DeletionStatus status = articleMutation.deleteArticle("slug");

    assertThat(status.getSuccess()).isTrue();
    verify(articleRepository).remove(article);
  }

  @Test
  void should_throw_when_delete_article_not_author() {
    setCurrentUser(user);
    Article othersArticle = new Article("t", "d", "b", Arrays.asList("x"), "another-user");
    when(articleRepository.findBySlug(eq("slug"))).thenReturn(Optional.of(othersArticle));

    assertThatThrownBy(() -> articleMutation.deleteArticle("slug"))
        .isInstanceOf(NoAuthorizationException.class);
    verify(articleRepository, never()).remove(any());
  }
}
