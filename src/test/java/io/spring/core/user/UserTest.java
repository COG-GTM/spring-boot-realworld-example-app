package io.spring.core.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class UserTest {

  @Test
  public void should_generate_id_and_keep_constructor_values() {
    User user = new User("email@test.com", "username", "password", "bio", "image");
    assertThat(user.getId(), is(notNullValue()));
    assertThat(user.getEmail(), is("email@test.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_update_all_fields_when_values_are_not_empty() {
    User user = new User("email@test.com", "username", "password", "bio", "image");
    user.update("new@test.com", "newname", "newpass", "newbio", "newimage");

    assertThat(user.getEmail(), is("new@test.com"));
    assertThat(user.getUsername(), is("newname"));
    assertThat(user.getPassword(), is("newpass"));
    assertThat(user.getBio(), is("newbio"));
    assertThat(user.getImage(), is("newimage"));
  }

  @Test
  public void should_keep_original_values_when_update_values_are_empty() {
    User user = new User("email@test.com", "username", "password", "bio", "image");
    user.update("", "", "", "", "");

    assertThat(user.getEmail(), is("email@test.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_keep_original_values_when_update_values_are_null() {
    User user = new User("email@test.com", "username", "password", "bio", "image");
    user.update(null, null, null, null, null);

    assertThat(user.getEmail(), is("email@test.com"));
    assertThat(user.getUsername(), is("username"));
    assertThat(user.getPassword(), is("password"));
    assertThat(user.getBio(), is("bio"));
    assertThat(user.getImage(), is("image"));
  }

  @Test
  public void should_update_only_provided_fields() {
    User user = new User("email@test.com", "username", "password", "bio", "image");
    user.update("new@test.com", "", "", "", "");

    assertThat(user.getEmail(), is("new@test.com"));
    assertThat(user.getUsername(), is("username"));
  }

  @Test
  public void should_be_equal_only_when_ids_are_equal() {
    User user = new User("email@test.com", "username", "password", "bio", "image");
    User differentId = new User("email@test.com", "username", "password", "bio", "image");
    User sameId = new User("other@test.com", "other", "otherpass", "otherbio", "otherimage");
    ReflectionTestUtils.setField(sameId, "id", user.getId());

    assertThat(user.equals(differentId), is(false));
    assertThat(user.equals(sameId), is(true));
    assertThat(user.hashCode(), is(sameId.hashCode()));
  }
}
