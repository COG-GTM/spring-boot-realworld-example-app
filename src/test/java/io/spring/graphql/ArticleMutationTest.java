package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.article.NewArticleParam;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ArticleMutationTest {

  @Mock private ArticleCommandService articleCommandService;
  @Mock private ArticleFavoriteRepository articleFavoriteRepository;
  @Mock private ArticleRepository articleRepository;

  private ArticleMutation articleMutation;
  private User user;

  @BeforeEach
  void setUp() {
    articleMutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    user = new User("test@example.com", "testuser", "password", "", "");
    TestingAuthenticationToken auth = new TestingAuthenticationToken(user, null);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_create_article() {
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Article")
            .description("Test desc")
            .body("Test body")
            .tagList(Arrays.asList("java", "spring"))
            .build();
    Article article =
        new Article(
            "Test Article",
            "Test desc",
            "Test body",
            Arrays.asList("java", "spring"),
            user.getId());
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    assertNotNull(result);
    assertEquals(article, result.getLocalContext());
  }

  @Test
  void should_create_article_with_null_tags() {
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Article")
            .description("Test desc")
            .body("Test body")
            .build();
    Article article =
        new Article(
            "Test Article", "Test desc", "Test body", Collections.emptyList(), user.getId());
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    assertNotNull(result);
  }

  @Test
  void should_update_article_by_owner() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    UpdateArticleInput input = UpdateArticleInput.newBuilder().title("New Title").build();
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.updateArticle("title", input);

    assertNotNull(result);
  }

  @Test
  void should_throw_when_updating_others_article() {
    User otherUser = new User("other@example.com", "other", "pass", "", "");
    Article article =
        new Article("Title", "desc", "body", Collections.emptyList(), otherUser.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    UpdateArticleInput input = UpdateArticleInput.newBuilder().title("New Title").build();

    assertThrows(
        NoAuthorizationException.class, () -> articleMutation.updateArticle("title", input));
  }

  @Test
  void should_throw_when_updating_nonexistent_article() {
    when(articleRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());
    UpdateArticleInput input = UpdateArticleInput.newBuilder().title("New Title").build();

    assertThrows(
        ResourceNotFoundException.class, () -> articleMutation.updateArticle("nonexistent", input));
  }

  @Test
  void should_favorite_article() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "otherId");
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle("title");

    assertNotNull(result);
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  void should_unfavorite_article() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), "otherId");
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("title");

    assertNotNull(result);
    verify(articleFavoriteRepository).remove(favorite);
  }

  @Test
  void should_delete_article_by_owner() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    DeletionStatus result = articleMutation.deleteArticle("title");

    assertTrue(result.getSuccess());
    verify(articleRepository).remove(article);
  }

  @Test
  void should_throw_when_deleting_others_article() {
    User otherUser = new User("other@example.com", "other", "pass", "", "");
    Article article =
        new Article("Title", "desc", "body", Collections.emptyList(), otherUser.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    assertThrows(NoAuthorizationException.class, () -> articleMutation.deleteArticle("title"));
  }
}
