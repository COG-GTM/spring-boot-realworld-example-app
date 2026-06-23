package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

class ArticleMutationTest {

  private ArticleCommandService articleCommandService;
  private ArticleFavoriteRepository articleFavoriteRepository;
  private ArticleRepository articleRepository;
  private ArticleMutation mutation;
  private User user;

  @BeforeEach
  void setUp() {
    articleCommandService = mock(ArticleCommandService.class);
    articleFavoriteRepository = mock(ArticleFavoriteRepository.class);
    articleRepository = mock(ArticleRepository.class);
    mutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    user = new User("user@test.com", "user", "123", "bio", "image");
  }

  @AfterEach
  void tearDown() {
    GraphQLTestSecurity.clear();
  }

  private Article article() {
    return new Article("title", "desc", "body", Collections.singletonList("java"), user.getId());
  }

  @Test
  void createArticle_returns_payload_with_article_context() {
    GraphQLTestSecurity.login(user);
    CreateArticleInput input =
        new CreateArticleInput("body", "desc", Arrays.asList("java"), "title");
    Article article = article();
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = mutation.createArticle(input);

    assertNotNull(result.getData());
    org.junit.jupiter.api.Assertions.assertEquals(article, result.getLocalContext());
  }

  @Test
  void createArticle_defaults_null_tag_list() {
    GraphQLTestSecurity.login(user);
    CreateArticleInput input = new CreateArticleInput("body", "desc", null, "title");
    Article article = article();
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = mutation.createArticle(input);

    assertNotNull(result.getData());
  }

  @Test
  void createArticle_when_anonymous_throws_authentication() {
    GraphQLTestSecurity.anonymous();
    CreateArticleInput input =
        new CreateArticleInput("body", "desc", Arrays.asList("java"), "title");

    assertThrows(AuthenticationException.class, () -> mutation.createArticle(input));
  }

  @Test
  void updateArticle_when_author_updates_successfully() {
    GraphQLTestSecurity.login(user);
    Article article = article();
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(article);
    UpdateArticleInput changes = new UpdateArticleInput("newbody", "newdesc", "newtitle");

    DataFetcherResult<ArticlePayload> result = mutation.updateArticle("slug", changes);

    assertNotNull(result.getData());
    org.junit.jupiter.api.Assertions.assertEquals(article, result.getLocalContext());
  }

  @Test
  void updateArticle_when_article_missing_throws_not_found() {
    GraphQLTestSecurity.login(user);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.empty());
    UpdateArticleInput changes = new UpdateArticleInput("b", "d", "t");

    assertThrows(ResourceNotFoundException.class, () -> mutation.updateArticle("slug", changes));
  }

  @Test
  void updateArticle_when_anonymous_throws_authentication() {
    GraphQLTestSecurity.anonymous();
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article()));
    UpdateArticleInput changes = new UpdateArticleInput("b", "d", "t");

    assertThrows(AuthenticationException.class, () -> mutation.updateArticle("slug", changes));
  }

  @Test
  void updateArticle_when_not_author_throws_no_authorization() {
    User other = new User("other@test.com", "other", "123", "", "");
    GraphQLTestSecurity.login(other);
    Article article = article();
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    UpdateArticleInput changes = new UpdateArticleInput("b", "d", "t");

    assertThrows(NoAuthorizationException.class, () -> mutation.updateArticle("slug", changes));
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  void favoriteArticle_saves_favorite() {
    GraphQLTestSecurity.login(user);
    Article article = article();
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = mutation.favoriteArticle("slug");

    assertNotNull(result.getData());
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  void favoriteArticle_when_anonymous_throws_authentication() {
    GraphQLTestSecurity.anonymous();
    assertThrows(AuthenticationException.class, () -> mutation.favoriteArticle("slug"));
  }

  @Test
  void favoriteArticle_when_article_missing_throws_not_found() {
    GraphQLTestSecurity.login(user);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> mutation.favoriteArticle("slug"));
  }

  @Test
  void unfavoriteArticle_removes_existing_favorite() {
    GraphQLTestSecurity.login(user);
    Article article = article();
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = mutation.unfavoriteArticle("slug");

    assertNotNull(result.getData());
    verify(articleFavoriteRepository).remove(favorite);
  }

  @Test
  void unfavoriteArticle_when_no_favorite_present_does_not_remove() {
    GraphQLTestSecurity.login(user);
    Article article = article();
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.empty());

    DataFetcherResult<ArticlePayload> result = mutation.unfavoriteArticle("slug");

    assertNotNull(result.getData());
    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  void unfavoriteArticle_when_anonymous_throws_authentication() {
    GraphQLTestSecurity.anonymous();
    assertThrows(AuthenticationException.class, () -> mutation.unfavoriteArticle("slug"));
  }

  @Test
  void deleteArticle_when_author_removes_article() {
    GraphQLTestSecurity.login(user);
    Article article = article();
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));

    DeletionStatus status = mutation.deleteArticle("slug");

    assertTrue(status.getSuccess());
    verify(articleRepository).remove(article);
  }

  @Test
  void deleteArticle_when_anonymous_throws_authentication() {
    GraphQLTestSecurity.anonymous();
    assertThrows(AuthenticationException.class, () -> mutation.deleteArticle("slug"));
  }

  @Test
  void deleteArticle_when_article_missing_throws_not_found() {
    GraphQLTestSecurity.login(user);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> mutation.deleteArticle("slug"));
  }

  @Test
  void deleteArticle_when_not_author_throws_no_authorization() {
    User other = new User("other@test.com", "other", "123", "", "");
    GraphQLTestSecurity.login(other);
    Article article = article();
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));

    assertThrows(NoAuthorizationException.class, () -> mutation.deleteArticle("slug"));
    verify(articleRepository, never()).remove(any());
  }
}
