package io.spring.api.ratelimit;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.TagsApi;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.TagsQueryService;
import io.spring.core.service.JwtService;
import io.spring.core.user.UserRepository;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TagsApi.class)
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
@TestPropertySource(
    properties = {
      "ratelimit.anonymous-per-minute=2",
      "ratelimit.authenticated-per-minute=3",
      "ratelimit.health-path=/actuator/health"
    })
public class RateLimitFilterIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private RateLimiter rateLimiter;
  @MockBean private TagsQueryService tagsQueryService;
  @MockBean private UserRepository userRepository;
  @MockBean private JwtService jwtService;

  @BeforeEach
  public void setUp() {
    RestAssuredMockMvc.mockMvc(mvc);
    rateLimiter.reset();
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java"));
  }

  @Test
  public void should_return_429_with_retry_after_when_anonymous_limit_exceeded() {
    given().header("X-Forwarded-For", "10.0.0.1").get("/tags").then().statusCode(200);
    given().header("X-Forwarded-For", "10.0.0.1").get("/tags").then().statusCode(200);

    given()
        .header("X-Forwarded-For", "10.0.0.1")
        .get("/tags")
        .then()
        .statusCode(429)
        .header("Retry-After", matchesPattern("[1-9][0-9]?"))
        .contentType("application/json")
        .body("error", equalTo("rate_limited"))
        .body("retry_after_seconds", greaterThan(0));

    given().header("X-Forwarded-For", "10.0.0.2").get("/tags").then().statusCode(200);
  }

  @Test
  public void should_limit_authenticated_requests_per_api_key() {
    when(jwtService.getSubFromToken("abc")).thenReturn(Optional.of("user-1"));
    when(userRepository.findById("user-1"))
        .thenReturn(Optional.of(new io.spring.core.user.User("a@b.c", "a", "p", "", "")));

    for (int i = 0; i < 3; i++) {
      given().header("Authorization", "Token abc").get("/tags").then().statusCode(200);
    }
    given()
        .header("Authorization", "Token abc")
        .get("/tags")
        .then()
        .statusCode(429)
        .header("Retry-After", not(equalTo("")))
        .body("error", equalTo("rate_limited"));

    given().get("/tags").then().statusCode(200);
  }

  @Test
  public void should_not_rate_limit_health_endpoint() {
    for (int i = 0; i < 5; i++) {
      given()
          .header("X-Forwarded-For", "10.0.0.9")
          .get("/actuator/health")
          .then()
          .statusCode(not(equalTo(429)));
    }
  }
}
