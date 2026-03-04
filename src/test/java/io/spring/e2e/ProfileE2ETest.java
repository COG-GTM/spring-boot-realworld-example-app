package io.spring.e2e;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

public class ProfileE2ETest extends BaseE2ETest {

  @Test
  void should_get_profile_by_username() {
    registerAndGetToken("profileuser@test.com", "profileuser", "password123");

    ResponseEntity<String> response = get("/profiles/profileuser");

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    JsonNode profile = body.get("profile");
    assertEquals("profileuser", profile.get("username").asText());
    assertFalse(profile.get("following").asBoolean());
  }

  @Test
  void should_follow_user() {
    registerAndGetToken("followee@test.com", "followeeuser", "password123");
    String followerToken = registerAndGetToken("follower@test.com", "followeruser", "password123");

    ResponseEntity<String> response = post("/profiles/followeeuser/follow", null, followerToken);

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    assertTrue(body.get("profile").get("following").asBoolean());
    assertEquals("followeeuser", body.get("profile").get("username").asText());
  }

  @Test
  void should_unfollow_user() {
    registerAndGetToken("unfollowee@test.com", "unfolloweeuser", "password123");
    String followerToken =
        registerAndGetToken("unfollower@test.com", "unfolloweruser", "password123");

    // First follow
    post("/profiles/unfolloweeuser/follow", null, followerToken);

    // Then unfollow
    ResponseEntity<String> response = delete("/profiles/unfolloweeuser/follow", followerToken);

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    assertFalse(body.get("profile").get("following").asBoolean());
  }

  @Test
  void should_show_following_true_after_follow() {
    registerAndGetToken("showfollowee@test.com", "showfollowee", "password123");
    String followerToken =
        registerAndGetToken("showfollower@test.com", "showfollower", "password123");

    post("/profiles/showfollowee/follow", null, followerToken);

    ResponseEntity<String> response = get("/profiles/showfollowee", followerToken);

    assertEquals(HttpStatus.OK.value(), response.getStatusCodeValue());
    JsonNode body = parseJson(response.getBody());
    assertTrue(body.get("profile").get("following").asBoolean());
  }

  @Test
  void should_return_404_for_nonexistent_profile() {
    HttpClientErrorException ex =
        expectClientError(HttpMethod.GET, "/profiles/nonexistentuser12345", null, null);

    assertEquals(HttpStatus.NOT_FOUND.value(), ex.getRawStatusCode());
  }

  @Test
  void should_fail_follow_without_auth() {
    registerAndGetToken("noauthfollowee@test.com", "noauthfollowee", "password123");

    HttpClientErrorException ex =
        expectClientError(HttpMethod.POST, "/profiles/noauthfollowee/follow", null, null);

    assertEquals(HttpStatus.UNAUTHORIZED.value(), ex.getRawStatusCode());
  }

  @Test
  void should_get_profile_via_selenium_driver() {
    registerAndGetToken("seleniumprofile@test.com", "seleniumprofile", "password123");

    String pageSource = navigateAndGetSource("/profiles/seleniumprofile");
    assertNotNull(pageSource);
    assertTrue(pageSource.contains("seleniumprofile"));
  }
}
