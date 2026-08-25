package io.spring.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_generate_id_and_keep_all_fields_on_creation() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    assertThat(user.getId()).isNotBlank();
    assertThat(user.getEmail()).isEqualTo("john@example.com");
    assertThat(user.getUsername()).isEqualTo("john");
    assertThat(user.getPassword()).isEqualTo("123");
    assertThat(user.getBio()).isEqualTo("bio");
    assertThat(user.getImage()).isEqualTo("image");
  }

  @Test
  public void should_update_all_provided_fields() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    user.update("new@example.com", "newname", "newpassword", "new bio", "new image");

    assertThat(user.getEmail()).isEqualTo("new@example.com");
    assertThat(user.getUsername()).isEqualTo("newname");
    assertThat(user.getPassword()).isEqualTo("newpassword");
    assertThat(user.getBio()).isEqualTo("new bio");
    assertThat(user.getImage()).isEqualTo("new image");
  }

  @Test
  public void should_ignore_empty_and_null_values_on_update() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    user.update("", null, "", null, "");

    assertThat(user.getEmail()).isEqualTo("john@example.com");
    assertThat(user.getUsername()).isEqualTo("john");
    assertThat(user.getPassword()).isEqualTo("123");
    assertThat(user.getBio()).isEqualTo("bio");
    assertThat(user.getImage()).isEqualTo("image");
  }

  @Test
  public void should_compare_users_by_id_only() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    assertThat(user).isEqualTo(user).hasSameHashCodeAs(user);
    assertThat(user).isNotEqualTo(new User("john@example.com", "john", "123", "bio", "image"));
    assertThat(user).isNotEqualTo(new User());
  }
}
