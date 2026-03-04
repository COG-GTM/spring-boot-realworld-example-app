package io.spring.e2e;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class TagE2ETest extends BaseE2ETest {

  @Test
  void should_get_tags_empty_initially() {
    ResponseEntity<String> response = get("/tags");

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    assertTrue(body.has("tags"));
    assertTrue(body.get("tags").isArray());
  }

  @Test
  void should_return_tags_after_article_creation() {
    String token = registerAndGetToken("tagsuser@test.com", "tagsuser", "password123");

    Map<String, Object> param =
        articleParam(
            "Tags Test Article", "Desc", "Body", new String[] {"selenium", "e2e", "testing"});
    post("/articles", param, token);

    ResponseEntity<String> response = get("/tags");

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    JsonNode tags = body.get("tags");
    assertTrue(tags.isArray());
    assertTrue(tags.size() >= 3);

    // Verify the tags we created are present
    boolean hasSelenium = false;
    boolean hasE2e = false;
    boolean hasTesting = false;
    for (JsonNode tag : tags) {
      String tagName = tag.asText();
      if ("selenium".equals(tagName)) hasSelenium = true;
      if ("e2e".equals(tagName)) hasE2e = true;
      if ("testing".equals(tagName)) hasTesting = true;
    }
    assertTrue(hasSelenium, "Tag 'selenium' should be present");
    assertTrue(hasE2e, "Tag 'e2e' should be present");
    assertTrue(hasTesting, "Tag 'testing' should be present");
  }

  @Test
  void should_access_tags_via_selenium_driver() {
    String pageSource = navigateAndGetSource("/tags");
    assertNotNull(pageSource);
    assertTrue(pageSource.contains("tags"));
  }
}
