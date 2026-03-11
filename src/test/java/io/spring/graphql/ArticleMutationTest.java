package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import io.spring.application.ArticleQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class ArticleMutationTest extends GraphQLTestBase {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleCommandService articleCommandService;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;

  @MockBean private ArticleQueryService articleQueryService;

  @Test
  public void should_create_article_when_authenticated() {
    setAuthenticatedUser(user);

    Article article =
        new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java", "spring"), user.getId());
    when(articleCommandService.createArticle(any(), eq(user))).thenReturn(article);

    ArticleData articleData = createArticleData(article);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { createArticle(input: {title: \"Test Title\", description: \"Test Description\", body: \"Test Body\", tagList: [\"java\", \"spring\"]}) { article { slug title description body tagList } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createArticle");

    assertThat(result).isNotNull();
  }

  @Test
  public void should_fail_create_article_without_authentication() {
    clearAuthentication();

    String mutation =
        "mutation { createArticle(input: {title: \"Test Title\", description: \"Test Description\", body: \"Test Body\"}) { article { slug title } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_update_article_when_author() {
    setAuthenticatedUser(user);

    Article article =
        new Article("Old Title", "Old Description", "Old Body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    Article updatedArticle =
        new Article("New Title", "New Description", "New Body", Arrays.asList("java"), user.getId());
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(updatedArticle);

    ArticleData articleData = createArticleData(updatedArticle);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { updateArticle(slug: \""
            + article.getSlug()
            + "\", changes: {title: \"New Title\", description: \"New Description\", body: \"New Body\"}) { article { slug title description body } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateArticle");

    assertThat(result).isNotNull();
  }

  @Test
  public void should_fail_update_article_when_not_author() {
    User anotherUser = new User("another@test.com", "another", "123", "", "");
    setAuthenticatedUser(user);

    Article article =
        new Article("Title", "Description", "Body", Arrays.asList("java"), anotherUser.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    String mutation =
        "mutation { updateArticle(slug: \""
            + article.getSlug()
            + "\", changes: {title: \"New Title\"}) { article { slug title } } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_favorite_article_when_authenticated() {
    setAuthenticatedUser(user);

    Article article =
        new Article("Title", "Description", "Body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    ArticleData articleData = createArticleData(article);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { favoriteArticle(slug: \"" + article.getSlug() + "\") { article { slug favorited } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.favoriteArticle");

    assertThat(result).isNotNull();
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  public void should_unfavorite_article_when_authenticated() {
    setAuthenticatedUser(user);

    Article article =
        new Article("Title", "Description", "Body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId()))).thenReturn(Optional.of(favorite));

    ArticleData articleData = createArticleData(article);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String mutation =
        "mutation { unfavoriteArticle(slug: \"" + article.getSlug() + "\") { article { slug favorited } } }";

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfavoriteArticle");

    assertThat(result).isNotNull();
  }

  @Test
  public void should_delete_article_when_author() {
    setAuthenticatedUser(user);

    Article article =
        new Article("Title", "Description", "Body", Arrays.asList("java"), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    String mutation =
        "mutation { deleteArticle(slug: \"" + article.getSlug() + "\") { success } }";

    Boolean success = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteArticle.success");

    assertThat(success).isTrue();
    verify(articleRepository).remove(eq(article));
  }

  @Test
  public void should_fail_delete_article_when_not_author() {
    User anotherUser = new User("another@test.com", "another", "123", "", "");
    setAuthenticatedUser(user);

    Article article =
        new Article("Title", "Description", "Body", Arrays.asList("java"), anotherUser.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    String mutation =
        "mutation { deleteArticle(slug: \"" + article.getSlug() + "\") { success } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  public void should_fail_delete_nonexistent_article() {
    setAuthenticatedUser(user);
    when(articleRepository.findBySlug(any())).thenReturn(Optional.empty());

    String mutation = "mutation { deleteArticle(slug: \"nonexistent\") { success } }";

    var result = dgsQueryExecutor.execute(mutation);

    assertThat(result.getErrors()).isNotEmpty();
  }

  private ArticleData createArticleData(Article article) {
    DateTime now = new DateTime();
    List<String> tagList = article.getTags().stream().map(Tag::getName).collect(Collectors.toList());
    return new ArticleData(
        article.getId(),
        article.getSlug(),
        article.getTitle(),
        article.getDescription(),
        article.getBody(),
        false,
        0,
        now,
        now,
        tagList,
        new ProfileData(user.getId(), user.getUsername(), user.getBio(), user.getImage(), false));
  }
}
