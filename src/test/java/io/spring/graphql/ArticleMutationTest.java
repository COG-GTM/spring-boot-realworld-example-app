package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class ArticleMutationTest {

  private ArticleCommandService articleCommandService;
  private ArticleFavoriteRepository articleFavoriteRepository;
  private ArticleRepository articleRepository;
  private ArticleMutation articleMutation;
  private User user;

  @BeforeEach
  void setUp() {
    articleCommandService = mock(ArticleCommandService.class);
    articleFavoriteRepository = mock(ArticleFavoriteRepository.class);
    articleRepository = mock(ArticleRepository.class);
    articleMutation = new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    user = new User("a@b.com", "testuser", "pass", "", "");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_article() {
    CreateArticleInput input = CreateArticleInput.newBuilder()
        .title("Test Article")
        .description("desc")
        .body("body")
        .tagList(Arrays.asList("java"))
        .build();
    Article article = new Article("Test Article", "desc", "body", Arrays.asList("java"), user.getId());
    when(articleCommandService.createArticle(any(), any())).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);
    assertNotNull(result);
    assertNotNull(result.getData());
  }

  @Test
  public void should_create_article_with_null_tags() {
    CreateArticleInput input = CreateArticleInput.newBuilder()
        .title("Test")
        .description("desc")
        .body("body")
        .build();
    Article article = new Article("Test", "desc", "body", Collections.emptyList(), user.getId());
    when(articleCommandService.createArticle(any(), any())).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);
    assertNotNull(result);
  }

  @Test
  public void should_update_article() {
    Article article = new Article("Old", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("old")).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(any(), any())).thenReturn(article);

    UpdateArticleInput input = UpdateArticleInput.newBuilder()
        .title("New")
        .body("new body")
        .description("new desc")
        .build();

    DataFetcherResult<ArticlePayload> result = articleMutation.updateArticle("old", input);
    assertNotNull(result);
  }

  @Test
  public void should_throw_when_update_article_not_found() {
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());
    UpdateArticleInput input = UpdateArticleInput.newBuilder().title("t").body("b").description("d").build();
    assertThrows(ResourceNotFoundException.class, () -> articleMutation.updateArticle("missing", input));
  }

  @Test
  public void should_throw_when_update_article_not_authorized() {
    User other = new User("c@d.com", "other", "pass", "", "");
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), other.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    UpdateArticleInput input = UpdateArticleInput.newBuilder().title("t").body("b").description("d").build();
    assertThrows(NoAuthorizationException.class, () -> articleMutation.updateArticle("title", input));
  }

  @Test
  public void should_favorite_article() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "other");
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle("title");
    assertNotNull(result);
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  public void should_unfavorite_article() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "other");
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    ArticleFavorite fav = new ArticleFavorite(article.getId(), user.getId());
    when(articleFavoriteRepository.find(article.getId(), user.getId())).thenReturn(Optional.of(fav));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("title");
    assertNotNull(result);
    verify(articleFavoriteRepository).remove(fav);
  }

  @Test
  public void should_unfavorite_article_when_not_favorited() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "other");
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId())).thenReturn(Optional.empty());

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("title");
    assertNotNull(result);
    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  public void should_delete_article() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    DeletionStatus result = articleMutation.deleteArticle("title");
    assertTrue(result.getSuccess());
    verify(articleRepository).remove(article);
  }

  @Test
  public void should_throw_when_delete_not_authorized() {
    User other = new User("c@d.com", "other", "pass", "", "");
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), other.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    assertThrows(NoAuthorizationException.class, () -> articleMutation.deleteArticle("title"));
  }

  @Test
  public void should_throw_when_create_not_authenticated() {
    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext().setAuthentication(null);
    CreateArticleInput input = CreateArticleInput.newBuilder()
        .title("t").description("d").body("b").build();
    assertThrows(Exception.class, () -> articleMutation.createArticle(input));
  }
}
