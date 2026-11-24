package io.spring.api.security;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = WebSecurityConfigTest.StubController.class)
@Import({WebSecurityConfig.class, WebSecurityConfigTest.StubController.class})
public class WebSecurityConfigTest {
  @Autowired private MockMvc mvc;

  @MockBean private UserRepository userRepository;

  @MockBean private JwtService jwtService;

  private final String token = "valid-token";

  @BeforeEach
  public void setUp() {
    RestAssuredMockMvc.mockMvc(mvc);
    User user = new User("john@jacob.com", "johnjacob", "123", "", "");
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));
    when(jwtService.getSubFromToken(eq("invalid-token"))).thenReturn(Optional.empty());
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));
  }

  @Test
  public void should_reject_anonymous_feed_request() {
    given().when().get("/articles/feed").then().statusCode(401);
  }

  @Test
  public void should_allow_authenticated_feed_request() {
    given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/articles/feed")
        .then()
        .statusCode(200)
        .body(equalTo("feed"));
  }

  @Test
  public void should_reject_request_with_invalid_token() {
    given()
        .header("Authorization", "Token invalid-token")
        .when()
        .get("/articles/feed")
        .then()
        .statusCode(401);
  }

  @Test
  public void should_reject_malformed_authorization_header() {
    given().header("Authorization", token).when().get("/articles/feed").then().statusCode(401);
  }

  @Test
  public void should_allow_anonymous_public_get_requests() {
    given().when().get("/articles/some-slug").then().statusCode(200).body(equalTo("article"));
    given().when().get("/profiles/johnjacob").then().statusCode(200).body(equalTo("profile"));
    given().when().get("/tags").then().statusCode(200).body(equalTo("tags"));
  }

  @Test
  public void should_allow_anonymous_registration_and_login() {
    given().when().post("/users").then().statusCode(200).body(equalTo("register"));
    given().when().post("/users/login").then().statusCode(200).body(equalTo("login"));
  }

  @Test
  public void should_allow_anonymous_graphql_requests() {
    given().when().post("/graphql").then().statusCode(200).body(equalTo("graphql"));
  }

  @Test
  public void should_reject_anonymous_write_requests() {
    given().when().post("/articles").then().statusCode(401);
    given().when().post("/articles/some-slug/comments").then().statusCode(401);
  }

  @Test
  public void should_reject_anonymous_current_user_request() {
    given().when().get("/user").then().statusCode(401);
  }

  @Test
  public void should_allow_authenticated_write_requests() {
    given()
        .header("Authorization", "Token " + token)
        .when()
        .post("/articles")
        .then()
        .statusCode(200)
        .body(equalTo("create"));
  }

  @Test
  public void should_allow_cors_preflight_without_authentication() {
    given()
        .header("Origin", "http://example.com")
        .header("Access-Control-Request-Method", "POST")
        .header("Access-Control-Request-Headers", "Authorization")
        .when()
        .options("/articles")
        .then()
        .statusCode(200)
        .header("Access-Control-Allow-Origin", "*");
  }

  @Test
  public void should_not_create_http_session() {
    given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/articles/feed")
        .then()
        .statusCode(200)
        .header("Set-Cookie", (String) null);
  }

  @RestController
  static class StubController {
    @GetMapping("/articles/feed")
    public String feed() {
      return "feed";
    }

    @GetMapping("/articles/{slug}")
    public String article(@PathVariable("slug") String slug) {
      return "article";
    }

    @PostMapping("/articles")
    public String createArticle() {
      return "create";
    }

    @PostMapping("/articles/{slug}/comments")
    public String createComment(@PathVariable("slug") String slug) {
      return "comment";
    }

    @GetMapping("/profiles/{username}")
    public String profile(@PathVariable("username") String username) {
      return "profile";
    }

    @GetMapping("/tags")
    public String tags() {
      return "tags";
    }

    @PostMapping("/users")
    public String register() {
      return "register";
    }

    @PostMapping("/users/login")
    public String login() {
      return "login";
    }

    @GetMapping("/user")
    public String currentUser() {
      return "user";
    }

    @PostMapping("/graphql")
    public String graphql() {
      return "graphql";
    }
  }
}
