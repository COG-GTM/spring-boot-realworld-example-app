package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

  private User currentUser;

  @BeforeEach
  public void setUp() {
    articleCommandService = mock(ArticleCommandService.class);
    articleFavoriteRepository = mock(ArticleFavoriteRepository.class);
    articleRepository = mock(ArticleRepository.class);
    articleMutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);

    currentUser = new User("author@example.com", "author", "password", "", "");
    authenticate(currentUser);
  }

  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  private void anonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  private Article articleBy(User user) {
    return new Article(
        "How to Test", "A description", "The body", Arrays.asList("java", "test"), user.getId());
  }

  @Test
  public void should_create_article_with_tag_list() {
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("How to Test")
            .description("A description")
            .body("The body")
            .tagList(Arrays.asList("java", "test"))
            .build();
    Article created = articleBy(currentUser);
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(currentUser)))
        .thenReturn(created);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    assertSame(created, result.getLocalContext());

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(currentUser));
    assertEquals("How to Test", captor.getValue().getTitle());
    assertEquals("A description", captor.getValue().getDescription());
    assertEquals("The body", captor.getValue().getBody());
    assertEquals(Arrays.asList("java", "test"), captor.getValue().getTagList());
  }

  @Test
  public void should_create_article_with_null_tag_list() {
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("No Tags")
            .description("A description")
            .body("The body")
            .tagList(null)
            .build();
    Article created = articleBy(currentUser);
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(currentUser)))
        .thenReturn(created);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    assertSame(created, result.getLocalContext());
    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(currentUser));
    assertEquals(Collections.emptyList(), captor.getValue().getTagList());
  }

  @Test
  public void should_throw_authentication_exception_when_create_unauthenticated() {
    anonymous();
    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("t").description("d").body("b").build();

    assertThrows(AuthenticationException.class, () -> articleMutation.createArticle(input));
    verify(articleCommandService, never()).createArticle(any(), any());
  }

  @Test
  public void should_update_article_success() {
    Article article = articleBy(currentUser);
    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder()
            .title("New Title")
            .body("New Body")
            .description("New Description")
            .build();
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any(UpdateArticleParam.class)))
        .thenReturn(article);

    DataFetcherResult<ArticlePayload> result =
        articleMutation.updateArticle(article.getSlug(), changes);

    assertSame(article, result.getLocalContext());
    ArgumentCaptor<UpdateArticleParam> captor = ArgumentCaptor.forClass(UpdateArticleParam.class);
    verify(articleCommandService).updateArticle(eq(article), captor.capture());
    assertEquals("New Title", captor.getValue().getTitle());
    assertEquals("New Body", captor.getValue().getBody());
    assertEquals("New Description", captor.getValue().getDescription());
  }

  @Test
  public void should_throw_resource_not_found_when_update_missing_article() {
    UpdateArticleInput changes = UpdateArticleInput.newBuilder().build();
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleMutation.updateArticle("missing", changes));
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  public void should_throw_no_authorization_when_update_others_article() {
    User other = new User("other@example.com", "other", "password", "", "");
    Article article = articleBy(other);
    UpdateArticleInput changes = UpdateArticleInput.newBuilder().build();
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    assertThrows(
        NoAuthorizationException.class,
        () -> articleMutation.updateArticle(article.getSlug(), changes));
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  public void should_throw_authentication_exception_when_update_unauthenticated() {
    anonymous();
    Article article = articleBy(currentUser);
    UpdateArticleInput changes = UpdateArticleInput.newBuilder().build();
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    assertThrows(
        AuthenticationException.class,
        () -> articleMutation.updateArticle(article.getSlug(), changes));
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  public void should_favorite_article_success() {
    Article article = articleBy(currentUser);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle(article.getSlug());

    assertSame(article, result.getLocalContext());
    ArgumentCaptor<ArticleFavorite> captor = ArgumentCaptor.forClass(ArticleFavorite.class);
    verify(articleFavoriteRepository).save(captor.capture());
    assertEquals(article.getId(), captor.getValue().getArticleId());
    assertEquals(currentUser.getId(), captor.getValue().getUserId());
  }

  @Test
  public void should_throw_resource_not_found_when_favorite_missing_article() {
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleMutation.favoriteArticle("missing"));
    verify(articleFavoriteRepository, never()).save(any());
  }

  @Test
  public void should_throw_authentication_exception_when_favorite_unauthenticated() {
    anonymous();

    assertThrows(AuthenticationException.class, () -> articleMutation.favoriteArticle("any-slug"));
    verify(articleFavoriteRepository, never()).save(any());
  }

  @Test
  public void should_unfavorite_article_success() {
    Article article = articleBy(currentUser);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), currentUser.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(currentUser.getId())))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(article.getSlug());

    assertSame(article, result.getLocalContext());
    verify(articleFavoriteRepository).remove(favorite);
  }

  @Test
  public void should_unfavorite_article_when_favorite_absent() {
    Article article = articleBy(currentUser);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(currentUser.getId())))
        .thenReturn(Optional.empty());

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(article.getSlug());

    assertSame(article, result.getLocalContext());
    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  public void should_throw_resource_not_found_when_unfavorite_missing_article() {
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> articleMutation.unfavoriteArticle("missing"));
    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  public void should_throw_authentication_exception_when_unfavorite_unauthenticated() {
    anonymous();

    assertThrows(
        AuthenticationException.class, () -> articleMutation.unfavoriteArticle("any-slug"));
    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  public void should_delete_article_success() {
    Article article = articleBy(currentUser);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    DeletionStatus status = articleMutation.deleteArticle(article.getSlug());

    assertTrue(status.getSuccess());
    verify(articleRepository).remove(article);
  }

  @Test
  public void should_throw_no_authorization_when_delete_others_article() {
    User other = new User("other@example.com", "other", "password", "", "");
    Article article = articleBy(other);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    assertThrows(
        NoAuthorizationException.class, () -> articleMutation.deleteArticle(article.getSlug()));
    verify(articleRepository, never()).remove(any());
  }

  @Test
  public void should_throw_resource_not_found_when_delete_missing_article() {
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> articleMutation.deleteArticle("missing"));
    verify(articleRepository, never()).remove(any());
  }

  @Test
  public void should_throw_authentication_exception_when_delete_unauthenticated() {
    anonymous();

    assertThrows(AuthenticationException.class, () -> articleMutation.deleteArticle("any-slug"));
    verify(articleRepository, never()).remove(any());
  }

  @Test
  public void should_return_false_deletion_status_is_not_used() {
    DeletionStatus status = DeletionStatus.newBuilder().success(false).build();
    assertFalse(status.getSuccess());
  }
}
