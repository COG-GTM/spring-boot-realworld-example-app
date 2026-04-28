package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ProfileQueryService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProfileApi.class)
@Import({WebSecurityConfig.class, JacksonCustomizations.class})
public class ProfileApiEdgeCaseTest extends TestWithCurrentUser {

  @Autowired private MockMvc mvc;

  @MockBean private ProfileQueryService profileQueryService;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_get_404_for_non_existent_profile() {
    when(profileQueryService.findByUsername(eq("nonexistent"), eq(null)))
        .thenReturn(Optional.empty());
    RestAssuredMockMvc.when().get("/profiles/{username}", "nonexistent").then().statusCode(404);
  }

  @Test
  public void should_get_404_when_following_non_existent_user() {
    when(userRepository.findByUsername(eq("nonexistent"))).thenReturn(Optional.empty());

    given()
        .header("Authorization", "Token " + token)
        .when()
        .post("/profiles/{username}/follow", "nonexistent")
        .then()
        .statusCode(404);
  }

  @Test
  public void should_get_404_when_unfollowing_non_existent_user() {
    when(userRepository.findByUsername(eq("nonexistent"))).thenReturn(Optional.empty());

    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/profiles/{username}/follow", "nonexistent")
        .then()
        .statusCode(404);
  }

  @Test
  public void should_get_401_when_following_without_auth() {
    given().when().post("/profiles/{username}/follow", "someuser").then().statusCode(401);
  }

  @Test
  public void should_get_401_when_unfollowing_without_auth() {
    given().when().delete("/profiles/{username}/follow", "someuser").then().statusCode(401);
  }
}
