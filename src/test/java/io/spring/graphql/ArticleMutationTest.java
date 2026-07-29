package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleMutationTest {

  @Mock private ArticleCommandService articleCommandService;
  @Mock private ArticleFavoriteRepository articleFavoriteRepository;
  @Mock private ArticleRepository articleRepository;

  @Captor private ArgumentCaptor<NewArticleParam> newArticleParamCaptor;
  @Captor private ArgumentCaptor<UpdateArticleParam> updateArticleParamCaptor;

  @InjectMocks private ArticleMutation articleMutation;

  private final User currentUser = new User("jake@jake.jake", "jake", "123", "bio", "image");

  @AfterEach
  void tearDown() {
    SecurityContextHelper.clear();
  }

  @Test
  public void should_create_article_with_tag_list() {
    SecurityContextHelper.authenticate(currentUser);
    Article article = ownedArticle();
    when(articleCommandService.createArticle(newArticleParamCaptor.capture(), eq(currentUser)))
        .thenReturn(article);

    DataFetcherResult<ArticlePayload> result =
        articleMutation.createArticle(
            CreateArticleInput.newBuilder()
                .title("a title")
                .description("desc")
                .body("body")
                .tagList(Arrays.asList("java", "spring"))
                .build());

    assertEquals(Arrays.asList("java", "spring"), newArticleParamCaptor.getValue().getTagList());
    assertEquals("a title", newArticleParamCaptor.getValue().getTitle());
    assertSame(article, result.getLocalContext());
  }

  @Test
  public void should_create_article_with_empty_tag_list_when_absent() {
    SecurityContextHelper.authenticate(currentUser);
    when(articleCommandService.createArticle(newArticleParamCaptor.capture(), eq(currentUser)))
        .thenReturn(ownedArticle());

    articleMutation.createArticle(
        CreateArticleInput.newBuilder().title("a title").description("desc").body("body").build());

    assertEquals(Collections.emptyList(), newArticleParamCaptor.getValue().getTagList());
  }

  @Test
  public void should_reject_article_creation_for_anonymous_user() {
    SecurityContextHelper.anonymous();

    assertThrows(
        AuthenticationException.class,
        () -> articleMutation.createArticle(CreateArticleInput.newBuilder().build()));
  }

  @Test
  public void should_update_owned_article() {
    SecurityContextHelper.authenticate(currentUser);
    Article article = ownedArticle();
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), updateArticleParamCaptor.capture()))
        .thenReturn(article);

    DataFetcherResult<ArticlePayload> result =
        articleMutation.updateArticle(
            "a-title",
            UpdateArticleInput.newBuilder()
                .title("new title")
                .body("new body")
                .description("new desc")
                .build());

    UpdateArticleParam param = updateArticleParamCaptor.getValue();
    assertEquals("new title", param.getTitle());
    assertEquals("new body", param.getBody());
    assertEquals("new desc", param.getDescription());
    assertSame(article, result.getLocalContext());
  }

  @Test
  public void should_throw_when_updating_unknown_article() {
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleMutation.updateArticle("missing", UpdateArticleInput.newBuilder().build()));
  }

  @Test
  public void should_reject_updating_article_of_another_user() {
    SecurityContextHelper.authenticate(currentUser);
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(otherUsersArticle()));

    assertThrows(
        NoAuthorizationException.class,
        () -> articleMutation.updateArticle("a-title", UpdateArticleInput.newBuilder().build()));
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  public void should_favorite_article() {
    SecurityContextHelper.authenticate(currentUser);
    Article article = otherUsersArticle();
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));
    ArgumentCaptor<ArticleFavorite> captor = ArgumentCaptor.forClass(ArticleFavorite.class);

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle("a-title");

    verify(articleFavoriteRepository).save(captor.capture());
    assertEquals(article.getId(), captor.getValue().getArticleId());
    assertEquals(currentUser.getId(), captor.getValue().getUserId());
    assertSame(article, result.getLocalContext());
  }

  @Test
  public void should_throw_when_favoriting_unknown_article() {
    SecurityContextHelper.authenticate(currentUser);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleMutation.favoriteArticle("missing"));
  }

  @Test
  public void should_reject_favoriting_for_anonymous_user() {
    SecurityContextHelper.anonymous();

    assertThrows(AuthenticationException.class, () -> articleMutation.favoriteArticle("a-title"));
  }

  @Test
  public void should_remove_existing_favorite() {
    SecurityContextHelper.authenticate(currentUser);
    Article article = otherUsersArticle();
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), currentUser.getId());
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), currentUser.getId()))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("a-title");

    verify(articleFavoriteRepository).remove(favorite);
    assertSame(article, result.getLocalContext());
  }

  @Test
  public void should_ignore_unfavorite_when_no_favorite_exists() {
    SecurityContextHelper.authenticate(currentUser);
    Article article = otherUsersArticle();
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), currentUser.getId()))
        .thenReturn(Optional.empty());

    articleMutation.unfavoriteArticle("a-title");

    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  public void should_delete_owned_article() {
    SecurityContextHelper.authenticate(currentUser);
    Article article = ownedArticle();
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));

    DeletionStatus status = articleMutation.deleteArticle("a-title");

    verify(articleRepository).remove(article);
    assertTrue(status.getSuccess());
  }

  @Test
  public void should_reject_deleting_article_of_another_user() {
    SecurityContextHelper.authenticate(currentUser);
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(otherUsersArticle()));

    assertThrows(NoAuthorizationException.class, () -> articleMutation.deleteArticle("a-title"));
    verify(articleRepository, never()).remove(any());
  }

  @Test
  public void should_throw_when_deleting_unknown_article() {
    SecurityContextHelper.authenticate(currentUser);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleMutation.deleteArticle("missing"));
  }

  private Article ownedArticle() {
    return new Article(
        "a title", "desc", "body", Collections.singletonList("java"), currentUser.getId());
  }

  private Article otherUsersArticle() {
    return new Article("a title", "desc", "body", Collections.singletonList("java"), "other-id");
  }
}
