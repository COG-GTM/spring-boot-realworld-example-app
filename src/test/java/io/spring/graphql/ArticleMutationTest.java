package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class ArticleMutationTest {

  private ArticleCommandService articleCommandService;
  private ArticleFavoriteRepository articleFavoriteRepository;
  private ArticleRepository articleRepository;
  private ArticleMutation articleMutation;

  private User currentUser;

  @BeforeEach
  void setUp() {
    articleCommandService = mock(ArticleCommandService.class);
    articleFavoriteRepository = mock(ArticleFavoriteRepository.class);
    articleRepository = mock(ArticleRepository.class);
    articleMutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    currentUser = new User("user@example.com", "user", "123", "bio", "image");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  private Article articleOwnedBy(User user) {
    return new Article("title", "desc", "body", List.of("java"), user.getId());
  }

  @Test
  void createArticle_authenticated_returnsResult() {
    authenticate(currentUser);
    Article article = articleOwnedBy(currentUser);
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(currentUser)))
        .thenReturn(article);
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("title")
            .description("desc")
            .body("body")
            .tagList(List.of("java"))
            .build();

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    verify(articleCommandService).createArticle(any(NewArticleParam.class), eq(currentUser));
    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(article, result.getLocalContext());
  }

  @Test
  void createArticle_nullTagList_defaultsToEmptyList() {
    authenticate(currentUser);
    Article article = articleOwnedBy(currentUser);
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(currentUser)))
        .thenReturn(article);
    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("title").description("desc").body("body").build();

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    assertNotNull(result);
    verify(articleCommandService).createArticle(any(NewArticleParam.class), eq(currentUser));
  }

  @Test
  void createArticle_noCurrentUser_throwsAuthenticationException() {
    anonymous();
    CreateArticleInput input = CreateArticleInput.newBuilder().title("title").build();

    assertThrows(AuthenticationException.class, () -> articleMutation.createArticle(input));
  }

  @Test
  void updateArticle_articleNotFound_throwsResourceNotFound() {
    authenticate(currentUser);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.empty());
    UpdateArticleInput changes = UpdateArticleInput.newBuilder().title("new").build();

    assertThrows(
        ResourceNotFoundException.class, () -> articleMutation.updateArticle("slug", changes));
  }

  @Test
  void updateArticle_author_updates() {
    authenticate(currentUser);
    Article article = articleOwnedBy(currentUser);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any(UpdateArticleParam.class)))
        .thenReturn(article);
    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder().title("new").body("newbody").description("newdesc").build();

    DataFetcherResult<ArticlePayload> result = articleMutation.updateArticle("slug", changes);

    verify(articleCommandService).updateArticle(eq(article), any(UpdateArticleParam.class));
    assertNotNull(result.getData());
    assertEquals(article, result.getLocalContext());
  }

  @Test
  void updateArticle_nonAuthor_throwsNoAuthorization() {
    authenticate(currentUser);
    User otherUser = new User("other@example.com", "other", "123", "bio", "image");
    Article article = articleOwnedBy(otherUser);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    UpdateArticleInput changes = UpdateArticleInput.newBuilder().title("new").build();

    assertThrows(
        NoAuthorizationException.class, () -> articleMutation.updateArticle("slug", changes));
    verify(articleCommandService, never())
        .updateArticle(any(Article.class), any(UpdateArticleParam.class));
  }

  @Test
  void updateArticle_noCurrentUser_throwsAuthenticationException() {
    anonymous();
    Article article = articleOwnedBy(currentUser);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    UpdateArticleInput changes = UpdateArticleInput.newBuilder().title("new").build();

    assertThrows(
        AuthenticationException.class, () -> articleMutation.updateArticle("slug", changes));
  }

  @Test
  void favoriteArticle_success_savesFavorite() {
    authenticate(currentUser);
    Article article = articleOwnedBy(currentUser);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle("slug");

    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
    assertNotNull(result.getData());
    assertEquals(article, result.getLocalContext());
  }

  @Test
  void favoriteArticle_articleNotFound_throwsResourceNotFound() {
    authenticate(currentUser);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleMutation.favoriteArticle("slug"));
  }

  @Test
  void favoriteArticle_noCurrentUser_throwsAuthenticationException() {
    anonymous();

    assertThrows(AuthenticationException.class, () -> articleMutation.favoriteArticle("slug"));
  }

  @Test
  void unfavoriteArticle_existingFavorite_removes() {
    authenticate(currentUser);
    Article article = articleOwnedBy(currentUser);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), currentUser.getId());
    when(articleFavoriteRepository.find(article.getId(), currentUser.getId()))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("slug");

    verify(articleFavoriteRepository).remove(favorite);
    assertNotNull(result.getData());
    assertEquals(article, result.getLocalContext());
  }

  @Test
  void unfavoriteArticle_noFavorite_doesNotRemove() {
    authenticate(currentUser);
    Article article = articleOwnedBy(currentUser);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), currentUser.getId()))
        .thenReturn(Optional.empty());

    articleMutation.unfavoriteArticle("slug");

    verify(articleFavoriteRepository, never()).remove(any(ArticleFavorite.class));
  }

  @Test
  void unfavoriteArticle_noCurrentUser_throwsAuthenticationException() {
    anonymous();

    assertThrows(AuthenticationException.class, () -> articleMutation.unfavoriteArticle("slug"));
  }

  @Test
  void deleteArticle_author_removesAndReturnsSuccess() {
    authenticate(currentUser);
    Article article = articleOwnedBy(currentUser);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));

    DeletionStatus status = articleMutation.deleteArticle("slug");

    verify(articleRepository).remove(article);
    assertTrue(status.getSuccess());
  }

  @Test
  void deleteArticle_nonAuthor_throwsNoAuthorization() {
    authenticate(currentUser);
    User otherUser = new User("other@example.com", "other", "123", "bio", "image");
    Article article = articleOwnedBy(otherUser);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.of(article));

    assertThrows(NoAuthorizationException.class, () -> articleMutation.deleteArticle("slug"));
    verify(articleRepository, never()).remove(any(Article.class));
  }

  @Test
  void deleteArticle_articleNotFound_throwsResourceNotFound() {
    authenticate(currentUser);
    when(articleRepository.findBySlug("slug")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleMutation.deleteArticle("slug"));
  }

  @Test
  void deleteArticle_noCurrentUser_throwsAuthenticationException() {
    anonymous();

    assertThrows(AuthenticationException.class, () -> articleMutation.deleteArticle("slug"));
  }
}
