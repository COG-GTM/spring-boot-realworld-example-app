package io.spring.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import graphql.ExecutionResult;
import io.spring.TestHelper;
import io.spring.application.data.ArticleData;
import io.spring.core.article.Article;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArticleMutationTest extends GraphQLTestBase {

  private Article article;
  private ArticleData articleData;

  @BeforeEach
  public void setUpFixtures() {
    article = new Article("title", "desc", "body", Arrays.asList("java"), user.getId());
    articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);
    when(articleQueryService.findById(eq(article.getId()), any()))
        .thenReturn(Optional.of(articleData));
  }

  @Test
  public void should_create_article() {
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "mutation { createArticle(input: {title: \"title\", description: \"desc\", body: \"body\", tagList: [\"java\"]}) { article { slug } } }",
            "data.createArticle.article.slug");

    Assertions.assertEquals(articleData.getSlug(), slug);
    verify(articleCommandService).createArticle(any(), eq(user));
  }

  @Test
  public void should_reject_create_article_when_anonymous() {
    logout();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { createArticle(input: {title: \"t\", description: \"d\", body: \"b\", tagList: []}) { article { slug } } }");

    Assertions.assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_update_article_when_author() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(article);

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "mutation { updateArticle(slug: \"%s\", changes: {title: \"new\"}) { article { slug } } }",
                article.getSlug()),
            "data.updateArticle.article.slug");

    Assertions.assertEquals(articleData.getSlug(), slug);
  }

  @Test
  public void should_reject_update_article_when_not_author() {
    Article other = new Article("t", "d", "b", Arrays.asList("java"), "another-user-id");
    when(articleRepository.findBySlug(eq(other.getSlug()))).thenReturn(Optional.of(other));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format(
                "mutation { updateArticle(slug: \"%s\", changes: {title: \"new\"}) { article { slug } } }",
                other.getSlug()));

    Assertions.assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_favorite_article() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    String slug =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format(
                "mutation { favoriteArticle(slug: \"%s\") { article { slug } } }", article.getSlug()),
            "data.favoriteArticle.article.slug");

    Assertions.assertEquals(articleData.getSlug(), slug);
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  public void should_unfavorite_article() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(favorite));

    dgsQueryExecutor.executeAndExtractJsonPath(
        String.format(
            "mutation { unfavoriteArticle(slug: \"%s\") { article { slug } } }", article.getSlug()),
        "data.unfavoriteArticle.article.slug");

    verify(articleFavoriteRepository).remove(favorite);
  }

  @Test
  public void should_delete_article_when_author() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    Boolean success =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format("mutation { deleteArticle(slug: \"%s\") { success } }", article.getSlug()),
            "data.deleteArticle.success");

    Assertions.assertTrue(success);
    verify(articleRepository).remove(article);
  }

  @Test
  public void should_reject_delete_article_when_not_author() {
    Article other = new Article("t", "d", "b", Arrays.asList("java"), "another-user-id");
    when(articleRepository.findBySlug(eq(other.getSlug()))).thenReturn(Optional.of(other));

    ExecutionResult result =
        dgsQueryExecutor.execute(
            String.format("mutation { deleteArticle(slug: \"%s\") { success } }", other.getSlug()));

    Assertions.assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_return_error_when_article_to_update_not_found() {
    when(articleRepository.findBySlug(any())).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { updateArticle(slug: \"missing\", changes: {title: \"new\"}) { article { slug } } }");

    Assertions.assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_use_current_user_from_context() {
    User another = new User("a@b.com", "another", "123", "", "");
    loginAs(another);
    Article owned = new Article("owned", "d", "b", Arrays.asList("java"), another.getId());
    when(articleRepository.findBySlug(eq(owned.getSlug()))).thenReturn(Optional.of(owned));

    Boolean success =
        dgsQueryExecutor.executeAndExtractJsonPath(
            String.format("mutation { deleteArticle(slug: \"%s\") { success } }", owned.getSlug()),
            "data.deleteArticle.success");

    Assertions.assertTrue(success);
  }
}
