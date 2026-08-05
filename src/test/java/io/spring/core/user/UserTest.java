package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_set_all_fields_from_constructor() {
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
    User user = new User("john@example.com", "john", "123", "bio", "image");
    User other = new User("john@example.com", "john", "123", "bio", "image");

    assertThat(user.getId(), is(not(other.getId())));
  }

  @Test
  public void should_keep_empty_image_as_is() {
    User user = new User("john@example.com", "john", "123", "bio", "");

    assertThat(user.getImage(), is(""));
  }

  @Test
  public void should_have_null_fields_with_default_constructor() {
    User user = new User();

    assertThat(user.getId(), is(nullValue()));
    assertThat(user.getEmail(), is(nullValue()));
    assertThat(user.getUsername(), is(nullValue()));
    assertThat(user.getPassword(), is(nullValue()));
    assertThat(user.getBio(), is(nullValue()));
    assertThat(user.getImage(), is(nullValue()));
  }

  @Test
  public void should_update_all_fields_when_all_values_are_present() {
    User user = new User("john@example.com", "john", "123", "bio", "image");
    String id = user.getId();

    user.update("new@example.com", "newname", "newpassword", "new bio", "new image");

    assertThat(user.getId(), is(id));
    assertThat(user.getEmail(), is("new@example.com"));
    assertThat(user.getUsername(), is("newname"));
    assertThat(user.getPassword(), is("newpassword"));
    assertThat(user.getBio(), is("new bio"));
    assertThat(user.getImage(), is("new image"));
  }

  @Test
  public void should_not_update_any_field_when_all_values_are_null() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    user.update(null, null, null, null, null);

    assertThat(user.getEmail(), is("john@example.com"));
    assertThat(user.getUsername(), is("john"));
    assertThat(user.getPassword(), is("123"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_not_update_any_field_when_all_values_are_empty() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    user.update("", "", "", "", "");

    assertThat(user.getEmail(), is("john@example.com"));
    assertThat(user.getUsername(), is("john"));
    assertThat(user.getPassword(), is("123"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_only_update_provided_fields() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    user.update("new@example.com", "", null, "new bio", "");

    assertThat(user.getEmail(), is("new@example.com"));
    assertThat(user.getUsername(), is("john"));
    assertThat(user.getPassword(), is("123"));
    assertThat(user.getBio(), is("new bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_update_field_with_blank_space_value() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    user.update(" ", " ", " ", " ", " ");

    assertThat(user.getEmail(), is(" "));
    assertThat(user.getUsername(), is(" "));
    assertThat(user.getPassword(), is(" "));
    assertThat(user.getBio(), is(" "));
    assertThat(user.getImage(), is(" "));
  }

  @Test
  public void should_be_equal_to_itself() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    assertThat(user.equals(user), is(true));
    assertThat(user.hashCode(), is(user.hashCode()));
  }

  @Test
  public void should_not_be_equal_when_ids_are_different() {
    User user = new User("john@example.com", "john", "123", "bio", "image");
    User other = new User("john@example.com", "john", "123", "bio", "image");

    assertThat(user.equals(other), is(false));
    assertThat(user.hashCode(), is(not(other.hashCode())));
  }

  @Test
  public void should_treat_two_default_users_as_equal() {
    assertThat(new User().equals(new User()), is(true));
    assertThat(new User().hashCode(), is(new User().hashCode()));
  }

  @Test
  public void should_not_be_equal_to_null_or_other_type() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    assertThat(user.equals(null), is(false));
    assertThat(user.equals("john"), is(false));
  }
}
