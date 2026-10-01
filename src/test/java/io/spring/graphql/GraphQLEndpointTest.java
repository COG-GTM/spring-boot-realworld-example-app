package io.spring.graphql;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GraphQLEndpointTest {

  @LocalServerPort private int port;

  @Test
  public void should_create_user_and_query_me_with_token() {
    String username = "gql" + UUID.randomUUID().toString().substring(0, 8);
    String createUser =
        "mutation($input: CreateUserInput) { createUser(input: $input) {"
            + " ... on UserPayload { user { username token } } } }";

    String token =
        given()
            .port(port)
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "query",
                    createUser,
                    "variables",
                    Map.of(
                        "input",
                        Map.of(
                            "username",
                            username,
                            "email",
                            username + "@example.com",
                            "password",
                            "password"))))
            .when()
            .post("/graphql")
            .then()
            .statusCode(200)
            .body("errors", nullValue())
            .body("data.createUser.user.username", equalTo(username))
            .body("data.createUser.user.token", notNullValue())
            .extract()
            .path("data.createUser.user.token");

    given()
        .port(port)
        .contentType(ContentType.JSON)
        .header("Authorization", "Token " + token)
        .body(Map.of("query", "{ me { username } }"))
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("errors", nullValue())
        .body("data.me.username", equalTo(username));
  }
}
