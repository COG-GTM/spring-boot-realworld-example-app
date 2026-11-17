package io.spring.api.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.RestAssured;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class Boot3SecurityAndGraphQLIntegrationTest {

  @LocalServerPort private int port;

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) throws IOException {
    Path db = Files.createTempFile("realworld-it", ".db");
    db.toFile().deleteOnExit();
    registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + db.toAbsolutePath());
  }

  @BeforeEach
  public void setUp() {
    RestAssured.port = port;
  }

  @Test
  public void graphql_endpoint_should_parse_unwrapped_request_body() {
    given()
        .contentType("application/json")
        .body(graphql("{ tags }"))
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("errors", nullValue())
        .body("data.tags", notNullValue());
  }

  @Test
  public void graphql_create_user_should_return_token_usable_on_rest_api() {
    String token =
        given()
            .contentType("application/json")
            .body(
                graphql(
                    "mutation { createUser(input: {email: \"gql@example.com\", username:"
                        + " \"gqluser\", password: \"password\"}) { ... on UserPayload { user {"
                        + " email token } } } }"))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("errors", nullValue())
            .body("data.createUser.user.email", equalTo("gql@example.com"))
            .extract()
            .path("data.createUser.user.token");

    given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/user")
        .then()
        .statusCode(200)
        .body("user.username", equalTo("gqluser"));
  }

  @Test
  public void graphql_failed_login_should_return_unauthenticated_error() {
    given()
        .contentType("application/json")
        .body(
            graphql(
                "mutation { login(email: \"nobody@example.com\", password: \"wrong\") { user {"
                    + " email } } }"))
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("errors[0].message", equalTo("invalid email or password"))
        .body("errors[0].extensions.errorType", equalTo("UNAUTHENTICATED"));
  }

  @Test
  public void rest_api_should_still_unwrap_root_value() {
    Map<String, Object> user = new HashMap<>();
    user.put("email", "rest@example.com");
    user.put("username", "restuser");
    user.put("password", "password");
    Map<String, Object> body = new HashMap<>();
    body.put("user", user);

    given()
        .contentType("application/json")
        .body(body)
        .when()
        .post("/users")
        .then()
        .statusCode(201)
        .body("user.username", equalTo("restuser"))
        .body("user.token", notNullValue());
  }

  @Test
  public void anonymous_request_to_missing_public_resource_should_return_404_not_401() {
    given().when().get("/articles/does-not-exist").then().statusCode(404);
  }

  @Test
  public void anonymous_request_to_feed_should_return_401() {
    given().when().get("/articles/feed").then().statusCode(401);
  }

  @Test
  public void anonymous_write_request_should_return_401() {
    given()
        .contentType("application/json")
        .body("{}")
        .when()
        .post("/articles")
        .then()
        .statusCode(401);
  }

  @Test
  public void invalid_token_should_return_401_on_protected_endpoint() {
    given()
        .header("Authorization", "Token not-a-jwt")
        .when()
        .get("/user")
        .then()
        .statusCode(401);
  }

  @Test
  public void public_get_endpoints_should_be_accessible_anonymously() {
    given().when().get("/tags").then().statusCode(200);
    given().when().get("/articles").then().statusCode(200);
  }

  @Test
  public void cors_preflight_should_be_permitted_without_authentication() {
    given()
        .header("Origin", "http://example.com")
        .header("Access-Control-Request-Method", "POST")
        .header("Access-Control-Request-Headers", "Authorization,Content-Type")
        .when()
        .options("/articles")
        .then()
        .statusCode(200)
        .header("Access-Control-Allow-Origin", "*");
  }

  private static Map<String, Object> graphql(String query) {
    Map<String, Object> body = new HashMap<>();
    body.put("query", query);
    return body;
  }
}
