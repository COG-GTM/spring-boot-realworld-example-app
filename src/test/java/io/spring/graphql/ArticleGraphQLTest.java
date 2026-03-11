package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import graphql.ExecutionResult;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ArticleGraphQLTest extends GraphQLTestBase {

  @Autowired private ArticleRepository articleRepository;

  @Autowired private UserRepository userRepository;

  private User user;
  private Article article;
  private String testUsername;

  @BeforeEach
  public void setUp() {
    String uniqueId = UUID.randomUUID().toString().substring(0, 8);
    testUsername = "testuser" + uniqueId;
    String testEmail = "test" + uniqueId + "@test.com";

    user = new User(testEmail, testUsername, "password123", "Test bio", "http://image.url");
    userRepository.save(user);

    article =
        new Article(
            "Test Article Title " + uniqueId,
            "Test description",
            "Test body content",
            Arrays.asList("java", "spring", "graphql"),
            user.getId());
    articleRepository.save(article);
  }

  @Test
  public void should_query_articles_without_filters() {
    String query =
        "query { "
            + "  articles(first: 10) { "
            + "    edges { "
            + "      node { "
            + "        slug "
            + "        title "
            + "        description "
            + "        body "
            + "        tagList "
            + "      } "
            + "      cursor "
            + "    } "
            + "    pageInfo { "
            + "      hasNextPage "
            + "      hasPreviousPage "
            + "    } "
            + "  } "
            + "}";

    ExecutionResult executionResult = dgsQueryExecutor.execute(query);
    assertTrue(
        executionResult.getErrors().isEmpty(),
        "GraphQL errors: " + executionResult.getErrors().toString());

    Map<String, Object> result = executionResult.getData();
    assertNotNull(result);

    Map<String, Object> articles = (Map<String, Object>) result.get("articles");
    assertNotNull(articles, "articles should not be null");

    List<Map<String, Object>> edges = (List<Map<String, Object>>) articles.get("edges");
    assertNotNull(edges);
    assertFalse(edges.isEmpty(), "edges should not be empty");

    Map<String, Object> firstEdge = edges.get(0);
    Map<String, Object> node = (Map<String, Object>) firstEdge.get("node");
    assertNotNull(node.get("slug"));
    assertNotNull(node.get("title"));
    assertNotNull(node.get("description"));
    assertNotNull(node.get("body"));
  }

  @Test
  public void should_query_articles_with_tag_filter() {
    String query =
        "query { "
            + "  articles(first: 10, withTag: \"java\") { "
            + "    edges { "
            + "      node { "
            + "        slug "
            + "        title "
            + "        tagList "
            + "      } "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> result = dgsQueryExecutor.execute(query).getData();
    assertNotNull(result);

    Map<String, Object> articles = (Map<String, Object>) result.get("articles");
    List<Map<String, Object>> edges = (List<Map<String, Object>>) articles.get("edges");
    assertFalse(edges.isEmpty());

    Map<String, Object> node = (Map<String, Object>) edges.get(0).get("node");
    List<String> tagList = (List<String>) node.get("tagList");
    assertTrue(tagList.contains("java"));
  }

  @Test
  public void should_query_articles_with_author_filter() {
    String query =
        "query { "
            + "  articles(first: 10, authoredBy: \""
            + testUsername
            + "\") { "
            + "    edges { "
            + "      node { "
            + "        slug "
            + "        title "
            + "        author { "
            + "          username "
            + "        } "
            + "      } "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> result = dgsQueryExecutor.execute(query).getData();
    assertNotNull(result);

    Map<String, Object> articles = (Map<String, Object>) result.get("articles");
    List<Map<String, Object>> edges = (List<Map<String, Object>>) articles.get("edges");
    assertFalse(edges.isEmpty());

    Map<String, Object> node = (Map<String, Object>) edges.get(0).get("node");
    Map<String, Object> author = (Map<String, Object>) node.get("author");
    assertEquals(testUsername, author.get("username"));
  }

  @Test
  public void should_query_single_article_by_slug() {
    String slug = article.getSlug();
    String query =
        "query { "
            + "  article(slug: \""
            + slug
            + "\") { "
            + "    slug "
            + "    title "
            + "    description "
            + "    body "
            + "    tagList "
            + "    favorited "
            + "    favoritesCount "
            + "    createdAt "
            + "    updatedAt "
            + "    author { "
            + "      username "
            + "      bio "
            + "      image "
            + "      following "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> result = dgsQueryExecutor.execute(query).getData();
    assertNotNull(result);

    Map<String, Object> articleResult = (Map<String, Object>) result.get("article");
    assertNotNull(articleResult);
    assertEquals(slug, articleResult.get("slug"));
    assertNotNull(articleResult.get("title"));
    assertEquals("Test description", articleResult.get("description"));
    assertEquals("Test body content", articleResult.get("body"));
    assertEquals(false, articleResult.get("favorited"));
    assertEquals(0, articleResult.get("favoritesCount"));

    Map<String, Object> author = (Map<String, Object>) articleResult.get("author");
    assertEquals(testUsername, author.get("username"));
  }

  @Test
  public void should_query_tags() {
    String query = "query { tags }";

    Map<String, Object> result = dgsQueryExecutor.execute(query).getData();
    assertNotNull(result);

    List<String> tags = (List<String>) result.get("tags");
    assertNotNull(tags);
    assertTrue(tags.contains("java"));
    assertTrue(tags.contains("spring"));
    assertTrue(tags.contains("graphql"));
  }

  @Test
  public void should_return_empty_for_nonexistent_tag_filter() {
    String query =
        "query { "
            + "  articles(first: 10, withTag: \"nonexistenttag\") { "
            + "    edges { "
            + "      node { "
            + "        slug "
            + "      } "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> result = dgsQueryExecutor.execute(query).getData();
    assertNotNull(result);

    Map<String, Object> articles = (Map<String, Object>) result.get("articles");
    List<Map<String, Object>> edges = (List<Map<String, Object>>) articles.get("edges");
    assertTrue(edges.isEmpty());
  }

  @Test
  public void should_return_empty_for_nonexistent_author_filter() {
    String query =
        "query { "
            + "  articles(first: 10, authoredBy: \"nonexistentuser\") { "
            + "    edges { "
            + "      node { "
            + "        slug "
            + "      } "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> result = dgsQueryExecutor.execute(query).getData();
    assertNotNull(result);

    Map<String, Object> articles = (Map<String, Object>) result.get("articles");
    List<Map<String, Object>> edges = (List<Map<String, Object>>) articles.get("edges");
    assertTrue(edges.isEmpty());
  }

  @Test
  public void should_handle_pagination_with_first_parameter() {
    Article secondArticle =
        new Article(
            "Second Article",
            "Second description",
            "Second body",
            Arrays.asList("test"),
            user.getId());
    articleRepository.save(secondArticle);

    String query =
        "query { "
            + "  articles(first: 1) { "
            + "    edges { "
            + "      node { "
            + "        slug "
            + "      } "
            + "      cursor "
            + "    } "
            + "    pageInfo { "
            + "      hasNextPage "
            + "      hasPreviousPage "
            + "      endCursor "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> result = dgsQueryExecutor.execute(query).getData();
    assertNotNull(result);

    Map<String, Object> articles = (Map<String, Object>) result.get("articles");
    List<Map<String, Object>> edges = (List<Map<String, Object>>) articles.get("edges");
    assertEquals(1, edges.size());

    Map<String, Object> pageInfo = (Map<String, Object>) articles.get("pageInfo");
    assertTrue((Boolean) pageInfo.get("hasNextPage"));
  }

  @Test
  public void should_query_article_with_comments_connection() {
    String slug = article.getSlug();
    String query =
        "query { "
            + "  article(slug: \""
            + slug
            + "\") { "
            + "    slug "
            + "    comments(first: 10) { "
            + "      edges { "
            + "        node { "
            + "          id "
            + "          body "
            + "        } "
            + "      } "
            + "      pageInfo { "
            + "        hasNextPage "
            + "      } "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> result = dgsQueryExecutor.execute(query).getData();
    assertNotNull(result);

    Map<String, Object> articleResult = (Map<String, Object>) result.get("article");
    assertNotNull(articleResult);

    Map<String, Object> comments = (Map<String, Object>) articleResult.get("comments");
    assertNotNull(comments);
    assertNotNull(comments.get("edges"));
    assertNotNull(comments.get("pageInfo"));
  }
}
