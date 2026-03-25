package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_create_user_with_all_fields() {
    User user = new User("test@example.com", "testuser", "password123", "bio text", "image.jpg");
    assertNotNull(user.getId());
    assertEquals("test@example.com", user.getEmail());
    assertEquals("testuser", user.getUsername());
    assertEquals("password123", user.getPassword());
    assertEquals("bio text", user.getBio());
    assertEquals("image.jpg", user.getImage());
  }

  @Test
  public void should_generate_unique_ids() {
    User user1 = new User("a@b.com", "user1", "pass", "", "");
    User user2 = new User("c@d.com", "user2", "pass", "", "");
    assertNotEquals(user1.getId(), user2.getId());
  }

  @Test
  public void should_update_email() {
    User user = new User("old@email.com", "user", "pass", "bio", "img");
    user.update("new@email.com", "", "", "", "");
    assertEquals("new@email.com", user.getEmail());
    assertEquals("user", user.getUsername());
  }

  @Test
  public void should_update_username() {
    User user = new User("a@b.com", "oldname", "pass", "bio", "img");
    user.update("", "newname", "", "", "");
    assertEquals("newname", user.getUsername());
    assertEquals("a@b.com", user.getEmail());
  }

  @Test
  public void should_update_password() {
    User user = new User("a@b.com", "user", "oldpass", "bio", "img");
    user.update("", "", "newpass", "", "");
    assertEquals("newpass", user.getPassword());
  }

  @Test
  public void should_update_bio() {
    User user = new User("a@b.com", "user", "pass", "old bio", "img");
    user.update("", "", "", "new bio", "");
    assertEquals("new bio", user.getBio());
  }

  @Test
  public void should_update_image() {
    User user = new User("a@b.com", "user", "pass", "bio", "old.jpg");
    user.update("", "", "", "", "new.jpg");
    assertEquals("new.jpg", user.getImage());
  }

  @Test
  public void should_not_update_with_null_values() {
    User user = new User("a@b.com", "user", "pass", "bio", "img");
    user.update(null, null, null, null, null);
    assertEquals("a@b.com", user.getEmail());
    assertEquals("user", user.getUsername());
    assertEquals("pass", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("img", user.getImage());
  }

  @Test
  public void should_not_update_with_empty_values() {
    User user = new User("a@b.com", "user", "pass", "bio", "img");
    user.update("", "", "", "", "");
    assertEquals("a@b.com", user.getEmail());
    assertEquals("user", user.getUsername());
    assertEquals("pass", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("img", user.getImage());
  }

  @Test
  public void should_update_multiple_fields_at_once() {
    User user = new User("old@email.com", "olduser", "oldpass", "old bio", "old.jpg");
    user.update("new@email.com", "newuser", "newpass", "new bio", "new.jpg");
    assertEquals("new@email.com", user.getEmail());
    assertEquals("newuser", user.getUsername());
    assertEquals("newpass", user.getPassword());
    assertEquals("new bio", user.getBio());
    assertEquals("new.jpg", user.getImage());
  }

  @Test
  public void should_equal_by_id() {
    User user1 = new User("a@b.com", "user1", "pass", "", "");
    User user2 = new User("c@d.com", "user2", "pass", "", "");
    assertNotEquals(user1, user2);
    assertEquals(user1, user1);
  }

  @Test
  public void should_have_consistent_hashcode() {
    User user = new User("a@b.com", "user", "pass", "", "");
    int hash1 = user.hashCode();
    int hash2 = user.hashCode();
    assertEquals(hash1, hash2);
  }
}
