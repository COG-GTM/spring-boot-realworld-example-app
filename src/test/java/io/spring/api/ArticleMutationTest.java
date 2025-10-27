package io.spring.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.api.TestWithCurrentUser;
import io.spring.graphql.ArticleMutation;
import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ArticleMutationTest extends GraphQLTestWithCurrentUser {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private io.spring.application.article.ArticleCommandService articleCommandService;
  @MockBean private ArticleRepository articleRepository;
  @MockBean private ArticleQueryService articleQueryService;
  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;

  private Article article;
  private ArticleData articleData;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    article = new Article("title", "desc", "body", Arrays.asList("java", "spring"), user.getId());
    articleData = new ArticleData(
      article.getId(),
      article.getSlug(),
      article.getTitle(),
      article.getDescription(),
      article.getBody(),
      false,
      0,
      new DateTime(),
      new DateTime(),
      Arrays.asList("java", "spring"),
      new ProfileData(user.getId(), username, "", "", false)
    );
  }

  @Test
  public void should_create_article_success() {
    when(articleCommandService.createArticle(any(io.spring.application.article.NewArticleParam.class), any())).thenReturn(article);
    when(articleQueryService.findById(any(), any())).thenReturn(Optional.of(articleData));

    String mutation = String.format(
      "mutation { createArticle(input: {title: \"%s\", description: \"%s\", body: \"%s\", tagList: [\"java\"]}) { article { title slug } } }",
      "Test Title", "Test Description", "Test Body"
    );

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.createArticle", Collections.emptyMap());
    assertNotNull(result);
  }

  @Test
  public void should_update_article_success() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(any(), any(io.spring.application.article.UpdateArticleParam.class))).thenReturn(article);
    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.of(articleData));

    String mutation = String.format(
      "mutation { updateArticle(slug: \"%s\", changes: {title: \"Updated Title\"}) { article { title } } }",
      article.getSlug()
    );

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.updateArticle", Collections.emptyMap());
    assertNotNull(result);
  }

  @Test
  public void should_favorite_article_success() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId()))).thenReturn(Optional.empty());
    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.of(articleData));

    String mutation = String.format(
      "mutation { favoriteArticle(slug: \"%s\") { article { slug favorited } } }",
      article.getSlug()
    );

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.favoriteArticle", Collections.emptyMap());
    assertNotNull(result);
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  public void should_unfavorite_article_success() {
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId()))).thenReturn(Optional.of(favorite));
    when(articleQueryService.findById(eq(article.getId()), any())).thenReturn(Optional.of(articleData));

    String mutation = String.format(
      "mutation { unfavoriteArticle(slug: \"%s\") { article { slug favorited } } }",
      article.getSlug()
    );

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.unfavoriteArticle", Collections.emptyMap());
    assertNotNull(result);
    verify(articleFavoriteRepository).remove(eq(favorite));
  }

  @Test
  public void should_delete_article_success() {
    when(articleRepository.findBySlug(eq(article.getSlug()))).thenReturn(Optional.of(article));

    String mutation = String.format(
      "mutation { deleteArticle(slug: \"%s\") { success } }",
      article.getSlug()
    );

    Map<String, Object> result = dgsQueryExecutor.executeAndExtractJsonPath(mutation, "data.deleteArticle", Collections.emptyMap());
    assertNotNull(result);
    assertTrue((Boolean) result.get("success"));
    verify(articleRepository).remove(eq(article));
  }
}
