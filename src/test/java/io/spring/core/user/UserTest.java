package io.spring.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_create_user_with_generated_id() {
    User user = new User("a@b.com", "user", "pwd", "bio", "image");

    assertThat(user.getId()).isNotEmpty();
    assertThat(user.getEmail()).isEqualTo("a@b.com");
    assertThat(user.getUsername()).isEqualTo("user");
    assertThat(user.getPassword()).isEqualTo("pwd");
    assertThat(user.getBio()).isEqualTo("bio");
    assertThat(user.getImage()).isEqualTo("image");
  }

  @Test
  public void should_generate_unique_ids() {
    User a = new User("a@b.com", "user", "pwd", "bio", "image");
    User b = new User("a@b.com", "user", "pwd", "bio", "image");

    assertThat(a.getId()).isNotEqualTo(b.getId());
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  public void should_update_all_non_empty_fields() {
    User user = new User("a@b.com", "user", "pwd", "bio", "image");
    String id = user.getId();

    user.update("new@b.com", "newuser", "newpwd", "newbio", "newimage");

    assertThat(user.getId()).isEqualTo(id);
    assertThat(user.getEmail()).isEqualTo("new@b.com");
    assertThat(user.getUsername()).isEqualTo("newuser");
    assertThat(user.getPassword()).isEqualTo("newpwd");
    assertThat(user.getBio()).isEqualTo("newbio");
    assertThat(user.getImage()).isEqualTo("newimage");
  }

  @Test
  public void should_ignore_null_and_empty_values_on_update() {
    User user = new User("a@b.com", "user", "pwd", "bio", "image");

    user.update(null, "", null, "", null);

    assertThat(user.getEmail()).isEqualTo("a@b.com");
    assertThat(user.getUsername()).isEqualTo("user");
    assertThat(user.getPassword()).isEqualTo("pwd");
    assertThat(user.getBio()).isEqualTo("bio");
    assertThat(user.getImage()).isEqualTo("image");
  }

  @Test
  public void should_update_only_provided_fields() {
    User user = new User("a@b.com", "user", "pwd", "bio", "image");

    user.update("", "", "", "newbio", "");

    assertThat(user.getEmail()).isEqualTo("a@b.com");
    assertThat(user.getBio()).isEqualTo("newbio");
    assertThat(user.getImage()).isEqualTo("image");
  }
}
