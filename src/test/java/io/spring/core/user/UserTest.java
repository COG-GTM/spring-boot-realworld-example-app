package io.spring.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_populate_fields_on_creation() {
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
    String id = user.getId();

    user.update("new@example.com", "newname", "456", "new bio", "new image");

    assertThat(user.getId()).isEqualTo(id);
    assertThat(user.getEmail()).isEqualTo("new@example.com");
    assertThat(user.getUsername()).isEqualTo("newname");
    assertThat(user.getPassword()).isEqualTo("456");
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
  public void should_update_only_non_empty_fields() {
    User user = new User("john@example.com", "john", "123", "bio", "image");

    user.update(null, "newname", "", "", null);

    assertThat(user.getEmail()).isEqualTo("john@example.com");
    assertThat(user.getUsername()).isEqualTo("newname");
    assertThat(user.getPassword()).isEqualTo("123");
  }

  @Test
  public void should_use_id_for_equality() {
    User user = new User("john@example.com", "john", "123", "bio", "image");
    User sameData = new User("john@example.com", "john", "123", "bio", "image");

    assertThat(user).isEqualTo(user);
    assertThat(user.hashCode()).isEqualTo(user.hashCode());
    assertThat(user).isNotEqualTo(sameData);
    assertThat(new User()).isEqualTo(new User());
  }
}
