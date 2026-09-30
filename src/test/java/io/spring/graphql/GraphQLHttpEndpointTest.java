package io.spring.graphql;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;

import io.restassured.RestAssured;
import io.spring.application.TagsQueryService;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class GraphQLHttpEndpointTest {

  @LocalServerPort private int port;

  @MockitoBean private TagsQueryService tagsQueryService;

  @BeforeEach
  public void setUp() {
    RestAssured.port = port;
  }

  @Test
  public void should_accept_unwrapped_graphql_request_body() {
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring"));

    given()
        .contentType("application/json")
        .body("{\"query\": \"{ tags }\"}")
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.tags", contains("java", "spring"));
  }
}
