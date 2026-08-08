package io.spring.application.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UserDataTest {

  @Test
  public void should_expose_all_constructor_values() {
    UserData userData = new UserData("user-id", "jane@example.com", "jane", "bio", "image");

    assertThat(userData.getId()).isEqualTo("user-id");
    assertThat(userData.getEmail()).isEqualTo("jane@example.com");
    assertThat(userData.getUsername()).isEqualTo("jane");
    assertThat(userData.getBio()).isEqualTo("bio");
    assertThat(userData.getImage()).isEqualTo("image");
  }

  @Test
  public void should_apply_setters_on_empty_instance() {
    UserData userData = new UserData();

    assertThat(userData.getEmail()).isNull();

    userData.setId("id");
    userData.setEmail("john@example.com");
    userData.setUsername("john");
    userData.setBio("new bio");
    userData.setImage("new image");

    assertThat(userData)
        .isEqualTo(new UserData("id", "john@example.com", "john", "new bio", "new image"));
  }

  @Test
  public void should_be_equal_for_same_values() {
    UserData one = new UserData("user-id", "jane@example.com", "jane", "bio", "image");
    UserData other = new UserData("user-id", "jane@example.com", "jane", "bio", "image");

    assertThat(one).isEqualTo(other).hasSameHashCodeAs(other);
    assertThat(one.toString()).contains("jane@example.com");

    other.setUsername("someone-else");
    assertThat(one).isNotEqualTo(other);
  }
}
