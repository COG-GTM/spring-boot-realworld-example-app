package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.NoAuthorizationException;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.article.NewArticleParam;
import io.spring.application.data.ProfileData;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ArticleMutationTest {

  @Mock
  private ArticleCommandService articleCommandService;

  @Mock
  private ArticleFavoriteRepository articleFavoriteRepository;

  @Mock
  private ArticleRepository articleRepository;

  private ArticleMutation articleMutation;

  private User user;
  private Article article;
  private ProfileData profileData;

  @BeforeEach
  void setUp() {
    articleMutation = new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    user = new User("test@test.com", "testuser", "password", "bio", "image");
    profileData = new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false);
    
    DateTime now = new DateTime();
    article = new Article("Test Article", "Test Description", "Test Body", Arrays.asList("java", "spring"), user.getId(), now);
    
    setAnonymousAuthentication();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAnonymousAuthentication() {
    AnonymousAuthenticationToken anonymousToken = new AnonymousAuthenticationToken(
        "anonymous", "anonymousUser", 
        java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymousToken);
  }

  @Test
  void shouldCreateArticleWhenAuthenticated() {
    setAuthenticatedUser(user);
    
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(article);

    CreateArticleInput input = CreateArticleInput.newBuilder()
        .title("Test Article")
        .description("Test Description")
        .body("Test Body")
        .tagList(Arrays.asList("java", "spring"))
        .build();
    
    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);
    
    assertThat(result.getData()).isNotNull();
    verify(articleCommandService).createArticle(any(NewArticleParam.class), eq(user));
  }

  @Test
  void shouldFailToCreateArticleWhenNotAuthenticated() {
    CreateArticleInput input = CreateArticleInput.newBuilder()
        .title("Test Article")
        .description("Test Description")
        .body("Test Body")
        .build();
    
    assertThatThrownBy(() -> articleMutation.createArticle(input))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  void shouldUpdateArticleWhenAuthorized() {
    setAuthenticatedUser(user);
    
    when(articleRepository.findBySlug(eq(article.getSlug())))
        .thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any()))
        .thenReturn(article);

    UpdateArticleInput changes = UpdateArticleInput.newBuilder()
        .title("Updated Title")
        .build();
    
    DataFetcherResult<ArticlePayload> result = articleMutation.updateArticle(article.getSlug(), changes);
    
    assertThat(result.getData()).isNotNull();
  }

  @Test
  void shouldFailToUpdateArticleWhenNotAuthenticated() {
    when(articleRepository.findBySlug(eq(article.getSlug())))
        .thenReturn(Optional.of(article));
    
    UpdateArticleInput changes = UpdateArticleInput.newBuilder()
        .title("Updated Title")
        .build();
    
    assertThatThrownBy(() -> articleMutation.updateArticle(article.getSlug(), changes))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  void shouldFavoriteArticleWhenAuthenticated() {
    setAuthenticatedUser(user);
    
    when(articleRepository.findBySlug(eq(article.getSlug())))
        .thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle(article.getSlug());
    
    assertThat(result.getData()).isNotNull();
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  void shouldUnfavoriteArticleWhenAuthenticated() {
    setAuthenticatedUser(user);
    
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    
    when(articleRepository.findBySlug(eq(article.getSlug())))
        .thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(article.getSlug());
    
    assertThat(result.getData()).isNotNull();
  }

  @Test
  void shouldDeleteArticleWhenAuthorized() {
    setAuthenticatedUser(user);
    
    when(articleRepository.findBySlug(eq(article.getSlug())))
        .thenReturn(Optional.of(article));

    DeletionStatus result = articleMutation.deleteArticle(article.getSlug());
    
    assertThat(result.getSuccess()).isTrue();
    verify(articleRepository).remove(eq(article));
  }

  @Test
  void shouldFailToDeleteArticleWhenNotAuthenticated() {
    assertThatThrownBy(() -> articleMutation.deleteArticle("test-article"))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  void shouldFailToDeleteArticleWhenNotAuthor() {
    User anotherUser = new User("another@test.com", "anotheruser", "password", "bio", "image");
    setAuthenticatedUser(anotherUser);
    
    when(articleRepository.findBySlug(eq(article.getSlug())))
        .thenReturn(Optional.of(article));

    assertThatThrownBy(() -> articleMutation.deleteArticle(article.getSlug()))
        .isInstanceOf(NoAuthorizationException.class);
  }

  private void setAuthenticatedUser(User user) {
    UsernamePasswordAuthenticationToken authentication = 
        new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
