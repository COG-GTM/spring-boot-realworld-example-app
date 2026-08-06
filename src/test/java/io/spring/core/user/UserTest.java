package io.spring.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  private User newUser() {
    return new User("old@test.com", "oldname", "oldpassword", "old bio", "old image");
  }

  @Test
  public void should_assign_random_id_and_all_fields_on_construction() {
    User user = newUser();

    assertThat(user.getId()).isNotBlank();
    assertThat(user.getEmail()).isEqualTo("old@test.com");
    assertThat(user.getUsername()).isEqualTo("oldname");
    assertThat(user.getPassword()).isEqualTo("oldpassword");
    assertThat(user.getBio()).isEqualTo("old bio");
    assertThat(user.getImage()).isEqualTo("old image");
  }

  @Test
  public void should_assign_different_ids_to_different_users() {
    assertThat(newUser().getId()).isNotEqualTo(newUser().getId());
  }

  @Test
  public void should_update_all_fields_when_all_values_present() {
    User user = newUser();

    user.update("new@test.com", "newname", "newpassword", "new bio", "new image");

    assertThat(user.getEmail()).isEqualTo("new@test.com");
    assertThat(user.getUsername()).isEqualTo("newname");
    assertThat(user.getPassword()).isEqualTo("newpassword");
    assertThat(user.getBio()).isEqualTo("new bio");
    assertThat(user.getImage()).isEqualTo("new image");
  }

  @Test
  public void should_keep_old_values_when_new_values_are_empty() {
    User user = newUser();

    user.update("", "", "", "", "");

    assertThat(user.getEmail()).isEqualTo("old@test.com");
    assertThat(user.getUsername()).isEqualTo("oldname");
    assertThat(user.getPassword()).isEqualTo("oldpassword");
    assertThat(user.getBio()).isEqualTo("old bio");
    assertThat(user.getImage()).isEqualTo("old image");
  }

  @Test
  public void should_keep_old_values_when_new_values_are_null() {
    User user = newUser();

    user.update(null, null, null, null, null);

    assertThat(user.getEmail()).isEqualTo("old@test.com");
    assertThat(user.getUsername()).isEqualTo("oldname");
    assertThat(user.getPassword()).isEqualTo("oldpassword");
    assertThat(user.getBio()).isEqualTo("old bio");
    assertThat(user.getImage()).isEqualTo("old image");
  }

  @Test
  public void should_update_only_the_fields_with_non_empty_values() {
    User user = newUser();

    user.update("new@test.com", "", null, "new bio", "");

    assertThat(user.getEmail()).isEqualTo("new@test.com");
    assertThat(user.getUsername()).isEqualTo("oldname");
    assertThat(user.getPassword()).isEqualTo("oldpassword");
    assertThat(user.getBio()).isEqualTo("new bio");
    assertThat(user.getImage()).isEqualTo("old image");
  }

  @Test
  public void should_not_change_id_on_update() {
    User user = newUser();
    String id = user.getId();

    user.update("new@test.com", "newname", "newpassword", "new bio", "new image");

    assertThat(user.getId()).isEqualTo(id);
  }

  @Test
  public void should_use_id_only_for_equality() {
    User user = newUser();
    User sameFieldsDifferentId =
        new User("old@test.com", "oldname", "oldpassword", "old bio", "old image");

    assertThat(user).isNotEqualTo(sameFieldsDifferentId);
    assertThat(user.hashCode()).isNotEqualTo(sameFieldsDifferentId.hashCode());
    assertThat(user).isEqualTo(user);
    assertThat(user.hashCode()).isEqualTo(user.hashCode());
  }

  @Test
  public void should_not_equal_null_or_other_types() {
    User user = newUser();

    assertThat(user).isNotEqualTo(null);
    assertThat(user).isNotEqualTo("not a user");
  }

  @Test
  public void should_create_empty_user_with_no_args_constructor() {
    User user = new User();

    assertThat(user.getId()).isNull();
    assertThat(user.getEmail()).isNull();
    assertThat(user.getUsername()).isNull();
  }
}
