package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class ArticleMutationTest {

  private ArticleCommandService articleCommandService;
  private ArticleFavoriteRepository articleFavoriteRepository;
  private ArticleRepository articleRepository;
  private ArticleMutation articleMutation;

  private final User user = new User("me@example.com", "me", "123", "", "");
  private Article article;

  @BeforeEach
  void setUp() {
    articleCommandService = mock(ArticleCommandService.class);
    articleFavoriteRepository = mock(ArticleFavoriteRepository.class);
    articleRepository = mock(ArticleRepository.class);
    articleMutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_create_article() {
    when(articleCommandService.createArticle(any(NewArticleParam.class), any(User.class)))
        .thenReturn(article);
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java"))
            .build();

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    assertEquals(article, result.getLocalContext());
  }

  @Test
  void should_throw_when_create_article_without_auth() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("t").description("d").body("b").build();
    assertThrows(AuthenticationException.class, () -> articleMutation.createArticle(input));
  }

  @Test
  void should_update_article_when_authorized() {
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(any(Article.class), any(UpdateArticleParam.class)))
        .thenReturn(article);
    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder().title("new").body("body").description("desc").build();

    DataFetcherResult<ArticlePayload> result =
        articleMutation.updateArticle(article.getSlug(), changes);

    assertEquals(article, result.getLocalContext());
  }

  @Test
  void should_throw_when_update_article_not_authorized() {
    Article othersArticle = new Article("t", "d", "b", Arrays.asList("x"), "someone-else");
    when(articleRepository.findBySlug(othersArticle.getSlug()))
        .thenReturn(Optional.of(othersArticle));
    UpdateArticleInput changes = UpdateArticleInput.newBuilder().title("new").build();

    assertThrows(
        NoAuthorizationException.class,
        () -> articleMutation.updateArticle(othersArticle.getSlug(), changes));
  }

  @Test
  void should_throw_when_update_article_missing() {
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());
    UpdateArticleInput changes = UpdateArticleInput.newBuilder().title("new").build();
    assertThrows(
        ResourceNotFoundException.class,
        () -> articleMutation.updateArticle("missing", changes));
  }

  @Test
  void should_favorite_article() {
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle(article.getSlug());

    assertEquals(article, result.getLocalContext());
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  void should_unfavorite_article() {
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(article.getSlug());

    assertEquals(article, result.getLocalContext());
    verify(articleFavoriteRepository).remove(favorite);
  }

  @Test
  void should_delete_article_when_authorized() {
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    DeletionStatus status = articleMutation.deleteArticle(article.getSlug());

    assertTrue(status.getSuccess());
    verify(articleRepository).remove(article);
  }

  @Test
  void should_throw_when_delete_article_not_authorized() {
    Article othersArticle = new Article("t", "d", "b", Arrays.asList("x"), "someone-else");
    when(articleRepository.findBySlug(othersArticle.getSlug()))
        .thenReturn(Optional.of(othersArticle));
    assertThrows(
        NoAuthorizationException.class,
        () -> articleMutation.deleteArticle(othersArticle.getSlug()));
  }
}
