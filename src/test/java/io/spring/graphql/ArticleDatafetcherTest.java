package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.TestHelper;
import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.data.ArticleData;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = {
      DgsAutoConfiguration.class,
      ArticleDatafetcher.class
    })
public class ArticleDatafetcherTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private UserRepository userRepository;

  private User user;
  private ArticleData articleData;

  @BeforeEach
  public void setUp() {
    user = new User("test@example.com", "testuser", "password", "bio", "image");
    articleData = TestHelper.articleDataFixture("1", user);
  }

  @Test
  public void should_query_article_by_slug() {
    when(articleQueryService.findBySlug(eq("title-1"), any()))
        .thenReturn(Optional.of(articleData));

    String query =
        "query Article($slug: String!) { " + "  article(slug: $slug) { " + "    slug " + "    title " + "    description " + "    body " + "  } " + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", "title-1");

    ExecutionResult result = dgsQueryExecutor.execute(query, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> article = (Map<String, Object>) data.get("article");
    assertNotNull(article);
    assertEquals("title-1", article.get("slug"));
    assertEquals("title 1", article.get("title"));
    assertEquals("desc 1", article.get("description"));
    assertEquals("body 1", article.get("body"));
  }

  @Test
  public void should_return_null_for_nonexistent_article() {
    when(articleQueryService.findBySlug(eq("nonexistent"), any())).thenReturn(Optional.empty());

    String query =
        "query Article($slug: String!) { " + "  article(slug: $slug) { " + "    slug " + "    title " + "  } " + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("slug", "nonexistent");

    ExecutionResult result = dgsQueryExecutor.execute(query, variables);
    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_query_articles_with_pagination() {
    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> pager =
        new CursorPager<>(articles, CursorPager.Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(any(), any(), any(), any(), any()))
        .thenReturn(pager);

    String query =
        "query Articles($first: Int) { "
            + "  articles(first: $first) { "
            + "    edges { "
            + "      cursor "
            + "      node { "
            + "        slug "
            + "        title "
            + "      } "
            + "    } "
            + "    pageInfo { "
            + "      hasNextPage "
            + "      hasPreviousPage "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("first", 10);

    ExecutionResult result = dgsQueryExecutor.execute(query, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> articlesResult = (Map<String, Object>) data.get("articles");
    assertNotNull(articlesResult);
    List<Map<String, Object>> edges = (List<Map<String, Object>>) articlesResult.get("edges");
    assertNotNull(edges);
    assertEquals(1, edges.size());
    Map<String, Object> node = (Map<String, Object>) edges.get(0).get("node");
    assertEquals("title-1", node.get("slug"));
  }

  @Test
  public void should_query_articles_with_tag_filter() {
    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> pager =
        new CursorPager<>(articles, CursorPager.Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(
            eq("java"), any(), any(), any(), any()))
        .thenReturn(pager);

    String query =
        "query Articles($first: Int, $withTag: String) { "
            + "  articles(first: $first, withTag: $withTag) { "
            + "    edges { "
            + "      node { "
            + "        slug "
            + "        title "
            + "      } "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("first", 10);
    variables.put("withTag", "java");

    ExecutionResult result = dgsQueryExecutor.execute(query, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> articlesResult = (Map<String, Object>) data.get("articles");
    assertNotNull(articlesResult);
  }

  @Test
  public void should_query_articles_by_author() {
    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> pager =
        new CursorPager<>(articles, CursorPager.Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(
            any(), eq("testuser"), any(), any(), any()))
        .thenReturn(pager);

    String query =
        "query Articles($first: Int, $authoredBy: String) { "
            + "  articles(first: $first, authoredBy: $authoredBy) { "
            + "    edges { "
            + "      node { "
            + "        slug "
            + "        title "
            + "      } "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("first", 10);
    variables.put("authoredBy", "testuser");

    ExecutionResult result = dgsQueryExecutor.execute(query, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> articlesResult = (Map<String, Object>) data.get("articles");
    assertNotNull(articlesResult);
  }

  @Test
  public void should_query_articles_favorited_by_user() {
    List<ArticleData> articles = Arrays.asList(articleData);
    CursorPager<ArticleData> pager =
        new CursorPager<>(articles, CursorPager.Direction.NEXT, false);

    when(articleQueryService.findRecentArticlesWithCursor(
            any(), any(), eq("testuser"), any(), any()))
        .thenReturn(pager);

    String query =
        "query Articles($first: Int, $favoritedBy: String) { "
            + "  articles(first: $first, favoritedBy: $favoritedBy) { "
            + "    edges { "
            + "      node { "
            + "        slug "
            + "        title "
            + "        favorited "
            + "      } "
            + "    } "
            + "  } "
            + "}";

    Map<String, Object> variables = new HashMap<>();
    variables.put("first", 10);
    variables.put("favoritedBy", "testuser");

    ExecutionResult result = dgsQueryExecutor.execute(query, variables);
    assertNotNull(result);
    assertTrue(result.getErrors().isEmpty());

    Map<String, Object> data = result.getData();
    assertNotNull(data);
    Map<String, Object> articlesResult = (Map<String, Object>) data.get("articles");
    assertNotNull(articlesResult);
  }

  @Test
  public void should_throw_error_when_both_first_and_last_are_null() {
    String query =
        "query Articles { "
            + "  articles { "
            + "    edges { "
            + "      node { "
            + "        slug "
            + "      } "
            + "    } "
            + "  } "
            + "}";

    ExecutionResult result = dgsQueryExecutor.execute(query);
    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }
}
