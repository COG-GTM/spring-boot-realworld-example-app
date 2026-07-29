package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_set_all_fields_on_construction() {
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
  public void should_generate_different_id_for_each_user() {
    User one = new User("john@example.com", "john", "123", "bio", "image");
    User another = new User("john@example.com", "john", "123", "bio", "image");

    assertThat(one.getId(), is(not(another.getId())));
  }

  @Test
  public void should_leave_id_null_with_default_constructor() {
    User user = new User();

    assertThat(user.getId(), is((String) null));
    assertThat(user.getEmail(), is((String) null));
  }

  @Test
  public void should_update_all_fields() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    user.update("new@example.com", "newname", "newpassword", "new bio", "new image");

    assertThat(user.getEmail(), is("new@example.com"));
    assertThat(user.getUsername(), is("newname"));
    assertThat(user.getPassword(), is("newpassword"));
    assertThat(user.getBio(), is("new bio"));
    assertThat(user.getImage(), is("new image"));
  }

  @Test
  public void should_ignore_null_fields_on_update() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    user.update(null, null, null, null, null);

    assertThat(user.getEmail(), is("john@example.com"));
    assertThat(user.getUsername(), is("john"));
    assertThat(user.getPassword(), is("123"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_ignore_empty_fields_on_update() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    user.update("", "", "", "", "");

    assertThat(user.getEmail(), is("john@example.com"));
    assertThat(user.getUsername(), is("john"));
    assertThat(user.getPassword(), is("123"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_update_only_provided_fields() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    user.update("new@example.com", "", null, "new bio", "");

    assertThat(user.getEmail(), is("new@example.com"));
    assertThat(user.getUsername(), is("john"));
    assertThat(user.getPassword(), is("123"));
    assertThat(user.getBio(), is("new bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_not_keep_id_unchanged_after_update() {
    User user = new User("john@example.com", "john", "123", "bio", "image");
    String id = user.getId();

    user.update("new@example.com", "newname", "newpassword", "new bio", "new image");

    assertThat(user.getId(), is(id));
  }

  @Test
  public void should_treat_blank_string_as_non_empty_on_update() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    user.update(" ", " ", " ", " ", " ");

    assertThat(user.getEmail(), is(" "));
    assertThat(user.getUsername(), is(" "));
    assertThat(user.getPassword(), is(" "));
    assertThat(user.getBio(), is(" "));
    assertThat(user.getImage(), is(" "));
  }

  @Test
  public void should_be_equal_when_id_is_equal() {
    User user = new User("john@example.com", "john", "123", "bio", "image");
    User sameId = new User("other@example.com", "other", "456", "other bio", "other image");
    setId(sameId, user.getId());

    assertThat(user.equals(sameId), is(true));
    assertThat(user.hashCode(), is(sameId.hashCode()));
  }

  @Test
  public void should_not_be_equal_when_id_differs() {
    User user = new User("john@example.com", "john", "123", "bio", "image");
    User another = new User("john@example.com", "john", "123", "bio", "image");

    assertThat(user.equals(another), is(false));
  }

  @Test
  public void should_not_be_equal_to_null_or_other_type() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    assertThat(user.equals(null), is(false));
    assertThat(user.equals("john"), is(false));
  }

  @Test
  public void should_be_equal_to_itself() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    assertThat(user.equals(user), is(true));
    assertThat(user.hashCode(), is(user.hashCode()));
  }

  private void setId(User user, String id) {
    try {
      java.lang.reflect.Field field = User.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(user, id);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
