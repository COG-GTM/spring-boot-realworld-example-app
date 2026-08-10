package io.spring.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

  private User newUser() {
    return new User("jake@jake.jake", "jake", "123", "bio", "image");
  }

  @Test
  void should_create_user_with_generated_id() {
    User user = newUser();

    assertThat(user.getId()).isNotBlank();
    assertThat(user.getEmail()).isEqualTo("jake@jake.jake");
    assertThat(user.getUsername()).isEqualTo("jake");
    assertThat(user.getPassword()).isEqualTo("123");
    assertThat(user.getBio()).isEqualTo("bio");
    assertThat(user.getImage()).isEqualTo("image");
  }

  @Test
  void should_generate_unique_id_per_user() {
    assertThat(newUser().getId()).isNotEqualTo(newUser().getId());
  }

  @Test
  void should_update_all_fields_when_all_values_provided() {
    User user = newUser();

    user.update("new@email.com", "newname", "newpass", "newbio", "newimage");

    assertThat(user.getEmail()).isEqualTo("new@email.com");
    assertThat(user.getUsername()).isEqualTo("newname");
    assertThat(user.getPassword()).isEqualTo("newpass");
    assertThat(user.getBio()).isEqualTo("newbio");
    assertThat(user.getImage()).isEqualTo("newimage");
  }

  @Test
  void should_ignore_null_values_on_update() {
    User user = newUser();

    user.update(null, null, null, null, null);

    assertThat(user.getEmail()).isEqualTo("jake@jake.jake");
    assertThat(user.getUsername()).isEqualTo("jake");
    assertThat(user.getPassword()).isEqualTo("123");
    assertThat(user.getBio()).isEqualTo("bio");
    assertThat(user.getImage()).isEqualTo("image");
  }

  @Test
  void should_ignore_empty_values_on_update() {
    User user = newUser();

    user.update("", "", "", "", "");

    assertThat(user.getEmail()).isEqualTo("jake@jake.jake");
    assertThat(user.getUsername()).isEqualTo("jake");
    assertThat(user.getPassword()).isEqualTo("123");
    assertThat(user.getBio()).isEqualTo("bio");
    assertThat(user.getImage()).isEqualTo("image");
  }

  @Test
  void should_update_only_provided_fields() {
    User user = newUser();

    user.update("", "newname", null, "", "newimage");

    assertThat(user.getEmail()).isEqualTo("jake@jake.jake");
    assertThat(user.getUsername()).isEqualTo("newname");
    assertThat(user.getPassword()).isEqualTo("123");
    assertThat(user.getBio()).isEqualTo("bio");
    assertThat(user.getImage()).isEqualTo("newimage");
  }

  @Test
  void should_keep_fields_null_with_no_args_constructor() {
    User user = new User();

    assertThat(user.getId()).isNull();
    assertThat(user.getEmail()).isNull();
  }

  @Test
  void should_compare_users_by_id_only() {
    User user = newUser();
    User other = newUser();

    assertThat(user).isEqualTo(user).hasSameHashCodeAs(user);
    assertThat(user).isNotEqualTo(other);
    assertThat(user).isNotEqualTo(new User());
    assertThat(user).isNotEqualTo(null).isNotEqualTo("jake");
  }
}
