package io.spring.graphql;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GraphQLHttpTransportTest {

  @LocalServerPort private int port;

  @Test
  public void should_accept_unwrapped_graphql_request_body() {
    given()
        .port(port)
        .contentType(ContentType.JSON)
        .body("{\"query\": \"{ tags }\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.tags", notNullValue())
        .body("errors", nullValue());
  }

  @Test
  public void should_still_unwrap_root_value_for_rest_request_body() {
    given()
        .port(port)
        .contentType(ContentType.JSON)
        .body("{\"user\": {\"email\": \"missing@example.com\", \"password\": \"secret\"}}")
        .when()
        .post("/users/login")
        .then()
        .statusCode(422)
        .body("message", notNullValue())
        .body("errors", nullValue());
  }
}
