package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class ArticleMutationTest {

  private ArticleCommandService articleCommandService;
  private ArticleFavoriteRepository articleFavoriteRepository;
  private ArticleRepository articleRepository;
  private ArticleMutation articleMutation;
  private User user;

  @BeforeEach
  public void setUp() {
    articleCommandService = mock(ArticleCommandService.class);
    articleFavoriteRepository = mock(ArticleFavoriteRepository.class);
    articleRepository = mock(ArticleRepository.class);
    articleMutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);

    user = new User("test@test.com", "testuser", "password", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(user, null));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void should_create_article() {
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Title")
            .description("Test Description")
            .body("Test Body")
            .tagList(Arrays.asList("java", "spring"))
            .build();

    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), user.getId());
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);
    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(article, result.getLocalContext());
  }

  @Test
  public void should_create_article_with_null_taglist() {
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Title")
            .description("Test Description")
            .body("Test Body")
            .build();

    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList(), user.getId());
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);
    assertNotNull(result);
  }

  @Test
  public void should_throw_when_not_logged_in_for_create() {
    SecurityContextHolder.clearContext();
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test")
            .description("desc")
            .body("body")
            .build();
    assertThrows(Exception.class, () -> articleMutation.createArticle(input));
  }

  @Test
  public void should_update_article() {
    Article article =
        new Article("Old Title", "Old Desc", "Old Body", Arrays.asList(), user.getId());
    when(articleRepository.findBySlug(eq("old-title"))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(any(), any())).thenReturn(article);

    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder()
            .title("New Title")
            .description("New Desc")
            .body("New Body")
            .build();

    DataFetcherResult<ArticlePayload> result =
        articleMutation.updateArticle("old-title", changes);
    assertNotNull(result);
  }

  @Test
  public void should_throw_when_updating_other_users_article() {
    User otherUser = new User("other@test.com", "other", "pass", "", "");
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList(), otherUser.getId());
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));

    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder().title("New Title").build();
    assertThrows(
        NoAuthorizationException.class,
        () -> articleMutation.updateArticle("title", changes));
  }

  @Test
  public void should_favorite_article() {
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList(), user.getId());
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle("title");
    assertNotNull(result);
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  public void should_unfavorite_article() {
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList(), user.getId());
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));

    ArticleFavorite fav = new ArticleFavorite(article.getId(), user.getId());
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.of(fav));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("title");
    assertNotNull(result);
    verify(articleFavoriteRepository).remove(fav);
  }

  @Test
  public void should_delete_article() {
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList(), user.getId());
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));

    DeletionStatus result = articleMutation.deleteArticle("title");
    assertTrue(result.getSuccess());
    verify(articleRepository).remove(article);
  }

  @Test
  public void should_throw_when_deleting_other_users_article() {
    User otherUser = new User("other@test.com", "other", "pass", "", "");
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList(), otherUser.getId());
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));

    assertThrows(
        NoAuthorizationException.class, () -> articleMutation.deleteArticle("title"));
  }

  @Test
  public void should_throw_when_article_not_found_for_update() {
    when(articleRepository.findBySlug(eq("not-found"))).thenReturn(Optional.empty());
    UpdateArticleInput changes = UpdateArticleInput.newBuilder().title("New").build();
    assertThrows(
        ResourceNotFoundException.class,
        () -> articleMutation.updateArticle("not-found", changes));
  }
}
