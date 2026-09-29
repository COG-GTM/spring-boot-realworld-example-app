package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_update_all_non_empty_fields() {
    User user = new User("a@b.com", "alice", "pass", "bio", "img");

    user.update("new@b.com", "bob", "newpass", "new bio", "new img");

    assertEquals("new@b.com", user.getEmail());
    assertEquals("bob", user.getUsername());
    assertEquals("newpass", user.getPassword());
    assertEquals("new bio", user.getBio());
    assertEquals("new img", user.getImage());
  }

  @Test
  public void should_ignore_null_and_empty_fields() {
    User user = new User("a@b.com", "alice", "pass", "bio", "img");

    user.update(null, "", null, "", null);

    assertEquals("a@b.com", user.getEmail());
    assertEquals("alice", user.getUsername());
    assertEquals("pass", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("img", user.getImage());
  }

  @Test
  public void should_compare_users_by_id() {
    User user = new User("a@b.com", "alice", "pass", "", "");
    User sameData = new User("a@b.com", "alice", "pass", "", "");

    assertEquals(user, user);
    assertNotEquals(user, sameData);
  }
}
