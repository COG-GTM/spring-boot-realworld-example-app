package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_set_fields_and_generate_uuid_id() {
    User user = new User("a@b.com", "alice", "secret", "bio", "image");

    assertNotNull(user.getId());
    UUID parsed = UUID.fromString(user.getId());
    assertEquals(user.getId(), parsed.toString());
    assertEquals("a@b.com", user.getEmail());
    assertEquals("alice", user.getUsername());
    assertEquals("secret", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image", user.getImage());
  }

  @Test
  public void should_generate_unique_ids() {
    User u1 = new User("a@b.com", "alice", "secret", "bio", "image");
    User u2 = new User("a@b.com", "alice", "secret", "bio", "image");
    assertTrue(!u1.getId().equals(u2.getId()));
  }

  @Test
  public void update_should_change_only_email_when_others_are_null() {
    User user = new User("a@b.com", "alice", "secret", "bio", "image");
    user.update("new@b.com", null, null, null, null);

    assertEquals("new@b.com", user.getEmail());
    assertEquals("alice", user.getUsername());
    assertEquals("secret", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image", user.getImage());
  }

  @Test
  public void update_should_ignore_empty_strings() {
    User user = new User("a@b.com", "alice", "secret", "bio", "image");
    user.update("", "", "", "", "");

    assertEquals("a@b.com", user.getEmail());
    assertEquals("alice", user.getUsername());
    assertEquals("secret", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image", user.getImage());
  }

  @Test
  public void update_should_apply_all_non_empty_fields() {
    User user = new User("a@b.com", "alice", "secret", "bio", "image");
    user.update("new@b.com", "alice2", "secret2", "bio2", "image2");

    assertEquals("new@b.com", user.getEmail());
    assertEquals("alice2", user.getUsername());
    assertEquals("secret2", user.getPassword());
    assertEquals("bio2", user.getBio());
    assertEquals("image2", user.getImage());
  }
}
