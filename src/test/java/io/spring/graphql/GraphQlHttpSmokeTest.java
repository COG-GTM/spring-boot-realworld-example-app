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
    assertThat(response.getBody()).contains("\"tags\"");
  }

  @Test
  public void should_return_errors_array_for_invalid_query() {
    ResponseEntity<String> response = postGraphQl("{\"query\":\"{ nonExistentField }\"}");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("errors");
  }
}
