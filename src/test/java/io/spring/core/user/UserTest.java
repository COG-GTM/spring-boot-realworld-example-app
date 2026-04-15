package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  void should_create_user_with_all_fields() {
    User user = new User("test@example.com", "testuser", "password123", "bio text", "image.jpg");
    assertNotNull(user.getId());
    assertEquals("test@example.com", user.getEmail());
    assertEquals("testuser", user.getUsername());
    assertEquals("password123", user.getPassword());
    assertEquals("bio text", user.getBio());
    assertEquals("image.jpg", user.getImage());
  }

  @Test
  void should_generate_unique_ids() {
    User user1 = new User("a@example.com", "user1", "pass1", "", "");
    User user2 = new User("b@example.com", "user2", "pass2", "", "");
    assertNotEquals(user1.getId(), user2.getId());
  }

  @Test
  void should_update_email_when_not_empty() {
    User user = new User("old@example.com", "user", "pass", "bio", "img");
    user.update("new@example.com", null, null, null, null);
    assertEquals("new@example.com", user.getEmail());
    assertEquals("user", user.getUsername());
  }

  @Test
  void should_update_username_when_not_empty() {
    User user = new User("email@example.com", "oldname", "pass", "bio", "img");
    user.update(null, "newname", null, null, null);
    assertEquals("newname", user.getUsername());
  }

  @Test
  void should_update_password_when_not_empty() {
    User user = new User("email@example.com", "user", "oldpass", "bio", "img");
    user.update(null, null, "newpass", null, null);
    assertEquals("newpass", user.getPassword());
  }

  @Test
  void should_update_bio_when_not_empty() {
    User user = new User("email@example.com", "user", "pass", "old bio", "img");
    user.update(null, null, null, "new bio", null);
    assertEquals("new bio", user.getBio());
  }

  @Test
  void should_update_image_when_not_empty() {
    User user = new User("email@example.com", "user", "pass", "bio", "old.jpg");
    user.update(null, null, null, null, "new.jpg");
    assertEquals("new.jpg", user.getImage());
  }

  @Test
  void should_update_all_fields_at_once() {
    User user = new User("old@example.com", "olduser", "oldpass", "old bio", "old.jpg");
    user.update("new@example.com", "newuser", "newpass", "new bio", "new.jpg");
    assertEquals("new@example.com", user.getEmail());
    assertEquals("newuser", user.getUsername());
    assertEquals("newpass", user.getPassword());
    assertEquals("new bio", user.getBio());
    assertEquals("new.jpg", user.getImage());
  }

  @Test
  void should_not_update_fields_when_null() {
    User user = new User("email@example.com", "user", "pass", "bio", "img");
    user.update(null, null, null, null, null);
    assertEquals("email@example.com", user.getEmail());
    assertEquals("user", user.getUsername());
    assertEquals("pass", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("img", user.getImage());
  }

  @Test
  void should_not_update_fields_when_empty_string() {
    User user = new User("email@example.com", "user", "pass", "bio", "img");
    user.update("", "", "", "", "");
    assertEquals("email@example.com", user.getEmail());
    assertEquals("user", user.getUsername());
    assertEquals("pass", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("img", user.getImage());
  }

  @Test
  void should_have_equality_based_on_id() {
    User user1 = new User("a@example.com", "user1", "pass1", "", "");
    User user2 = new User("b@example.com", "user2", "pass2", "", "");
    assertNotEquals(user1, user2);
    assertEquals(user1, user1);
  }
}
