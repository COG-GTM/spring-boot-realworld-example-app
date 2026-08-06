package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  void should_update_all_fields_when_provided() {
    User user = new User("old@example.com", "old", "oldpass", "old bio", "old-image");

    user.update("new@example.com", "newname", "newpass", "new bio", "new-image");

    assertEquals("new@example.com", user.getEmail());
    assertEquals("newname", user.getUsername());
    assertEquals("newpass", user.getPassword());
    assertEquals("new bio", user.getBio());
    assertEquals("new-image", user.getImage());
  }

  @Test
  void should_keep_existing_fields_when_update_values_are_empty() {
    User user = new User("old@example.com", "old", "oldpass", "old bio", "old-image");

    user.update("", "", "", "", "");

    assertEquals("old@example.com", user.getEmail());
    assertEquals("old", user.getUsername());
    assertEquals("oldpass", user.getPassword());
    assertEquals("old bio", user.getBio());
    assertEquals("old-image", user.getImage());
  }

  @Test
  void should_update_only_provided_fields() {
    User user = new User("old@example.com", "old", "oldpass", "old bio", "old-image");

    user.update("", "newname", "", "", "");

    assertEquals("old@example.com", user.getEmail());
    assertEquals("newname", user.getUsername());
    assertEquals("oldpass", user.getPassword());
  }
}
