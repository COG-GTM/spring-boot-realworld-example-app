package io.spring.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

/**
 * End-to-end test driving the running application over real HTTP: the whole stack (security, REST
 * layer, services, MyBatis and the database) is exercised without any mocking.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.datasource.url=jdbc:sqlite:build/e2e-test.db"})
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class RealWorldApiE2ETest {

  private static final String SUFFIX = UUID.randomUUID().toString().substring(0, 8);
  private static final String AUTHOR_USERNAME = "author-" + SUFFIX;
  private static final String READER_USERNAME = "reader-" + SUFFIX;
  private static final String TITLE = "How to train your dragon " + SUFFIX;

  private String authorToken;
  private String readerToken;
  private String slug;
  private String commentId;

  @LocalServerPort private int port;

  @BeforeAll
  public void setUpRestAssured() {
    RestAssured.port = port;
  }

  @Test
  @Order(1)
  public void should_register_and_login_users() {
    authorToken = register(AUTHOR_USERNAME);
    readerToken = register(READER_USERNAME);

    given()
        .contentType(ContentType.JSON)
        .body(wrap("user", Map.of("email", email(AUTHOR_USERNAME), "password", "password")))
        .when()
        .post("/users/login")
        .then()
        .statusCode(200)
        .body("user.username", equalTo(AUTHOR_USERNAME))
        .body("user.token", notNullValue());

    given()
        .header("Authorization", "Token " + authorToken)
        .when()
        .get("/user")
        .then()
        .statusCode(200)
        .body("user.email", equalTo(email(AUTHOR_USERNAME)));
  }

  @Test
  @Order(2)
  public void should_reject_anonymous_access_to_current_user() {
    given().when().get("/user").then().statusCode(401);
  }

  @Test
  @Order(3)
  public void should_update_current_user_bio() {
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Token " + authorToken)
        .body(wrap("user", Map.of("bio", "writes about dragons")))
        .when()
        .put("/user")
        .then()
        .statusCode(200)
        .body("user.bio", equalTo("writes about dragons"));
  }

  @Test
  @Order(4)
  public void should_create_and_read_article() {
    slug =
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Token " + authorToken)
            .body(
                wrap(
                    "article",
                    Map.of(
                        "title",
                        TITLE,
                        "description",
                        "Ever wonder how?",
                        "body",
                        "You have to believe",
                        "tagList",
                        List.of("dragons", "training"))))
            .when()
            .post("/articles")
            .then()
            .statusCode(200)
            .body("article.title", equalTo(TITLE))
            .body("article.author.username", equalTo(AUTHOR_USERNAME))
            .body("article.favoritesCount", equalTo(0))
            .extract()
            .path("article.slug");

    given()
        .when()
        .get("/articles/{slug}", slug)
        .then()
        .statusCode(200)
        .body("article.body", equalTo("You have to believe"))
        .body("article.tagList", hasSize(2));

    given()
        .when()
        .get("/articles?tag=dragons")
        .then()
        .statusCode(200)
        .body("articles.slug", hasItem(slug));

    given().when().get("/tags").then().statusCode(200).body("tags", hasItem("dragons"));
  }

  @Test
  @Order(5)
  public void should_reject_article_update_from_another_user() {
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Token " + readerToken)
        .body(wrap("article", Map.of("body", "hijacked")))
        .when()
        .put("/articles/{slug}", slug)
        .then()
        .statusCode(403);
  }

  @Test
  @Order(6)
  public void should_update_article_as_author() {
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Token " + authorToken)
        .body(wrap("article", Map.of("body", "You really have to believe")))
        .when()
        .put("/articles/{slug}", slug)
        .then()
        .statusCode(200)
        .body("article.body", equalTo("You really have to believe"));
  }

  @Test
  @Order(7)
  public void should_comment_on_article() {
    commentId =
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Token " + readerToken)
            .body(wrap("comment", Map.of("body", "Nice article!")))
            .when()
            .post("/articles/{slug}/comments", slug)
            .then()
            .statusCode(201)
            .body("comment.body", equalTo("Nice article!"))
            .body("comment.author.username", equalTo(READER_USERNAME))
            .extract()
            .path("comment.id");

    given()
        .when()
        .get("/articles/{slug}/comments", slug)
        .then()
        .statusCode(200)
        .body("comments", hasSize(1))
        .body("comments[0].body", equalTo("Nice article!"));
  }

  @Test
  @Order(8)
  public void should_favorite_and_unfavorite_article() {
    given()
        .header("Authorization", "Token " + readerToken)
        .when()
        .post("/articles/{slug}/favorite", slug)
        .then()
        .statusCode(200)
        .body("article.favorited", equalTo(true))
        .body("article.favoritesCount", equalTo(1));

    given()
        .header("Authorization", "Token " + readerToken)
        .when()
        .delete("/articles/{slug}/favorite", slug)
        .then()
        .statusCode(200)
        .body("article.favorited", equalTo(false))
        .body("article.favoritesCount", equalTo(0));
  }

  @Test
  @Order(9)
  public void should_follow_author_and_see_article_in_feed() {
    given()
        .header("Authorization", "Token " + readerToken)
        .when()
        .post("/profiles/{username}/follow", AUTHOR_USERNAME)
        .then()
        .statusCode(200)
        .body("profile.following", equalTo(true));

    given()
        .header("Authorization", "Token " + readerToken)
        .when()
        .get("/articles/feed")
        .then()
        .statusCode(200)
        .body("articles.slug", hasItem(slug));

    given()
        .header("Authorization", "Token " + readerToken)
        .when()
        .delete("/profiles/{username}/follow", AUTHOR_USERNAME)
        .then()
        .statusCode(200)
        .body("profile.following", equalTo(false));
  }

  @Test
  @Order(10)
  public void should_delete_comment_and_article() {
    given()
        .header("Authorization", "Token " + readerToken)
        .when()
        .delete("/articles/{slug}/comments/{id}", slug, commentId)
        .then()
        .statusCode(204);

    given()
        .header("Authorization", "Token " + authorToken)
        .when()
        .delete("/articles/{slug}", slug)
        .then()
        .statusCode(204);

    given().when().get("/articles/{slug}", slug).then().statusCode(404);
  }

  private String register(String username) {
    return given()
        .contentType(ContentType.JSON)
        .body(
            wrap(
                "user",
                Map.of("email", email(username), "username", username, "password", "password")))
        .when()
        .post("/users")
        .then()
        .statusCode(201)
        .body("user.username", equalTo(username))
        .extract()
        .path("user.token");
  }

  private String email(String username) {
    return username + "@example.com";
  }

  private Map<String, Object> wrap(String root, Map<String, Object> payload) {
    Map<String, Object> body = new HashMap<>();
    body.put(root, payload);
    return body;
  }
}
