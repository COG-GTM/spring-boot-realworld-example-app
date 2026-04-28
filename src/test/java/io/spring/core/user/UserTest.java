package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class UserTest {

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
  public void should_update_email_only() {
    User user = new User("old@test.com", "username", "pass", "bio", "img");
    user.update("new@test.com", "", "", "", "");
    assertEquals("new@test.com", user.getEmail());
    assertEquals("username", user.getUsername());
    assertEquals("pass", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("img", user.getImage());
  }

  @Test
  public void should_update_username_only() {
    User user = new User("test@test.com", "old", "pass", "bio", "img");
    user.update("", "newname", "", "", "");
    assertEquals("test@test.com", user.getEmail());
    assertEquals("newname", user.getUsername());
  }

  @Test
  public void should_update_password_only() {
    User user = new User("test@test.com", "username", "oldpass", "bio", "img");
    user.update("", "", "newpass", "", "");
    assertEquals("newpass", user.getPassword());
  }

  @Test
  public void should_update_bio_only() {
    User user = new User("test@test.com", "username", "pass", "oldbio", "img");
    user.update("", "", "", "newbio", "");
    assertEquals("newbio", user.getBio());
  }

  @Test
  public void should_update_image_only() {
    User user = new User("test@test.com", "username", "pass", "bio", "oldimg");
    user.update("", "", "", "", "newimg");
    assertEquals("newimg", user.getImage());
  }

  @Test
  public void should_update_all_fields_at_once() {
    User user = new User("old@test.com", "old", "oldpass", "oldbio", "oldimg");
    user.update("new@test.com", "newuser", "newpass", "newbio", "newimg");
    assertEquals("new@test.com", user.getEmail());
    assertEquals("newuser", user.getUsername());
    assertEquals("newpass", user.getPassword());
    assertEquals("newbio", user.getBio());
    assertEquals("newimg", user.getImage());
  }

  @Test
  public void should_not_update_fields_when_null() {
    User user = new User("test@test.com", "username", "pass", "bio", "img");
    user.update(null, null, null, null, null);
    assertEquals("test@test.com", user.getEmail());
    assertEquals("username", user.getUsername());
    assertEquals("pass", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("img", user.getImage());
  }

  @Test
  public void should_generate_unique_ids() {
    User user1 = new User("a@test.com", "user1", "pass", "", "");
    User user2 = new User("b@test.com", "user2", "pass", "", "");
    assertNotNull(user1.getId());
    assertNotNull(user2.getId());
    assertEquals(false, user1.getId().equals(user2.getId()));
  }
}
