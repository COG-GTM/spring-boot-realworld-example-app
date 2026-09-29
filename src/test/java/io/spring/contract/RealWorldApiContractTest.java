package io.spring.contract;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Contract tests for the RealWorld REST API.
 *
 * <p>Each test drives the full application over HTTP with spec-shaped request bodies and checks the
 * status code plus the response body against a JSON schema in {@code src/test/resources/contract}.
 * Schemas forbid unknown properties, so adding, removing, renaming or retyping a field in any
 * response is a contract change and must be made in the schema as well.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class RealWorldApiContractTest {

  @LocalServerPort private int port;

  private RequestSpecification spec;

  @DynamicPropertySource
  static void isolatedDatabase(DynamicPropertyRegistry registry) throws IOException {
    File db = Files.createTempFile("realworld-contract", ".db").toFile();
    db.deleteOnExit();
    registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + db.getAbsolutePath());
  }

  @BeforeEach
  public void setUp() {
    spec = new RequestSpecBuilder().setPort(port).setContentType(ContentType.JSON).build();
  }

  // ---------------------------------------------------------------- users & authentication

  @Test
  public void register_accepts_user_envelope_and_returns_user() {
    String username = uniqueName("reg");
    String email = username + "@example.com";

    request()
        .body(wrap("user", map("username", username, "email", email, "password", "secret")))
        .post("/users")
        .then()
        .statusCode(201)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/user.json"))
        .body("user.username", equalTo(username))
        .body("user.email", equalTo(email))
        .body("user.token", not(emptyString()));
  }

  @Test
  public void register_with_invalid_fields_returns_errors_keyed_by_field() {
    request()
        .body(wrap("user", map("username", "", "email", "not-an-email", "password", "")))
        .post("/users")
        .then()
        .statusCode(422)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/errors.json"))
        .body("errors", hasKey("username"))
        .body("errors", hasKey("email"))
        .body("errors", hasKey("password"));
  }

  @Test
  public void login_accepts_user_envelope_and_returns_user() {
    Account account = register();

    request()
        .body(wrap("user", map("email", account.email, "password", account.password)))
        .post("/users/login")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/user.json"))
        .body("user.username", equalTo(account.username))
        .body("user.email", equalTo(account.email));
  }

  @Test
  public void login_with_wrong_password_returns_422_message() {
    Account account = register();

    request()
        .body(wrap("user", map("email", account.email, "password", "wrong")))
        .post("/users/login")
        .then()
        .statusCode(422)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/login-error.json"));
  }

  @Test
  public void get_current_user_echoes_presented_token() {
    Account account = register();

    request(account)
        .get("/user")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/user.json"))
        .body("user.username", equalTo(account.username))
        .body("user.token", equalTo(account.token));
  }

  @Test
  public void get_current_user_without_token_returns_401() {
    request().get("/user").then().statusCode(401);
  }

  @Test
  public void update_current_user_accepts_partial_user_envelope() {
    Account account = register();
    String image = "https://example.com/avatar.png";

    request(account)
        .body(wrap("user", map("bio", "I like to skateboard", "image", image)))
        .put("/user")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/user.json"))
        .body("user.username", equalTo(account.username))
        .body("user.email", equalTo(account.email))
        .body("user.bio", equalTo("I like to skateboard"))
        .body("user.image", equalTo(image));
  }

  // ---------------------------------------------------------------- profiles

  @Test
  public void get_profile_anonymously_returns_profile() {
    Account celeb = register();

    request()
        .get("/profiles/{username}", celeb.username)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/profile.json"))
        .body("profile.username", equalTo(celeb.username))
        .body("profile.following", equalTo(false));
  }

  @Test
  public void follow_and_unfollow_return_profile() {
    Account celeb = register();
    Account fan = register();

    request(fan)
        .post("/profiles/{username}/follow", celeb.username)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/profile.json"))
        .body("profile.following", equalTo(true));

    request(fan)
        .delete("/profiles/{username}/follow", celeb.username)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/profile.json"))
        .body("profile.following", equalTo(false));
  }

  @Test
  public void get_unknown_profile_returns_404() {
    request().get("/profiles/{username}", uniqueName("ghost")).then().statusCode(404);
  }

  // ---------------------------------------------------------------- articles

  @Test
  public void create_article_accepts_article_envelope_and_returns_article() {
    Account author = register();
    String title = "How to train your dragon " + uniqueName("t");

    request(author)
        .body(
            wrap(
                "article",
                map(
                    "title",
                    title,
                    "description",
                    "Ever wonder how?",
                    "body",
                    "You have to believe",
                    "tagList",
                    Arrays.asList("dragons", "training"))))
        .post("/articles")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/article.json"))
        .body("article.title", equalTo(title))
        .body("article.description", equalTo("Ever wonder how?"))
        .body("article.body", equalTo("You have to believe"))
        .body("article.tagList", containsInAnyOrder("dragons", "training"))
        .body("article.favorited", equalTo(false))
        .body("article.favoritesCount", equalTo(0))
        .body("article.author.username", equalTo(author.username))
        .body("article.author.following", equalTo(false));
  }

  @Test
  public void create_article_with_blank_fields_returns_errors_keyed_by_field() {
    Account author = register();

    request(author)
        .body(wrap("article", map("title", "", "description", "", "body", "")))
        .post("/articles")
        .then()
        .statusCode(422)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/errors.json"))
        .body("errors", hasKey("title"))
        .body("errors", hasKey("description"))
        .body("errors", hasKey("body"));
  }

  @Test
  public void create_article_without_token_returns_401() {
    request()
        .body(wrap("article", map("title", "t", "description", "d", "body", "b")))
        .post("/articles")
        .then()
        .statusCode(401);
  }

  @Test
  public void get_article_anonymously_returns_article() {
    Account author = register();
    String slug = createArticle(author, Collections.singletonList("contract"));

    request()
        .get("/articles/{slug}", slug)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/article.json"))
        .body("article.slug", equalTo(slug));
  }

  @Test
  @Disabled(
      "Known bug: ArticleQueryService#findBySlug only fills favoritesCount for signed-in users,"
          + " so anonymous readers always see 0")
  public void get_article_anonymously_reports_favorites_count() {
    Account author = register();
    Account fan = register();
    String slug = createArticle(author, Collections.emptyList());
    request(fan).post("/articles/{slug}/favorite", slug).then().statusCode(200);

    request()
        .get("/articles/{slug}", slug)
        .then()
        .statusCode(200)
        .body("article.favorited", equalTo(false))
        .body("article.favoritesCount", equalTo(1));
  }

  @Test
  public void get_unknown_article_returns_404() {
    request().get("/articles/{slug}", uniqueName("missing")).then().statusCode(404);
  }

  @Test
  public void update_article_accepts_partial_article_envelope() {
    Account author = register();
    String slug = createArticle(author, Collections.emptyList());
    String newTitle = "Did you train your dragon " + uniqueName("u");

    request(author)
        .body(wrap("article", map("title", newTitle)))
        .put("/articles/{slug}", slug)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/article.json"))
        .body("article.title", equalTo(newTitle))
        .body("article.slug", not(equalTo(slug)));
  }

  @Test
  public void update_article_by_non_author_returns_403() {
    Account author = register();
    Account other = register();
    String slug = createArticle(author, Collections.emptyList());

    request(other)
        .body(wrap("article", map("title", "hijacked")))
        .put("/articles/{slug}", slug)
        .then()
        .statusCode(403);
  }

  @Test
  public void delete_article_returns_204_and_article_is_gone() {
    Account author = register();
    String slug = createArticle(author, Collections.emptyList());

    request(author).delete("/articles/{slug}", slug).then().statusCode(204);
    request().get("/articles/{slug}", slug).then().statusCode(404);
  }

  @Test
  public void list_articles_supports_spec_query_parameters() {
    Account author = register();
    Account otherAuthor = register();
    Account fan = register();
    String tag = uniqueName("tag");
    String taggedByAuthor = createArticle(author, Collections.singletonList(tag));
    String untaggedByAuthor = createArticle(author, Collections.singletonList(uniqueName("tag")));
    String taggedByOther = createArticle(otherAuthor, Collections.singletonList(tag));
    request(fan).post("/articles/{slug}/favorite", taggedByAuthor).then().statusCode(200);

    articles(request().queryParam("tag", tag))
        .body("articlesCount", equalTo(2))
        .body("articles.slug", containsInAnyOrder(taggedByAuthor, taggedByOther));

    articles(request().queryParam("author", author.username))
        .body("articlesCount", equalTo(2))
        .body("articles.slug", containsInAnyOrder(taggedByAuthor, untaggedByAuthor));

    articles(request().queryParam("favorited", fan.username))
        .body("articlesCount", equalTo(1))
        .body("articles.slug", containsInAnyOrder(taggedByAuthor));

    articles(request().queryParam("tag", tag).queryParam("author", otherAuthor.username))
        .body("articlesCount", equalTo(1))
        .body("articles.slug", containsInAnyOrder(taggedByOther));

    articles(request().queryParam("author", author.username).queryParam("limit", 1))
        .body("articlesCount", equalTo(2))
        .body("articles.size()", equalTo(1));

    articles(request().queryParam("author", author.username).queryParam("offset", 1))
        .body("articlesCount", equalTo(2))
        .body("articles.size()", equalTo(1));
  }

  @Test
  public void list_articles_without_filters_returns_multiple_articles() {
    Account author = register();
    createArticle(author, Collections.emptyList());

    request()
        .get("/articles")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/articles.json"));
  }

  @Test
  public void feed_returns_articles_from_followed_authors() {
    Account author = register();
    Account reader = register();
    String slug = createArticle(author, Collections.emptyList());
    request(reader).post("/profiles/{username}/follow", author.username).then().statusCode(200);

    request(reader)
        .queryParam("limit", 20)
        .queryParam("offset", 0)
        .get("/articles/feed")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/articles.json"))
        .body("articles.slug", hasItem(slug))
        .body("articles.find { it.slug == '" + slug + "' }.author.following", equalTo(true));
  }

  @Test
  public void feed_without_token_returns_401() {
    request().get("/articles/feed").then().statusCode(401);
  }

  @Test
  public void favorite_and_unfavorite_return_article() {
    Account author = register();
    Account fan = register();
    String slug = createArticle(author, Collections.emptyList());

    request(fan)
        .post("/articles/{slug}/favorite", slug)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/article.json"))
        .body("article.favorited", equalTo(true))
        .body("article.favoritesCount", equalTo(1));

    request(fan)
        .delete("/articles/{slug}/favorite", slug)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/article.json"))
        .body("article.favorited", equalTo(false))
        .body("article.favoritesCount", equalTo(0));
  }

  // ---------------------------------------------------------------- comments

  @Test
  public void add_list_and_delete_comments() {
    Account author = register();
    Account commenter = register();
    String slug = createArticle(author, Collections.emptyList());

    String commentId =
        request(commenter)
            .body(wrap("comment", map("body", "Thank you so much!")))
            .post("/articles/{slug}/comments", slug)
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body(matchesJsonSchemaInClasspath("contract/comment.json"))
            .body("comment.body", equalTo("Thank you so much!"))
            .body("comment.author.username", equalTo(commenter.username))
            .extract()
            .path("comment.id");

    request()
        .get("/articles/{slug}/comments", slug)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/comments.json"))
        .body("comments.id", hasItem(commentId));

    request(commenter)
        .delete("/articles/{slug}/comments/{id}", slug, commentId)
        .then()
        .statusCode(204);

    request()
        .get("/articles/{slug}/comments", slug)
        .then()
        .statusCode(200)
        .body(matchesJsonSchemaInClasspath("contract/comments.json"))
        .body("comments.id", not(hasItem(commentId)));
  }

  @Test
  public void add_blank_comment_returns_errors_keyed_by_field() {
    Account author = register();
    String slug = createArticle(author, Collections.emptyList());

    request(author)
        .body(wrap("comment", map("body", "")))
        .post("/articles/{slug}/comments", slug)
        .then()
        .statusCode(422)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/errors.json"))
        .body("errors", hasKey("body"));
  }

  // ---------------------------------------------------------------- tags

  @Test
  public void get_tags_returns_tag_list() {
    Account author = register();
    String tag = uniqueName("tag");
    createArticle(author, Collections.singletonList(tag));

    request()
        .get("/tags")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/tags.json"))
        .body("tags", hasItem(tag));
  }

  // ---------------------------------------------------------------- helpers

  private static class Account {
    final String username;
    final String email;
    final String password;
    final String token;

    Account(String username, String email, String password, String token) {
      this.username = username;
      this.email = email;
      this.password = password;
      this.token = token;
    }
  }

  private RequestSpecification request() {
    return given().spec(spec);
  }

  private RequestSpecification request(Account account) {
    return request().header("Authorization", "Token " + account.token);
  }

  private Account register() {
    String username = uniqueName("user");
    String email = username + "@example.com";
    String password = "password-" + username;
    String token =
        request()
            .body(wrap("user", map("username", username, "email", email, "password", password)))
            .post("/users")
            .then()
            .statusCode(201)
            .extract()
            .path("user.token");
    return new Account(username, email, password, token);
  }

  private String createArticle(Account author, List<String> tags) {
    return request(author)
        .body(
            wrap(
                "article",
                map(
                    "title",
                    "Article " + uniqueName("a"),
                    "description",
                    "description",
                    "body",
                    "body",
                    "tagList",
                    tags)))
        .post("/articles")
        .then()
        .statusCode(200)
        .extract()
        .path("article.slug");
  }

  private ValidatableResponse articles(RequestSpecification request) {
    return request
        .get("/articles")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(matchesJsonSchemaInClasspath("contract/articles.json"));
  }

  private static String uniqueName(String prefix) {
    return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }

  private static Map<String, Object> wrap(String root, Map<String, Object> body) {
    return map(root, body);
  }

  private static Map<String, Object> map(Object... keyValues) {
    Map<String, Object> map = new HashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      map.put((String) keyValues[i], keyValues[i + 1]);
    }
    return map;
  }
}
