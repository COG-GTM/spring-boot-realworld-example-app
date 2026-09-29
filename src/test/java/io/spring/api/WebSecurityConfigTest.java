package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ArticleQueryService;
import io.spring.application.Page;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleDataList;
import io.spring.core.article.ArticleRepository;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArticlesApi.class)
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class WebSecurityConfigTest extends TestWithCurrentUser {
  @MockBean private ArticleRepository articleRepository;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private ArticleCommandService articleCommandService;

  @Autowired private MockMvc mvc;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_require_auth_for_feed_with_trailing_slash() {
    RestAssuredMockMvc.when().get("/articles/feed/").then().statusCode(401);
    verify(articleQueryService, never()).findUserFeed(any(), any());
  }

  @Test
  public void should_serve_feed_with_trailing_slash_when_authenticated() {
    when(articleQueryService.findUserFeed(eq(user), eq(new Page(0, 20))))
        .thenReturn(new ArticleDataList(new ArrayList<>(), 0));
    given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/articles/feed/")
        .then()
        .statusCode(200);
  }

  @Test
  public void should_require_auth_for_create_article() {
    given()
        .contentType("application/json")
        .body("{\"article\":{\"title\":\"t\",\"description\":\"d\",\"body\":\"b\"}}")
        .when()
        .post("/articles")
        .then()
        .statusCode(401);
    verify(articleCommandService, never()).createArticle(any(), any());
  }

  @Test
  public void should_reject_unknown_authorization_scheme() {
    given()
        .header("Authorization", "Basic " + token)
        .when()
        .get("/articles/feed")
        .then()
        .statusCode(401);
  }

  @Test
  public void should_accept_bearer_authorization_scheme() {
    when(articleQueryService.findUserFeed(eq(user), eq(new Page(0, 20))))
        .thenReturn(new ArticleDataList(new ArrayList<>(), 0));
    given()
        .header("Authorization", "Bearer " + token)
        .when()
        .get("/articles/feed")
        .then()
        .statusCode(200);
  }
}
