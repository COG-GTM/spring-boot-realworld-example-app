package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_create_user_with_all_fields() {
    User user = new User("test@example.com", "testuser", "password123", "my bio", "image.jpg");

    assertNotNull(user.getId());
    assertEquals("test@example.com", user.getEmail());
    assertEquals("testuser", user.getUsername());
    assertEquals("password123", user.getPassword());
    assertEquals("my bio", user.getBio());
    assertEquals("image.jpg", user.getImage());
  }

  @Test
  public void should_create_user_with_empty_bio_and_image() {
    User user = new User("test@example.com", "testuser", "password123", "", "");

    assertEquals("", user.getBio());
    assertEquals("", user.getImage());
  }

  @Test
  public void should_generate_unique_ids() {
    User user1 = new User("test1@example.com", "user1", "pass1", "", "");
    User user2 = new User("test2@example.com", "user2", "pass2", "", "");

    assertNotEquals(user1.getId(), user2.getId());
  }

  @Test
  public void should_update_email() {
    User user = new User("old@example.com", "testuser", "password", "bio", "image.jpg");
    user.update("new@example.com", "", "", "", "");

    assertEquals("new@example.com", user.getEmail());
    assertEquals("testuser", user.getUsername());
    assertEquals("password", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image.jpg", user.getImage());
  }

  @Test
  public void should_update_username() {
    User user = new User("test@example.com", "olduser", "password", "bio", "image.jpg");
    user.update("", "newuser", "", "", "");

    assertEquals("test@example.com", user.getEmail());
    assertEquals("newuser", user.getUsername());
  }

  @Test
  public void should_update_password() {
    User user = new User("test@example.com", "testuser", "oldpass", "bio", "image.jpg");
    user.update("", "", "newpass", "", "");

    assertEquals("newpass", user.getPassword());
  }

  @Test
  public void should_update_bio() {
    User user = new User("test@example.com", "testuser", "password", "old bio", "image.jpg");
    user.update("", "", "", "new bio", "");

    assertEquals("new bio", user.getBio());
  }

  @Test
  public void should_update_image() {
    User user = new User("test@example.com", "testuser", "password", "bio", "old.jpg");
    user.update("", "", "", "", "new.jpg");

    assertEquals("new.jpg", user.getImage());
  }

  @Test
  public void should_update_all_fields() {
    User user = new User("old@example.com", "olduser", "oldpass", "old bio", "old.jpg");
    user.update("new@example.com", "newuser", "newpass", "new bio", "new.jpg");

    assertEquals("new@example.com", user.getEmail());
    assertEquals("newuser", user.getUsername());
    assertEquals("newpass", user.getPassword());
    assertEquals("new bio", user.getBio());
    assertEquals("new.jpg", user.getImage());
  }

  @Test
  public void should_not_update_with_null_values() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    user.update(null, null, null, null, null);

    assertEquals("test@example.com", user.getEmail());
    assertEquals("testuser", user.getUsername());
    assertEquals("password", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image.jpg", user.getImage());
  }

  @Test
  public void should_not_update_with_empty_values() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    user.update("", "", "", "", "");

    assertEquals("test@example.com", user.getEmail());
    assertEquals("testuser", user.getUsername());
    assertEquals("password", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image.jpg", user.getImage());
  }

  @Test
  public void should_have_equal_users_with_same_id() {
    User user1 = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    assertEquals(user1, user1);
  }

  @Test
  public void should_have_unequal_users_with_different_ids() {
    User user1 = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    User user2 = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    assertNotEquals(user1, user2);
  }

  @Test
  public void should_not_equal_null() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    assertNotEquals(null, user);
  }

  @Test
  public void should_have_consistent_hashcode() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    int hashCode1 = user.hashCode();
    int hashCode2 = user.hashCode();
    assertEquals(hashCode1, hashCode2);
  }

  @Test
  public void should_have_same_hashcode_for_equal_objects() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    assertEquals(user.hashCode(), user.hashCode());
  }
}
