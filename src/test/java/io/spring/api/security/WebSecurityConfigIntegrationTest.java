package io.spring.api.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.restassured.RestAssured;
import io.spring.application.ArticleQueryService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class WebSecurityConfigIntegrationTest {
  @LocalServerPort private int port;

  @MockBean private ArticleQueryService articleQueryService;

  @BeforeEach
  public void setUp() {
    RestAssured.port = port;
  }

  @Test
  public void should_keep_404_status_for_anonymous_request_to_missing_article() {
    when(articleQueryService.findBySlug(eq("missing"), any())).thenReturn(Optional.empty());

    given().when().get("/articles/missing").then().statusCode(404);
  }

  @Test
  public void should_return_401_for_anonymous_feed_request() {
    given().when().get("/articles/feed").then().statusCode(401);
  }

  @Test
  public void should_return_401_for_anonymous_request_to_protected_endpoint() {
    given().when().get("/user").then().statusCode(401);
  }

  @Test
  public void should_return_401_for_invalid_token() {
    given().header("Authorization", "Token invalid").when().get("/user").then().statusCode(401);
  }

  @Test
  public void should_allow_anonymous_cors_preflight() {
    given()
        .header("Origin", "http://example.com")
        .header("Access-Control-Request-Method", "POST")
        .header("Access-Control-Request-Headers", "Authorization,Content-Type")
        .when()
        .options("/articles")
        .then()
        .statusCode(200)
        .header("Access-Control-Allow-Origin", equalTo("*"));
  }

  @Test
  public void should_allow_anonymous_graphql_requests() {
    given()
        .contentType("application/json")
        .body("{\"query\":\"{ tags }\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200);
  }
}
