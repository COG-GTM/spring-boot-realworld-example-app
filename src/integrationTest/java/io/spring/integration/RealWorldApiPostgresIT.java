package io.spring.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.LocalServerPort;

/** Exercises the REST API over real HTTP with PostgreSQL as the backing store. */
class RealWorldApiPostgresIT extends PostgresIntegrationTest {

  @LocalServerPort private int port;

  @BeforeEach
  void setUpRestAssured() {
    RestAssured.port = port;
  }

  @Test
  void should_register_login_and_update_current_user() {
    String token = register("jake", "jake@jake.jake");

    given()
        .contentType(ContentType.JSON)
        .body(wrap("user", "email", "jake@jake.jake", "password", "password"))
        .when()
        .post("/users/login")
        .then()
        .statusCode(200)
        .body("user.username", equalTo("jake"))
        .body("user.token", notNullValue());

    auth(token)
        .body(wrap("user", "bio", "I work at statefarm"))
        .when()
        .put("/user")
        .then()
        .statusCode(200)
        .body("user.bio", equalTo("I work at statefarm"));

    auth(token)
        .when()
        .get("/user")
        .then()
        .statusCode(200)
        .body("user.email", equalTo("jake@jake.jake"));
  }

  @Test
  void should_reject_duplicated_username() {
    register("jake", "jake@jake.jake");

    given()
        .contentType(ContentType.JSON)
        .body(wrap("user", "email", "other@jake.jake", "username", "jake", "password", "password"))
        .when()
        .post("/users")
        .then()
        .statusCode(422);
  }

  @Test
  void should_support_article_lifecycle_with_comments_favorites_and_feed() {
    String authorToken = register("author", "author@example.com");
    String readerToken = register("reader", "reader@example.com");

    Map<String, Object> article = new HashMap<>();
    article.put("title", "How to train your dragon");
    article.put("description", "Ever wonder how?");
    article.put("body", "You have to believe");
    article.put("tagList", new String[] {"dragons", "training"});
    Map<String, Object> newArticle = new HashMap<>();
    newArticle.put("article", article);

    String slug =
        auth(authorToken)
            .body(newArticle)
            .when()
            .post("/articles")
            .then()
            .statusCode(200)
            .body("article.author.username", equalTo("author"))
            .body("article.tagList", containsInAnyOrder("dragons", "training"))
            .extract()
            .path("article.slug");

    given()
        .when()
        .get("/articles?tag=dragons&limit=1&offset=0")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(1))
        .body("articles.slug", contains(slug));

    given().when().get("/tags").then().statusCode(200).body("tags", hasSize(2));

    auth(readerToken)
        .when()
        .post("/articles/{slug}/favorite", slug)
        .then()
        .statusCode(200)
        .body("article.favorited", equalTo(true))
        .body("article.favoritesCount", equalTo(1));

    String commentId =
        auth(readerToken)
            .body(wrap("comment", "body", "Great article"))
            .when()
            .post("/articles/{slug}/comments", slug)
            .then()
            .statusCode(201)
            .body("comment.author.username", equalTo("reader"))
            .extract()
            .path("comment.id");

    given()
        .when()
        .get("/articles/{slug}/comments", slug)
        .then()
        .statusCode(200)
        .body("comments.id", contains(commentId));

    auth(readerToken)
        .when()
        .post("/profiles/author/follow")
        .then()
        .statusCode(200)
        .body("profile.following", equalTo(true));

    auth(readerToken)
        .when()
        .get("/articles/feed")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(1))
        .body("articles[0].slug", equalTo(slug))
        .body("articles[0].author.following", equalTo(true));

    auth(authorToken)
        .body(wrap("article", "title", "Dragons revisited"))
        .when()
        .put("/articles/{slug}", slug)
        .then()
        .statusCode(200)
        .body("article.slug", equalTo("dragons-revisited"));

    auth(readerToken).when().delete("/articles/dragons-revisited").then().statusCode(403);
    auth(authorToken).when().delete("/articles/dragons-revisited").then().statusCode(204);
    given().when().get("/articles/dragons-revisited").then().statusCode(404);
  }

  private String register(String username, String email) {
    return given()
        .contentType(ContentType.JSON)
        .body(wrap("user", "email", email, "username", username, "password", "password"))
        .when()
        .post("/users")
        .then()
        .statusCode(201)
        .extract()
        .path("user.token");
  }

  private RequestSpecification auth(String token) {
    return given().contentType(ContentType.JSON).header("Authorization", "Token " + token);
  }

  private static Map<String, Object> wrap(String root, String... keyValues) {
    Map<String, Object> inner = new HashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      inner.put(keyValues[i], keyValues[i + 1]);
    }
    Map<String, Object> outer = new HashMap<>();
    outer.put(root, inner);
    return outer;
  }
}
