package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.article.ArticleCommandService;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.ArticlePayload;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class ArticleMutationFavoriteTest {
  private ArticleFavoriteRepository articleFavoriteRepository;
  private ArticleRepository articleRepository;
  private ArticleMutation articleMutation;

  private User user;
  private Article article;

  @BeforeEach
  public void setUp() {
    articleFavoriteRepository = mock(ArticleFavoriteRepository.class);
    articleRepository = mock(ArticleRepository.class);
    articleMutation =
        new ArticleMutation(
            mock(ArticleCommandService.class), articleFavoriteRepository, articleRepository);

    user = new User("john@jacob.com", "johnjacob", "123", "", "");
    User author = new User("other@test.com", "other", "123", "", "");
    article = new Article("title", "desc", "body", Arrays.asList("java"), author.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void signIn(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, null));
  }

  private void signInAnonymously() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @Test
  public void should_favorite_article_and_return_it_in_local_context() {
    signIn(user);

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle(article.getSlug());

    verify(articleFavoriteRepository).save(new ArticleFavorite(article.getId(), user.getId()));
    assertSame(article, result.getLocalContext());
  }

  @Test
  public void should_unfavorite_article_when_favorite_exists() {
    signIn(user);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(article.getSlug());

    verify(articleFavoriteRepository).remove(favorite);
    assertSame(article, result.getLocalContext());
  }

  @Test
  public void should_not_remove_anything_when_unfavoriting_article_not_favorited() {
    signIn(user);
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.empty());

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(article.getSlug());

    verify(articleFavoriteRepository, never()).remove(any());
    assertSame(article, result.getLocalContext());
  }

  @Test
  public void should_reject_favorite_from_anonymous_user() {
    signInAnonymously();

    assertThrows(
        AuthenticationException.class, () -> articleMutation.favoriteArticle(article.getSlug()));
    verify(articleFavoriteRepository, never()).save(any());
  }

  @Test
  public void should_reject_unfavorite_from_anonymous_user() {
    signInAnonymously();

    assertThrows(
        AuthenticationException.class, () -> articleMutation.unfavoriteArticle(article.getSlug()));
    verify(articleFavoriteRepository, never()).find(anyString(), anyString());
    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  public void should_fail_to_favorite_unknown_article() {
    signIn(user);
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleMutation.favoriteArticle("missing"));
    verify(articleFavoriteRepository, never()).save(any());
  }

  @Test
  public void should_fail_to_unfavorite_unknown_article() {
    signIn(user);
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleMutation.unfavoriteArticle("missing"));
    verify(articleFavoriteRepository, never()).remove(any());
  }
}
