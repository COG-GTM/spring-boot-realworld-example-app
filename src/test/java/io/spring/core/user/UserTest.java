package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_construct_user() {
    User user = new User("email", "username", "password", "bio", "image");

    assertThat(user.getId() != null, is(true));
    assertThat(user.getEmail(), is("email"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_update_non_empty_fields() {
    User user = new User("email", "username", "password", "bio", "image");

    user.update("new-email", "new-username", "new-password", "new-bio", "new-image");

    assertThat(user.getEmail(), is("new-email"));
    assertThat(user.getUsername(), is("new-username"));
    assertThat(user.getPassword(), is("new-password"));
    assertThat(user.getBio(), is("new-bio"));
    assertThat(user.getImage(), is("new-image"));
  }

  @Test
  public void should_ignore_null_and_empty_update_fields() {
    User user = new User("email", "username", "password", "bio", "image");

    user.update(null, "", null, "", null);

    assertThat(user.getEmail(), is("email"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_be_equal_by_id_only() {
    User first = new User("email", "username", "password", "bio", "image");
    User second = new User("email", "username", "password", "bio", "image");

    assertThat(first.equals(second), is(false));
    assertThat(first.equals(first), is(true));
  }
}
