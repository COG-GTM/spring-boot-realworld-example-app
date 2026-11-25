package io.spring.api.security;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = WebSecurityConfigTest.StubController.class)
@Import({WebSecurityConfig.class, WebSecurityConfigTest.StubController.class})
public class WebSecurityConfigTest {

  @Autowired private MockMvc mvc;

  @MockBean private UserRepository userRepository;

  @MockBean private JwtService jwtService;

  private String token;

  @RestController
  static class StubController {
    @GetMapping({"/articles/feed", "/articles/{slug}", "/profiles/{username}", "/tags", "/user"})
    public String get() {
      return "ok";
    }

    @PostMapping({"/users", "/users/login", "/articles", "/graphql"})
    public String post() {
      return "ok";
    }
  }

  @BeforeEach
  public void setUp() {
    RestAssuredMockMvc.mockMvc(mvc);
    User user = new User("john@jacob.com", "johnjacob", "123", "", "");
    token = "token";
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));
  }

  @Test
  public void should_permit_anonymous_get_on_public_read_endpoints() {
    given().when().get("/articles/some-slug").then().statusCode(200);
    given().when().get("/profiles/johnjacob").then().statusCode(200);
    given().when().get("/tags").then().statusCode(200);
  }

  @Test
  public void should_permit_anonymous_registration_and_login() {
    given().when().post("/users").then().statusCode(200);
    given().when().post("/users/login").then().statusCode(200);
  }

  @Test
  public void should_permit_anonymous_graphql() {
    given().when().post("/graphql").then().statusCode(200);
  }

  @Test
  public void should_require_authentication_for_feed_even_though_articles_are_public() {
    given().when().get("/articles/feed").then().statusCode(401);
    given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/articles/feed")
        .then()
        .statusCode(200);
  }

  @Test
  public void should_require_authentication_for_other_endpoints() {
    given().when().post("/articles").then().statusCode(401);
    given().when().get("/user").then().statusCode(401);
    given().when().get("/not-mapped").then().statusCode(401);
  }

  @Test
  public void should_allow_authenticated_write() {
    given()
        .header("Authorization", "Token " + token)
        .when()
        .post("/articles")
        .then()
        .statusCode(200);
  }

  @Test
  public void should_answer_cors_preflight_without_authentication() {
    given()
        .header("Origin", "http://example.com")
        .header("Access-Control-Request-Method", "POST")
        .header("Access-Control-Request-Headers", "Authorization")
        .when()
        .options("/articles")
        .then()
        .statusCode(200)
        .header("Access-Control-Allow-Origin", equalTo("*"));
  }

  @Test
  public void should_not_require_csrf_token_or_create_session() throws Exception {
    MvcResult result =
        mvc.perform(post("/articles").header("Authorization", "Token " + token))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("Set-Cookie"))
            .andReturn();
    Assertions.assertNull(result.getRequest().getSession(false));
  }
}
