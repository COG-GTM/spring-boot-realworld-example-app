package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  void should_generate_non_null_uuid_as_id() {
    User user = new User("email@test.com", "username", "password", "bio", "image");
    assertNotNull(user.getId());
    assertFalse(user.getId().isEmpty());
  }

  @Test
  void should_set_fields_correctly_in_constructor() {
    User user = new User("email@test.com", "username", "password", "bio", "image");
    assertEquals("email@test.com", user.getEmail());
    assertEquals("username", user.getUsername());
    assertEquals("password", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image", user.getImage());
  }

  @Test
  void should_update_only_email() {
    User user = new User("old@test.com", "username", "password", "bio", "image");
    user.update("new@test.com", "", "", "", "");
    assertEquals("new@test.com", user.getEmail());
    assertEquals("username", user.getUsername());
    assertEquals("password", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image", user.getImage());
  }

  @Test
  void should_update_all_fields() {
    User user = new User("old@test.com", "olduser", "oldpass", "oldbio", "oldimage");
    user.update("new@test.com", "newuser", "newpass", "newbio", "newimage");
    assertEquals("new@test.com", user.getEmail());
    assertEquals("newuser", user.getUsername());
    assertEquals("newpass", user.getPassword());
    assertEquals("newbio", user.getBio());
    assertEquals("newimage", user.getImage());
  }

  @Test
  void should_not_update_fields_when_empty_or_null() {
    User user = new User("email@test.com", "username", "password", "bio", "image");
    user.update("", null, "", null, "");
    assertEquals("email@test.com", user.getEmail());
    assertEquals("username", user.getUsername());
    assertEquals("password", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image", user.getImage());
  }

  @Test
  void should_be_equal_when_same_id() {
    User user1 = new User("a@test.com", "user1", "pass1", "bio1", "img1");
    User user2 = new User("b@test.com", "user2", "pass2", "bio2", "img2");
    assertNotEquals(user1, user2);

    // Use reflection to set the same id
    try {
      java.lang.reflect.Field idField = User.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(user2, user1.getId());
    } catch (Exception e) {
      fail("Reflection failed");
    }
    assertEquals(user1, user2);
    assertEquals(user1.hashCode(), user2.hashCode());
  }

  @Test
  void should_not_be_equal_when_different_id() {
    User user1 = new User("a@test.com", "user1", "pass", "bio", "img");
    User user2 = new User("a@test.com", "user1", "pass", "bio", "img");
    assertNotEquals(user1, user2);
  }
}
