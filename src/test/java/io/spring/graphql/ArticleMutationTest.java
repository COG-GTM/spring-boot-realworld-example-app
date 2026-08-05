package io.spring.graphql;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

class ArticleMutationTest {

  private ArticleCommandService articleCommandService;
  private ArticleFavoriteRepository articleFavoriteRepository;
  private ArticleRepository articleRepository;
  private ArticleMutation articleMutation;

  private User currentUser;

  @BeforeEach
  void setUp() {
    articleCommandService = mock(ArticleCommandService.class);
    articleFavoriteRepository = mock(ArticleFavoriteRepository.class);
    articleRepository = mock(ArticleRepository.class);
    articleMutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    currentUser = new User("john@example.com", "john", "123", "bio", "avatar");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setCurrentUser(User user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  private void setAnonymous() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
  }

  private Article articleOwnedBy(User user) {
    return new Article("title", "description", "body", Arrays.asList("java"), user.getId());
  }

  // ----- createArticle -----

  @Test
  void createArticle_throws_authentication_when_no_current_user() {
    setAnonymous();
    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("t").description("d").body("b").build();
    assertThrows(AuthenticationException.class, () -> articleMutation.createArticle(input));
    verify(articleCommandService, never()).createArticle(any(), any());
  }

  @Test
  void createArticle_happy_path_passes_params_and_sets_local_context() {
    setCurrentUser(currentUser);
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("How to Test")
            .description("A guide")
            .body("Some body")
            .tagList(Arrays.asList("java", "test"))
            .build();
    Article created = articleOwnedBy(currentUser);
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(currentUser)))
        .thenReturn(created);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(currentUser));
    NewArticleParam param = captor.getValue();
    assertThat(param.getTitle(), is("How to Test"));
    assertThat(param.getDescription(), is("A guide"));
    assertThat(param.getBody(), is("Some body"));
    assertThat(param.getTagList(), is(Arrays.asList("java", "test")));

    assertThat(result.getData(), is(notNullValue()));
    assertThat(result.getLocalContext(), is(sameInstance(created)));
  }

  @Test
  void createArticle_null_tag_list_defaults_to_empty() {
    setCurrentUser(currentUser);
    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("t").description("d").body("b").build();
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(currentUser)))
        .thenReturn(articleOwnedBy(currentUser));

    articleMutation.createArticle(input);

    ArgumentCaptor<NewArticleParam> captor = ArgumentCaptor.forClass(NewArticleParam.class);
    verify(articleCommandService).createArticle(captor.capture(), eq(currentUser));
    assertThat(captor.getValue().getTagList(), is(Collections.emptyList()));
  }

  // ----- updateArticle -----

  @Test
  void updateArticle_throws_not_found_when_article_missing() {
    setCurrentUser(currentUser);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());
    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder().title("t").body("b").description("d").build();

    assertThrows(
        ResourceNotFoundException.class, () -> articleMutation.updateArticle("missing", changes));
  }

  @Test
  void updateArticle_throws_authentication_when_no_current_user() {
    setAnonymous();
    Article article = articleOwnedBy(currentUser);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder().title("t").body("b").description("d").build();

    assertThrows(
        AuthenticationException.class,
        () -> articleMutation.updateArticle(article.getSlug(), changes));
  }

  @Test
  void updateArticle_throws_no_authorization_when_not_owner() {
    setCurrentUser(currentUser);
    User otherUser = new User("other@example.com", "other", "123", "", "");
    Article article = articleOwnedBy(otherUser);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder().title("t").body("b").description("d").build();

    assertThrows(
        NoAuthorizationException.class,
        () -> articleMutation.updateArticle(article.getSlug(), changes));
    verify(articleCommandService, never()).updateArticle(any(), any());
  }

  @Test
  void updateArticle_happy_path_updates_and_sets_local_context() {
    setCurrentUser(currentUser);
    Article article = articleOwnedBy(currentUser);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    Article updated = articleOwnedBy(currentUser);
    when(articleCommandService.updateArticle(eq(article), any(UpdateArticleParam.class)))
        .thenReturn(updated);
    UpdateArticleInput changes =
        UpdateArticleInput.newBuilder()
            .title("new title")
            .body("new body")
            .description("new desc")
            .build();

    DataFetcherResult<ArticlePayload> result =
        articleMutation.updateArticle(article.getSlug(), changes);

    ArgumentCaptor<UpdateArticleParam> captor = ArgumentCaptor.forClass(UpdateArticleParam.class);
    verify(articleCommandService).updateArticle(eq(article), captor.capture());
    UpdateArticleParam param = captor.getValue();
    assertThat(param.getTitle(), is("new title"));
    assertThat(param.getBody(), is("new body"));
    assertThat(param.getDescription(), is("new desc"));
    assertThat(result.getLocalContext(), is(sameInstance(updated)));
  }

  // ----- favoriteArticle -----

  @Test
  void favoriteArticle_throws_authentication_when_no_current_user() {
    setAnonymous();
    assertThrows(AuthenticationException.class, () -> articleMutation.favoriteArticle("slug"));
    verify(articleFavoriteRepository, never()).save(any());
  }

  @Test
  void favoriteArticle_throws_not_found_when_article_missing() {
    setCurrentUser(currentUser);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> articleMutation.favoriteArticle("missing"));
  }

  @Test
  void favoriteArticle_happy_path_saves_favorite() {
    setCurrentUser(currentUser);
    Article article = articleOwnedBy(currentUser);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle(article.getSlug());

    ArgumentCaptor<ArticleFavorite> captor = ArgumentCaptor.forClass(ArticleFavorite.class);
    verify(articleFavoriteRepository).save(captor.capture());
    assertThat(captor.getValue().getArticleId(), is(article.getId()));
    assertThat(captor.getValue().getUserId(), is(currentUser.getId()));
    assertThat(result.getLocalContext(), is(sameInstance(article)));
  }

  // ----- unfavoriteArticle -----

  @Test
  void unfavoriteArticle_throws_authentication_when_no_current_user() {
    setAnonymous();
    assertThrows(AuthenticationException.class, () -> articleMutation.unfavoriteArticle("slug"));
  }

  @Test
  void unfavoriteArticle_throws_not_found_when_article_missing() {
    setCurrentUser(currentUser);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class, () -> articleMutation.unfavoriteArticle("missing"));
  }

  @Test
  void unfavoriteArticle_removes_existing_favorite() {
    setCurrentUser(currentUser);
    Article article = articleOwnedBy(currentUser);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), currentUser.getId());
    when(articleFavoriteRepository.find(article.getId(), currentUser.getId()))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle(article.getSlug());

    verify(articleFavoriteRepository).remove(favorite);
    assertThat(result.getLocalContext(), is(sameInstance(article)));
  }

  @Test
  void unfavoriteArticle_no_op_when_favorite_absent() {
    setCurrentUser(currentUser);
    Article article = articleOwnedBy(currentUser);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), currentUser.getId()))
        .thenReturn(Optional.empty());

    articleMutation.unfavoriteArticle(article.getSlug());

    verify(articleFavoriteRepository, never()).remove(any());
  }

  // ----- deleteArticle -----

  @Test
  void deleteArticle_throws_authentication_when_no_current_user() {
    setAnonymous();
    assertThrows(AuthenticationException.class, () -> articleMutation.deleteArticle("slug"));
    verify(articleRepository, never()).remove(any());
  }

  @Test
  void deleteArticle_throws_not_found_when_article_missing() {
    setCurrentUser(currentUser);
    when(articleRepository.findBySlug("missing")).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> articleMutation.deleteArticle("missing"));
  }

  @Test
  void deleteArticle_throws_no_authorization_when_not_owner() {
    setCurrentUser(currentUser);
    User otherUser = new User("other@example.com", "other", "123", "", "");
    Article article = articleOwnedBy(otherUser);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    assertThrows(
        NoAuthorizationException.class, () -> articleMutation.deleteArticle(article.getSlug()));
    verify(articleRepository, never()).remove(any());
  }

  @Test
  void deleteArticle_happy_path_removes_article() {
    setCurrentUser(currentUser);
    Article article = articleOwnedBy(currentUser);
    when(articleRepository.findBySlug(article.getSlug())).thenReturn(Optional.of(article));

    DeletionStatus status = articleMutation.deleteArticle(article.getSlug());

    verify(articleRepository).remove(article);
    assertThat(status.getSuccess(), is(true));
  }
}
