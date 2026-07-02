package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserTest {

  private User user;

  @BeforeEach
  public void setUp() {
    user = new User("jane@example.com", "jane", "secret", "bio", "image.png");
  }

  @Test
  public void should_expose_constructor_values() {
    assertNotNull(user.getId());
    assertEquals("jane@example.com", user.getEmail());
    assertEquals("jane", user.getUsername());
    assertEquals("secret", user.getPassword());
    assertEquals("bio", user.getBio());
    assertEquals("image.png", user.getImage());
  }

  @Test
  public void should_update_only_non_empty_fields() {
    user.update("new@example.com", "", null, "new bio", "");

    assertEquals("new@example.com", user.getEmail());
    assertEquals("jane", user.getUsername());
    assertEquals("secret", user.getPassword());
    assertEquals("new bio", user.getBio());
    assertEquals("image.png", user.getImage());
  }

  @Test
  public void should_update_all_fields_when_present() {
    user.update("a@b.c", "amy", "pw2", "b2", "img2");

    assertEquals("a@b.c", user.getEmail());
    assertEquals("amy", user.getUsername());
    assertEquals("pw2", user.getPassword());
    assertEquals("b2", user.getBio());
    assertEquals("img2", user.getImage());
  }

  @Test
  public void equality_should_be_based_on_id() {
    User other = new User("jane@example.com", "jane", "secret", "bio", "image.png");
    assertNotEquals(user, other);
    assertEquals(user, user);
  }

  @Test
  public void follow_relation_should_hold_ids() {
    FollowRelation relation = new FollowRelation("u1", "u2");
    assertEquals("u1", relation.getUserId());
    assertEquals("u2", relation.getTargetId());
  }
}
