package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.application.article.ArticleCommandService;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ArticleMutation.class
    })
@TestPropertySource(properties = "dgs.graphql.schema-locations=classpath*:schema/**/*.graphqls")
public class ArticleMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleCommandService articleCommandService;

  @MockBean private ArticleFavoriteRepository articleFavoriteRepository;

  @MockBean private ArticleRepository articleRepository;

  private User user;
  private Article article;

  @BeforeEach
  public void setUp() {
    user = new User("test@example.com", "testuser", "password", "bio", "image");
    article =
        new Article(
            "Test Title",
            "Test Description",
            "Test Body",
            Arrays.asList("java", "spring"),
            user.getId(),
            new DateTime());
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList()));
  }

  @Test
  public void should_create_article_successfully() {
    when(articleCommandService.createArticle(any(), any())).thenReturn(article);

    String mutation =
        "mutation CreateArticle($input: CreateArticleInput!) { "
            + "  createArticle(input: $input) { "
            + "    article { "
            + "      slug "
            + "      title "
            + "      description "
            + "      body "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    Map<String, Object> input = new HashMap<>();
    input.put("title", "Test Title");
    input.put("description", "Test Description");
    input.put("body", "Test Body");
    input.put("tagList", Arrays.asList("java", "spring"));
    variables.put("input", input);

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> createArticle = (Map<String, Object>) data.get("createArticle");
    assertNotNull(createArticle);

    verify(articleCommandService).createArticle(any(), any());
  }

  @Test
  public void should_create_article_without_tags() {
    when(articleCommandService.createArticle(any(), any())).thenReturn(article);

    String mutation =
        "mutation CreateArticle($input: CreateArticleInput!) { "
            + "  createArticle(input: $input) { "
            + "    article { "
            + "      slug "
            + "      title "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    Map<String, Object> input = new HashMap<>();
    input.put("title", "Test Title");
    input.put("description", "Test Description");
    input.put("body", "Test Body");
    variables.put("input", input);

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    verify(articleCommandService).createArticle(any(), any());
  }

  @Test
  public void should_update_article_successfully() {
    when(articleRepository.findBySlug(eq("test-title"))).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(any(), any())).thenReturn(article);

    String mutation =
        "mutation UpdateArticle($slug: String!, $changes: UpdateArticleInput!) { "
            + "  updateArticle(slug: $slug, changes: $changes) { "
            + "    article { "
            + "      slug "
            + "      title "
            + "      description "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", "test-title");
    Map<String, Object> changes = new HashMap<>();
    changes.put("title", "Updated Title");
    changes.put("description", "Updated Description");
    variables.put("changes", changes);

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> updateArticle = (Map<String, Object>) data.get("updateArticle");
    assertNotNull(updateArticle);

    verify(articleCommandService).updateArticle(any(), any());
  }

  @Test
  public void should_fail_update_article_when_not_found() {
    when(articleRepository.findBySlug(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation =
        "mutation UpdateArticle($slug: String!, $changes: UpdateArticleInput!) { "
            + "  updateArticle(slug: $slug, changes: $changes) { "
            + "    article { "
            + "      slug "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", "nonexistent");
    Map<String, Object> changes = new HashMap<>();
    changes.put("title", "Updated Title");
    variables.put("changes", changes);

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_favorite_article_successfully() {
    when(articleRepository.findBySlug(eq("test-title"))).thenReturn(Optional.of(article));

    String mutation =
        "mutation FavoriteArticle($slug: String!) { "
            + "  favoriteArticle(slug: $slug) { "
            + "    article { "
            + "      slug "
            + "      favorited "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", "test-title");

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> favoriteArticle = (Map<String, Object>) data.get("favoriteArticle");
    assertNotNull(favoriteArticle);

    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
  }

  @Test
  public void should_unfavorite_article_successfully() {
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    when(articleRepository.findBySlug(eq("test-title"))).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(eq(article.getId()), eq(user.getId())))
        .thenReturn(Optional.of(favorite));

    String mutation =
        "mutation UnfavoriteArticle($slug: String!) { "
            + "  unfavoriteArticle(slug: $slug) { "
            + "    article { "
            + "      slug "
            + "      favorited "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", "test-title");

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> unfavoriteArticle = (Map<String, Object>) data.get("unfavoriteArticle");
    assertNotNull(unfavoriteArticle);

    verify(articleFavoriteRepository).remove(any(ArticleFavorite.class));
  }

  @Test
  public void should_delete_article_successfully() {
    when(articleRepository.findBySlug(eq("test-title"))).thenReturn(Optional.of(article));

    String mutation =
        "mutation DeleteArticle($slug: String!) { "
            + "  deleteArticle(slug: $slug) { "
            + "    success "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", "test-title");

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> deleteArticle = (Map<String, Object>) data.get("deleteArticle");
    assertNotNull(deleteArticle);
    assertEquals(true, deleteArticle.get("success"));

    verify(articleRepository).remove(any(Article.class));
  }

  @Test
  public void should_fail_delete_article_when_not_found() {
    when(articleRepository.findBySlug(eq("nonexistent"))).thenReturn(Optional.empty());

    String mutation =
        "mutation DeleteArticle($slug: String!) { "
            + "  deleteArticle(slug: $slug) { "
            + "    success "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", "nonexistent");

    ExecutionResult result = dgsQueryExecutor.execute(mutation, variables);
    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }
}
