package io.spring.api.security;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.ArticlesApi;
import io.spring.api.TagsApi;
import io.spring.application.ArticleQueryService;
import io.spring.application.TagsQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleDataList;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ArticlesApi.class, TagsApi.class})
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class WebSecurityConfigTest {
  @Autowired private MockMvc mvc;

  @MockitoBean private UserRepository userRepository;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private ArticleQueryService articleQueryService;
  @MockitoBean private ArticleCommandService articleCommandService;
  @MockitoBean private TagsQueryService tagsQueryService;

  private User user;

  @BeforeEach
  public void setUp() {
    RestAssuredMockMvc.mockMvc(mvc);
    user = new User("john@jacob.com", "johnjacob", "123", "", "");
    when(jwtService.getSubFromToken(eq("valid"))).thenReturn(Optional.of(user.getId()));
    when(jwtService.getSubFromToken(eq("invalid"))).thenReturn(Optional.empty());
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));
    when(tagsQueryService.allTags()).thenReturn(Arrays.asList("java", "spring"));
    when(articleQueryService.findUserFeed(any(), any()))
        .thenReturn(new ArticleDataList(new ArrayList<>(), 0));
    when(articleQueryService.findRecentArticles(any(), any(), any(), any(), any()))
        .thenReturn(new ArticleDataList(new ArrayList<>(), 0));
  }

  @Test
  public void should_permit_anonymous_get_tags() {
    given().when().get("/tags").then().statusCode(200).body("tags[0]", equalTo("java"));
  }

  @Test
  public void should_permit_anonymous_get_articles() {
    given().when().get("/articles").then().statusCode(200).body("articlesCount", equalTo(0));
  }

  @Test
  public void should_return_401_for_anonymous_feed() {
    given().when().get("/articles/feed").then().statusCode(401);
  }

  @Test
  public void should_return_401_for_feed_with_invalid_token() {
    given()
        .header("Authorization", "Token invalid")
        .when()
        .get("/articles/feed")
        .then()
        .statusCode(401);
  }

  @Test
  public void should_allow_feed_with_valid_token() {
    given()
        .header("Authorization", "Token valid")
        .when()
        .get("/articles/feed")
        .then()
        .statusCode(200)
        .body("articlesCount", equalTo(0));
    verify(articleQueryService).findUserFeed(eq(user), any());
  }

  @Test
  public void should_return_401_for_anonymous_post_articles() {
    given()
        .contentType("application/json")
        .body("{\"article\":{\"title\":\"t\",\"description\":\"d\",\"body\":\"b\"}}")
        .when()
        .post("/articles")
        .then()
        .statusCode(401);
  }

  @Test
  public void should_return_401_for_anonymous_request_to_unlisted_path() {
    given().when().delete("/tags").then().statusCode(401);
  }

  @Test
  public void should_permit_cors_preflight_without_token() {
    given()
        .header("Origin", "http://example.com")
        .header("Access-Control-Request-Method", "POST")
        .header("Access-Control-Request-Headers", "Authorization,Content-Type")
        .when()
        .options("/articles")
        .then()
        .statusCode(200)
        .header("Access-Control-Allow-Origin", equalTo("*"));
  }
}
