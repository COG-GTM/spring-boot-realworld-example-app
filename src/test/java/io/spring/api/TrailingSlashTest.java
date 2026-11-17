package io.spring.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class TrailingSlashTest {

  @LocalServerPort private int port;

  @Test
  public void should_map_trailing_slash_to_same_endpoint() {
    given()
        .port(port)
        .when()
        .get("/articles/")
        .then()
        .statusCode(200)
        .body("articlesCount", notNullValue());
  }

  @Test
  public void should_authorize_the_path_as_sent() {
    given().port(port).when().get("/tags").then().statusCode(200);
    given().port(port).when().get("/tags/").then().statusCode(401);
  }
}
