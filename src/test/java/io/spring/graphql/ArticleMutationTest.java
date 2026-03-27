package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ArticleMutationTest {

  @Mock private ArticleCommandService articleCommandService;

  @Mock private ArticleFavoriteRepository articleFavoriteRepository;

  @Mock private ArticleRepository articleRepository;

  @InjectMocks private ArticleMutation articleMutation;

  private User user;
  private Article article;

  @BeforeEach
  public void setUp() {
    SecurityContextHolder.clearContext();
    user = new User("test@example.com", "testuser", "password", "bio", "image");
    article =
        new Article(
            "Test Title",
            "Test Description",
            "Test Body",
            Arrays.asList("tag1", "tag2"),
            user.getId(),
            new DateTime());
  }

  @Test
  public void should_create_article_successfully() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Title")
            .description("Test Description")
            .body("Test Body")
            .tagList(Arrays.asList("tag1", "tag2"))
            .build();

    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(article, result.getLocalContext());
    verify(articleCommandService).createArticle(any(), eq(user));
  }

  @Test
  public void should_create_article_with_empty_tag_list() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Title")
            .description("Test Description")
            .body("Test Body")
            .build();

    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    assertNotNull(result);
    assertNotNull(result.getData());
    verify(articleCommandService).createArticle(any(), eq(user));
  }

  @Test
  public void should_fail_to_create_article_without_authentication() {
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Title")
            .description("Test Description")
            .body("Test Body")
            .build();

    assertThrows(NullPointerException.class, () -> articleMutation.createArticle(input));
  }

  @Test
  public void should_update_article_successfully() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    String slug = "test-title";
    UpdateArticleInput input =
        UpdateArticleInput.newBuilder()
            .title("Updated Title")
            .description("Updated Description")
            .body("Updated Body")
            .build();

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.updateArticle(slug, input);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(article, result.getLocalContext());
    verify(articleCommandService).updateArticle(eq(article), any());
  }

  @Test
  public void should_fail_to_update_article_not_found() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    String slug = "non-existent-slug";
    UpdateArticleInput input =
        UpdateArticleInput.newBuilder().title("Updated Title").build();

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleMutation.updateArticle(slug, input));
  }

  @Test
  public void should_fail_to_update_article_without_authorization() {
    User otherUser =
        new User("other@example.com", "otheruser", "password", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(otherUser, null));

    String slug = "test-title";
    UpdateArticleInput input =
        UpdateArticleInput.newBuilder().title("Updated Title").build();

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    assertThrows(
        NoAuthorizationException.class, () -> articleMutation.updateArticle(slug, input));
  }

  @Test
  public void should_favorite_article_successfully() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    String slug = "test-title";
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle(slug);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(article, result.getLocalContext());
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  public void should_fail_to_favorite_article_not_found() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    String slug = "non-existent-slug";
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleMutation.favoriteArticle(slug));
  }

  @Test
  public void should_unfavorite_article_successfully() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    String slug = "test-title";
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());

    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(slug);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(article, result.getLocalContext());
    verify(articleFavoriteRepository).remove(eq(favorite));
  }

  @Test
  public void should_unfavorite_article_when_not_favorited() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    String slug = "test-title";
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.empty());

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(slug);

    assertNotNull(result);
    assertNotNull(result.getData());
  }

  @Test
  public void should_delete_article_successfully() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    String slug = "test-title";
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    DeletionStatus result = articleMutation.deleteArticle(slug);

    assertNotNull(result);
    assertTrue(result.getSuccess());
    verify(articleRepository).remove(eq(article));
  }

  @Test
  public void should_fail_to_delete_article_without_authorization() {
    User otherUser =
        new User("other@example.com", "otheruser", "password", "bio", "image");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(otherUser, null));

    String slug = "test-title";
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    assertThrows(NoAuthorizationException.class, () -> articleMutation.deleteArticle(slug));
  }

  @Test
  public void should_fail_to_delete_article_not_found() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

    String slug = "non-existent-slug";
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleMutation.deleteArticle(slug));
  }
}
