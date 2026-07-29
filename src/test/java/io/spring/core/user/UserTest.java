package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_assign_all_fields_from_constructor() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    assertThat(user.getEmail(), is("john@example.com"));
    assertThat(user.getUsername(), is("john"));
    assertThat(user.getPassword(), is("123"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_generate_uuid_as_id() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    assertThat(user.getId(), is(notNullValue()));
    assertThat(UUID.fromString(user.getId()).toString(), is(user.getId()));
  }

  @Test
  public void should_generate_distinct_ids_for_different_users() {
    User user = new User("john@example.com", "john", "123", "bio", "image");
    User other = new User("jane@example.com", "jane", "123", "bio", "image");

    assertThat(user.getId(), is(not(other.getId())));
  }

  @Test
  public void should_have_null_id_when_created_with_default_constructor() {
    User user = new User();

    assertThat(user.getId(), is(nullValue()));
  }

  @Test
  public void should_update_all_fields_when_all_values_present() {
    User user = new User("john@example.com", "john", "123", "bio", "image");
    String id = user.getId();

    user.update("new@example.com", "jane", "456", "new bio", "new image");

    assertThat(user.getId(), is(id));
    assertThat(user.getEmail(), is("new@example.com"));
    assertThat(user.getUsername(), is("jane"));
    assertThat(user.getPassword(), is("456"));
    assertThat(user.getBio(), is("new bio"));
    assertThat(user.getImage(), is("new image"));
  }

  @Test
  public void should_keep_original_values_when_updating_with_null() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    user.update(null, null, null, null, null);

    assertThat(user.getEmail(), is("john@example.com"));
    assertThat(user.getUsername(), is("john"));
    assertThat(user.getPassword(), is("123"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_keep_original_values_when_updating_with_empty_string() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    user.update("", "", "", "", "");

    assertThat(user.getEmail(), is("john@example.com"));
    assertThat(user.getUsername(), is("john"));
    assertThat(user.getPassword(), is("123"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_update_only_the_provided_fields() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    user.update("new@example.com", "", null, "new bio", "");

    assertThat(user.getEmail(), is("new@example.com"));
    assertThat(user.getUsername(), is("john"));
    assertThat(user.getPassword(), is("123"));
    assertThat(user.getBio(), is("new bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_update_field_with_blank_string() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    user.update(null, null, null, " ", null);

    assertThat(user.getBio(), is(" "));
  }

  @Test
  public void should_be_equal_only_when_id_is_equal() {
    User user = new User("john@example.com", "john", "123", "bio", "image");
    User same = new User("other@example.com", "other", "456", "other", "other");
    User other = new User("john@example.com", "john", "123", "bio", "image");

    setId(same, user.getId());

    assertThat(user, is(same));
    assertThat(user.hashCode(), is(same.hashCode()));
    assertThat(user, is(not(other)));
  }

  @Test
  public void should_not_be_equal_to_null_or_other_type() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    assertThat(user.equals(null), is(false));
    assertThat(user.equals("john"), is(false));
  }

  private static void setId(User user, String id) {
    try {
      Field field = User.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(user, id);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}
