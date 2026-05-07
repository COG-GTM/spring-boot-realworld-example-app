package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_create_user_with_all_fields() {
    User user = new User("test@test.com", "testuser", "password", "bio", "image");
    assertEquals("test@test.com", user.getEmail());
    assertEquals("testuser", user.getUsername());
    assertEquals("password", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image", user.getImage());
    assertNotNull(user.getId());
  }

  @Test
  public void should_update_email() {
    User user = new User("old@test.com", "user", "pass", "bio", "img");
    user.update("new@test.com", null, null, null, null);
    assertEquals("new@test.com", user.getEmail());
    assertEquals("user", user.getUsername());
  }

  @Test
  public void should_update_username() {
    User user = new User("test@test.com", "oldname", "pass", "bio", "img");
    user.update(null, "newname", null, null, null);
    assertEquals("newname", user.getUsername());
  }

  @Test
  public void should_update_password() {
    User user = new User("test@test.com", "user", "oldpass", "bio", "img");
    user.update(null, null, "newpass", null, null);
    assertEquals("newpass", user.getPassword());
  }

  @Test
  public void should_update_bio() {
    User user = new User("test@test.com", "user", "pass", "oldbio", "img");
    user.update(null, null, null, "newbio", null);
    assertEquals("newbio", user.getBio());
  }

  @Test
  public void should_update_image() {
    User user = new User("test@test.com", "user", "pass", "bio", "oldimg");
    user.update(null, null, null, null, "newimg");
    assertEquals("newimg", user.getImage());
  }

  @Test
  public void should_not_update_with_empty_values() {
    User user = new User("test@test.com", "user", "pass", "bio", "img");
    user.update("", "", "", "", "");
    assertEquals("test@test.com", user.getEmail());
    assertEquals("user", user.getUsername());
    assertEquals("pass", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("img", user.getImage());
  }

  @Test
  public void should_update_all_fields() {
    User user = new User("old@test.com", "olduser", "oldpass", "oldbio", "oldimg");
    user.update("new@test.com", "newuser", "newpass", "newbio", "newimg");
    assertEquals("new@test.com", user.getEmail());
    assertEquals("newuser", user.getUsername());
    assertEquals("newpass", user.getPassword());
    assertEquals("newbio", user.getBio());
    assertEquals("newimg", user.getImage());
  }

  @Test
  public void should_have_equals_based_on_id() {
    User user1 = new User("a@test.com", "user1", "pass", "", "");
    User user2 = new User("b@test.com", "user2", "pass", "", "");
    assertNotEquals(user1, user2);
    assertEquals(user1, user1);
  }

  @Test
  public void should_have_consistent_hashcode() {
    User user = new User("test@test.com", "user", "pass", "", "");
    assertEquals(user.hashCode(), user.hashCode());
  }
}
