package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.TestHelper;
import io.spring.application.ArticleQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {DgsAutoConfiguration.class, ArticleMutation.class, ArticleDatafetcher.class})
public class ArticleMutationTest extends TestWithCurrentUser {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleCommandService articleCommandService;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;

  @MockBean private ArticleQueryService articleQueryService;

  @Test
  public void should_create_article_success() {
    setAuthenticatedUser(user);

    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java", "spring"), user.getId());
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);

    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { createArticle(input: { title: \"Test Title\", description: \"Test Description\", body: \"Test Body\", tagList: [\"java\", \"spring\"] }) { article { slug title body } } }";

    String resultSlug =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createArticle.article.slug");
    String resultTitle =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createArticle.article.title");

    assertThat(resultSlug).isEqualTo(articleData.getSlug());
    assertThat(resultTitle).isEqualTo(articleData.getTitle());
  }

  @Test
  public void should_update_article_success() {
    setAuthenticatedUser(user);

    Article article =
        new Article("Old Title", "Old Description", "Old Body", Arrays.asList("java"), user.getId());
    Article updatedArticle =
        new Article("New Title", "New Description", "New Body", Arrays.asList("java"), user.getId());
    ArticleData updatedArticleData = TestHelper.getArticleDataFromArticleAndUser(updatedArticle, user);

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(updatedArticle);
    when(articleQueryService.findById(eq(updatedArticle.getId()), any()))
        .thenReturn(Optional.of(updatedArticleData));

    String mutation =
        "mutation { updateArticle(slug: \""
            + article.getSlug()
            + "\", changes: { title: \"New Title\", description: \"New Description\", body: \"New Body\" }) { article { slug title body } } }";

    String resultTitle =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateArticle.article.title");

    assertThat(resultTitle).isEqualTo(updatedArticleData.getTitle());
  }

  @Test
  public void should_favorite_article_success() {
    setAuthenticatedUser(user);

    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), user.getId());
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { favoriteArticle(slug: \"" + article.getSlug() + "\") { article { slug favorited } } }";

    String resultSlug =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.favoriteArticle.article.slug");

    assertThat(resultSlug).isEqualTo(articleData.getSlug());
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  public void should_unfavorite_article_success() {
    setAuthenticatedUser(user);

    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), user.getId());
    ArticleData articleData = TestHelper.getArticleDataFromArticleAndUser(article, user);
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(favorite));
    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { unfavoriteArticle(slug: \"" + article.getSlug() + "\") { article { slug } } }";

    String resultSlug =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfavoriteArticle.article.slug");

    assertThat(resultSlug).isEqualTo(articleData.getSlug());
    verify(articleFavoriteRepository).remove(eq(favorite));
  }

  @Test
  public void should_delete_article_success() {
    setAuthenticatedUser(user);

    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), user.getId());

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    String mutation =
        "mutation { deleteArticle(slug: \"" + article.getSlug() + "\") { success } }";

    Boolean success =
        dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteArticle.success");

    assertThat(success).isTrue();
    verify(articleRepository).remove(eq(article));
  }

  @Test
  public void should_fail_delete_article_when_not_author() {
    User anotherUser = new User("another@test.com", "another", "123", "", "");
    setAuthenticatedUser(anotherUser);

    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), user.getId());

    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    String mutation =
        "mutation { deleteArticle(slug: \"" + article.getSlug() + "\") { success } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }
}
