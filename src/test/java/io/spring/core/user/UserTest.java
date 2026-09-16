package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserTest {

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("john@jacob.com", "johnjacob", "secret", "bio", "image.png");
  }

  @Test
  public void should_generate_id_on_creation() {
    assertNotNull(user.getId());
    assertNotEquals(user.getId(), new User("a@b.com", "ab", "p", "", "").getId());
  }

  @Test
  public void should_update_all_provided_fields() {
    user.update("new@email.com", "newname", "newpass", "new bio", "new.png");
    assertEquals("new@email.com", user.getEmail());
    assertEquals("newname", user.getUsername());
    assertEquals("newpass", user.getPassword());
    assertEquals("new bio", user.getBio());
    assertEquals("new.png", user.getImage());
  }

  @Test
  public void should_keep_existing_values_for_empty_fields() {
    user.update("", "", "", "", "");
    assertEquals("john@jacob.com", user.getEmail());
    assertEquals("johnjacob", user.getUsername());
    assertEquals("secret", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image.png", user.getImage());
  }

  @Test
  public void should_keep_existing_values_for_null_fields() {
    user.update(null, null, null, null, null);
    assertEquals("john@jacob.com", user.getEmail());
    assertEquals("johnjacob", user.getUsername());
    assertEquals("secret", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image.png", user.getImage());
  }

  @Test
  public void should_update_only_some_fields() {
    user.update(null, "renamed", "", "", null);
    assertEquals("john@jacob.com", user.getEmail());
    assertEquals("renamed", user.getUsername());
    assertEquals("secret", user.getPassword());
  }

  @Test
  public void should_compare_users_by_id_only() {
    User other = new User("other@email.com", "other", "x", "", "");
    assertNotEquals(user, other);

    User same = new User("a@b.com", "ab", "p", "", "");
    same.update("john@jacob.com", "johnjacob", "secret", "bio", "image.png");
    assertNotEquals(user, same);
    assertEquals(user, user);
    assertEquals(user.hashCode(), user.hashCode());
  }
}
