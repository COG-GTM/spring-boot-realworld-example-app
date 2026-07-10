package io.spring.api.security;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WebSecurityConfigTest {

  @Autowired private MockMvc mvc;

  @MockBean private JwtService jwtService;

  @MockBean private UserRepository userRepository;

  private User user;
  private final String token = "valid.jwt.token";

  @BeforeEach
  public void setUp() {
    RestAssuredMockMvc.mockMvc(mvc);
    user = new User("secconfig@example.com", "secconfig", "123", "", "");
    when(jwtService.getSubFromToken(eq(token))).thenReturn(Optional.of(user.getId()));
    when(userRepository.findById(eq(user.getId()))).thenReturn(Optional.of(user));
  }

  @Test
  public void public_get_tags_is_accessible_without_authentication() {
    given().contentType("application/json").when().get("/tags").then().statusCode(200);
  }

  @Test
  public void public_get_articles_is_accessible_without_authentication() {
    given().contentType("application/json").when().get("/articles").then().statusCode(200);
  }

  @Test
  public void public_post_login_passes_security_layer() {
    given()
        .contentType("application/json")
        .body("{\"user\":{\"email\":\"nobody@example.com\",\"password\":\"whatever\"}}")
        .when()
        .post("/users/login")
        .then()
        .statusCode(422);
  }

  @Test
  public void options_requests_are_permitted() {
    given().when().options("/articles/feed").then().statusCode(200);
  }

  @Test
  public void protected_feed_requires_authentication() {
    given().contentType("application/json").when().get("/articles/feed").then().statusCode(401);
  }

  @Test
  public void protected_current_user_requires_authentication() {
    given().contentType("application/json").when().get("/user").then().statusCode(401);
  }

  @Test
  public void protected_feed_is_accessible_with_valid_token() {
    given()
        .header("Authorization", "Token " + token)
        .contentType("application/json")
        .when()
        .get("/articles/feed")
        .then()
        .statusCode(200);
  }
}
