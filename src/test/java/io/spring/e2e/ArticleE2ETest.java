package io.spring.e2e;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

public class ArticleE2ETest extends BaseE2ETest {

  @Test
  void should_create_article_successfully() {
    String token = registerAndGetToken("article-author@test.com", "articleauthor", "password123");

    Map<String, Object> param =
        articleParam(
            "Test Article Title",
            "Test Description",
            "Test Body Content",
            new String[] {"java", "spring"});
    ResponseEntity<String> response = post("/articles", param, token);

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    JsonNode article = body.get("article");
    assertEquals("Test Article Title", article.get("title").asText());
    assertEquals("Test Description", article.get("description").asText());
    assertEquals("Test Body Content", article.get("body").asText());
    assertEquals("test-article-title", article.get("slug").asText());
    assertFalse(article.get("favorited").asBoolean());
    assertEquals(0, article.get("favoritesCount").asInt());
    assertNotNull(article.get("author"));
    assertEquals("articleauthor", article.get("author").get("username").asText());
  }

  @Test
  void should_get_article_by_slug() {
    String token = registerAndGetToken("getarticle@test.com", "getarticleuser", "password123");

    Map<String, Object> param =
        articleParam("Get Article Test", "Description", "Body", new String[] {"test"});
    post("/articles", param, token);

    ResponseEntity<String> response = get("/articles/get-article-test");

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    assertEquals("Get Article Test", body.get("article").get("title").asText());
  }

  @Test
  void should_update_article() {
    String token = registerAndGetToken("updateart@test.com", "updateartuser", "password123");

    Map<String, Object> createParam =
        articleParam("Original Title", "Original Desc", "Original Body", null);
    post("/articles", createParam, token);

    Map<String, Object> article = new HashMap<>();
    article.put("title", "Updated Title");
    article.put("body", "Updated Body");
    Map<String, Object> updateParam = new HashMap<>();
    updateParam.put("article", article);

    ResponseEntity<String> response = put("/articles/original-title", updateParam, token);

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    assertEquals("Updated Title", body.get("article").get("title").asText());
    assertEquals("Updated Body", body.get("article").get("body").asText());
  }

  @Test
  void should_delete_article() {
    String token = registerAndGetToken("deleteart@test.com", "deleteartuser", "password123");

    Map<String, Object> param = articleParam("Article To Delete", "Desc", "Body", null);
    post("/articles", param, token);

    ResponseEntity<String> response = delete("/articles/article-to-delete", token);

    assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatusCodeValue());

    // Verify the article no longer exists
    HttpClientErrorException ex =
        expectClientError(HttpMethod.GET, "/articles/article-to-delete", null, null);
    assertEquals(HttpStatus.NOT_FOUND.value(), ex.getRawStatusCode());
  }

  @Test
  void should_list_articles() {
    String token = registerAndGetToken("listart@test.com", "listartuser", "password123");

    Map<String, Object> param1 =
        articleParam("List Article One", "Desc 1", "Body 1", new String[] {"list"});
    post("/articles", param1, token);

    Map<String, Object> param2 =
        articleParam("List Article Two", "Desc 2", "Body 2", new String[] {"list"});
    post("/articles", param2, token);

    ResponseEntity<String> response = get("/articles");

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    assertTrue(body.has("articles"));
    assertTrue(body.has("articlesCount"));
    assertTrue(body.get("articlesCount").asInt() >= 2);
  }

  @Test
  void should_list_articles_filtered_by_tag() {
    String token = registerAndGetToken("tagart@test.com", "tagartuser", "password123");

    Map<String, Object> param =
        articleParam("Tagged Article", "Desc", "Body", new String[] {"uniquetag123"});
    post("/articles", param, token);

    ResponseEntity<String> response = get("/articles?tag=uniquetag123");

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    assertTrue(body.get("articlesCount").asInt() >= 1);
  }

  @Test
  void should_list_articles_filtered_by_author() {
    String token = registerAndGetToken("authorfilter@test.com", "authorfilteruser", "password123");

    Map<String, Object> param = articleParam("Author Filtered Article", "Desc", "Body", null);
    post("/articles", param, token);

    ResponseEntity<String> response = get("/articles?author=authorfilteruser");

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    assertTrue(body.get("articlesCount").asInt() >= 1);
  }

  @Test
  void should_get_feed_for_authenticated_user() {
    String token1 = registerAndGetToken("feeduser1@test.com", "feeduser1", "password123");
    String token2 = registerAndGetToken("feeduser2@test.com", "feeduser2", "password123");

    // feeduser1 follows feeduser2
    post("/profiles/feeduser2/follow", null, token1);

    // feeduser2 creates an article
    Map<String, Object> param = articleParam("Feed Article", "Desc", "Body", null);
    post("/articles", param, token2);

    // feeduser1 should see the article in their feed
    ResponseEntity<String> response = get("/articles/feed", token1);

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    assertTrue(body.has("articles"));
    assertTrue(body.has("articlesCount"));
  }

  @Test
  void should_fail_create_article_without_auth() {
    Map<String, Object> param = articleParam("Unauth Article", "Desc", "Body", null);
    HttpClientErrorException ex = expectClientError(HttpMethod.POST, "/articles", param, null);

    assertEquals(HttpStatus.UNAUTHORIZED.value(), ex.getRawStatusCode());
  }

  @Test
  void should_fail_delete_article_by_non_author() {
    String token1 = registerAndGetToken("artowner@test.com", "artowneruser", "password123");
    String token2 = registerAndGetToken("artother@test.com", "artotheruser", "password123");

    Map<String, Object> param = articleParam("Owner Only Article", "Desc", "Body", null);
    post("/articles", param, token1);

    HttpClientErrorException ex =
        expectClientError(HttpMethod.DELETE, "/articles/owner-only-article", null, token2);

    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getRawStatusCode());
  }

  @Test
  void should_access_articles_list_via_selenium_driver() {
    String pageSource = navigateAndGetSource("/articles");
    assertNotNull(pageSource);
    assertTrue(pageSource.contains("articles"));
  }
}
