package io.spring.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    // every connection to jdbc:sqlite::memory: opens its own database, so requests have to
    // reuse the connection Flyway migrated
    properties = "spring.datasource.hikari.maximum-pool-size=1")
public class ErrorDispatchTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  public void should_return_404_to_anonymous_user_on_missing_article() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/articles/not-exists", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  public void should_return_401_to_anonymous_user_on_protected_endpoint() {
    ResponseEntity<String> response = restTemplate.getForEntity("/articles/feed", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
