package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class GraphQLHttpTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  public void should_create_user_and_query_me_with_token_over_http() {
    String username = "gql" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    String createUser =
        "mutation { createUser(input: {email: \""
            + username
            + "@example.com\", username: \""
            + username
            + "\", password: \"password\"}) { ... on UserPayload { user { token } } } }";

    ResponseEntity<String> created = post(createUser, null);
    assertEquals(HttpStatus.OK, created.getStatusCode());
    String token = JsonPath.read(created.getBody(), "$.data.createUser.user.token");
    assertNotNull(token);

    ResponseEntity<String> me = post("{ me { username } }", token);
    assertEquals(HttpStatus.OK, me.getStatusCode());
    assertEquals(username, JsonPath.read(me.getBody(), "$.data.me.username"));
  }

  @Test
  public void should_serve_rest_routes_with_trailing_slash() {
    assertEquals(HttpStatus.OK, restTemplate.getForEntity("/tags/", String.class).getStatusCode());
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        restTemplate.getForEntity("/articles/feed/", String.class).getStatusCode());
  }

  private ResponseEntity<String> post(String query, String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null) {
      headers.set("Authorization", "Token " + token);
    }
    return restTemplate.postForEntity(
        "/graphql", new HttpEntity<>(Map.of("query", query), headers), String.class);
  }
}
