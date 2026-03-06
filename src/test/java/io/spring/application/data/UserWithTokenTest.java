package io.spring.application.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UserWithTokenTest {

  @Test
  public void should_create_user_with_token() {
    UserData userData = new UserData("id1", "test@example.com", "testuser", "bio", "image.jpg");
    UserWithToken userWithToken = new UserWithToken(userData, "jwt-token-123");

    assertEquals("test@example.com", userWithToken.getEmail());
    assertEquals("testuser", userWithToken.getUsername());
    assertEquals("bio", userWithToken.getBio());
    assertEquals("image.jpg", userWithToken.getImage());
    assertEquals("jwt-token-123", userWithToken.getToken());
  }

  @Test
  public void should_create_user_with_token_and_empty_fields() {
    UserData userData = new UserData("id1", "test@example.com", "testuser", "", "");
    UserWithToken userWithToken = new UserWithToken(userData, "token");

    assertEquals("", userWithToken.getBio());
    assertEquals("", userWithToken.getImage());
  }

  @Test
  public void should_create_user_with_token_preserving_all_user_data() {
    UserData userData =
        new UserData("id1", "email@test.com", "user1", "detailed bio", "https://img.com/pic.jpg");
    String token = "eyJhbGciOiJIUzI1NiJ9.test";
    UserWithToken userWithToken = new UserWithToken(userData, token);

    assertEquals(userData.getEmail(), userWithToken.getEmail());
    assertEquals(userData.getUsername(), userWithToken.getUsername());
    assertEquals(userData.getBio(), userWithToken.getBio());
    assertEquals(userData.getImage(), userWithToken.getImage());
    assertEquals(token, userWithToken.getToken());
  }
}
