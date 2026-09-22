package io.spring.graphql;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import io.spring.graphql.types.CreateArticleInput;
import io.spring.graphql.types.UpdateArticleInput;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ArticleMutationTest {

  @Mock private ArticleCommandService articleCommandService;
  @Mock private ArticleFavoriteRepository articleFavoriteRepository;
  @Mock private ArticleRepository articleRepository;

  private ArticleMutation articleMutation;
  private final User user = new User("email@test.com", "username", "123", "", "");
  private final Article article =
      new Article("a title", "desc", "body", singletonList("java"), user.getId());

  @BeforeEach
  public void setUp() {
    articleMutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void login(User currentUser) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(currentUser, null, emptyList()));
  }

  private void loginAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @Test
  public void should_create_article_for_current_user() {
    login(user);
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("a title")
            .description("desc")
            .body("body")
            .tagList(singletonList("java"))
            .build();

    assertEquals(article, articleMutation.createArticle(input).getLocalContext());

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    assertEquals(singletonList("java"), captor.getValue().getTagList());
  }

  @Test
  public void should_default_tag_list_to_empty() {
    login(user);
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    articleMutation.createArticle(CreateArticleInput.newBuilder().title("a title").build());

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    assertTrue(captor.getValue().getTagList().isEmpty());
  }

  @Test
  public void should_not_create_article_for_anonymous_user() {
    loginAnonymous();

    assertThrows(
        AuthenticationException.class,
        () -> articleMutation.createArticle(CreateArticleInput.newBuilder().build()));
  }

  @Test
  public void should_update_own_article() {
    login(user);
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(article);
    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder()
            .title("new")
            .body("new body")
            .description("new desc")
            .build();

    assertEquals(article, articleMutation.updateArticle("a-title", changes).getLocalContext());

    ArgumentCaptor<UpdateArticleParam> captor = ArgumentCaptor.forClass(UpdateArticleParam.class);
    verify(articleCommandService).updateArticle(eq(article), captor.capture());
    assertEquals("new", captor.getValue().getTitle());
    assertEquals("new body", captor.getValue().getBody());
    assertEquals("new desc", captor.getValue().getDescription());
  }

  @Test
  public void should_not_update_article_of_other_user() {
    login(new User("other@test.com", "other", "123", "", ""));
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));

    assertThrows(
        NoAuthorizationException.class,
        () -> articleMutation.updateArticle("a-title", UpdateArticleInput.newBuilder().build()));
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  public void should_not_update_missing_article() {
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleMutation.updateArticle("missing", UpdateArticleInput.newBuilder().build()));
  }

  @Test
  public void should_favorite_article() {
    login(user);
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));

    assertEquals(article, articleMutation.favoriteArticle("a-title").getLocalContext());

    ArgumentCaptor<ArticleFavorite> captor = ArgumentCaptor.forClass(ArticleFavorite.class);
    verify(articleFavoriteRepository).save(captor.capture());
    assertEquals(article.getId(), captor.getValue().getArticleId());
    assertEquals(user.getId(), captor.getValue().getUserId());
  }

  @Test
  public void should_remove_existing_favorite() {
    login(user);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.of(favorite));

    assertEquals(article, articleMutation.unfavoriteArticle("a-title").getLocalContext());
    verify(articleFavoriteRepository).remove(favorite);
  }

  @Test
  public void should_ignore_unfavorite_without_existing_favorite() {
    login(user);
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId()))
        .thenReturn(Optional.empty());

    articleMutation.unfavoriteArticle("a-title");

    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  public void should_delete_own_article() {
    login(user);
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));

    assertTrue(articleMutation.deleteArticle("a-title").getSuccess());
    verify(articleRepository).remove(article);
  }

  @Test
  public void should_not_delete_article_of_other_user() {
    login(new User("other@test.com", "other", "123", "", ""));
    when(articleRepository.findBySlug("a-title")).thenReturn(Optional.of(article));

    assertThrows(NoAuthorizationException.class, () -> articleMutation.deleteArticle("a-title"));
    verify(articleRepository, never()).remove(any());
  }
}
