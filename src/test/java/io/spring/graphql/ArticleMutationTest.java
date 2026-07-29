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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleMutationTest extends GraphqlTestBase {

  @Mock private ArticleCommandService articleCommandService;
  @Mock private ArticleFavoriteRepository articleFavoriteRepository;
  @Mock private ArticleRepository articleRepository;

  @Captor private ArgumentCaptor<NewArticleParam> newArticleParamCaptor;
  @Captor private ArgumentCaptor<UpdateArticleParam> updateArticleParamCaptor;

  @InjectMocks private ArticleMutation articleMutation;

  private User user;
  private Article article;

  @BeforeEach
  void setUp() {
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
    article = new Article("title", "desc", "body", Arrays.asList("joda"), user.getId());
  }

  @Test
  void should_create_article() {
    login(user);
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("joda"))
            .build();
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    assertThat((Article) result.getLocalContext()).isEqualTo(article);
    verify(articleCommandService).createArticle(newArticleParamCaptor.capture(), eq(user));
    assertThat(newArticleParamCaptor.getValue().getTitle()).isEqualTo("title");
    assertThat(newArticleParamCaptor.getValue().getTagList()).containsExactly("joda");
  }

  @Test
  void should_create_article_without_tags() {
    login(user);
    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("title").description("desc").body("body").build();
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(article);

    articleMutation.createArticle(input);

    verify(articleCommandService).createArticle(newArticleParamCaptor.capture(), eq(user));
    assertThat(newArticleParamCaptor.getValue().getTagList()).isEmpty();
  }

  @Test
  void should_not_create_article_without_login() {
    logout();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> articleMutation.createArticle(CreateArticleInput.newBuilder().build()));
  }

  @Test
  void should_update_article() {
    login(user);
    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder()
            .title("new title")
            .body("new body")
            .description("new desc")
            .build();
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any(UpdateArticleParam.class)))
        .thenReturn(article);

    DataFetcherResult<ArticlePayload> result =
        articleMutation.updateArticle(article.getSlug(), changes);

    assertThat((Article) result.getLocalContext()).isEqualTo(article);
    verify(articleCommandService).updateArticle(eq(article), updateArticleParamCaptor.capture());
    assertThat(updateArticleParamCaptor.getValue().getTitle()).isEqualTo("new title");
    assertThat(updateArticleParamCaptor.getValue().getBody()).isEqualTo("new body");
    assertThat(updateArticleParamCaptor.getValue().getDescription()).isEqualTo("new desc");
  }

  @Test
  void should_not_update_article_of_other_user() {
    User other = new User("other@test.com", "other", "123", "", "");
    login(other);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    assertThatExceptionOfType(NoAuthorizationException.class)
        .isThrownBy(
            () ->
                articleMutation.updateArticle(
                    article.getSlug(), UpdateArticleInput.newBuilder().build()));
  }

  @Test
  void should_not_update_missing_article() {
    when(articleRepository.findBySlug("unknown")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(
            () ->
                articleMutation.updateArticle("unknown", UpdateArticleInput.newBuilder().build()));
  }

  @Test
  void should_favorite_article() {
    login(user);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle(article.getSlug());

    assertThat((Article) result.getLocalContext()).isEqualTo(article);
    ArgumentCaptor<ArticleFavorite> captor = ArgumentCaptor.forClass(ArticleFavorite.class);
    verify(articleFavoriteRepository).save(captor.capture());
    assertThat(captor.getValue().getArticleId()).isEqualTo(article.getId());
    assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
  }

  @Test
  void should_not_favorite_missing_article() {
    login(user);
    when(articleRepository.findBySlug("unknown")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleMutation.favoriteArticle("unknown"));
  }

  @Test
  void should_not_favorite_article_without_login() {
    logout();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> articleMutation.favoriteArticle("slug"));
  }

  @Test
  void should_unfavorite_article() {
    login(user);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(article.getSlug());

    assertThat((Article) result.getLocalContext()).isEqualTo(article);
    verify(articleFavoriteRepository).remove(favorite);
  }

  @Test
  void should_ignore_unfavorite_when_article_is_not_favorited() {
    login(user);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.empty());

    articleMutation.unfavoriteArticle(article.getSlug());

    verify(articleFavoriteRepository, never()).remove(any(ArticleFavorite.class));
  }

  @Test
  void should_delete_article() {
    login(user);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    DeletionStatus status = articleMutation.deleteArticle(article.getSlug());

    assertThat(status.getSuccess()).isTrue();
    verify(articleRepository).remove(article);
  }

  @Test
  void should_not_delete_article_of_other_user() {
    User other = new User("other@test.com", "other", "123", "", "");
    login(other);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    assertThatExceptionOfType(NoAuthorizationException.class)
        .isThrownBy(() -> articleMutation.deleteArticle(article.getSlug()));
    verify(articleRepository, never()).remove(any(Article.class));
  }

  @Test
  void should_not_delete_missing_article() {
    login(user);
    when(articleRepository.findBySlug("unknown")).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleMutation.deleteArticle("unknown"));
  }
}
