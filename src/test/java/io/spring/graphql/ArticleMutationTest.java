package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleMutationTest {
  @Mock private ArticleCommandService articleCommandService;
  @Mock private ArticleFavoriteRepository articleFavoriteRepository;
  @Mock private ArticleRepository articleRepository;

  private ArticleMutation articleMutation;
  private User user;
  private User stranger;
  private Article article;

  @BeforeEach
  public void setUp() {
    articleMutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    user = new User("a@test.com", "a", "123", "", "");
    stranger = new User("b@test.com", "b", "123", "", "");
    article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
  }

  @AfterEach
  public void tearDown() {
    GraphQLTestSupport.clear();
  }

  @Test
  public void should_create_article_for_current_user() {
    GraphQLTestSupport.authenticate(user);
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java"))
            .build();

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    Assertions.assertEquals("title", captor.getValue().getTitle());
    Assertions.assertEquals("desc", captor.getValue().getDescription());
    Assertions.assertEquals("body", captor.getValue().getBody());
    Assertions.assertEquals(Arrays.asList("java"), captor.getValue().getTagList());
    Assertions.assertNotNull(result.getData());
    Assertions.assertSame(article, result.getLocalContext());
  }

  @Test
  public void should_default_to_empty_tag_list_when_creating_article() {
    GraphQLTestSupport.authenticate(user);
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    articleMutation.createArticle(
        CreateArticleInput.newBuilder().title("title").description("desc").body("body").build());

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    Assertions.assertTrue(captor.getValue().getTagList().isEmpty());
  }

  @Test
  public void should_reject_anonymous_article_creation() {
    GraphQLTestSupport.anonymous();

    Assertions.assertThrows(
        AuthenticationException.class,
        () -> articleMutation.createArticle(CreateArticleInput.newBuilder().build()));
    verifyNoInteractions(articleCommandService);
  }

  @Test
  public void should_update_article_by_author() {
    GraphQLTestSupport.authenticate(user);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(article);
    UpdateArticleInput input =
        UpdateArticleInput.newBuilder()
            .title("new title")
            .body("new body")
            .description("new desc")
            .build();

    DataFetcherResult<ArticlePayload> result =
        articleMutation.updateArticle(article.getSlug(), input);

    ArgumentCaptor<UpdateArticleParam> captor = ArgumentCaptor.forClass(UpdateArticleParam.class);
    verify(articleCommandService).updateArticle(eq(article), captor.capture());
    Assertions.assertEquals("new title", captor.getValue().getTitle());
    Assertions.assertEquals("new body", captor.getValue().getBody());
    Assertions.assertEquals("new desc", captor.getValue().getDescription());
    Assertions.assertSame(article, result.getLocalContext());
  }

  @Test
  public void should_reject_update_by_non_author() {
    GraphQLTestSupport.authenticate(stranger);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    Assertions.assertThrows(
        NoAuthorizationException.class,
        () ->
            articleMutation.updateArticle(
                article.getSlug(), UpdateArticleInput.newBuilder().title("x").build()));
    verifyNoInteractions(articleCommandService);
  }

  @Test
  public void should_fail_update_for_unknown_article() {
    when(articleRepository.findBySlug("unknown")).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class,
        () -> articleMutation.updateArticle("unknown", UpdateArticleInput.newBuilder().build()));
  }

  @Test
  public void should_favorite_article() {
    GraphQLTestSupport.authenticate(stranger);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle(article.getSlug());

    verify(articleFavoriteRepository).save(new ArticleFavorite(article.getId(), stranger.getId()));
    Assertions.assertSame(article, result.getLocalContext());
  }

  @Test
  public void should_fail_favorite_for_unknown_article() {
    GraphQLTestSupport.authenticate(user);
    when(articleRepository.findBySlug("unknown")).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class, () -> articleMutation.favoriteArticle("unknown"));
    verifyNoInteractions(articleFavoriteRepository);
  }

  @Test
  public void should_reject_anonymous_favorite() {
    GraphQLTestSupport.anonymous();

    Assertions.assertThrows(
        AuthenticationException.class, () -> articleMutation.favoriteArticle(article.getSlug()));
  }

  @Test
  public void should_unfavorite_article() {
    GraphQLTestSupport.authenticate(stranger);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), stranger.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), stranger.getId()))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(article.getSlug());

    verify(articleFavoriteRepository).remove(favorite);
    Assertions.assertSame(article, result.getLocalContext());
  }

  @Test
  public void should_ignore_unfavorite_when_not_favorited() {
    GraphQLTestSupport.authenticate(stranger);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), stranger.getId()))
        .thenReturn(Optional.empty());

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(article.getSlug());

    verify(articleFavoriteRepository, never()).remove(any());
    Assertions.assertSame(article, result.getLocalContext());
  }

  @Test
  public void should_delete_article_by_author() {
    GraphQLTestSupport.authenticate(user);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    DeletionStatus status = articleMutation.deleteArticle(article.getSlug());

    verify(articleRepository).remove(article);
    Assertions.assertTrue(status.getSuccess());
  }

  @Test
  public void should_reject_delete_by_non_author() {
    GraphQLTestSupport.authenticate(stranger);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    Assertions.assertThrows(
        NoAuthorizationException.class, () -> articleMutation.deleteArticle(article.getSlug()));
    verify(articleRepository, never()).remove(any());
  }
}
