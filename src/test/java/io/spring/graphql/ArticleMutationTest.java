package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.article.NewArticleParam;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.ArticlePayload;
import io.spring.graphql.types.CreateArticleInput;
import io.spring.graphql.types.DeletionStatus;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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
    testUser = new User("test@example.com", "testuser", "password", "bio", "image");
    testArticle =
        new Article(
            "Test Title",
            "Test Description",
            "Test Body",
            Arrays.asList("tag1", "tag2"),
            testUser.getId());
  }

  @Test
  public void should_create_article_successfully() {
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Title")
            .description("Test Description")
            .body("Test Body")
            .tagList(Arrays.asList("tag1", "tag2"))
            .build();

    when(articleCommandService.createArticle(any(NewArticleParam.class), any()))
        .thenReturn(testArticle);

    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(testArticle, result.getLocalContext());
      verify(articleCommandService).createArticle(any(NewArticleParam.class), any());
    }
  }

  @Test
  public void should_fail_create_article_without_authentication() {
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Title")
            .description("Test Description")
            .body("Test Body")
            .build();

    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      assertThrows(AuthenticationException.class, () -> articleMutation.createArticle(input));
    }
  }

  @Test
  public void should_favorite_article_successfully() {
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(testArticle));

    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle("test-slug");

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(testArticle, result.getLocalContext());
      verify(articleFavoriteRepository).save(any());
    }
  }

  @Test
  public void should_fail_favorite_non_existent_article() {
    when(articleRepository.findBySlug(eq("non-existent"))).thenReturn(Optional.empty());

    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      assertThrows(
          ResourceNotFoundException.class, () -> articleMutation.favoriteArticle("non-existent"));
    }
  }

  @Test
  public void should_delete_article_successfully() {
    when(articleRepository.findBySlug(eq("test-slug"))).thenReturn(Optional.of(testArticle));

    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      DeletionStatus result = articleMutation.deleteArticle("test-slug");

      assertNotNull(result);
      assertTrue(result.getSuccess());
      verify(articleRepository).remove(any());
    }
  }

  @Test
  public void should_fail_delete_non_existent_article() {
    when(articleRepository.findBySlug(eq("non-existent"))).thenReturn(Optional.empty());

    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(testUser));

      assertThrows(
          ResourceNotFoundException.class, () -> articleMutation.deleteArticle("non-existent"));
    }
  }
}
