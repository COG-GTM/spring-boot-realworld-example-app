package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.mockito.ArgumentMatchers.any;
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
public class ProfileApiErrorTest extends TestWithCurrentUser {

  @Autowired private MockMvc mvc;

  @MockBean private ProfileQueryService profileQueryService;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_return_404_when_profile_not_found() throws Exception {
    String nonExistentUsername = "nonexistent";
    when(profileQueryService.findByUsername(eq(nonExistentUsername), any()))
        .thenReturn(Optional.empty());
    when(userRepository.findByUsername(eq(nonExistentUsername))).thenReturn(Optional.empty());

    given()
        .contentType("application/json")
        .when()
        .get("/profiles/{username}", nonExistentUsername)
        .then()
        .statusCode(404);
  }

  @Test
  public void should_return_401_when_following_without_auth() throws Exception {
    given()
        .contentType("application/json")
        .when()
        .post("/profiles/{username}/follow", "someuser")
        .then()
        .statusCode(401);
  }

  @Test
  public void should_return_401_when_unfollowing_without_auth() throws Exception {
    given()
        .contentType("application/json")
        .when()
        .delete("/profiles/{username}/follow", "someuser")
        .then()
        .statusCode(401);
  }
}
