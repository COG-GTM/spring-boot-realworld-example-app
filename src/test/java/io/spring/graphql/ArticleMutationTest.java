package io.spring.graphql;

import static java.util.Arrays.asList;
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
  private Article article;

  @BeforeEach
  void setUp() {
    articleMutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    user = new User("john@jacob.com", "johnjacob", "123", "", "https://example.com/avatar.jpg");
    article =
        new Article(
            "Test Article", "A test description", "Article body", asList("java"), user.getId());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAuthenticatedUser(User u) {
    TestingAuthenticationToken auth = new TestingAuthenticationToken(u, null);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  // ========== createArticle tests ==========

  @Test
  void createArticle_success() {
    setAuthenticatedUser(user);
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Article")
            .description("A test description")
            .body("Article body")
            .tagList(asList("java", "spring"))
            .build();

    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertNotNull(result.getLocalContext());
    assertEquals(article, result.getLocalContext());
    verify(articleCommandService).createArticle(any(), eq(user));
  }

  @Test
  void createArticle_withNullTagList_usesEmptyList() {
    setAuthenticatedUser(user);
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Article")
            .description("A test description")
            .body("Article body")
            .build();

    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    assertNotNull(result);
    verify(articleCommandService).createArticle(any(), eq(user));
  }

  @Test
  void createArticle_withNoAuth_throwsAuthenticationException() {
    SecurityContextHolder.clearContext();
    TestingAuthenticationToken anonAuth = new TestingAuthenticationToken(null, null);
    SecurityContextHolder.getContext().setAuthentication(anonAuth);

    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Article")
            .description("A test description")
            .body("Article body")
            .build();

    assertThrows(AuthenticationException.class, () -> articleMutation.createArticle(input));
  }

  @Test
  void createArticle_withTagList_passesTagsToService() {
    setAuthenticatedUser(user);
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Article")
            .description("A test description")
            .body("Article body")
            .tagList(asList("java", "spring", "graphql"))
            .build();

    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    articleMutation.createArticle(input);

    verify(articleCommandService).createArticle(any(), eq(user));
  }

  // ========== updateArticle tests ==========

  @Test
  void updateArticle_success() {
    setAuthenticatedUser(user);
    when(articleRepository.findBySlug("test-article")).thenReturn(Optional.of(article));

    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder()
            .title("Updated Title")
            .description("Updated description")
            .body("Updated body")
            .build();

    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(article);

    DataFetcherResult<ArticlePayload> result =
        articleMutation.updateArticle("test-article", changes);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertNotNull(result.getLocalContext());
    verify(articleCommandService).updateArticle(eq(article), any());
  }

  @Test
  void updateArticle_articleNotFound_throwsResourceNotFoundException() {
    setAuthenticatedUser(user);
    when(articleRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

    UpdateArticleInput changes = UpdateArticleInput.newBuilder().title("Updated Title").build();

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleMutation.updateArticle("nonexistent", changes));
  }

  @Test
  void updateArticle_withNoAuth_throwsAuthenticationException() {
    SecurityContextHolder.clearContext();
    TestingAuthenticationToken anonAuth = new TestingAuthenticationToken(null, null);
    SecurityContextHolder.getContext().setAuthentication(anonAuth);

    when(articleRepository.findBySlug("test-article")).thenReturn(Optional.of(article));

    UpdateArticleInput changes = UpdateArticleInput.newBuilder().title("Updated Title").build();

    assertThrows(
        AuthenticationException.class,
        () -> articleMutation.updateArticle("test-article", changes));
  }

  @Test
  void updateArticle_notAuthor_throwsNoAuthorizationException() {
    User otherUser =
        new User("other@user.com", "otheruser", "123", "", "https://example.com/other.jpg");
    setAuthenticatedUser(otherUser);
    when(articleRepository.findBySlug("test-article")).thenReturn(Optional.of(article));

    UpdateArticleInput changes = UpdateArticleInput.newBuilder().title("Updated Title").build();

    assertThrows(
        NoAuthorizationException.class,
        () -> articleMutation.updateArticle("test-article", changes));
  }

  // ========== favoriteArticle tests ==========

  @Test
  void favoriteArticle_success() {
    setAuthenticatedUser(user);
    when(articleRepository.findBySlug("test-article")).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle("test-article");

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(article, result.getLocalContext());
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  void favoriteArticle_articleNotFound_throwsResourceNotFoundException() {
    setAuthenticatedUser(user);
    when(articleRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleMutation.favoriteArticle("nonexistent"));
  }

  @Test
  void favoriteArticle_withNoAuth_throwsAuthenticationException() {
    SecurityContextHolder.clearContext();
    TestingAuthenticationToken anonAuth = new TestingAuthenticationToken(null, null);
    SecurityContextHolder.getContext().setAuthentication(anonAuth);

    assertThrows(
        AuthenticationException.class, () -> articleMutation.favoriteArticle("test-article"));
  }

  // ========== unfavoriteArticle tests ==========

  @Test
  void unfavoriteArticle_success_withExistingFavorite() {
    setAuthenticatedUser(user);
    when(articleRepository.findBySlug("test-article")).thenReturn(Optional.of(article));
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("test-article");

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(article, result.getLocalContext());
    verify(articleFavoriteRepository).remove(favorite);
  }

  @Test
  void unfavoriteArticle_success_withNoExistingFavorite() {
    setAuthenticatedUser(user);
    when(articleRepository.findBySlug("test-article")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.empty());

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("test-article");

    assertNotNull(result);
    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  void unfavoriteArticle_articleNotFound_throwsResourceNotFoundException() {
    setAuthenticatedUser(user);
    when(articleRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleMutation.unfavoriteArticle("nonexistent"));
  }

  @Test
  void unfavoriteArticle_withNoAuth_throwsAuthenticationException() {
    SecurityContextHolder.clearContext();
    TestingAuthenticationToken anonAuth = new TestingAuthenticationToken(null, null);
    SecurityContextHolder.getContext().setAuthentication(anonAuth);

    assertThrows(
        AuthenticationException.class, () -> articleMutation.unfavoriteArticle("test-article"));
  }

  // ========== deleteArticle tests ==========

  @Test
  void deleteArticle_success() {
    setAuthenticatedUser(user);
    when(articleRepository.findBySlug("test-article")).thenReturn(Optional.of(article));

    DeletionStatus result = articleMutation.deleteArticle("test-article");

    assertNotNull(result);
    assertTrue(result.getSuccess());
    verify(articleRepository).remove(article);
  }

  @Test
  void deleteArticle_articleNotFound_throwsResourceNotFoundException() {
    setAuthenticatedUser(user);
    when(articleRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleMutation.deleteArticle("nonexistent"));
  }

  @Test
  void deleteArticle_withNoAuth_throwsAuthenticationException() {
    SecurityContextHolder.clearContext();
    TestingAuthenticationToken anonAuth = new TestingAuthenticationToken(null, null);
    SecurityContextHolder.getContext().setAuthentication(anonAuth);

    assertThrows(
        AuthenticationException.class, () -> articleMutation.deleteArticle("test-article"));
  }

  @Test
  void deleteArticle_notAuthor_throwsNoAuthorizationException() {
    User otherUser =
        new User("other@user.com", "otheruser", "123", "", "https://example.com/other.jpg");
    setAuthenticatedUser(otherUser);
    when(articleRepository.findBySlug("test-article")).thenReturn(Optional.of(article));

    assertThrows(
        NoAuthorizationException.class, () -> articleMutation.deleteArticle("test-article"));
  }
}
