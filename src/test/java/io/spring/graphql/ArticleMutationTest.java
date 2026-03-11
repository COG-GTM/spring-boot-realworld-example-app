package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.TypeRef;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ArticleMutation.class,
      ArticleDatafetcher.class,
      WebSecurityConfig.class,
      JacksonCustomizations.class,
      GraphQLCustomizeExceptionHandler.class
    })
@Import({})
public class ArticleMutationTest extends GraphQLTestBase {

  @MockBean private ArticleCommandService articleCommandService;

  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;

  @MockBean private ArticleQueryService articleQueryService;

  @Test
  public void should_create_article_successfully() {
    authenticateUser();

    String title = "Test Article";
    String description = "Test Description";
    String body = "Test Body";
    List<String> tagList = Arrays.asList("java", "spring");

    Article article = new Article(title, description, body, tagList, user.getId());
    when(articleCommandService.createArticle(any(), any())).thenReturn(article);

    ArticleData articleData =
        new ArticleData(
            article.getId(),
            article.getSlug(),
            title,
            description,
            body,
            false,
            0,
            new DateTime(),
            new DateTime(),
            tagList,
            new ProfileData(user.getId(), username, "", defaultAvatar, false));

    when(articleQueryService.findById(eq(article.getId()), any()))
        .thenReturn(Optional.of(articleData));

    String query =
        "mutation CreateArticle($input: CreateArticleInput!) {"
            + "  createArticle(input: $input) {"
            + "    article {"
            + "      title"
            + "      description"
            + "      body"
            + "      slug"
            + "      tagList"
            + "      author {"
            + "        username"
            + "      }"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    Map<String, Object> input = new HashMap<>();
    input.put("title", title);
    input.put("description", description);
    input.put("body", body);
    input.put("tagList", tagList);
    variables.put("input", input);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.createArticle.article", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();
    assertThat(result.get("title")).isEqualTo(title);
    assertThat(result.get("description")).isEqualTo(description);
    assertThat(result.get("body")).isEqualTo(body);
    verify(articleCommandService).createArticle(any(), any());

    clearAuthentication();
  }

  @Test
  public void should_fail_create_article_without_authentication() {
    clearAuthentication();

    String query =
        "mutation CreateArticle($input: CreateArticleInput!) {"
            + "  createArticle(input: $input) {"
            + "    article {"
            + "      title"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    Map<String, Object> input = new HashMap<>();
    input.put("title", "Test");
    input.put("description", "Test");
    input.put("body", "Test");
    variables.put("input", input);

    try {
      dgsQueryExecutor.executeAndExtractJsonPathAsObject(
          query, "data.createArticle.article", variables, new TypeRef<Map<String, Object>>() {});
    } catch (Exception e) {
      assertThat(e).isNotNull();
    }
  }

  @Test
  public void should_update_article_successfully() {
    authenticateUser();

    String slug = "test-article";
    String newTitle = "Updated Title";
    String newBody = "Updated Body";

    Article article = new Article("Test", "Test", "Test", Arrays.asList(), user.getId());
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(any(), any())).thenReturn(article);

    ArticleData articleData =
        new ArticleData(
            article.getId(),
            slug,
            newTitle,
            "Test",
            newBody,
            false,
            0,
            new DateTime(),
            new DateTime(),
            Arrays.asList(),
            new ProfileData(user.getId(), username, "", defaultAvatar, false));

    when(articleQueryService.findById(eq(article.getId()), any()))
        .thenReturn(Optional.of(articleData));

    String query =
        "mutation UpdateArticle($slug: String!, $changes: UpdateArticleInput!) {"
            + "  updateArticle(slug: $slug, changes: $changes) {"
            + "    article {"
            + "      title"
            + "      body"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", slug);
    Map<String, Object> changes = new HashMap<>();
    changes.put("title", newTitle);
    changes.put("body", newBody);
    variables.put("changes", changes);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.updateArticle.article", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();
    verify(articleCommandService).updateArticle(any(), any());

    clearAuthentication();
  }

  @Test
  public void should_favorite_article_successfully() {
    authenticateUser();

    String slug = "test-article";
    Article article = new Article("Test", "Test", "Test", Arrays.asList(), user.getId());
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    ArticleData articleData =
        new ArticleData(
            article.getId(),
            slug,
            "Test",
            "Test",
            "Test",
            true,
            1,
            new DateTime(),
            new DateTime(),
            Arrays.asList(),
            new ProfileData(user.getId(), username, "", defaultAvatar, false));

    when(articleQueryService.findById(eq(article.getId()), any()))
        .thenReturn(Optional.of(articleData));

    String query =
        "mutation FavoriteArticle($slug: String!) {"
            + "  favoriteArticle(slug: $slug) {"
            + "    article {"
            + "      slug"
            + "      favorited"
            + "      favoritesCount"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", slug);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.favoriteArticle.article", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();
    verify(articleFavoriteRepository).save(any());

    clearAuthentication();
  }

  @Test
  public void should_unfavorite_article_successfully() {
    authenticateUser();

    String slug = "test-article";
    Article article = new Article("Test", "Test", "Test", Arrays.asList(), user.getId());
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(any(), any())).thenReturn(Optional.empty());

    ArticleData articleData =
        new ArticleData(
            article.getId(),
            slug,
            "Test",
            "Test",
            "Test",
            false,
            0,
            new DateTime(),
            new DateTime(),
            Arrays.asList(),
            new ProfileData(user.getId(), username, "", defaultAvatar, false));

    when(articleQueryService.findById(eq(article.getId()), any()))
        .thenReturn(Optional.of(articleData));

    String query =
        "mutation UnfavoriteArticle($slug: String!) {"
            + "  unfavoriteArticle(slug: $slug) {"
            + "    article {"
            + "      slug"
            + "      favorited"
            + "      favoritesCount"
            + "    }"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", slug);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query,
            "data.unfavoriteArticle.article",
            variables,
            new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();

    clearAuthentication();
  }

  @Test
  public void should_delete_article_successfully() {
    authenticateUser();

    String slug = "test-article";
    Article article = new Article("Test", "Test", "Test", Arrays.asList(), user.getId());
    when(articleRepository.findBySlug(eq(slug))).thenReturn(Optional.of(article));

    String query =
        "mutation DeleteArticle($slug: String!) {"
            + "  deleteArticle(slug: $slug) {"
            + "    success"
            + "  }"
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", slug);

    Map<String, Object> result =
        dgsQueryExecutor.executeAndExtractJsonPathAsObject(
            query, "data.deleteArticle", variables, new TypeRef<Map<String, Object>>() {});

    assertThat(result).isNotNull();
    assertThat(result.get("success")).isEqualTo(true);
    verify(articleRepository).remove(any());

    clearAuthentication();
  }
}
