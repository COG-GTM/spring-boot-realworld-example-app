package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_create_user_with_all_fields() {
    User user = new User("test@example.com", "testuser", "password", "bio", "image.jpg");
    assertNotNull(user.getId());
    assertEquals("test@example.com", user.getEmail());
    assertEquals("testuser", user.getUsername());
    assertEquals("password", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image.jpg", user.getImage());
  }

  @Test
  public void should_generate_unique_ids() {
    User user1 = new User("a@b.com", "user1", "pass1", "", "");
    User user2 = new User("c@d.com", "user2", "pass2", "", "");
    assertNotEquals(user1.getId(), user2.getId());
  }

  @Test
  public void should_update_email() {
    User user = new User("old@email.com", "username", "pass", "bio", "img");
    user.update("new@email.com", "", "", "", "");
    assertEquals("new@email.com", user.getEmail());
    assertEquals("username", user.getUsername());
  }

  @Test
  public void should_update_username() {
    User user = new User("e@e.com", "oldname", "pass", "bio", "img");
    user.update("", "newname", "", "", "");
    assertEquals("newname", user.getUsername());
    assertEquals("e@e.com", user.getEmail());
  }

  @Test
  public void should_update_password() {
    User user = new User("e@e.com", "name", "oldpass", "bio", "img");
    user.update("", "", "newpass", "", "");
    assertEquals("newpass", user.getPassword());
  }

  @Test
  public void should_update_bio_and_image() {
    User user = new User("e@e.com", "name", "pass", "oldbio", "oldimg");
    user.update("", "", "", "newbio", "newimg");
    assertEquals("newbio", user.getBio());
    assertEquals("newimg", user.getImage());
  }

  @Test
  public void should_not_update_fields_when_empty() {
    User user = new User("e@e.com", "name", "pass", "bio", "img");
    user.update("", "", "", "", "");
    assertEquals("e@e.com", user.getEmail());
    assertEquals("name", user.getUsername());
    assertEquals("pass", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("img", user.getImage());
  }

  @Test
  public void should_not_update_fields_when_null() {
    User user = new User("e@e.com", "name", "pass", "bio", "img");
    user.update(null, null, null, null, null);
    assertEquals("e@e.com", user.getEmail());
    assertEquals("name", user.getUsername());
    assertEquals("pass", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("img", user.getImage());
  }

  @Test
  public void should_update_multiple_fields_at_once() {
    User user = new User("e@e.com", "name", "pass", "bio", "img");
    user.update("new@e.com", "newname", "newpass", "newbio", "newimg");
    assertEquals("new@e.com", user.getEmail());
    assertEquals("newname", user.getUsername());
    assertEquals("newpass", user.getPassword());
    assertEquals("newbio", user.getBio());
    assertEquals("newimg", user.getImage());
  }

  @Test
  public void should_have_equality_based_on_id() {
    User user1 = new User("e@e.com", "name", "pass", "", "");
    User user2 = new User("e@e.com", "name", "pass", "", "");
    assertNotEquals(user1, user2);
    assertEquals(user1, user1);
  }
}
