package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.exceptions.QueryException;
import io.spring.TestHelper;
import io.spring.application.data.ArticleData;
import io.spring.core.article.Article;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ArticleMutationTest extends DgsGraphQLTestBase {

  private Article articleOf(User author) {
    return new Article(
        "How to Test", "desc", "body", Arrays.asList("java", "spring"), author.getId());
  }

  @Test
  void should_create_article() {
    setAuthenticatedUser(user);
    Article article = articleOf(user);
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { createArticle(input: {title: \"How to Test\", description: \"desc\", body:"
                + " \"body\", tagList: [\"java\"]}) { article { slug title body } } }",
            "data.createArticle.article.slug");

    assertEquals(article.getSlug(), slug);
  }

  @Test
  void should_create_article_without_taglist() {
    setAuthenticatedUser(user);
    Article article = articleOf(user);
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String title =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { createArticle(input: {title: \"How to Test\", description: \"desc\", body:"
                + " \"body\"}) { article { title } } }",
            "data.createArticle.article.title");

    assertEquals(articleData.getTitle(), title);
  }

  @Test
  void should_reject_create_article_when_not_authenticated() {
    setAnonymous();

    QueryException error =
        assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "mutation { createArticle(input: {title: \"t\", description: \"d\", body: \"b\"})"
                        + " { article { slug } } }",
                    "data.createArticle"));

    assertFalse(error.getErrors().isEmpty());
  }

  @Test
  void should_update_article() {
    setAuthenticatedUser(user);
    Article article = articleOf(user);
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(article);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { updateArticle(slug: \""
                + article.getSlug()
                + "\", changes: {title: \"new\"}) { article { slug } } }",
            "data.updateArticle.article.slug");

    assertEquals(article.getSlug(), slug);
  }

  @Test
  void should_reject_update_article_when_not_found() {
    setAuthenticatedUser(user);
    when(articleRepository.findBySlug(any())).thenReturn(Optional.empty());

    QueryException error =
        assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "mutation { updateArticle(slug: \"missing\", changes: {title: \"new\"}) {"
                        + " article { slug } } }",
                    "data.updateArticle"));

    assertFalse(error.getErrors().isEmpty());
  }

  @Test
  void should_reject_update_article_when_not_author() {
    setAuthenticatedUser(user);
    User other = new User("other@test.com", "other", "123", "", "");
    Article article = articleOf(other);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    QueryException error =
        assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "mutation { updateArticle(slug: \""
                        + article.getSlug()
                        + "\", changes: {title: \"new\"}) { article { slug } } }",
                    "data.updateArticle"));

    assertFalse(error.getErrors().isEmpty());
  }

  @Test
  void should_favorite_article() {
    setAuthenticatedUser(user);
    Article article = articleOf(user);
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { favoriteArticle(slug: \""
                + article.getSlug()
                + "\") { article { slug } } }",
            "data.favoriteArticle.article.slug");

    assertEquals(article.getSlug(), slug);
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  void should_unfavorite_article() {
    setAuthenticatedUser(user);
    Article article = articleOf(user);
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(favorite));
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { unfavoriteArticle(slug: \""
                + article.getSlug()
                + "\") { article { slug } } }",
            "data.unfavoriteArticle.article.slug");

    assertEquals(article.getSlug(), slug);
    verify(articleFavoriteRepository).remove(eq(favorite));
  }

  @Test
  void should_delete_article() {
    setAuthenticatedUser(user);
    Article article = articleOf(user);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    Boolean success =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { deleteArticle(slug: \"" + article.getSlug() + "\") { success } }",
            "data.deleteArticle.success");

    assertTrue(success);
    verify(articleRepository).remove(eq(article));
  }

  @Test
  void should_reject_delete_article_when_not_author() {
    setAuthenticatedUser(user);
    User other = new User("other@test.com", "other", "123", "", "");
    Article article = articleOf(other);
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    QueryException error =
        assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "mutation { deleteArticle(slug: \"" + article.getSlug() + "\") { success } }",
                    "data.deleteArticle"));

    assertFalse(error.getErrors().isEmpty());
  }
}
