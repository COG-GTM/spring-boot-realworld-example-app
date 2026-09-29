package io.spring.integration;

import static io.restassured.RestAssured.given;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Boots the full application on a random port against a MySQL instance started by Testcontainers.
 * The container is a JVM-wide singleton so every subclass shares one container and one Spring
 * context; tables are truncated before each test.
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class MySqlIntegrationTestBase {
  private static final List<String> TABLES =
      Arrays.asList(
          "article_favorites", "article_tags", "tags", "comments", "follows", "articles", "users");

  static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>(DockerImageName.parse("mysql:8.0.36"))
          .withDatabaseName("realworld")
          .withUsername("realworld")
          .withPassword("realworld");

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    MYSQL.start();
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
  }

  @LocalServerPort private int port;

  @Autowired protected JdbcTemplate jdbcTemplate;

  @BeforeEach
  void resetDatabaseAndClient() {
    RestAssured.port = port;
    TABLES.forEach(table -> jdbcTemplate.execute("DELETE FROM " + table));
  }

  protected String register(String username) {
    Map<String, Object> user = new HashMap<>();
    user.put("username", username);
    user.put("email", username + "@example.com");
    user.put("password", "password");
    return given()
        .contentType(ContentType.JSON)
        .body(wrap("user", user))
        .when()
        .post("/users")
        .then()
        .statusCode(201)
        .extract()
        .path("user.token");
  }

  protected String createArticle(String token, String title, String... tags) {
    Map<String, Object> article = new HashMap<>();
    article.put("title", title);
    article.put("description", "description of " + title);
    article.put("body", "body of " + title);
    article.put("tagList", Arrays.asList(tags));
    return given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Token " + token)
        .body(wrap("article", article))
        .when()
        .post("/articles")
        .then()
        .statusCode(200)
        .extract()
        .path("article.slug");
  }

  protected void follow(String token, String username) {
    given()
        .header("Authorization", "Token " + token)
        .when()
        .post("/profiles/{username}/follow", username)
        .then()
        .statusCode(200);
  }

  protected void unfollow(String token, String username) {
    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/profiles/{username}/follow", username)
        .then()
        .statusCode(200);
  }

  protected ValidatableResponse favorite(String token, String slug) {
    return given()
        .header("Authorization", "Token " + token)
        .when()
        .post("/articles/{slug}/favorite", slug)
        .then();
  }

  protected ValidatableResponse unfavorite(String token, String slug) {
    return given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/articles/{slug}/favorite", slug)
        .then();
  }

  protected ValidatableResponse feed(String token, Map<String, ?> queryParams) {
    return given()
        .header("Authorization", "Token " + token)
        .queryParams(queryParams)
        .when()
        .get("/articles/feed")
        .then();
  }

  /** Backdates an article so ordering assertions don't depend on second-level timestamp ties. */
  protected void setCreatedAt(String slug, String timestamp) {
    jdbcTemplate.update(
        "UPDATE articles SET created_at = ?, updated_at = ? WHERE slug = ?",
        timestamp,
        timestamp,
        slug);
  }

  private static Map<String, Object> wrap(String root, Map<String, Object> body) {
    Map<String, Object> wrapped = new HashMap<>();
    wrapped.put(root, body);
    return wrapped;
  }
}
