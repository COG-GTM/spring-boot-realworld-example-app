package io.spring.core.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class UserTest {

  private static void setId(User user, String id) throws Exception {
    Field field = User.class.getDeclaredField("id");
    field.setAccessible(true);
    field.set(user, id);
  }

  @Test
  public void should_set_all_fields_and_generate_uuid_id_in_constructor() {
    User user = new User("jake@jake.jake", "jake", "secret", "I work at statefarm", "image-url");

    assertEquals("jake@jake.jake", user.getEmail());
    assertEquals("jake", user.getUsername());
    assertEquals("secret", user.getPassword());
    assertEquals("I work at statefarm", user.getBio());
    assertEquals("image-url", user.getImage());
    assertNotNull(user.getId());
    assertEquals(user.getId(), UUID.fromString(user.getId()).toString());
  }

  @Test
  public void should_generate_distinct_ids_for_distinct_users() {
    User first = new User("a@a.com", "a", "p", "", "");
    User second = new User("b@b.com", "b", "p", "", "");

    assertNotEquals(first.getId(), second.getId());
  }

  @Test
  public void should_update_all_fields_when_values_are_not_empty() {
    User user = new User("old@mail.com", "old", "oldpass", "old bio", "old image");

    user.update("new@mail.com", "new", "newpass", "new bio", "new image");

    assertEquals("new@mail.com", user.getEmail());
    assertEquals("new", user.getUsername());
    assertEquals("newpass", user.getPassword());
    assertEquals("new bio", user.getBio());
    assertEquals("new image", user.getImage());
  }

  @Test
  public void should_keep_existing_values_when_update_values_are_empty_or_null() {
    User user = new User("old@mail.com", "old", "oldpass", "old bio", "old image");

    user.update("", null, "", null, "");

    assertEquals("old@mail.com", user.getEmail());
    assertEquals("old", user.getUsername());
    assertEquals("oldpass", user.getPassword());
    assertEquals("old bio", user.getBio());
    assertEquals("old image", user.getImage());
  }

  @Test
  public void should_only_update_provided_fields() {
    User user = new User("old@mail.com", "old", "oldpass", "old bio", "old image");

    user.update("new@mail.com", "", null, "new bio", "");

    assertEquals("new@mail.com", user.getEmail());
    assertEquals("old", user.getUsername());
    assertEquals("oldpass", user.getPassword());
    assertEquals("new bio", user.getBio());
    assertEquals("old image", user.getImage());
  }

  @Test
  public void should_be_equal_and_share_hashcode_when_ids_are_equal() throws Exception {
    User first = new User("a@a.com", "a", "p", "", "");
    User second = new User("b@b.com", "b", "p", "", "");
    setId(first, "same-id");
    setId(second, "same-id");

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  public void should_not_be_equal_when_ids_differ() throws Exception {
    User first = new User("a@a.com", "a", "p", "", "");
    User second = new User("a@a.com", "a", "p", "", "");
    setId(first, "id-1");
    setId(second, "id-2");

    assertNotEquals(first, second);
  }

  @Test
  public void should_not_be_equal_to_null_or_other_type() {
    User user = new User("a@a.com", "a", "p", "", "");

    assertNotEquals(user, null);
    assertNotEquals(user, "a string");
    assertTrue(user.equals(user));
  }
}
