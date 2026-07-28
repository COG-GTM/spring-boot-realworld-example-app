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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleMutationTest extends GraphQLTestBase {

  @Mock private ArticleCommandService articleCommandService;
  @Mock private ArticleFavoriteRepository articleFavoriteRepository;
  @Mock private ArticleRepository articleRepository;

  @InjectMocks private ArticleMutation articleMutation;

  @Captor private ArgumentCaptor<NewArticleParam> newArticleParamCaptor;
  @Captor private ArgumentCaptor<UpdateArticleParam> updateArticleParamCaptor;

  private Article article(User author) {
    return new Article("title", "desc", "body", Collections.singletonList("joda"), author.getId());
  }

  @Test
  void should_create_article() {
    Article article = article(user);
    when(articleCommandService.createArticle(newArticleParamCaptor.capture(), eq(user)))
        .thenReturn(article);

    DataFetcherResult<ArticlePayload> result =
        articleMutation.createArticle(
            CreateArticleInput.newBuilder()
                .title("title")
                .description("desc")
                .body("body")
                .tagList(Arrays.asList("joda", "spring"))
                .build());

    assertThat(result.getData()).isNotNull();
    assertThat((Object) result.getLocalContext()).isEqualTo(article);
    assertThat(newArticleParamCaptor.getValue().getTitle()).isEqualTo("title");
    assertThat(newArticleParamCaptor.getValue().getTagList()).containsExactly("joda", "spring");
  }

  @Test
  void should_create_article_with_empty_tag_list_when_null() {
    Article article = article(user);
    when(articleCommandService.createArticle(newArticleParamCaptor.capture(), eq(user)))
        .thenReturn(article);

    articleMutation.createArticle(CreateArticleInput.newBuilder().title("title").build());

    assertThat(newArticleParamCaptor.getValue().getTagList()).isEmpty();
  }

  @Test
  void should_not_create_article_for_anonymous_user() {
    anonymous();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> articleMutation.createArticle(CreateArticleInput.newBuilder().build()));
  }

  @Test
  void should_update_article() {
    Article article = article(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), updateArticleParamCaptor.capture()))
        .thenReturn(article);

    DataFetcherResult<ArticlePayload> result =
        articleMutation.updateArticle(
            article.getSlug(),
            UpdateArticleInput.newBuilder()
                .title("new title")
                .body("new body")
                .description("new desc")
                .build());

    assertThat((Object) result.getLocalContext()).isEqualTo(article);
    assertThat(updateArticleParamCaptor.getValue().getTitle()).isEqualTo("new title");
    assertThat(updateArticleParamCaptor.getValue().getBody()).isEqualTo("new body");
    assertThat(updateArticleParamCaptor.getValue().getDescription()).isEqualTo("new desc");
  }

  @Test
  void should_fail_update_article_when_not_found() {
    when(articleRepository.findBySlug(eq("unknown"))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(
            () ->
                articleMutation.updateArticle("unknown", UpdateArticleInput.newBuilder().build()));
  }

  @Test
  void should_fail_update_article_of_other_user() {
    Article article = article(anotherUser());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    assertThatExceptionOfType(NoAuthorizationException.class)
        .isThrownBy(
            () ->
                articleMutation.updateArticle(
                    article.getSlug(), UpdateArticleInput.newBuilder().build()));
  }

  @Test
  void should_favorite_article() {
    Article article = article(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle(article.getSlug());

    assertThat((Object) result.getLocalContext()).isEqualTo(article);
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  void should_unfavorite_article() {
    Article article = article(user);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(favorite));

    articleMutation.unfavoriteArticle(article.getSlug());

    verify(articleFavoriteRepository).remove(eq(favorite));
  }

  @Test
  void should_ignore_unfavorite_when_favorite_absent() {
    Article article = article(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.empty());

    articleMutation.unfavoriteArticle(article.getSlug());

    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  void should_delete_own_article() {
    Article article = article(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    DeletionStatus status = articleMutation.deleteArticle(article.getSlug());

    assertThat(status.getSuccess()).isTrue();
    verify(articleRepository).remove(eq(article));
  }

  @Test
  void should_fail_delete_article_of_other_user() {
    Article article = article(anotherUser());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    assertThatExceptionOfType(NoAuthorizationException.class)
        .isThrownBy(() -> articleMutation.deleteArticle(article.getSlug()));
    verify(articleRepository, never()).remove(any());
  }

  @Test
  void should_fail_delete_article_when_not_found() {
    when(articleRepository.findBySlug(eq("unknown"))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleMutation.deleteArticle("unknown"));
  }

  @Test
  void should_fail_favorite_article_for_anonymous_user() {
    anonymous();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> articleMutation.favoriteArticle("slug"));
  }

  @Test
  void should_fail_unfavorite_article_for_anonymous_user() {
    anonymous();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> articleMutation.unfavoriteArticle("slug"));
  }

  @Test
  void should_fail_delete_article_for_anonymous_user() {
    anonymous();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> articleMutation.deleteArticle("slug"));
  }

  private User anotherUser() {
    return new User("other@test.com", "other", "123", "", "");
  }
}
