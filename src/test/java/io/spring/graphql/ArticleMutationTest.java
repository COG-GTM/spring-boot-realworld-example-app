package io.spring.graphql;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.NoAuthorizationException;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ArticleMutationTest {

  @Mock private ArticleCommandService articleCommandService;
  @Mock private ArticleFavoriteRepository articleFavoriteRepository;
  @Mock private ArticleRepository articleRepository;

  private ArticleMutation articleMutation;
  private User user;

  @BeforeEach
  public void setUp() {
    articleMutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    user = new User("user@test.com", "testuser", "password", "", "");
  }

  @AfterEach
  public void cleanup() {
    SecurityContextHolder.clearContext();
  }

  private void setAuthenticated(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));
  }

  @Test
  public void should_create_article_when_authenticated() {
    setAuthenticated(user);
    Article article =
        new Article("test title", "desc", "body", Arrays.asList("java"), user.getId());
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("test title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java"))
            .build();

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);
    assertThat(result, notNullValue());
    assertThat(result.getData(), notNullValue());
    verify(articleCommandService).createArticle(any(), eq(user));
  }

  @Test
  public void should_fail_create_article_when_not_authenticated() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymous",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("title")
            .description("desc")
            .body("body")
            .build();
    assertThrows(AuthenticationException.class, () -> articleMutation.createArticle(input));
  }

  @Test
  public void should_update_article_when_author() {
    setAuthenticated(user);
    Article article =
        new Article("old title", "desc", "body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug("old-title")).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(article);

    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder().title("new title").build();

    DataFetcherResult<ArticlePayload> result =
        articleMutation.updateArticle("old-title", changes);
    assertThat(result, notNullValue());
    verify(articleCommandService).updateArticle(eq(article), any());
  }

  @Test
  public void should_fail_update_article_when_not_author() {
    setAuthenticated(user);
    User otherUser = new User("other@test.com", "other", "pass", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList(), otherUser.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    UpdateArticleInput changes = UpdateArticleInput.newBuilder().title("new").build();
    assertThrows(
        NoAuthorizationException.class,
        () -> articleMutation.updateArticle("title", changes));
  }

  @Test
  public void should_favorite_article() {
    setAuthenticated(user);
    Article article =
        new Article("title", "desc", "body", Arrays.asList(), "other-user-id");
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle("title");
    assertThat(result, notNullValue());
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  public void should_unfavorite_article() {
    setAuthenticated(user);
    Article article =
        new Article("title", "desc", "body", Arrays.asList(), "other-user-id");
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    ArticleFavorite fav = new ArticleFavorite(article.getId(), user.getId());
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.of(fav));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("title");
    assertThat(result, notNullValue());
    verify(articleFavoriteRepository).remove(fav);
  }

  @Test
  public void should_delete_article_when_author() {
    setAuthenticated(user);
    Article article =
        new Article("title", "desc", "body", Arrays.asList(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    DeletionStatus result = articleMutation.deleteArticle("title");
    assertThat(result.getSuccess(), is(true));
    verify(articleRepository).remove(article);
  }

  @Test
  public void should_fail_delete_article_when_not_author() {
    setAuthenticated(user);
    User otherUser = new User("other@test.com", "other", "pass", "", "");
    Article article =
        new Article("title", "desc", "body", Arrays.asList(), otherUser.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    assertThrows(
        NoAuthorizationException.class, () -> articleMutation.deleteArticle("title"));
  }
}
