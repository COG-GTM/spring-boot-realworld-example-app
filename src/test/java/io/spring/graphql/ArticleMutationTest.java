package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
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
    user = new User("email@example.com", "username", "123", "", "");
    article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void login(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
  }

  private void loginAnonymously() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  @Test
  public void should_create_article() {
    login(user);
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("title").description("desc").body("body").build();
    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    Assertions.assertEquals(article, result.getLocalContext());
  }

  @Test
  public void should_throw_authentication_exception_when_not_logged_in() {
    loginAnonymously();
    Assertions.assertThrows(
        AuthenticationException.class,
        () ->
            articleMutation.createArticle(
                CreateArticleInput.newBuilder().title("t").description("d").body("b").build()));
  }

  @Test
  public void should_update_article_by_author() {
    login(user);
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(article);

    DataFetcherResult<ArticlePayload> result =
        articleMutation.updateArticle(
            "title", UpdateArticleInput.newBuilder().title("new").build());

    Assertions.assertEquals(article, result.getLocalContext());
  }

  @Test
  public void should_not_update_article_of_other_user() {
    User other = new User("other@example.com", "other", "123", "", "");
    login(other);
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    Assertions.assertThrows(
        NoAuthorizationException.class,
        () -> articleMutation.updateArticle("title", UpdateArticleInput.newBuilder().build()));
  }

  @Test
  public void should_throw_not_found_for_missing_slug_on_update() {
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    Assertions.assertThrows(
        ResourceNotFoundException.class,
        () -> articleMutation.updateArticle("missing", UpdateArticleInput.newBuilder().build()));
  }

  @Test
  public void should_favorite_article() {
    login(user);
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle("title");

    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
    Assertions.assertEquals(article, result.getLocalContext());
  }

  @Test
  public void should_unfavorite_article() {
    login(user);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("title");

    verify(articleFavoriteRepository).remove(favorite);
    Assertions.assertEquals(article, result.getLocalContext());
  }

  @Test
  public void should_skip_remove_when_favorite_not_found() {
    login(user);
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.empty());

    articleMutation.unfavoriteArticle("title");

    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  public void should_delete_article_by_author() {
    login(user);
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    DeletionStatus status = articleMutation.deleteArticle("title");

    verify(articleRepository).remove(article);
    Assertions.assertTrue(status.getSuccess());
  }

  @Test
  public void should_not_delete_article_of_other_user() {
    User other = new User("other@example.com", "other", "123", "", "");
    login(other);
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    Assertions.assertThrows(
        NoAuthorizationException.class, () -> articleMutation.deleteArticle("title"));
    verify(articleRepository, never()).remove(any());
  }
}
