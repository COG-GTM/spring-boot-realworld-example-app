package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.spring.api.exception.NoAuthorizationException;
import io.spring.application.article.ArticleCommandService;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.graphql.exception.AuthenticationException;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ArticleMutationTest {

  @Mock private ArticleCommandService articleCommandService;

  @Mock private ArticleFavoriteRepository articleFavoriteRepository;

  @Mock private ArticleRepository articleRepository;

  @InjectMocks private ArticleMutation articleMutation;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User("e@t.com", "testuser", "pass", "", "");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createArticle_should_call_service() {
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("Title")
            .description("Desc")
            .body("Body")
            .tagList(Arrays.asList("java"))
            .build();
    Article article = new Article("Title", "Desc", "Body", Arrays.asList("java"), user.getId());
    when(articleCommandService.createArticle(any(), any())).thenReturn(article);

    var result = articleMutation.createArticle(input);

    assertNotNull(result);
    verify(articleCommandService).createArticle(any(), eq(user));
  }

  @Test
  void updateArticle_should_verify_authorization_and_update() {
    Article article = new Article("Title", "Desc", "Body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(any(), any())).thenReturn(article);

    UpdateArticleInput params = UpdateArticleInput.newBuilder().title("New Title").build();

    var result = articleMutation.updateArticle("title", params);
    assertNotNull(result);
    verify(articleCommandService).updateArticle(any(), any());
  }

  @Test
  void updateArticle_without_authorization_should_throw() {
    User otherUser = new User("o@t.com", "other", "pass", "", "");
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList("java"), otherUser.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    UpdateArticleInput params = UpdateArticleInput.newBuilder().title("New Title").build();

    assertThrows(
        NoAuthorizationException.class, () -> articleMutation.updateArticle("title", params));
  }

  @Test
  void deleteArticle_should_remove_article() {
    Article article = new Article("Title", "Desc", "Body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    DeletionStatus result = articleMutation.deleteArticle("title");

    assertTrue(result.getSuccess());
    verify(articleRepository).remove(article);
  }

  @Test
  void deleteArticle_without_authorization_should_throw() {
    User otherUser = new User("o@t.com", "other", "pass", "", "");
    Article article =
        new Article("Title", "Desc", "Body", Arrays.asList("java"), otherUser.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    assertThrows(NoAuthorizationException.class, () -> articleMutation.deleteArticle("title"));
  }

  @Test
  void favoriteArticle_should_save_favorite() {
    Article article = new Article("Title", "Desc", "Body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));

    var result = articleMutation.favoriteArticle("title");

    assertNotNull(result);
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  void unfavoriteArticle_should_remove_favorite() {
    Article article = new Article("Title", "Desc", "Body", Arrays.asList("java"), user.getId());
    ArticleFavorite fav = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug("title")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.of(fav));

    var result = articleMutation.unfavoriteArticle("title");

    assertNotNull(result);
    verify(articleFavoriteRepository).remove(fav);
  }

  @Test
  void any_operation_without_authenticated_user_should_throw() {
    SecurityContextHolder.clearContext();
    // Need to set up an anonymous auth so SecurityUtil.getCurrentUser() returns empty
    org.springframework.security.authentication.AnonymousAuthenticationToken anonAuth =
        new org.springframework.security.authentication.AnonymousAuthenticationToken(
            "key",
            "anonymous",
            Collections.singletonList(
                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonAuth);

    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("T").description("D").body("B").build();

    assertThrows(AuthenticationException.class, () -> articleMutation.createArticle(input));
    assertThrows(AuthenticationException.class, () -> articleMutation.favoriteArticle("slug"));
    assertThrows(AuthenticationException.class, () -> articleMutation.unfavoriteArticle("slug"));
    assertThrows(AuthenticationException.class, () -> articleMutation.deleteArticle("slug"));
  }
}
