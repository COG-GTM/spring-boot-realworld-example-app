package io.spring.api.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.spring.application.ArticleQueryService;
import io.spring.application.TagsQueryService;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Runs on a real servlet container so ERROR dispatches go through the security filter chain. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class WebSecurityConfigTest {
  @LocalServerPort private int port;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private TagsQueryService tagsQueryService;

  @Test
  public void should_keep_404_status_for_anonymous_request_to_missing_article() {
    when(articleQueryService.findBySlug(any(), any())).thenReturn(Optional.empty());

    given().port(port).when().get("/articles/not-exists").then().statusCode(404);
  }

  @Test
  public void should_permit_anonymous_get_on_public_endpoints() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java"));

    given().port(port).when().get("/tags").then().statusCode(200).body("tags[0]", equalTo("java"));
  }

  @Test
  public void should_return_401_for_anonymous_feed_request() {
    given().port(port).when().get("/articles/feed").then().statusCode(401);
  }

  @Test
  public void should_return_401_for_anonymous_write_request() {
    given()
        .port(port)
        .contentType("application/json")
        .body("{\"article\": {\"title\": \"t\", \"description\": \"d\", \"body\": \"b\"}}")
        .when()
        .post("/articles")
        .then()
        .statusCode(401);
  }

  @Test
  public void should_return_401_for_invalid_token() {
    given()
        .port(port)
        .header("Authorization", "Token not-a-jwt")
        .when()
        .get("/user")
        .then()
        .statusCode(401);
  }

  @Test
  public void should_permit_anonymous_cors_preflight() {
    given()
        .port(port)
        .header("Origin", "http://example.com")
        .header("Access-Control-Request-Method", "POST")
        .header("Access-Control-Request-Headers", "Authorization,Content-Type")
        .when()
        .options("/articles")
        .then()
        .statusCode(200)
        .header("Access-Control-Allow-Origin", equalTo("*"));
  }
}
