package io.spring.api;

import static io.restassured.RestAssured.given;

import io.restassured.RestAssured;
import io.spring.application.ArticleQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ErrorDispatchSecurityTest {

  @LocalServerPort private int port;

  @MockitoBean private ArticleQueryService articleQueryService;

  @BeforeEach
  public void setUp() {
    RestAssured.port = port;
  }

  @Test
  public void should_return_404_for_missing_article_to_anonymous_user() {
    given().when().get("/articles/missing-slug").then().statusCode(404);
  }
}
