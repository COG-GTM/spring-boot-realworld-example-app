package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  void should_create_user_with_all_fields() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.png");

    assertNotNull(user.getId());
    assertEquals("test@example.com", user.getEmail());
    assertEquals("testuser", user.getUsername());
    assertEquals("password", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image.png", user.getImage());
  }

  @Test
  void should_update_only_email() {
    User user = new User("old@example.com", "user1", "pass", "bio", "img.png");

    user.update("new@example.com", "", "", "", "");

    assertEquals("new@example.com", user.getEmail());
    assertEquals("user1", user.getUsername());
    assertEquals("pass", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("img.png", user.getImage());
  }

  @Test
  void should_update_only_username() {
    User user = new User("a@b.com", "olduser", "pass", "bio", "img.png");

    user.update("", "newuser", "", "", "");

    assertEquals("a@b.com", user.getEmail());
    assertEquals("newuser", user.getUsername());
    assertEquals("pass", user.getPassword());
  }

  @Test
  void should_update_only_password() {
    User user = new User("a@b.com", "user1", "oldpass", "bio", "img.png");

    user.update("", "", "newpass", "", "");

    assertEquals("a@b.com", user.getEmail());
    assertEquals("user1", user.getUsername());
    assertEquals("newpass", user.getPassword());
  }

  @Test
  void should_update_only_bio() {
    User user = new User("a@b.com", "user1", "pass", "old bio", "img.png");

    user.update("", "", "", "new bio", "");

    assertEquals("new bio", user.getBio());
    assertEquals("img.png", user.getImage());
  }

  @Test
  void should_update_only_image() {
    User user = new User("a@b.com", "user1", "pass", "bio", "old.png");

    user.update("", "", "", "", "new.png");

    assertEquals("bio", user.getBio());
    assertEquals("new.png", user.getImage());
  }

  @Test
  void should_not_change_existing_values_when_update_with_empty_strings() {
    User user = new User("a@b.com", "user1", "pass", "bio", "img.png");

    user.update("", "", "", "", "");

    assertEquals("a@b.com", user.getEmail());
    assertEquals("user1", user.getUsername());
    assertEquals("pass", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("img.png", user.getImage());
  }

  @Test
  void should_not_change_existing_values_when_update_with_null_values() {
    User user = new User("a@b.com", "user1", "pass", "bio", "img.png");

    user.update(null, null, null, null, null);

    assertEquals("a@b.com", user.getEmail());
    assertEquals("user1", user.getUsername());
    assertEquals("pass", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("img.png", user.getImage());
  }

  @Test
  void should_update_multiple_fields_at_once() {
    User user = new User("a@b.com", "user1", "pass", "bio", "img.png");

    user.update("new@b.com", "user2", "newpass", "", "");

    assertEquals("new@b.com", user.getEmail());
    assertEquals("user2", user.getUsername());
    assertEquals("newpass", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("img.png", user.getImage());
  }

  @Test
  void should_generate_uuid_id() {
    User user = new User("a@b.com", "user1", "pass", "bio", "img.png");

    assertNotNull(user.getId());
    assertFalse(user.getId().isEmpty());
  }
}
