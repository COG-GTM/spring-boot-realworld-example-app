package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.spring.JacksonCustomizations;
import io.spring.api.security.WebSecurityConfig;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
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
public class ProfileApiTest extends TestWithCurrentUser {
  private User anotherUser;

  @Autowired private MockMvc mvc;

  @MockBean private ProfileQueryService profileQueryService;

  private ProfileData profileData;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    RestAssuredMockMvc.mockMvc(mvc);
    anotherUser = new User("username@test.com", "username", "123", "", "");
    profileData =
        new ProfileData(
            anotherUser.getId(),
            anotherUser.getUsername(),
            anotherUser.getBio(),
            anotherUser.getImage(),
            false);
    when(userRepository.findByUsername(eq(anotherUser.getUsername())))
        .thenReturn(Optional.of(anotherUser));
  }

  @Test
  public void should_get_user_profile_success() throws Exception {
    when(profileQueryService.findByUsername(eq(profileData.getUsername()), eq(null)))
        .thenReturn(Optional.of(profileData));
    RestAssuredMockMvc.when()
        .get("/profiles/{username}", profileData.getUsername())
        .prettyPeek()
        .then()
        .statusCode(200)
        .body("profile.username", equalTo(profileData.getUsername()));
  }

  @Test
  public void should_follow_user_success() throws Exception {
    when(profileQueryService.findByUsername(eq(profileData.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileData));
    given()
        .header("Authorization", "Token " + token)
        .when()
        .post("/profiles/{username}/follow", anotherUser.getUsername())
        .prettyPeek()
        .then()
        .statusCode(200);
    verify(userRepository).saveRelation(new FollowRelation(user.getId(), anotherUser.getId()));
  }

  @Test
  public void should_unfollow_user_success() throws Exception {
    FollowRelation followRelation = new FollowRelation(user.getId(), anotherUser.getId());
    when(userRepository.findRelation(eq(user.getId()), eq(anotherUser.getId())))
        .thenReturn(Optional.of(followRelation));
    when(profileQueryService.findByUsername(eq(profileData.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileData));

    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/profiles/{username}/follow", anotherUser.getUsername())
        .prettyPeek()
        .then()
        .statusCode(200);

    verify(userRepository).removeRelation(eq(followRelation));
  }

  @Test
  public void should_get_404_when_profile_not_found() throws Exception {
    String nonExistentUsername = "nonexistent";
    when(profileQueryService.findByUsername(eq(nonExistentUsername), eq(null)))
        .thenReturn(Optional.empty());

    RestAssuredMockMvc.when()
        .get("/profiles/{username}", nonExistentUsername)
        .then()
        .statusCode(404);
  }

  @Test
  public void should_get_401_when_follow_without_authentication() throws Exception {
    given()
        .when()
        .post("/profiles/{username}/follow", anotherUser.getUsername())
        .then()
        .statusCode(401);
  }

  @Test
  public void should_get_401_when_unfollow_without_authentication() throws Exception {
    given()
        .when()
        .delete("/profiles/{username}/follow", anotherUser.getUsername())
        .then()
        .statusCode(401);
  }

  @Test
  public void should_get_404_when_follow_nonexistent_user() throws Exception {
    String nonExistentUsername = "nonexistent";
    when(userRepository.findByUsername(eq(nonExistentUsername))).thenReturn(Optional.empty());

    given()
        .header("Authorization", "Token " + token)
        .when()
        .post("/profiles/{username}/follow", nonExistentUsername)
        .then()
        .statusCode(404);
  }

  @Test
  public void should_get_404_when_unfollow_nonexistent_user() throws Exception {
    String nonExistentUsername = "nonexistent";
    when(userRepository.findByUsername(eq(nonExistentUsername))).thenReturn(Optional.empty());

    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/profiles/{username}/follow", nonExistentUsername)
        .then()
        .statusCode(404);
  }

  @Test
  public void should_get_404_when_unfollow_user_not_followed() throws Exception {
    when(userRepository.findRelation(eq(user.getId()), eq(anotherUser.getId())))
        .thenReturn(Optional.empty());

    given()
        .header("Authorization", "Token " + token)
        .when()
        .delete("/profiles/{username}/follow", anotherUser.getUsername())
        .then()
        .statusCode(404);
  }

  @Test
  public void should_get_profile_with_authentication() throws Exception {
    ProfileData profileDataWithFollowing =
        new ProfileData(
            anotherUser.getId(),
            anotherUser.getUsername(),
            anotherUser.getBio(),
            anotherUser.getImage(),
            true);

    when(profileQueryService.findByUsername(eq(profileData.getUsername()), eq(user)))
        .thenReturn(Optional.of(profileDataWithFollowing));

    given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/profiles/{username}", anotherUser.getUsername())
        .then()
        .statusCode(200)
        .body("profile.username", equalTo(anotherUser.getUsername()))
        .body("profile.following", equalTo(true));
  }

  @Test
  public void should_get_profile_without_authentication_shows_not_following() throws Exception {
    when(profileQueryService.findByUsername(eq(profileData.getUsername()), eq(null)))
        .thenReturn(Optional.of(profileData));

    RestAssuredMockMvc.when()
        .get("/profiles/{username}", profileData.getUsername())
        .then()
        .statusCode(200)
        .body("profile.username", equalTo(profileData.getUsername()))
        .body("profile.following", equalTo(false));
  }

  @Test
  public void should_get_401_when_follow_with_invalid_token() throws Exception {
    String invalidToken = "invalidtoken";
    when(jwtService.getSubFromToken(eq(invalidToken))).thenReturn(Optional.empty());

    given()
        .header("Authorization", "Token " + invalidToken)
        .when()
        .post("/profiles/{username}/follow", anotherUser.getUsername())
        .then()
        .statusCode(401);
  }

  @Test
  public void should_get_401_when_unfollow_with_invalid_token() throws Exception {
    String invalidToken = "invalidtoken";
    when(jwtService.getSubFromToken(eq(invalidToken))).thenReturn(Optional.empty());

    given()
        .header("Authorization", "Token " + invalidToken)
        .when()
        .delete("/profiles/{username}/follow", anotherUser.getUsername())
        .then()
        .statusCode(401);
  }

  @Test
  public void should_return_profile_data_with_bio_and_image() throws Exception {
    User userWithDetails = new User("detailed@test.com", "detaileduser", "123", "This is my bio", "https://example.com/avatar.jpg");
    ProfileData detailedProfile =
        new ProfileData(
            userWithDetails.getId(),
            userWithDetails.getUsername(),
            userWithDetails.getBio(),
            userWithDetails.getImage(),
            false);

    when(userRepository.findByUsername(eq(userWithDetails.getUsername())))
        .thenReturn(Optional.of(userWithDetails));
    when(profileQueryService.findByUsername(eq(userWithDetails.getUsername()), eq(null)))
        .thenReturn(Optional.of(detailedProfile));

    RestAssuredMockMvc.when()
        .get("/profiles/{username}", userWithDetails.getUsername())
        .then()
        .statusCode(200)
        .body("profile.username", equalTo(userWithDetails.getUsername()))
        .body("profile.bio", equalTo("This is my bio"))
        .body("profile.image", equalTo("https://example.com/avatar.jpg"))
        .body("profile.following", equalTo(false));
  }
}
