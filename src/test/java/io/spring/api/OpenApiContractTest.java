package io.spring.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.filter.Filter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Contract tests for {@code src/main/resources/static/openapi.yaml}.
 *
 * <p>Runs the real application (real HTTP, security, Jackson and an in-memory SQLite database) and
 * validates every request/response against the spec. The spec is also checked against the Spring
 * MVC handler mappings, and after all tests have run, every documented response status of every
 * operation must have been exercised. Together this keeps the spec and the implementation from
 * drifting apart silently.
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties =
        "spring.datasource.url=jdbc:sqlite:file:openapi-contract-test?mode=memory&cache=shared")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OpenApiContractTest {
  private static final String MISSING = "does-not-exist";

  private final OpenApiContract contract = OpenApiContract.load();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AtomicInteger finishedTests = new AtomicInteger();

  @LocalServerPort private int port;

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping handlerMapping;

  @Test
  public void spec_is_a_valid_openapi_document() {
    assertThat(contract.parseMessages()).isEmpty();
    assertThat(contract.openApi()).isNotNull();
  }

  @Test
  public void spec_documents_exactly_the_implemented_rest_endpoints() {
    Set<String> implemented = new TreeSet<>();
    handlerMapping
        .getHandlerMethods()
        .forEach(
            (info, handler) -> {
              if (handler.getBeanType().getPackage().getName().startsWith("io.spring.api")) {
                implemented.addAll(endpointsOf(info));
              }
            });
    Set<String> documented = new TreeSet<>();
    contract.operations().forEach(operation -> documented.add(operation.key()));

    assertThat(documented)
        .as(
            "operations in %s must match the @RestController mappings",
            OpenApiContract.SPEC_RESOURCE)
        .isEqualTo(implemented);
  }

  @Test
  public void spec_is_served_without_authentication() {
    given()
        .port(port)
        .when()
        .get("/openapi.yaml")
        .then()
        .statusCode(200)
        .body(equalTo(contract.specContent()));
  }

  @Test
  public void users_and_authentication() {
    TestUser jake = register("jake");

    api()
        .body(json(Map.of("user", Map.of("email", jake.email, "password", jake.password))))
        .post("/users/login")
        .then()
        .statusCode(200)
        .body("user.username", equalTo(jake.username));
    api()
        .body(json(Map.of("user", Map.of("email", jake.email, "password", "wrong-password"))))
        .post("/users/login")
        .then()
        .statusCode(422)
        .body("message", equalTo("invalid email or password"));
    invalidRequest()
        .body(json(Map.of("user", Map.of("email", "", "password", ""))))
        .post("/users/login")
        .then()
        .statusCode(422);

    invalidRequest()
        .body(json(Map.of("user", Map.of("email", "not-an-email", "username", "", "password", ""))))
        .post("/users")
        .then()
        .statusCode(422)
        .body("errors.email", hasItem("should be an email"));
    api()
        .body(
            json(
                Map.of(
                    "user", Map.of("email", jake.email, "username", "other", "password", "pass"))))
        .post("/users")
        .then()
        .statusCode(422)
        .body("errors.email", hasItem("duplicated email"));

    api(jake).get("/user").then().statusCode(200).body("user.email", equalTo(jake.email));
    api(jake)
        .body(json(Map.of("user", Map.of("bio", "I like to skateboard"))))
        .put("/user")
        .then()
        .statusCode(200)
        .body("user.bio", equalTo("I like to skateboard"));
    invalidRequest(jake)
        .body(json(Map.of("user", Map.of("email", "not-an-email"))))
        .put("/user")
        .then()
        .statusCode(422);
  }

  @Test
  public void profiles() {
    TestUser jake = register("jake");
    TestUser celeb = register("celeb");

    api().get("/profiles/{username}", celeb.username).then().statusCode(200);
    api().get("/profiles/{username}", MISSING).then().statusCode(404);

    api(jake)
        .post("/profiles/{username}/follow", celeb.username)
        .then()
        .statusCode(200)
        .body("profile.following", equalTo(true));
    api(jake)
        .get("/profiles/{username}", celeb.username)
        .then()
        .statusCode(200)
        .body("profile.following", equalTo(true));
    api(jake).post("/profiles/{username}/follow", MISSING).then().statusCode(404);

    api(jake)
        .delete("/profiles/{username}/follow", celeb.username)
        .then()
        .statusCode(200)
        .body("profile.following", equalTo(false));
    api(jake).delete("/profiles/{username}/follow", celeb.username).then().statusCode(404);
    api(jake).delete("/profiles/{username}/follow", MISSING).then().statusCode(404);
  }

  @Test
  public void articles_and_favorites() {
    TestUser author = register("author");
    TestUser reader = register("reader");
    String tag = "tag-" + uniqueSuffix();
    String slug = createArticle(author, List.of(tag, "dragons"));

    invalidRequest(author)
        .body(json(Map.of("article", Map.of("title", "", "description", "", "body", ""))))
        .post("/articles")
        .then()
        .statusCode(422)
        .body("errors.body", hasItem("can't be empty"));

    api().get("/articles").then().statusCode(200);
    api(reader)
        .queryParam("tag", tag)
        .queryParam("author", author.username)
        .queryParam("offset", 0)
        .queryParam("limit", 5)
        .get("/articles")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(1))
        .body("articles[0].slug", equalTo(slug));

    api().get("/articles/{slug}", slug).then().statusCode(200);
    api().get("/articles/{slug}", MISSING).then().statusCode(404);

    api(reader).get("/articles/feed").then().statusCode(200).body("articlesCount", equalTo(0));
    api(reader).post("/profiles/{username}/follow", author.username).then().statusCode(200);
    api(reader)
        .queryParam("limit", 10)
        .get("/articles/feed")
        .then()
        .statusCode(200)
        .body("articles.slug", hasItem(slug))
        .body("articles[0].author.following", equalTo(true));

    api(reader)
        .post("/articles/{slug}/favorite", slug)
        .then()
        .statusCode(200)
        .body("article.favorited", equalTo(true))
        .body("article.favoritesCount", equalTo(1));
    api()
        .queryParam("favorited", reader.username)
        .get("/articles")
        .then()
        .statusCode(200)
        .body("articles.slug", hasItem(slug));
    api(reader)
        .delete("/articles/{slug}/favorite", slug)
        .then()
        .statusCode(200)
        .body("article.favorited", equalTo(false));
    api(reader).post("/articles/{slug}/favorite", MISSING).then().statusCode(404);
    api(reader).delete("/articles/{slug}/favorite", MISSING).then().statusCode(404);

    api().get("/tags").then().statusCode(200).body("tags", hasItem(tag));

    String newTitle = "Updated " + uniqueSuffix();
    String updateBody = json(Map.of("article", Map.of("title", newTitle)));
    api(reader).body(updateBody).put("/articles/{slug}", slug).then().statusCode(403);
    api(author).body(updateBody).put("/articles/{slug}", MISSING).then().statusCode(404);
    String newSlug =
        api(author)
            .body(updateBody)
            .put("/articles/{slug}", slug)
            .then()
            .statusCode(200)
            .body("article.title", equalTo(newTitle))
            .extract()
            .path("article.slug");

    api(reader).delete("/articles/{slug}", newSlug).then().statusCode(403);
    api(author).delete("/articles/{slug}", MISSING).then().statusCode(404);
    api(author).delete("/articles/{slug}", newSlug).then().statusCode(204);
    api().get("/articles/{slug}", newSlug).then().statusCode(404);
  }

  @Test
  public void comments() {
    TestUser author = register("author");
    TestUser commenter = register("commenter");
    String slug = createArticle(author, List.of());
    String commentBody = json(Map.of("comment", Map.of("body", "Thank you so much!")));

    String commentId =
        api(commenter)
            .body(commentBody)
            .post("/articles/{slug}/comments", slug)
            .then()
            .statusCode(201)
            .body("comment.author.username", equalTo(commenter.username))
            .extract()
            .path("comment.id");
    invalidRequest(commenter)
        .body(json(Map.of("comment", Map.of("body", ""))))
        .post("/articles/{slug}/comments", slug)
        .then()
        .statusCode(422);
    api(commenter)
        .body(commentBody)
        .post("/articles/{slug}/comments", MISSING)
        .then()
        .statusCode(404);

    api().get("/articles/{slug}/comments", slug).then().statusCode(200);
    api(author)
        .get("/articles/{slug}/comments", slug)
        .then()
        .statusCode(200)
        .body("comments.id", hasItem(commentId));
    api().get("/articles/{slug}/comments", MISSING).then().statusCode(404);

    TestUser stranger = register("stranger");
    api(stranger).delete("/articles/{slug}/comments/{id}", slug, commentId).then().statusCode(403);
    api(commenter).delete("/articles/{slug}/comments/{id}", slug, MISSING).then().statusCode(404);
    api(commenter).delete("/articles/{slug}/comments/{id}", slug, commentId).then().statusCode(204);
    api()
        .get("/articles/{slug}/comments", slug)
        .then()
        .statusCode(200)
        .body("comments.id", not(hasItem(commentId)));
  }

  @Test
  public void protected_operations_reject_anonymous_requests() {
    for (OpenApiContract.Operation operation : contract.operations()) {
      if (!operation.statuses.contains("401")) {
        continue;
      }
      RequestSpecification request = invalidRequest();
      if (operation.hasRequestBody) {
        request.body("{}");
      }
      request.request(operation.method, operation.pathWith(MISSING)).then().statusCode(401);
    }
  }

  @AfterEach
  public void countFinishedTest() {
    finishedTests.incrementAndGet();
  }

  @AfterAll
  public void every_documented_response_is_exercised() {
    long totalTests =
        Arrays.stream(getClass().getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(Test.class))
            .count();
    if (finishedTests.get() < totalTests) {
      // Only a subset of the tests was selected, so coverage would be incomplete.
      return;
    }
    Set<String> unexercised = contract.unexercisedResponses();
    assertThat(unexercised)
        .as(
            "documented responses never produced by %s:%n%s",
            getClass().getSimpleName(), OpenApiContract.describe(unexercised))
        .isEmpty();
  }

  private static Set<String> endpointsOf(RequestMappingInfo info) {
    Set<String> endpoints = new TreeSet<>();
    for (String pattern : info.getPatternValues()) {
      String path = pattern.startsWith("/") ? pattern : "/" + pattern;
      info.getMethodsCondition()
          .getMethods()
          .forEach(method -> endpoints.add(method.name() + " " + path));
    }
    return endpoints;
  }

  private RequestSpecification api() {
    return request(contract.validating());
  }

  private RequestSpecification api(TestUser user) {
    return authenticated(api(), user);
  }

  private RequestSpecification invalidRequest() {
    return request(contract.validatingResponseOnly());
  }

  private RequestSpecification invalidRequest(TestUser user) {
    return authenticated(invalidRequest(), user);
  }

  private RequestSpecification request(Filter filter) {
    return given().port(port).filter(filter).contentType(ContentType.JSON);
  }

  private static RequestSpecification authenticated(RequestSpecification request, TestUser user) {
    return request.header("Authorization", "Token " + user.token);
  }

  private TestUser register(String prefix) {
    TestUser user = new TestUser(prefix + "-" + uniqueSuffix());
    String token =
        api()
            .body(
                json(
                    Map.of(
                        "user",
                        Map.of(
                            "email",
                            user.email,
                            "username",
                            user.username,
                            "password",
                            user.password))))
            .post("/users")
            .then()
            .statusCode(201)
            .extract()
            .path("user.token");
    user.token = token;
    return user;
  }

  private String createArticle(TestUser author, List<String> tags) {
    return api(author)
        .body(
            json(
                Map.of(
                    "article",
                    Map.of(
                        "title",
                        "How to train your dragon " + uniqueSuffix(),
                        "description",
                        "Ever wonder how?",
                        "body",
                        "You have to believe",
                        "tagList",
                        tags))))
        .post("/articles")
        .then()
        .statusCode(200)
        .extract()
        .path("article.slug");
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String uniqueSuffix() {
    return UUID.randomUUID().toString().substring(0, 8);
  }

  private static final class TestUser {
    final String username;
    final String email;
    final String password = "password";
    String token;

    TestUser(String username) {
      this.username = username;
      this.email = username + "@example.com";
    }
  }
}
