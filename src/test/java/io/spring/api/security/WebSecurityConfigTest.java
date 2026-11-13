package io.spring.api.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSecurityConfigTest {

  @LocalServerPort private int port;

  @BeforeEach
  public void setUp() {
    RestAssured.port = port;
  }

  @Test
  public void should_allow_cors_preflight_without_token() {
    given()
        .header("Origin", "http://localhost:4200")
        .header("Access-Control-Request-Method", "POST")
        .header("Access-Control-Request-Headers", "Authorization,Content-Type")
        .when()
        .options("/articles")
        .then()
        .statusCode(200)
        .header("Access-Control-Allow-Origin", equalTo("*"));
  }

  @Test
  public void should_reject_cors_preflight_with_unlisted_header() {
    given()
        .header("Origin", "http://localhost:4200")
        .header("Access-Control-Request-Method", "POST")
        .header("Access-Control-Request-Headers", "X-Not-Allowed")
        .when()
        .options("/articles")
        .then()
        .statusCode(403);
  }

  @Test
  public void should_permit_anonymous_public_reads() {
    given().when().get("/tags").then().statusCode(200);
    given().when().get("/articles").then().statusCode(200);
    given().when().get("/profiles/nobody-" + UUID.randomUUID()).then().statusCode(404);
  }

  @Test
  public void should_require_authentication_for_feed_and_writes() {
    given().when().get("/articles/feed").then().statusCode(401);
    given().when().get("/user").then().statusCode(401);
    given()
        .contentType(ContentType.JSON)
        .body("{}")
        .when()
        .post("/articles")
        .then()
        .statusCode(401);
    given().when().delete("/articles/some-slug").then().statusCode(401);
    given().when().post("/profiles/someone/follow").then().statusCode(401);
  }

  @Test
  public void should_reject_invalid_token() {
    given()
        .header("Authorization", "Token not-a-jwt")
        .when()
        .get("/user")
        .then()
        .statusCode(401);
  }

  @Test
  public void should_permit_anonymous_login_and_graphql() {
    Map<String, Object> login = new HashMap<>();
    login.put("email", "nobody-" + UUID.randomUUID() + "@example.com");
    login.put("password", "wrong");
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("user", login))
        .when()
        .post("/users/login")
        .then()
        .statusCode(422);

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("query", "{ tags }"))
        .when()
        .post("/graphql")
        .then()
        .statusCode(200);
  }

  @Test
  public void should_authenticate_rest_request_with_issued_jwt() {
    String username = "sec" + UUID.randomUUID().toString().substring(0, 8);
    Map<String, Object> user = new HashMap<>();
    user.put("email", username + "@example.com");
    user.put("username", username);
    user.put("password", "password");

    String token =
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("user", user))
            .when()
            .post("/users")
            .then()
            .statusCode(201)
            .body("user.token", notNullValue())
            .extract()
            .path("user.token");

    given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/user")
        .then()
        .statusCode(200)
        .body("user.username", equalTo(username));
  }
}
