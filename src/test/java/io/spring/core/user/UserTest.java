package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_create_user_with_all_fields() {
    User user = new User("test@email.com", "testuser", "password123", "bio text", "http://image.url");

    assertNotNull(user.getId());
    assertEquals("test@email.com", user.getEmail());
    assertEquals("testuser", user.getUsername());
    assertEquals("password123", user.getPassword());
    assertEquals("bio text", user.getBio());
    assertEquals("http://image.url", user.getImage());
  }

  @Test
  public void should_generate_unique_id_for_each_user() {
    User user1 = new User("test1@email.com", "user1", "pass1", "", "");
    User user2 = new User("test2@email.com", "user2", "pass2", "", "");

    assertNotEquals(user1.getId(), user2.getId());
  }

  @Test
  public void should_update_email_when_not_empty() {
    User user = new User("old@email.com", "username", "password", "bio", "image");
    user.update("new@email.com", null, null, null, null);

    assertEquals("new@email.com", user.getEmail());
    assertEquals("username", user.getUsername());
  }

  @Test
  public void should_not_update_email_when_empty() {
    User user = new User("old@email.com", "username", "password", "bio", "image");
    user.update("", null, null, null, null);

    assertEquals("old@email.com", user.getEmail());
  }

  @Test
  public void should_not_update_email_when_null() {
    User user = new User("old@email.com", "username", "password", "bio", "image");
    user.update(null, null, null, null, null);

    assertEquals("old@email.com", user.getEmail());
  }

  @Test
  public void should_update_username_when_not_empty() {
    User user = new User("email@test.com", "oldname", "password", "bio", "image");
    user.update(null, "newname", null, null, null);

    assertEquals("newname", user.getUsername());
  }

  @Test
  public void should_not_update_username_when_empty() {
    User user = new User("email@test.com", "oldname", "password", "bio", "image");
    user.update(null, "", null, null, null);

    assertEquals("oldname", user.getUsername());
  }

  @Test
  public void should_update_password_when_not_empty() {
    User user = new User("email@test.com", "username", "oldpass", "bio", "image");
    user.update(null, null, "newpass", null, null);

    assertEquals("newpass", user.getPassword());
  }

  @Test
  public void should_not_update_password_when_empty() {
    User user = new User("email@test.com", "username", "oldpass", "bio", "image");
    user.update(null, null, "", null, null);

    assertEquals("oldpass", user.getPassword());
  }

  @Test
  public void should_update_bio_when_not_empty() {
    User user = new User("email@test.com", "username", "password", "old bio", "image");
    user.update(null, null, null, "new bio", null);

    assertEquals("new bio", user.getBio());
  }

  @Test
  public void should_not_update_bio_when_empty() {
    User user = new User("email@test.com", "username", "password", "old bio", "image");
    user.update(null, null, null, "", null);

    assertEquals("old bio", user.getBio());
  }

  @Test
  public void should_update_image_when_not_empty() {
    User user = new User("email@test.com", "username", "password", "bio", "old-image.jpg");
    user.update(null, null, null, null, "new-image.jpg");

    assertEquals("new-image.jpg", user.getImage());
  }

  @Test
  public void should_not_update_image_when_empty() {
    User user = new User("email@test.com", "username", "password", "bio", "old-image.jpg");
    user.update(null, null, null, null, "");

    assertEquals("old-image.jpg", user.getImage());
  }

  @Test
  public void should_update_multiple_fields_at_once() {
    User user = new User("old@email.com", "oldname", "oldpass", "old bio", "old-image.jpg");
    user.update("new@email.com", "newname", "newpass", "new bio", "new-image.jpg");

    assertEquals("new@email.com", user.getEmail());
    assertEquals("newname", user.getUsername());
    assertEquals("newpass", user.getPassword());
    assertEquals("new bio", user.getBio());
    assertEquals("new-image.jpg", user.getImage());
  }

  @Test
  public void should_have_equals_based_on_id() {
    User user1 = new User("test@email.com", "user", "pass", "", "");
    User user2 = new User("test@email.com", "user", "pass", "", "");

    assertNotEquals(user1, user2);
    assertEquals(user1, user1);
  }
}
