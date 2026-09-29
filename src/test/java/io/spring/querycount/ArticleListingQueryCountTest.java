package io.spring.querycount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.service.JwtService;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

/**
 * End-to-end (REST + GraphQL) query-count benchmark for the article listing read paths. Every
 * listing must issue a constant number of SQL statements regardless of page size.
 */
@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=1")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(ArticleListingQueryCountTest.QueryCounterConfig.class)
public class ArticleListingQueryCountTest {
  private static final int AUTHORS = 3;
  private static final int ARTICLES_PER_AUTHOR = 10;
  private static final int SMALL_PAGE = 2;
  private static final int LARGE_PAGE = 10;
  private static final String ARTICLE_FIELDS =
      "edges { node { slug title favorited favoritesCount tagList author { username following } } }";

  @TestConfiguration
  static class QueryCounterConfig {
    @Bean
    QueryCounter queryCounter() {
      return new QueryCounter();
    }
  }

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private QueryCounter queryCounter;
  @Autowired private UserRepository userRepository;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private ArticleFavoriteRepository articleFavoriteRepository;
  @Autowired private JwtService jwtService;

  private String token;
  private Article favoritedArticle;
  private int favoritedArticleFavorites;

  @BeforeAll
  public void seed() {
    User viewer = new User("viewer@example.com", "viewer", "123", "", "");
    userRepository.save(viewer);
    token = "Token " + jwtService.toToken(viewer);

    List<User> fans = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      User fan = new User("fan" + i + "@example.com", "fan" + i, "123", "", "");
      userRepository.save(fan);
      fans.add(fan);
    }

    DateTime base = new DateTime().minusDays(1);
    int n = 0;
    for (int a = 0; a < AUTHORS; a++) {
      User author = new User("author" + a + "@example.com", "author" + a, "123", "bio", "img");
      userRepository.save(author);
      userRepository.saveRelation(new FollowRelation(viewer.getId(), author.getId()));
      for (int i = 0; i < ARTICLES_PER_AUTHOR; i++, n++) {
        Article article =
            new Article(
                "article " + a + " " + i,
                "desc",
                "body",
                Arrays.asList("java", "spring", "tag" + (n % 4)),
                author.getId(),
                base.plusMinutes(n));
        articleRepository.save(article);
        int favorites = n % (fans.size() + 1);
        for (int f = 0; f < favorites; f++) {
          articleFavoriteRepository.save(new ArticleFavorite(article.getId(), fans.get(f).getId()));
        }
        if (n % 2 == 0) {
          articleFavoriteRepository.save(new ArticleFavorite(article.getId(), viewer.getId()));
          favorites++;
        }
        if (favoritedArticle == null && favorites > 1) {
          favoritedArticle = article;
          favoritedArticleFavorites = favorites;
        }
      }
    }
  }

  private Map<String, IntFunction<RequestBuilder>> listingScenarios() {
    Map<String, IntFunction<RequestBuilder>> scenarios = new LinkedHashMap<>();
    scenarios.put("REST   GET /articles (anonymous)", l -> get("/articles?limit=" + l));
    scenarios.put(
        "REST   GET /articles (signed in)",
        l -> get("/articles?limit=" + l).header("Authorization", token));
    scenarios.put(
        "REST   GET /articles?tag=java (signed in)",
        l -> get("/articles?tag=java&limit=" + l).header("Authorization", token));
    scenarios.put(
        "REST   GET /articles/feed (signed in)",
        l -> get("/articles/feed?limit=" + l).header("Authorization", token));
    scenarios.put(
        "GQL    articles (anonymous)",
        l -> graphql("{ articles(first: " + l + ") { " + ARTICLE_FIELDS + " } }", null));
    scenarios.put(
        "GQL    articles (signed in)",
        l -> graphql("{ articles(first: " + l + ") { " + ARTICLE_FIELDS + " } }", token));
    scenarios.put(
        "GQL    feed (signed in)",
        l -> graphql("{ feed(first: " + l + ") { " + ARTICLE_FIELDS + " } }", token));
    scenarios.put(
        "GQL    profile.articles (signed in)",
        l ->
            graphql(
                "{ profile(username: \"author0\") { profile { articles(first: "
                    + l
                    + ") { "
                    + ARTICLE_FIELDS
                    + " } } } }",
                token));
    return scenarios;
  }

  private RequestBuilder graphql(String query, String authorization) {
    String body;
    try {
      body = objectMapper.writeValueAsString(Collections.singletonMap("query", query));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
    if (authorization == null) {
      return post("/graphql").contentType(MediaType.APPLICATION_JSON).content(body);
    }
    return post("/graphql")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .header("Authorization", authorization);
  }

  private static class Measurement {
    final int articles;
    final int queries;
    final Map<String, Integer> byStatement;

    Measurement(int articles, int queries, Map<String, Integer> byStatement) {
      this.articles = articles;
      this.queries = queries;
      this.byStatement = byStatement;
    }
  }

  private Measurement measure(RequestBuilder request) throws Exception {
    queryCounter.reset();
    String json = mvc.perform(request).andReturn().getResponse().getContentAsString();
    int queries = queryCounter.count();
    Map<String, Integer> byStatement = queryCounter.countsByStatement();
    List<Object> errors = JsonPath.parse(json).read("$..errors[*]");
    assertTrue(errors.isEmpty(), "unexpected GraphQL errors: " + json);
    List<Object> slugs = JsonPath.parse(json).read("$..slug");
    return new Measurement(slugs.size(), queries, byStatement);
  }

  @Test
  public void listing_query_count_is_independent_of_page_size() throws Exception {
    Map<String, Measurement[]> results = new LinkedHashMap<>();
    for (Map.Entry<String, IntFunction<RequestBuilder>> scenario : listingScenarios().entrySet()) {
      results.put(
          scenario.getKey(),
          new Measurement[] {
            measure(scenario.getValue().apply(SMALL_PAGE)),
            measure(scenario.getValue().apply(LARGE_PAGE))
          });
    }

    StringBuilder report = new StringBuilder("\n=== Article listing query counts ===\n");
    report.append(
        String.format(
            "%-42s | %8s %8s | %8s %8s%n",
            "scenario", "rows@" + SMALL_PAGE, "queries", "rows@" + LARGE_PAGE, "queries"));
    results.forEach(
        (name, m) ->
            report.append(
                String.format(
                    "%-42s | %8d %8d | %8d %8d%n",
                    name, m[0].articles, m[0].queries, m[1].articles, m[1].queries)));
    report.append("--- statements per request at page size ").append(LARGE_PAGE).append(" ---\n");
    results.forEach(
        (name, m) -> report.append(name).append(": ").append(m[1].byStatement).append('\n'));
    System.out.println(report);

    results.forEach(
        (name, m) -> {
          assertEquals(SMALL_PAGE, m[0].articles, name + " returned wrong page size");
          assertEquals(LARGE_PAGE, m[1].articles, name + " returned wrong page size");
          assertEquals(m[0].queries, m[1].queries, name + " query count grows with page size");
        });
  }

  @Test
  public void single_article_query_counts() throws Exception {
    String slug = favoritedArticle.getSlug();
    Map<String, RequestBuilder> scenarios = new LinkedHashMap<>();
    scenarios.put("REST   GET /articles/{slug} (anonymous)", get("/articles/" + slug));
    scenarios.put(
        "REST   GET /articles/{slug} (signed in)",
        get("/articles/" + slug).header("Authorization", token));
    String query =
        "{ article(slug: \"" + slug + "\") { slug favorited favoritesCount author { username } } }";
    scenarios.put("GQL    article(slug) (anonymous)", graphql(query, null));
    scenarios.put("GQL    article(slug) (signed in)", graphql(query, token));

    StringBuilder report = new StringBuilder("\n=== Single article query counts ===\n");
    for (Map.Entry<String, RequestBuilder> scenario : scenarios.entrySet()) {
      Measurement m = measure(scenario.getValue());
      report
          .append(String.format("%-42s | %8d queries ", scenario.getKey(), m.queries))
          .append(m.byStatement)
          .append('\n');
    }
    System.out.println(report);
  }

  @Test
  public void anonymous_single_article_reports_favorites_count() throws Exception {
    String json =
        mvc.perform(get("/articles/" + favoritedArticle.getSlug()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    int favoritesCount = JsonPath.parse(json).read("$.article.favoritesCount");
    assertEquals(favoritedArticleFavorites, favoritesCount);
  }
}
