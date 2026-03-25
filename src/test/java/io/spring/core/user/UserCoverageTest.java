package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UserCoverageTest {

  @Test
  public void should_create_user_with_all_fields() {
    User user = new User("test@test.com", "testuser", "password", "bio", "image.jpg");

    assertNotNull(user.getId());
    assertEquals("test@test.com", user.getEmail());
    assertEquals("testuser", user.getUsername());
    assertEquals("password", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image.jpg", user.getImage());
  }

  @Test
  public void should_update_email() {
    User user = new User("old@test.com", "user", "pass", "bio", "img");
    user.update("new@test.com", "", "", "", "");
    assertEquals("new@test.com", user.getEmail());
  }

  @Test
  public void should_update_username() {
    User user = new User("test@test.com", "olduser", "pass", "bio", "img");
    user.update("", "newuser", "", "", "");
    assertEquals("newuser", user.getUsername());
  }

  @Test
  public void should_update_password() {
    User user = new User("test@test.com", "user", "oldpass", "bio", "img");
    user.update("", "", "newpass", "", "");
    assertEquals("newpass", user.getPassword());
  }

  @Test
  public void should_update_bio() {
    User user = new User("test@test.com", "user", "pass", "oldbio", "img");
    user.update("", "", "", "newbio", "");
    assertEquals("newbio", user.getBio());
  }

  @Test
  public void should_update_image() {
    User user = new User("test@test.com", "user", "pass", "bio", "oldimg");
    user.update("", "", "", "", "newimg");
    assertEquals("newimg", user.getImage());
  }

  @Test
  public void should_not_update_with_null_values() {
    User user = new User("test@test.com", "user", "pass", "bio", "img");
    user.update(null, null, null, null, null);
    assertEquals("test@test.com", user.getEmail());
    assertEquals("user", user.getUsername());
    assertEquals("pass", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("img", user.getImage());
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
  public void should_have_no_arg_constructor() {
    User user = new User();
    assertNull(user.getId());
    assertNull(user.getEmail());
  }
}
