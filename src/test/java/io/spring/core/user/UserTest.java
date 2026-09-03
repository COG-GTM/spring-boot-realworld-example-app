package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_assign_id_and_fields() {
    User user = new User("email", "username", "password", "bio", "image");

    assertNotNull(user.getId());
    assertEquals("email", user.getEmail());
    assertEquals("username", user.getUsername());
    assertEquals("password", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image", user.getImage());
  }

  @Test
  public void should_keep_fields_for_empty_update_values() {
    User user = new User("email", "username", "password", "bio", "image");

    user.update(null, null, null, null, null);
    user.update("", "", "", "", "");

    assertEquals("email", user.getEmail());
    assertEquals("username", user.getUsername());
    assertEquals("password", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image", user.getImage());
  }

  @Test
  public void should_update_all_fields() {
    User user = new User("email", "username", "password", "bio", "image");

    user.update("new email", "new username", "new password", "new bio", "new image");

    assertEquals("new email", user.getEmail());
    assertEquals("new username", user.getUsername());
    assertEquals("new password", user.getPassword());
    assertEquals("new bio", user.getBio());
    assertEquals("new image", user.getImage());
  }

  @Test
  public void should_use_id_for_equality() {
    User first = new User("email", "username", "password", "bio", "image");
    User second = new User("email", "username", "password", "bio", "image");

    assertNotEquals(first, second);
    assertEquals(first, first);
  }
}
