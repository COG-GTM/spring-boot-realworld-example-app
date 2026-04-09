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
    user = new User("test@test.com", "testuser", "password", "bio", "image");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAuthentication(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(user, null));
  }

  private void setAnonymousAuthentication() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new org.springframework.security.authentication.AnonymousAuthenticationToken(
                "key",
                "anonymous",
                Collections.singletonList(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_ANONYMOUS"))));
  }

  // ---- createArticle tests ----

  @Test
  void createArticle_withValidInput_shouldReturnArticlePayload() {
    setAuthentication(user);
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Title")
            .description("Test Description")
            .body("Test Body")
            .tagList(Arrays.asList("java", "spring"))
            .build();
    Article article =
        new Article(
            "Test Title", "Test Description", "Test Body", Arrays.asList("java", "spring"),
            user.getId());
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertNotNull(result.getLocalContext());
    assertEquals(article, result.getLocalContext());
    verify(articleCommandService).createArticle(any(), eq(user));
  }

  @Test
  void createArticle_withNullTagList_shouldUseEmptyList() {
    setAuthentication(user);
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Title")
            .description("Test Description")
            .body("Test Body")
            .build();
    Article article =
        new Article(
            "Test Title", "Test Description", "Test Body", Collections.emptyList(), user.getId());
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    assertNotNull(result);
    verify(articleCommandService).createArticle(any(), eq(user));
  }

  @Test
  void createArticle_withNoAuthentication_shouldThrowAuthenticationException() {
    setAnonymousAuthentication();
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Title")
            .description("Test Description")
            .body("Test Body")
            .build();

    assertThrows(AuthenticationException.class, () -> articleMutation.createArticle(input));
  }

  // ---- updateArticle tests ----

  @Test
  void updateArticle_withValidInput_shouldReturnUpdatedArticlePayload() {
    setAuthentication(user);
    Article article =
        new Article(
            "Old Title",
            "Old Description",
            "Old Body",
            Arrays.asList("java"),
            user.getId());
    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder()
            .title("New Title")
            .description("New Description")
            .body("New Body")
            .build();
    Article updatedArticle =
        new Article(
            "New Title",
            "New Description",
            "New Body",
            Arrays.asList("java"),
            user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(updatedArticle);

    DataFetcherResult<ArticlePayload> result =
        articleMutation.updateArticle(article.getSlug(), changes);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(updatedArticle, result.getLocalContext());
    verify(articleCommandService).updateArticle(eq(article), any());
  }

  @Test
  void updateArticle_withNonExistentArticle_shouldThrowResourceNotFoundException() {
    setAuthentication(user);
    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder().title("New Title").build();
    when(articleRepository.findBySlug("non-existent")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleMutation.updateArticle("non-existent", changes));
  }

  @Test
  void updateArticle_withNoAuthentication_shouldThrowAuthenticationException() {
    setAnonymousAuthentication();
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList("java"), user.getId());
    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder().title("New Title").build();
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    assertThrows(
        AuthenticationException.class,
        () -> articleMutation.updateArticle(article.getSlug(), changes));
  }

  @Test
  void updateArticle_withUnauthorizedUser_shouldThrowNoAuthorizationException() {
    User anotherUser = new User("other@test.com", "otheruser", "password", "bio", "image");
    setAuthentication(anotherUser);
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList("java"), user.getId());
    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder().title("New Title").build();
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    assertThrows(
        NoAuthorizationException.class,
        () -> articleMutation.updateArticle(article.getSlug(), changes));
  }

  // ---- favoriteArticle tests ----

  @Test
  void favoriteArticle_withValidSlug_shouldSaveFavoriteAndReturnPayload() {
    setAuthentication(user);
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result =
        articleMutation.favoriteArticle(article.getSlug());

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(article, result.getLocalContext());
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  void favoriteArticle_withNonExistentArticle_shouldThrowResourceNotFoundException() {
    setAuthentication(user);
    when(articleRepository.findBySlug("non-existent")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleMutation.favoriteArticle("non-existent"));
  }

  @Test
  void favoriteArticle_withNoAuthentication_shouldThrowAuthenticationException() {
    setAnonymousAuthentication();

    assertThrows(
        AuthenticationException.class, () -> articleMutation.favoriteArticle("some-slug"));
  }

  // ---- unfavoriteArticle tests ----

  @Test
  void unfavoriteArticle_withExistingFavorite_shouldRemoveFavoriteAndReturnPayload() {
    setAuthentication(user);
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList("java"), user.getId());
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result =
        articleMutation.unfavoriteArticle(article.getSlug());

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(article, result.getLocalContext());
    verify(articleFavoriteRepository).remove(eq(favorite));
  }

  @Test
  void unfavoriteArticle_withNoExistingFavorite_shouldNotRemoveAndReturnPayload() {
    setAuthentication(user);
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.empty());

    DataFetcherResult<ArticlePayload> result =
        articleMutation.unfavoriteArticle(article.getSlug());

    assertNotNull(result);
    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  void unfavoriteArticle_withNonExistentArticle_shouldThrowResourceNotFoundException() {
    setAuthentication(user);
    when(articleRepository.findBySlug("non-existent")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleMutation.unfavoriteArticle("non-existent"));
  }

  @Test
  void unfavoriteArticle_withNoAuthentication_shouldThrowAuthenticationException() {
    setAnonymousAuthentication();

    assertThrows(
        AuthenticationException.class, () -> articleMutation.unfavoriteArticle("some-slug"));
  }

  // ---- deleteArticle tests ----

  @Test
  void deleteArticle_withAuthorizedUser_shouldDeleteAndReturnSuccess() {
    setAuthentication(user);
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    DeletionStatus result = articleMutation.deleteArticle(article.getSlug());

    assertNotNull(result);
    assertTrue(result.getSuccess());
    verify(articleRepository).remove(eq(article));
  }

  @Test
  void deleteArticle_withNonExistentArticle_shouldThrowResourceNotFoundException() {
    setAuthentication(user);
    when(articleRepository.findBySlug("non-existent")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleMutation.deleteArticle("non-existent"));
  }

  @Test
  void deleteArticle_withNoAuthentication_shouldThrowAuthenticationException() {
    setAnonymousAuthentication();

    assertThrows(
        AuthenticationException.class, () -> articleMutation.deleteArticle("some-slug"));
  }

  @Test
  void deleteArticle_withUnauthorizedUser_shouldThrowNoAuthorizationException() {
    User anotherUser = new User("other@test.com", "otheruser", "password", "bio", "image");
    setAuthentication(anotherUser);
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    assertThrows(
        NoAuthorizationException.class,
        () -> articleMutation.deleteArticle(article.getSlug()));
    verify(articleRepository, never()).remove(any());
  }
}
