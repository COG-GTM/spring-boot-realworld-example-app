package io.spring.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.metrics.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMetrics
public class PrometheusEndpointTest {
  @Autowired private TestRestTemplate restTemplate;

  @Test
  public void should_expose_request_latency_and_mybatis_metrics_without_auth() {
    assertEquals(HttpStatus.OK, restTemplate.getForEntity("/tags", String.class).getStatusCode());

    ResponseEntity<String> response =
        restTemplate.getForEntity("/actuator/prometheus", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    String body = response.getBody();
    assertThat(body, containsString("http_server_requests_seconds_bucket{"));
    assertThat(body, containsString("uri=\"/tags\""));
    assertThat(
        body,
        containsString(
            "statement=\"io.spring.infrastructure.mybatis.readservice.TagReadService.all\""));
    assertThat(body, containsString("mybatis_queries_total{"));
    assertThat(body, containsString("mybatis_query_duration_seconds_count{"));
  }
}
