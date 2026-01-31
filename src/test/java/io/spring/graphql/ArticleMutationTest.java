package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArticleMutationTest {

  @Mock private ArticleCommandService articleCommandService;
  @Mock private ArticleFavoriteRepository articleFavoriteRepository;
  @Mock private ArticleRepository articleRepository;

  private ArticleMutation articleMutation;
  private User user;
  private Article article;

  @BeforeEach
  void setUp() {
    articleMutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    article = new Article("Test Title", "description", "body", Arrays.asList("tag1"), user.getId());
  }

  @Test
  void createArticle_withValidInput_returnsArticlePayload() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      CreateArticleInput input =
          CreateArticleInput.newBuilder()
              .title("Test Title")
              .description("Test Description")
              .body("Test Body")
              .tagList(Arrays.asList("tag1", "tag2"))
              .build();

      when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
          .thenReturn(article);

      DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(article, result.getLocalContext());
      verify(articleCommandService).createArticle(any(NewArticleParam.class), eq(user));
    }
  }

  @Test
  void createArticle_withNullTagList_returnsArticlePayload() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      CreateArticleInput input =
          CreateArticleInput.newBuilder()
              .title("Test Title")
              .description("Test Description")
              .body("Test Body")
              .tagList(null)
              .build();

      when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
          .thenReturn(article);

      DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

      assertNotNull(result);
      verify(articleCommandService).createArticle(any(NewArticleParam.class), eq(user));
    }
  }

  @Test
  void createArticle_withoutAuthentication_throwsAuthenticationException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      CreateArticleInput input =
          CreateArticleInput.newBuilder()
              .title("Test Title")
              .description("Test Description")
              .body("Test Body")
              .build();

      assertThrows(AuthenticationException.class, () -> articleMutation.createArticle(input));
    }
  }

  @Test
  void updateArticle_withValidInput_returnsArticlePayload() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));
      when(articleCommandService.updateArticle(eq(article), any(UpdateArticleParam.class)))
          .thenReturn(article);

      UpdateArticleInput params =
          UpdateArticleInput.newBuilder()
              .title("Updated Title")
              .body("Updated Body")
              .description("Updated Description")
              .build();

      DataFetcherResult<ArticlePayload> result =
          articleMutation.updateArticle("test-slug", params);

      assertNotNull(result);
      assertNotNull(result.getData());
      verify(articleCommandService).updateArticle(eq(article), any(UpdateArticleParam.class));
    }
  }

  @Test
  void updateArticle_articleNotFound_throwsResourceNotFoundException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

      UpdateArticleInput params = UpdateArticleInput.newBuilder().title("Updated Title").build();

      assertThrows(
          ResourceNotFoundException.class,
          () -> articleMutation.updateArticle("nonexistent", params));
    }
  }

  @Test
  void updateArticle_withoutAuthentication_throwsAuthenticationException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));

      UpdateArticleInput params = UpdateArticleInput.newBuilder().title("Updated Title").build();

      assertThrows(
          AuthenticationException.class,
          () -> articleMutation.updateArticle("test-slug", params));
    }
  }

  @Test
  void updateArticle_withoutAuthorization_throwsNoAuthorizationException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      User anotherUser = new User("other@test.com", "otheruser", "password", "bio", "image");
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(anotherUser));

      when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));

      UpdateArticleInput params = UpdateArticleInput.newBuilder().title("Updated Title").build();

      assertThrows(
          NoAuthorizationException.class,
          () -> articleMutation.updateArticle("test-slug", params));
    }
  }

  @Test
  void favoriteArticle_withValidSlug_returnsArticlePayload() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));

      DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle("test-slug");

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(article, result.getLocalContext());
      verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
    }
  }

  @Test
  void favoriteArticle_articleNotFound_throwsResourceNotFoundException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class, () -> articleMutation.favoriteArticle("nonexistent"));
    }
  }

  @Test
  void favoriteArticle_withoutAuthentication_throwsAuthenticationException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      assertThrows(
          AuthenticationException.class, () -> articleMutation.favoriteArticle("test-slug"));
    }
  }

  @Test
  void unfavoriteArticle_withValidSlug_returnsArticlePayload() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));
      ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
      when(articleFavoriteRepository.find(article.getId(), user.getId()))
          .thenReturn(Optional.of(favorite));

      DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("test-slug");

      assertNotNull(result);
      assertNotNull(result.getData());
      assertEquals(article, result.getLocalContext());
      verify(articleFavoriteRepository).remove(favorite);
    }
  }

  @Test
  void unfavoriteArticle_favoriteNotFound_returnsArticlePayloadWithoutRemoval() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));
      when(articleFavoriteRepository.find(article.getId(), user.getId()))
          .thenReturn(Optional.empty());

      DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("test-slug");

      assertNotNull(result);
      verify(articleFavoriteRepository, never()).remove(any());
    }
  }

  @Test
  void unfavoriteArticle_articleNotFound_throwsResourceNotFoundException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class, () -> articleMutation.unfavoriteArticle("nonexistent"));
    }
  }

  @Test
  void unfavoriteArticle_withoutAuthentication_throwsAuthenticationException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      assertThrows(
          AuthenticationException.class, () -> articleMutation.unfavoriteArticle("test-slug"));
    }
  }

  @Test
  void deleteArticle_withValidSlug_returnsDeletionStatus() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));

      DeletionStatus result = articleMutation.deleteArticle("test-slug");

      assertNotNull(result);
      assertTrue(result.getSuccess());
      verify(articleRepository).remove(article);
    }
  }

  @Test
  void deleteArticle_articleNotFound_throwsResourceNotFoundException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(user));

      when(articleRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class, () -> articleMutation.deleteArticle("nonexistent"));
    }
  }

  @Test
  void deleteArticle_withoutAuthentication_throwsAuthenticationException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.empty());

      assertThrows(
          AuthenticationException.class, () -> articleMutation.deleteArticle("test-slug"));
    }
  }

  @Test
  void deleteArticle_withoutAuthorization_throwsNoAuthorizationException() {
    try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
      User anotherUser = new User("other@test.com", "otheruser", "password", "bio", "image");
      securityUtil.when(SecurityUtil::getCurrentUser).thenReturn(Optional.of(anotherUser));

      when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));

      assertThrows(
          NoAuthorizationException.class, () -> articleMutation.deleteArticle("test-slug"));
    }
  }
}
