package io.spring.api.exception;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "server.error.path=/problem")
public class CustomErrorPathIntegrationTest {

  @LocalServerPort private int port;

  @Test
  public void should_render_404_problem_for_anonymous_request_with_custom_error_path() {
    given()
        .port(port)
        .when()
        .get("/articles/not/a/route")
        .then()
        .statusCode(404)
        .contentType("application/problem+json")
        .body("type", equalTo("about:blank"))
        .body("title", equalTo("Not Found"))
        .body("status", equalTo(404))
        .body("instance", equalTo("/articles/not/a/route"));
  }
}
