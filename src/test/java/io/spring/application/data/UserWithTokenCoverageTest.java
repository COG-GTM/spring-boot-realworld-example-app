package io.spring.application.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UserWithTokenCoverageTest {

  @Test
  public void should_create_from_user_data_and_token() {
    UserData userData = new UserData("id1", "test@test.com", "user", "bio", "img");
    UserWithToken userWithToken = new UserWithToken(userData, "jwt-token");

    assertEquals("test@test.com", userWithToken.getEmail());
    assertEquals("user", userWithToken.getUsername());
    assertEquals("bio", userWithToken.getBio());
    assertEquals("img", userWithToken.getImage());
    assertEquals("jwt-token", userWithToken.getToken());
  }

  @Test
  public void should_create_with_null_fields() {
    UserData userData = new UserData("id1", null, null, null, null);
    UserWithToken userWithToken = new UserWithToken(userData, "token");

    assertNull(userWithToken.getEmail());
    assertNull(userWithToken.getUsername());
    assertNull(userWithToken.getBio());
    assertNull(userWithToken.getImage());
    assertEquals("token", userWithToken.getToken());
  }

  @Test
  public void should_create_with_all_fields_populated() {
    UserData userData =
        new UserData("id1", "user@example.com", "username", "My bio text", "http://img.com/pic.jpg");
    UserWithToken userWithToken = new UserWithToken(userData, "abc123");

    assertEquals("user@example.com", userWithToken.getEmail());
    assertEquals("username", userWithToken.getUsername());
    assertEquals("My bio text", userWithToken.getBio());
    assertEquals("http://img.com/pic.jpg", userWithToken.getImage());
    assertEquals("abc123", userWithToken.getToken());
  }
}
