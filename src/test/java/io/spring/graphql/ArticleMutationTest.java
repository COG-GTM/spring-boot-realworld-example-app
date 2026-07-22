package io.spring.graphql;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ArticleMutationTest extends GraphQLTestBase {

  @Mock private ArticleCommandService articleCommandService;
  @Mock private ArticleFavoriteRepository articleFavoriteRepository;
  @Mock private ArticleRepository articleRepository;

  private ArticleMutation articleMutation;
  private User user;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    articleMutation =
        new ArticleMutation(articleCommandService, articleFavoriteRepository, articleRepository);
    user = new User("email@test.com", "username", "pass", "", "");
  }

  private Article articleOf(User owner) {
    return new Article("title", "desc", "body", Arrays.asList("java"), owner.getId());
  }

  @Test
  public void should_create_article() {
    setCurrentUser(user);
    CreateArticleInput input =
        CreateArticleInput.newBuilder()
            .title("title")
            .description("desc")
            .body("body")
            .tagList(Arrays.asList("java"))
            .build();
    Article article = articleOf(user);
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);

    assertThat(result.getData(), instanceOf(ArticlePayload.class));
    assertThat(result.getLocalContext(), is(article));
  }

  @Test
  public void should_create_article_with_null_tag_list() {
    setCurrentUser(user);
    CreateArticleInput input =
        CreateArticleInput.newBuilder().title("title").description("desc").body("body").build();
    Article article = articleOf(user);
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user)))
        .thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(input);
    assertThat(result.getLocalContext(), is(article));
  }

  @Test
  public void should_throw_when_create_article_anonymous() {
    setAnonymous();
    CreateArticleInput input = CreateArticleInput.newBuilder().title("title").build();
    assertThrows(AuthenticationException.class, () -> articleMutation.createArticle(input));
  }

  @Test
  public void should_update_article_when_author() {
    setCurrentUser(user);
    Article article = articleOf(user);
    UpdateArticleInput input =
        UpdateArticleInput.newBuilder().title("new").body("new").description("new").build();
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any(UpdateArticleParam.class)))
        .thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.updateArticle("title", input);

    assertThat(result.getData(), instanceOf(ArticlePayload.class));
    assertThat(result.getLocalContext(), is(article));
  }

  @Test
  public void should_throw_when_update_article_not_found() {
    setCurrentUser(user);
    UpdateArticleInput input = UpdateArticleInput.newBuilder().title("new").build();
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class, () -> articleMutation.updateArticle("missing", input));
  }

  @Test
  public void should_throw_when_update_article_not_author() {
    User other = new User("other@test.com", "other", "pass", "", "");
    setCurrentUser(user);
    Article article = articleOf(other);
    UpdateArticleInput input = UpdateArticleInput.newBuilder().title("new").build();
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));
    assertThrows(
        NoAuthorizationException.class, () -> articleMutation.updateArticle("title", input));
  }

  @Test
  public void should_favorite_article() {
    setCurrentUser(user);
    Article article = articleOf(user);
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle("title");

    assertThat(result.getLocalContext(), is(article));
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  public void should_throw_when_favorite_article_not_found() {
    setCurrentUser(user);
    when(articleRepository.findBySlug(eq("missing"))).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> articleMutation.favoriteArticle("missing"));
  }

  @Test
  public void should_unfavorite_article() {
    setCurrentUser(user);
    Article article = articleOf(user);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("title");

    assertThat(result.getLocalContext(), is(article));
    verify(articleFavoriteRepository).remove(eq(favorite));
  }

  @Test
  public void should_delete_article_when_author() {
    setCurrentUser(user);
    Article article = articleOf(user);
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));

    DeletionStatus status = articleMutation.deleteArticle("title");

    assertThat(status.getSuccess(), is(true));
    verify(articleRepository).remove(eq(article));
  }

  @Test
  public void should_throw_when_delete_article_not_author() {
    User other = new User("other@test.com", "other", "pass", "", "");
    setCurrentUser(user);
    Article article = articleOf(other);
    when(articleRepository.findBySlug(eq("title"))).thenReturn(Optional.of(article));
    assertThrows(NoAuthorizationException.class, () -> articleMutation.deleteArticle("title"));
  }
}
