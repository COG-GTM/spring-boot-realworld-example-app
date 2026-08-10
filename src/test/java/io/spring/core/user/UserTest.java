package io.spring.core.user;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserTest {

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("email@example.com", "username", "password", "bio", "image");
  }

  @Test
  public void should_update_all_fields() {
    user.update("new@example.com", "newname", "newpass", "newbio", "newimage");
    Assertions.assertEquals("new@example.com", user.getEmail());
    Assertions.assertEquals("newname", user.getUsername());
    Assertions.assertEquals("newpass", user.getPassword());
    Assertions.assertEquals("newbio", user.getBio());
    Assertions.assertEquals("newimage", user.getImage());
  }

  @Test
  public void should_keep_fields_when_update_values_are_empty() {
    user.update("", "", "", "", "");
    Assertions.assertEquals("email@example.com", user.getEmail());
    Assertions.assertEquals("username", user.getUsername());
    Assertions.assertEquals("password", user.getPassword());
    Assertions.assertEquals("bio", user.getBio());
    Assertions.assertEquals("image", user.getImage());
  }

  @Test
  public void users_with_different_ids_are_not_equal() {
    User another = new User("email@example.com", "username", "password", "bio", "image");
    Assertions.assertNotEquals(user, another);
  }
}
