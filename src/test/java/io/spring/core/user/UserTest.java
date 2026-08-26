package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  private User user() {
    return new User("email@test.com", "username", "password", "bio", "image");
  }

  @Test
  public void should_generate_id_for_new_user() {
    assertThat(user().getId(), notNullValue());
  }

  @Test
  public void should_update_all_provided_fields() {
    User user = user();
    user.update("new@test.com", "newname", "newpassword", "new bio", "new image");

    assertThat(user.getEmail(), is("new@test.com"));
    assertThat(user.getUsername(), is("newname"));
    assertThat(user.getPassword(), is("newpassword"));
    assertThat(user.getBio(), is("new bio"));
    assertThat(user.getImage(), is("new image"));
  }

  @Test
  public void should_ignore_empty_fields_on_update() {
    User user = user();
    user.update("", null, "", null, "");

    assertThat(user.getEmail(), is("email@test.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }
}
