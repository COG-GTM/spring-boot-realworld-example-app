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
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ArticleMutationTest {

  @Mock private ArticleCommandService articleCommandService;
  @Mock private ArticleFavoriteRepository articleFavoriteRepository;
  @Mock private ArticleRepository articleRepository;

  @InjectMocks private ArticleMutation articleMutation;

  private User user;
  private User otherUser;

  @BeforeEach
  void setUp() {
    user = new User("john@jacob.com", "johnjacob", "123", "", "default-avatar");
    otherUser = new User("other@user.com", "otheruser", "456", "", "");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymous",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  private void logIn(User loginUser) {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(loginUser, null));
  }

  // ── createArticle ──

  @Test
  void createArticle_success() {
    logIn(user);
    Article article =
        new Article("Test Title", "desc", "body", Arrays.asList("java"), user.getId());
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Test Title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java"))
            .build();

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(article, result.getLocalContext());
    verify(articleCommandService).createArticle(any(), eq(user));
  }

  @Test
  void createArticle_withNullTagList() {
    logIn(user);
    Article article = new Article("Test Title", "desc", "body", Arrays.asList(), user.getId());
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("Test Title").description("desc").body("body").build();

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    assertNotNull(result);
    verify(articleCommandService).createArticle(any(), eq(user));
  }

  @Test
  void createArticle_unauthenticated() {
    setAnonymous();
    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("t").description("d").body("b").build();

    assertThrows(AuthenticationException.class, () -> articleMutation.createArticle(input));
  }

  // ── updateArticle ──

  @Test
  void updateArticle_success() {
    logIn(user);
    Article article =
        new Article("Old Title", "old desc", "old body", Arrays.asList(), user.getId());
    Article updatedArticle =
        new Article("New Title", "new desc", "new body", Arrays.asList(), user.getId());

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(updatedArticle);

    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder()
            .title("New Title")
            .description("new desc")
            .body("new body")
            .build();

    DataFetcherResult<ArticlePayload> result =
        articleMutation.updateArticle(article.getSlug(), changes);

    assertNotNull(result);
    assertEquals(updatedArticle, result.getLocalContext());
    verify(articleCommandService).updateArticle(eq(article), any());
  }

  @Test
  void updateArticle_unauthenticated() {
    setAnonymous();
    Article article = new Article("Title", "desc", "body", Arrays.asList(), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    UpdateArticleInput changes = UpdateArticleInput.newBuilder().title("New").build();

    assertThrows(
        AuthenticationException.class,
        () -> articleMutation.updateArticle(article.getSlug(), changes));
  }

  @Test
  void updateArticle_notAuthor() {
    logIn(user);
    Article article =
        new Article("Title", "desc", "body", Arrays.asList(), otherUser.getId());

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    UpdateArticleInput changes = UpdateArticleInput.newBuilder().title("New").build();

    assertThrows(
        NoAuthorizationException.class,
        () -> articleMutation.updateArticle(article.getSlug(), changes));
  }

  @Test
  void updateArticle_notFound() {
    logIn(user);
    when(articleRepository.findBySlug(eq("no-such-slug"))).thenReturn(Optional.empty());

    UpdateArticleInput changes = UpdateArticleInput.newBuilder().title("New").build();

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleMutation.updateArticle("no-such-slug", changes));
  }

  // ── favoriteArticle ──

  @Test
  void favoriteArticle_success() {
    logIn(user);
    Article article =
        new Article("Title", "desc", "body", Arrays.asList(), otherUser.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result =
        articleMutation.favoriteArticle(article.getSlug());

    assertNotNull(result);
    assertEquals(article, result.getLocalContext());
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  void favoriteArticle_unauthenticated() {
    setAnonymous();
    assertThrows(
        AuthenticationException.class, () -> articleMutation.favoriteArticle("some-slug"));
  }

  @Test
  void favoriteArticle_articleNotFound() {
    logIn(user);
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleMutation.favoriteArticle("missing"));
  }

  // ── unfavoriteArticle ──

  @Test
  void unfavoriteArticle_success() {
    logIn(user);
    Article article =
        new Article("Title", "desc", "body", Arrays.asList(), otherUser.getId());
    ArticleFavorite fav = new ArticleFavorite(article.getId(), user.getId());

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(fav));

    DataFetcherResult<ArticlePayload> result =
        articleMutation.unfavoriteArticle(article.getSlug());

    assertNotNull(result);
    assertEquals(article, result.getLocalContext());
    verify(articleFavoriteRepository).remove(eq(fav));
  }

  @Test
  void unfavoriteArticle_unauthenticated() {
    setAnonymous();
    assertThrows(
        AuthenticationException.class, () -> articleMutation.unfavoriteArticle("some-slug"));
  }

  @Test
  void unfavoriteArticle_articleNotFound() {
    logIn(user);
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleMutation.unfavoriteArticle("missing"));
  }

  // ── deleteArticle ──

  @Test
  void deleteArticle_success() {
    logIn(user);
    Article article =
        new Article("Title", "desc", "body", Arrays.asList(), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    DeletionStatus result = articleMutation.deleteArticle(article.getSlug());

    assertTrue(result.getSuccess());
    verify(articleRepository).remove(eq(article));
  }

  @Test
  void deleteArticle_unauthenticated() {
    setAnonymous();
    assertThrows(
        AuthenticationException.class, () -> articleMutation.deleteArticle("some-slug"));
  }

  @Test
  void deleteArticle_notAuthor() {
    logIn(user);
    Article article =
        new Article("Title", "desc", "body", Arrays.asList(), otherUser.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    assertThrows(
        NoAuthorizationException.class,
        () -> articleMutation.deleteArticle(article.getSlug()));
  }

  @Test
  void deleteArticle_notFound() {
    logIn(user);
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleMutation.deleteArticle("missing"));
  }
}
