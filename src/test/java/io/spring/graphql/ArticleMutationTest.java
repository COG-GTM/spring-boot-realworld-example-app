package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class ArticleMutationTest {

  private ArticleCommandService articleCommandService;
  private ArticleFavoriteRepository articleFavoriteRepository;
  private ArticleRepository articleRepository;
  private ArticleMutation articleMutation;

  private User user;

  @BeforeEach
  void setUp() {
    articleCommandService = org.mockito.Mockito.mock(ArticleCommandService.class);
    articleFavoriteRepository = org.mockito.Mockito.mock(ArticleFavoriteRepository.class);
    articleRepository = org.mockito.Mockito.mock(ArticleRepository.class);
    articleMutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    user = new User("john@example.com", "john", "123", "", "");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAs(User u) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(u, null));
  }

  private void authenticateAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  private Article articleBy(User author) {
    return new Article("How to Test", "desc", "body", Arrays.asList("java"), author.getId());
  }

  @Test
  void should_create_article_and_expose_it_via_local_context() {
    authenticateAs(user);
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("How to Test")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java", "spring"))
            .build();
    Article created = articleBy(user);
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(created);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    assertThat(result.getData()).isNotNull();
    assertThat(result.getLocalContext()).isEqualTo(created);

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    NewArticleParam param = captor.getValue();
    assertThat(param.getTitle()).isEqualTo("How to Test");
    assertThat(param.getDescription()).isEqualTo("desc");
    assertThat(param.getBody()).isEqualTo("body");
    assertThat(param.getTagList()).containsExactlyInAnyOrder("java", "spring");
  }

  @Test
  void should_default_tag_list_to_empty_when_null() {
    authenticateAs(user);
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("No Tags")
            .description("desc")
            .body("body")
            .tagList(null)
            .build();
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(articleBy(user));

    articleMutation.createArticle(input);

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(user));
    assertThat(captor.getValue().getTagList()).isEmpty();
  }

  @Test
  void should_reject_create_article_when_not_authenticated() {
    authenticateAnonymous();
    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("t").description("d").body("b").build();

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> articleMutation.createArticle(input));
    verify(articleCommandService, never()).createArticle(any(), any());
  }

  @Test
  void should_update_article_when_author() {
    authenticateAs(user);
    Article article = articleBy(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any(UpdateArticleParam.class)))
        .thenReturn(article);
    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder().title("new").body("nb").description("nd").build();

    DataFetcherResult<ArticlePayload> result =
        articleMutation.updateArticle(article.getSlug(), changes);

    assertThat(result.getLocalContext()).isEqualTo(article);
    verify(articleCommandService).updateArticle(eq(article), any(UpdateArticleParam.class));
  }

  @Test
  void should_reject_update_article_when_missing() {
    authenticateAs(user);
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(
            () ->
                articleMutation.updateArticle("missing", UpdateArticleInput.newBuilder().build()));
  }

  @Test
  void should_reject_update_article_when_not_author() {
    authenticateAs(user);
    User another = new User("a@a.com", "another", "123", "", "");
    Article article = articleBy(another);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    assertThatExceptionOfType(NoAuthorizationException.class)
        .isThrownBy(
            () ->
                articleMutation.updateArticle(
                    article.getSlug(), UpdateArticleInput.newBuilder().build()));
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  void should_favorite_article() {
    authenticateAs(user);
    Article article = articleBy(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle(article.getSlug());

    assertThat(result.getLocalContext()).isEqualTo(article);
    ArgumentCaptor<ArticleFavorite> captor = ArgumentCaptor.forClass(ArticleFavorite.class);
    verify(articleFavoriteRepository).save(captor.capture());
    assertThat(captor.getValue().getArticleId()).isEqualTo(article.getId());
    assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
  }

  @Test
  void should_reject_favorite_article_when_not_authenticated() {
    authenticateAnonymous();
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> articleMutation.favoriteArticle("slug"));
  }

  @Test
  void should_reject_favorite_article_when_missing() {
    authenticateAs(user);
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleMutation.favoriteArticle("missing"));
    verify(articleFavoriteRepository, never()).save(any());
  }

  @Test
  void should_unfavorite_article_and_remove_existing_favorite() {
    authenticateAs(user);
    Article article = articleBy(user);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(article.getSlug());

    assertThat(result.getLocalContext()).isEqualTo(article);
    verify(articleFavoriteRepository).remove(favorite);
  }

  @Test
  void should_unfavorite_article_without_removal_when_no_favorite() {
    authenticateAs(user);
    Article article = articleBy(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.empty());

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(article.getSlug());

    assertThat(result.getLocalContext()).isEqualTo(article);
    verify(articleFavoriteRepository, never()).remove(any());
  }

  @Test
  void should_delete_article_when_author() {
    authenticateAs(user);
    Article article = articleBy(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    DeletionStatus status = articleMutation.deleteArticle(article.getSlug());

    assertThat(status.getSuccess()).isTrue();
    verify(articleRepository).remove(article);
  }

  @Test
  void should_reject_delete_article_when_not_author() {
    authenticateAs(user);
    User another = new User("a@a.com", "another", "123", "", "");
    Article article = articleBy(another);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    assertThatExceptionOfType(NoAuthorizationException.class)
        .isThrownBy(() -> articleMutation.deleteArticle(article.getSlug()));
    verify(articleRepository, never()).remove(any());
  }

  @Test
  void should_reject_delete_article_when_missing() {
    authenticateAs(user);
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());

    assertThatExceptionOfType(ResourceNotFoundException.class)
        .isThrownBy(() -> articleMutation.deleteArticle("missing"));
  }

  @Test
  void should_reject_delete_article_when_not_authenticated() {
    authenticateAnonymous();
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> articleMutation.deleteArticle("slug"));
  }
}
