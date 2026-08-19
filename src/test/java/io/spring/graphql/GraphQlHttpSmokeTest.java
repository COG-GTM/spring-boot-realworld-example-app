package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GraphQlHttpSmokeTest {

  @Autowired private TestRestTemplate restTemplate;

  private ResponseEntity<String> postGraphQl(String body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.postForEntity("/graphql", new HttpEntity<>(body, headers), String.class);
  }

  @Test
  public void should_accept_standard_graphql_request_body() {
    ResponseEntity<String> response = postGraphQl("{\"query\":\"{ tags }\"}");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"data\"").contains("\"tags\"");
    assertThat(response.getBody()).doesNotContain("\"errors\"");
  }

  @Test
  public void should_execute_authenticated_query_with_security_context() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<String> register =
        restTemplate.postForEntity(
            "/users",
            new HttpEntity<>(
                "{\"user\":{\"username\":\"graphqlsmoke\",\"email\":\"graphqlsmoke@example.com\",\"password\":\"password123\"}}",
                headers),
            String.class);
    assertThat(register.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    String token = register.getBody().replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

    HttpHeaders authHeaders = new HttpHeaders();
    authHeaders.setContentType(MediaType.APPLICATION_JSON);
    authHeaders.set("Authorization", "Token " + token);
    ResponseEntity<String> response =
        restTemplate.postForEntity(
            "/graphql",
            new HttpEntity<>("{\"query\":\"{ me { username } }\"}", authHeaders),
            String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"username\":\"graphqlsmoke\"");
    assertThat(response.getBody()).doesNotContain("\"errors\"");
  }

  @Test
  public void should_return_errors_array_for_invalid_query() {
    ResponseEntity<String> response = postGraphQl("{\"query\":\"{ nonExistentField }\"}");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"errors\"").contains("nonExistentField");
  }
}
