package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class ArticleMutationTest {

  private ArticleCommandService articleCommandService;
  private ArticleFavoriteRepository articleFavoriteRepository;
  private ArticleRepository articleRepository;
  private ArticleMutation articleMutation;
  private User owner;
  private User other;
  private Article article;

  @BeforeEach
  public void setUp() {
    articleCommandService = mock(ArticleCommandService.class);
    articleFavoriteRepository = mock(ArticleFavoriteRepository.class);
    articleRepository = mock(ArticleRepository.class);
    articleMutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    owner = new User("owner@example.com", "owner", "pw", "", "");
    other = new User("other@example.com", "other", "pw", "", "");
    article = new Article("My Title", "desc", "body", Arrays.asList("java"), owner.getId());
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  // ---- createArticle ----

  @Test
  public void create_article_should_build_param_and_return_payload_with_entity_context() {
    authenticate(owner);
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("My Title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "spring"))
            .build();
    when(articleCommandService.createArticle(any(), eq(owner))).thenReturn(article);
    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    verify(articleCommandService).createArticle(captor.capture(), eq(owner));
    assertEquals("My Title", captor.getValue().getTitle());
    assertEquals("desc", captor.getValue().getDescription());
    assertEquals("body", captor.getValue().getBody());
    assertEquals(Arrays.asList("java", "spring"), captor.getValue().getTagList());
    assertNotNull(result.getData());
    assertSame(article, result.getLocalContext());
  }

  @Test
  public void create_article_should_default_null_tag_list_to_empty() {
    authenticate(owner);
    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("My Title").description("d").body("b").build();
    when(articleCommandService.createArticle(any(), eq(owner))).thenReturn(article);
    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);

    articleMutation.createArticle(input);

    verify(articleCommandService).createArticle(captor.capture(), eq(owner));
    assertTrue(captor.getValue().getTagList().isEmpty());
  }

  @Test
  public void create_article_should_require_authentication() {
    anonymous();
    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("t").description("d").body("b").build();

    assertThrows(AuthenticationException.class, () -> articleMutation.createArticle(input));
    verifyNoInteractions(articleCommandService);
  }

  // ---- updateArticle ----

  @Test
  public void update_article_should_update_when_owner() {
    authenticate(owner);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    Article updated = new Article("New Title", "desc", "body", Arrays.asList(), owner.getId());
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(updated);
    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder()
            .title("New Title")
            .body("new body")
            .description("nd")
            .build();
    ArgumentCaptor<UpdateArticleParam> captor = ArgumentCaptor.forClass(UpdateArticleParam.class);

    DataFetcherResult<ArticlePayload> result =
        articleMutation.updateArticle(article.getSlug(), changes);

    verify(articleCommandService).updateArticle(eq(article), captor.capture());
    assertEquals("New Title", captor.getValue().getTitle());
    assertEquals("new body", captor.getValue().getBody());
    assertEquals("nd", captor.getValue().getDescription());
    assertNotNull(result.getData());
    assertSame(updated, result.getLocalContext());
  }

  @Test
  public void update_article_should_throw_not_found_for_unknown_slug() {
    authenticate(owner);
    when(articleRepository.findBySlug("nope")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> articleMutation.updateArticle("nope", UpdateArticleInput.newBuilder().build()));
    verifyNoInteractions(articleCommandService);
  }

  @Test
  public void update_article_should_require_authentication() {
    anonymous();
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    assertThrows(
        AuthenticationException.class,
        () ->
            articleMutation.updateArticle(
                article.getSlug(), UpdateArticleInput.newBuilder().build()));
    verifyNoInteractions(articleCommandService);
  }

  @Test
  public void update_article_should_reject_non_owner() {
    authenticate(other);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    assertThrows(
        NoAuthorizationException.class,
        () ->
            articleMutation.updateArticle(
                article.getSlug(), UpdateArticleInput.newBuilder().title("x").build()));
    verifyNoInteractions(articleCommandService);
  }

  // ---- favoriteArticle ----

  @Test
  public void favorite_article_should_save_favorite_for_current_user() {
    authenticate(other);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle(article.getSlug());

    verify(articleFavoriteRepository).save(new ArticleFavorite(article.getId(), other.getId()));
    assertNotNull(result.getData());
    assertSame(article, result.getLocalContext());
  }

  @Test
  public void favorite_article_should_throw_not_found_for_unknown_slug() {
    authenticate(other);
    when(articleRepository.findBySlug("nope")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleMutation.favoriteArticle("nope"));
    verifyNoInteractions(articleFavoriteRepository);
  }

  @Test
  public void favorite_article_should_require_authentication() {
    anonymous();

    assertThrows(
        AuthenticationException.class, () -> articleMutation.favoriteArticle(article.getSlug()));
    verifyNoInteractions(articleRepository, articleFavoriteRepository);
  }

  // ---- unfavoriteArticle ----

  @Test
  public void unfavorite_article_should_remove_existing_favorite() {
    authenticate(other);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), other.getId());
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), other.getId()))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(article.getSlug());

    verify(articleFavoriteRepository).remove(favorite);
    assertNotNull(result.getData());
    assertSame(article, result.getLocalContext());
  }

  @Test
  public void unfavorite_article_should_be_noop_when_not_favorited() {
    authenticate(other);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), other.getId()))
        .thenReturn(Optional.empty());

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(article.getSlug());

    verify(articleFavoriteRepository, never()).remove(any());
    assertSame(article, result.getLocalContext());
  }

  @Test
  public void unfavorite_article_should_throw_not_found_for_unknown_slug() {
    authenticate(other);
    when(articleRepository.findBySlug("nope")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleMutation.unfavoriteArticle("nope"));
    verifyNoInteractions(articleFavoriteRepository);
  }

  @Test
  public void unfavorite_article_should_require_authentication() {
    anonymous();

    assertThrows(
        AuthenticationException.class, () -> articleMutation.unfavoriteArticle(article.getSlug()));
    verifyNoInteractions(articleRepository, articleFavoriteRepository);
  }

  // ---- deleteArticle ----

  @Test
  public void delete_article_should_remove_when_owner() {
    authenticate(owner);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    DeletionStatus status = articleMutation.deleteArticle(article.getSlug());

    verify(articleRepository).remove(article);
    assertTrue(status.getSuccess());
  }

  @Test
  public void delete_article_should_reject_non_owner() {
    authenticate(other);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    assertThrows(
        NoAuthorizationException.class, () -> articleMutation.deleteArticle(article.getSlug()));
    verify(articleRepository, never()).remove(any());
  }

  @Test
  public void delete_article_should_throw_not_found_for_unknown_slug() {
    authenticate(owner);
    when(articleRepository.findBySlug("nope")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleMutation.deleteArticle("nope"));
    verify(articleRepository, never()).remove(any());
  }

  @Test
  public void delete_article_should_require_authentication() {
    anonymous();

    assertThrows(
        AuthenticationException.class, () -> articleMutation.deleteArticle(article.getSlug()));
    verifyNoInteractions(articleRepository);
  }
}
