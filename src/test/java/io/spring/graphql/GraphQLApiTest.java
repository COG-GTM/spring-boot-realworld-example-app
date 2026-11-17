package io.spring.graphql;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class GraphQLApiTest {

  @LocalServerPort private int port;

  @Test
  public void should_execute_raw_application_graphql_body() {
    given()
        .port(port)
        .contentType("application/graphql")
        .body(utf8("query { tags }"))
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.tags", notNullValue())
        .body("errors", nullValue());
  }

  @Test
  public void should_execute_raw_application_graphql_body_with_charset() {
    given()
        .port(port)
        .contentType("application/graphql; charset=utf-8")
        .body(utf8("{ article(slug: \"no-such-article\") { slug } }"))
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("errors[0].path", hasItem("article"));
  }

  @Test
  public void should_reject_empty_application_graphql_body() {
    given()
        .port(port)
        .contentType("application/graphql")
        .when()
        .post("/graphql")
        .then()
        .statusCode(400);
  }

  @Test
  public void should_execute_json_body() {
    given()
        .port(port)
        .contentType("application/json")
        .body(Map.of("query", "query { tags }"))
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.tags", notNullValue());
  }

  @Test
  public void should_return_empty_locations_for_default_handled_errors() {
    given()
        .port(port)
        .contentType("application/json")
        .body(Map.of("query", "query { me { username } }"))
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.me", nullValue())
        .body("errors[0].extensions.errorType", equalTo("INTERNAL"))
        .body("errors[0].path", equalTo(java.util.List.of("me")))
        .body("errors[0].locations", hasSize(0));
  }

  @Test
  public void should_return_empty_locations_for_failed_login() {
    given()
        .port(port)
        .contentType("application/json")
        .body(
            Map.of(
                "query",
                "mutation { login(email: \"nobody@example.com\", password: \"wrong\") { user { token } } }"))
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.login", nullValue())
        .body("errors[0].extensions.errorType", equalTo("UNAUTHENTICATED"))
        .body("errors[0].locations", hasSize(0));
  }

  private static byte[] utf8(String body) {
    return body.getBytes(StandardCharsets.UTF_8);
  }
}
