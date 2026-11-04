package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_create_user_with_generated_id() {
    User user = new User("a@test.com", "a", "123", "bio", "image");

    assertThat(user.getId(), notNullValue());
    assertThat(user.getEmail(), is("a@test.com"));
    assertThat(user.getUsername(), is("a"));
    assertThat(user.getPassword(), is("123"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_generate_different_ids_for_different_users() {
    User user1 = new User("a@test.com", "a", "123", "", "");
    User user2 = new User("a@test.com", "a", "123", "", "");

    assertThat(user1.getId(), not(user2.getId()));
    assertThat(user1, not(user2));
  }

  @Test
  public void should_update_all_fields() {
    User user = new User("a@test.com", "a", "123", "bio", "image");
    String id = user.getId();

    user.update("b@test.com", "b", "456", "new bio", "new image");

    assertThat(user.getId(), is(id));
    assertThat(user.getEmail(), is("b@test.com"));
    assertThat(user.getUsername(), is("b"));
    assertThat(user.getPassword(), is("456"));
    assertThat(user.getBio(), is("new bio"));
    assertThat(user.getImage(), is("new image"));
  }

  @Test
  public void should_ignore_empty_values_on_update() {
    User user = new User("a@test.com", "a", "123", "bio", "image");

    user.update("", "", "", "", "");

    assertThat(user.getEmail(), is("a@test.com"));
    assertThat(user.getUsername(), is("a"));
    assertThat(user.getPassword(), is("123"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_ignore_null_values_on_update() {
    User user = new User("a@test.com", "a", "123", "bio", "image");

    user.update(null, "b", null, null, null);

    assertThat(user.getEmail(), is("a@test.com"));
    assertThat(user.getUsername(), is("b"));
    assertThat(user.getPassword(), is("123"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_be_equal_when_ids_match() {
    User user = new User("a@test.com", "a", "123", "bio", "image");
    String hash = String.valueOf(user.hashCode());

    user.update("b@test.com", "b", "456", "", "");

    assertThat(user, is(user));
    assertThat(String.valueOf(user.hashCode()), is(hash));
  }
}
