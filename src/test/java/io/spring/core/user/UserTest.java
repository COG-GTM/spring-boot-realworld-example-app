package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  private User newUser() {
    return new User("email@test.com", "username", "123", "bio", "image");
  }

  @Test
  public void should_generate_id_on_creation() {
    User user = newUser();
    assertThat(user.getId(), is(notNullValue()));
    assertThat(user.getId(), is(not(newUser().getId())));
  }

  @Test
  public void should_update_all_fields() {
    User user = newUser();
    user.update("new@test.com", "new username", "456", "new bio", "new image");

    assertThat(user.getEmail(), is("new@test.com"));
    assertThat(user.getUsername(), is("new username"));
    assertThat(user.getPassword(), is("456"));
    assertThat(user.getBio(), is("new bio"));
    assertThat(user.getImage(), is("new image"));
  }

  @Test
  public void should_keep_original_fields_when_updating_with_empty_values() {
    User user = newUser();
    user.update("", null, "", null, "");

    assertThat(user.getEmail(), is("email@test.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("123"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_update_only_provided_fields() {
    User user = newUser();
    user.update("", "new username", "", "", "");

    assertThat(user.getUsername(), is("new username"));
    assertThat(user.getEmail(), is("email@test.com"));
  }

  @Test
  public void should_be_equal_only_by_id() {
    User user = newUser();
    User another = newUser();
    assertThat(user.equals(another), is(false));
    assertThat(user.equals(user), is(true));
  }
}
