package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class ArticleMutationTest {

  private ArticleCommandService articleCommandService;
  private ArticleFavoriteRepository articleFavoriteRepository;
  private ArticleRepository articleRepository;
  private ArticleMutation articleMutation;
  private User user;

  @BeforeEach
  void setUp() {
    articleCommandService = mock(ArticleCommandService.class);
    articleFavoriteRepository = mock(ArticleFavoriteRepository.class);
    articleRepository = mock(ArticleRepository.class);
    articleMutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    user = new User("test@test.com", "testuser", "password", "", "");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
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
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java"))
            .build();

    Article article = new Article("Test Article", "desc", "body", Arrays.asList("java"), user.getId());
    when(articleCommandService.createArticle(any(), any())).thenReturn(article);

    var result = articleMutation.createArticle(input);

    assertNotNull(result);
    assertNotNull(result.getData());
  }

  @Test
  void should_throw_when_not_authenticated_create() {
    SecurityContextHolder.clearContext();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new org.springframework.security.authentication.AnonymousAuthenticationToken(
                "key",
                "anon",
                Collections.singletonList(
                    new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "ROLE_ANONYMOUS"))));
    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("t").description("d").body("b").build();
    assertThrows(AuthenticationException.class, () -> articleMutation.createArticle(input));
  }

  @Test
  void should_delete_article() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));

    DeletionStatus status = articleMutation.deleteArticle("title");

    assertTrue(status.getSuccess());
    verify(articleRepository).remove(article);
  }

  @Test
  void should_favorite_article() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    var result = articleMutation.favoriteArticle("title");

    assertNotNull(result);
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  void should_unfavorite_article() {
    Article article = new Article("Title", "desc", "body", Collections.emptyList(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    ArticleFavorite fav = new ArticleFavorite(article.getId(), user.getId());
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.of(fav));

    var result = articleMutation.unfavoriteArticle("title");

    assertNotNull(result);
    verify(articleFavoriteRepository).remove(fav);
  }
}
