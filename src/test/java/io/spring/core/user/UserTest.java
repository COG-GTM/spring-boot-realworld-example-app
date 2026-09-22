package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_generate_id_on_creation() {
    User user = new User("email@test.com", "username", "123", "bio", "image");
    assertNotNull(user.getId());
  }

  @Test
  public void should_update_all_provided_fields() {
    User user = new User("email@test.com", "username", "123", "bio", "image");
    user.update("new@test.com", "newname", "456", "new bio", "new image");

    assertEquals("new@test.com", user.getEmail());
    assertEquals("newname", user.getUsername());
    assertEquals("456", user.getPassword());
    assertEquals("new bio", user.getBio());
    assertEquals("new image", user.getImage());
  }

  @Test
  public void should_keep_original_values_when_update_params_are_empty() {
    User user = new User("email@test.com", "username", "123", "bio", "image");
    user.update("", null, "", null, "");

    assertEquals("email@test.com", user.getEmail());
    assertEquals("username", user.getUsername());
    assertEquals("123", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image", user.getImage());
  }

  @Test
  public void should_compare_users_by_id() {
    User user = new User("email@test.com", "username", "123", "bio", "image");
    User sameIdentity = new User("email@test.com", "username", "123", "bio", "image");

    assertEquals(user, user);
    assertNotEquals(user, sameIdentity);
  }
}
