package io.spring.api.security;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.ArticlesApi;
import io.spring.api.UsersApi;
import io.spring.application.ArticleQueryService;
import io.spring.application.Page;
import io.spring.application.UserQueryService;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.data.ArticleDataList;
import io.spring.application.data.UserData;
import io.spring.application.user.UserService;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ArticlesApi.class, UsersApi.class})
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class WebSecurityConfigTest {
  private static final String TOKEN = "token";

  @Autowired private MockMvc mvc;

  @MockBean private ArticleQueryService articleQueryService;

  @MockBean private ArticleCommandService articleCommandService;

  @MockBean private UserQueryService userQueryService;

  @MockBean private UserService userService;

  @MockBean private UserRepository userRepository;

  @MockBean private JwtService jwtService;

  private User user;

  @BeforeEach
  public void setUp() {
    RestAssuredMockMvc.mockMvc(mvc);
    user =
        new User(
            "john@jacob.com",
            "johnjacob",
            "123",
            "",
            "https://static.productionready.io/images/smiley-cyrus.jpg");
  }

  @Test
  public void should_reject_anonymous_feed_request() {
    given().when().get("/articles/feed").then().statusCode(401);
  }

  @Test
  public void should_allow_feed_request_with_valid_token() {
    when(jwtService.getSubFromToken(eq(TOKEN))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));
    when(articleQueryService.findUserFeed(eq(user), any(Page.class)))
        .thenReturn(new ArticleDataList(Collections.emptyList(), 0));

    given()
        .header("Authorization", "Token " + TOKEN)
        .when()
        .get("/articles/feed")
        .then()
        .statusCode(200);
  }

  @Test
  public void should_allow_anonymous_article_listing() {
    when(articleQueryService.findRecentArticles(
            eq(null), eq(null), eq(null), any(Page.class), eq(null)))
        .thenReturn(new ArticleDataList(Collections.emptyList(), 0));

    given().when().get("/articles").then().statusCode(200);
  }

  @Test
  public void should_reject_anonymous_article_creation() {
    Map<String, Object> article = new HashMap<>();
    article.put("title", "a title");
    article.put("description", "a description");
    article.put("body", "a body");

    given()
        .contentType("application/json")
        .body(Collections.singletonMap("article", article))
        .when()
        .post("/articles")
        .then()
        .statusCode(401);
  }

  @Test
  public void should_allow_anonymous_user_registration() {
    Map<String, Object> registration = new HashMap<>();
    registration.put("email", user.getEmail());
    registration.put("username", user.getUsername());
    registration.put("password", "johnjacobpassword");

    when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.empty());
    when(userRepository.findByUsername(eq(user.getUsername()))).thenReturn(Optional.empty());
    when(userService.createUser(any())).thenReturn(user);
    when(userQueryService.findById(eq(user.getId())))
        .thenReturn(
            Optional.of(
                new UserData(
                    user.getId(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getBio(),
                    user.getImage())));
    when(jwtService.toToken(eq(user))).thenReturn(TOKEN);

    given()
        .contentType("application/json")
        .body(Collections.singletonMap("user", registration))
        .when()
        .post("/users")
        .then()
        .statusCode(201);
  }
}
