package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleMutationTest {

  @Mock private ArticleCommandService articleCommandService;

  @Mock private ArticleFavoriteRepository articleFavoriteRepository;

  @Mock private ArticleRepository articleRepository;

  @InjectMocks private ArticleMutation articleMutation;

  private User testUser;
  private Article testArticle;

  @BeforeEach
  public void setUp() {
    testUser = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    testArticle = new Article("Test Title", "Test Description", "Test Body", Arrays.asList("tag1"), testUser.getId());
  }

  @Test
  public void should_create_article_successfully() {
    CreateArticleInput input = CreateArticleInput.newBuilder()
        .title("New Article")
        .description("Article Description")
        .body("Article Body")
        .tagList(Arrays.asList("java", "spring"))
        .build();

    Article createdArticle = new Article("New Article", "Article Description", "Article Body", Arrays.asList("java", "spring"), testUser.getId());
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(testUser))).thenReturn(createdArticle);

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

      Assertions.assertNotNull(result);
      Assertions.assertNotNull(result.getData());
      Assertions.assertEquals(createdArticle, result.getLocalContext());
      verify(articleCommandService).createArticle(any(NewArticleParam.class), eq(testUser));
    }
  }

  @Test
  public void should_throw_exception_when_creating_article_without_authentication() {
    CreateArticleInput input = CreateArticleInput.newBuilder()
        .title("New Article")
        .description("Description")
        .body("Body")
        .build();

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      Assertions.assertThrows(AuthenticationException.class, () -> {
        articleMutation.createArticle(input);
      });
    }
  }

  @Test
  public void should_update_article_successfully() {
    String slug = "test-title";
    UpdateArticleInput input = UpdateArticleInput.newBuilder()
        .title("Updated Title")
        .description("Updated Description")
        .body("Updated Body")
        .build();

    when(articleRepository.findBySlug(slug)).thenReturn(Optional.of(testArticle));
    when(articleCommandService.updateArticle(eq(testArticle), any(UpdateArticleParam.class))).thenReturn(testArticle);

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      DataFetcherResult<ArticlePayload> result = articleMutation.updateArticle(slug, input);

      Assertions.assertNotNull(result);
      Assertions.assertEquals(testArticle, result.getLocalContext());
      verify(articleCommandService).updateArticle(eq(testArticle), any(UpdateArticleParam.class));
    }
  }

  @Test
  public void should_throw_exception_when_updating_nonexistent_article() {
    String slug = "nonexistent-slug";
    UpdateArticleInput input = UpdateArticleInput.newBuilder()
        .title("Updated Title")
        .build();

    when(articleRepository.findBySlug(slug)).thenReturn(Optional.empty());

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      Assertions.assertThrows(ResourceNotFoundException.class, () -> {
        articleMutation.updateArticle(slug, input);
      });
    }
  }

  @Test
  public void should_favorite_article_successfully() {
    String slug = "test-title";
    when(articleRepository.findBySlug(slug)).thenReturn(Optional.of(testArticle));

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle(slug);

      Assertions.assertNotNull(result);
      Assertions.assertEquals(testArticle, result.getLocalContext());
      verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
    }
  }

  @Test
  public void should_unfavorite_article_successfully() {
    String slug = "test-title";
    ArticleFavorite favorite = new ArticleFavorite(testArticle.getId(), testUser.getId());
    
    when(articleRepository.findBySlug(slug)).thenReturn(Optional.of(testArticle));
    when(articleFavoriteRepository.find(testArticle.getId(), testUser.getId())).thenReturn(Optional.of(favorite));

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(slug);

      Assertions.assertNotNull(result);
      Assertions.assertEquals(testArticle, result.getLocalContext());
      verify(articleFavoriteRepository).remove(favorite);
    }
  }

  @Test
  public void should_delete_article_successfully() {
    String slug = "test-title";
    when(articleRepository.findBySlug(slug)).thenReturn(Optional.of(testArticle));

    try (MockedStatic<SecurityUtil> mockedSecurityUtil = Mockito.mockStatic(SecurityUtil.class)) {
      mockedSecurityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      DeletionStatus result = articleMutation.deleteArticle(slug);

      Assertions.assertNotNull(result);
      Assertions.assertTrue(result.getSuccess());
      verify(articleRepository).remove(testArticle);
    }
  }
}
