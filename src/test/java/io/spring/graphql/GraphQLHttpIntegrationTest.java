package io.spring.graphql;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GraphQLHttpIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    RestAssured.port = port;
  }

  @Test
  public void should_keep_unwrap_root_value_enabled_for_rest_object_mapper() {
    assertTrue(objectMapper.isEnabled(DeserializationFeature.UNWRAP_ROOT_VALUE));
  }

  @Test
  public void should_accept_unwrapped_graphql_request_body() {
    graphql(null, "{ tags }", new HashMap<>())
        .then()
        .statusCode(200)
        .body("errors", nullValue())
        .body("data.tags", notNullValue());
  }

  @Test
  public void should_return_unauthenticated_error_for_bad_login() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("email", "nobody-" + UUID.randomUUID() + "@example.com");
    variables.put("password", "wrong");

    graphql(
            null,
            "mutation($email: String!, $password: String!) {"
                + " login(email: $email, password: $password) { user { token } } }",
            variables)
        .then()
        .statusCode(200)
        .body("data.login", nullValue())
        .body("errors", hasSize(1))
        .body("errors[0].message", equalTo("invalid email or password"))
        .body("errors[0].path[0]", equalTo("login"))
        .body("errors[0].extensions.errorType", equalTo("UNAUTHENTICATED"));
  }

  @Test
  public void should_return_field_errors_for_invalid_create_user() {
    Map<String, Object> input = new HashMap<>();
    input.put("email", "not-an-email");
    input.put("username", "");
    input.put("password", "123");

    JsonPath json =
        graphql(
                null,
                "mutation($input: CreateUserInput) { createUser(input: $input) {"
                    + " ... on Error { message errors { key value } } } }",
                Map.of("input", input))
            .then()
            .statusCode(200)
            .body("errors", nullValue())
            .body("data.createUser.message", equalTo("BAD_REQUEST"))
            .extract()
            .jsonPath();

    assertTrue(json.getList("data.createUser.errors.key").contains("email"));
    assertTrue(json.getList("data.createUser.errors.key").contains("username"));
  }

  @Test
  public void should_authenticate_with_jwt_and_build_article_and_comment_page_info() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String username = "gql" + suffix;
    String token = createUser(username, username + "@example.com");

    graphql(token, "{ me { username } }", new HashMap<>())
        .then()
        .statusCode(200)
        .body("errors", nullValue())
        .body("data.me.username", equalTo(username));

    String firstSlug = createArticle(token, "first " + suffix);
    createArticle(token, "second " + suffix);

    JsonPath firstPage =
        graphql(
                token,
                "query($author: String) { articles(first: 1, authoredBy: $author) {"
                    + " edges { cursor node { slug } }"
                    + " pageInfo { startCursor endCursor hasNextPage hasPreviousPage } } }",
                Map.of("author", username))
            .then()
            .statusCode(200)
            .body("errors", nullValue())
            .body("data.articles.edges", hasSize(1))
            .body("data.articles.pageInfo.hasNextPage", equalTo(true))
            .body("data.articles.pageInfo.hasPreviousPage", equalTo(false))
            .extract()
            .jsonPath();
    String cursor = firstPage.getString("data.articles.edges[0].cursor");
    assertEquals(cursor, firstPage.getString("data.articles.pageInfo.startCursor"));
    assertEquals(cursor, firstPage.getString("data.articles.pageInfo.endCursor"));

    Map<String, Object> nextVariables = new HashMap<>();
    nextVariables.put("author", username);
    nextVariables.put("after", cursor);
    graphql(
            token,
            "query($author: String, $after: String) {"
                + " articles(first: 1, after: $after, authoredBy: $author) {"
                + " edges { node { slug } } pageInfo { hasNextPage hasPreviousPage } } }",
            nextVariables)
        .then()
        .statusCode(200)
        .body("errors", nullValue())
        .body("data.articles.edges", hasSize(1))
        .body("data.articles.edges[0].node.slug", equalTo(firstSlug))
        .body("data.articles.pageInfo.hasNextPage", equalTo(false));

    Map<String, Object> commentVariables = new HashMap<>();
    commentVariables.put("slug", firstSlug);
    commentVariables.put("body", "nice");
    graphql(
            token,
            "mutation($slug: String!, $body: String!) {"
                + " addComment(slug: $slug, body: $body) { comment { id } } }",
            commentVariables)
        .then()
        .statusCode(200)
        .body("errors", nullValue());

    JsonPath comments =
        graphql(
                token,
                "query($slug: String!) { article(slug: $slug) { comments(first: 10) {"
                    + " edges { cursor node { body } }"
                    + " pageInfo { startCursor endCursor hasNextPage hasPreviousPage } } } }",
                Map.of("slug", firstSlug))
            .then()
            .statusCode(200)
            .body("errors", nullValue())
            .body("data.article.comments.edges", hasSize(1))
            .body("data.article.comments.edges[0].node.body", equalTo("nice"))
            .body("data.article.comments.pageInfo.hasNextPage", equalTo(false))
            .body("data.article.comments.pageInfo.hasPreviousPage", equalTo(false))
            .extract()
            .jsonPath();
    String commentCursor = comments.getString("data.article.comments.edges[0].cursor");
    assertEquals(commentCursor, comments.getString("data.article.comments.pageInfo.startCursor"));
    assertEquals(commentCursor, comments.getString("data.article.comments.pageInfo.endCursor"));
  }

  @Test
  public void should_return_null_page_cursors_for_empty_connection() {
    graphql(
            null,
            "query($author: String) { articles(first: 5, authoredBy: $author) {"
                + " edges { cursor } pageInfo { startCursor endCursor hasNextPage } } }",
            Map.of("author", "nobody-" + UUID.randomUUID()))
        .then()
        .statusCode(200)
        .body("errors", nullValue())
        .body("data.articles.edges", hasSize(0))
        .body("data.articles.pageInfo.startCursor", nullValue())
        .body("data.articles.pageInfo.endCursor", nullValue())
        .body("data.articles.pageInfo.hasNextPage", equalTo(false));
  }

  private String createUser(String username, String email) {
    Map<String, Object> input = new HashMap<>();
    input.put("email", email);
    input.put("username", username);
    input.put("password", "password");
    return graphql(
            null,
            "mutation($input: CreateUserInput) { createUser(input: $input) {"
                + " ... on UserPayload { user { token } } } }",
            Map.of("input", input))
        .then()
        .statusCode(200)
        .body("errors", nullValue())
        .extract()
        .jsonPath()
        .getString("data.createUser.user.token");
  }

  private String createArticle(String token, String title) {
    Map<String, Object> input = new HashMap<>();
    input.put("title", title);
    input.put("description", "desc");
    input.put("body", "body");
    return graphql(
            token,
            "mutation($input: CreateArticleInput!) {"
                + " createArticle(input: $input) { article { slug } } }",
            Map.of("input", input))
        .then()
        .statusCode(200)
        .body("errors", nullValue())
        .extract()
        .jsonPath()
        .getString("data.createArticle.article.slug");
  }

  private io.restassured.response.Response graphql(
      String token, String query, Map<String, Object> variables) {
    Map<String, Object> body = new HashMap<>();
    body.put("query", query);
    body.put("variables", variables);
    RequestSpecification request = given().contentType(ContentType.JSON).body(body);
    if (token != null) {
      request = request.header("Authorization", "Token " + token);
    }
    return request.when().post("/graphql");
  }
}
