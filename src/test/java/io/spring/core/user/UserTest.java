package io.spring.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  @Test
  public void should_generate_id_on_construction() {
    User user = new User("user@example.com", "username", "password", "bio", "image");
    assertThat(user.getId()).isNotNull();
    assertThat(user.getId()).isNotEmpty();
  }

  @Test
  public void should_set_all_fields_on_construction() {
    User user = new User("user@example.com", "username", "password", "bio", "image");
    assertThat(user.getEmail()).isEqualTo("user@example.com");
    assertThat(user.getUsername()).isEqualTo("username");
    assertThat(user.getPassword()).isEqualTo("password");
    assertThat(user.getBio()).isEqualTo("bio");
    assertThat(user.getImage()).isEqualTo("image");
  }

  @Test
  public void should_generate_distinct_ids_for_different_users() {
    User first = new User("a@example.com", "a", "pw", "", "");
    User second = new User("b@example.com", "b", "pw", "", "");
    assertThat(first.getId()).isNotEqualTo(second.getId());
  }

  @Test
  public void should_update_all_fields_when_all_values_present() {
    User user = new User("old@example.com", "oldname", "oldpw", "oldbio", "oldimage");
    user.update("new@example.com", "newname", "newpw", "newbio", "newimage");
    assertThat(user.getEmail()).isEqualTo("new@example.com");
    assertThat(user.getUsername()).isEqualTo("newname");
    assertThat(user.getPassword()).isEqualTo("newpw");
    assertThat(user.getBio()).isEqualTo("newbio");
    assertThat(user.getImage()).isEqualTo("newimage");
  }

  @Test
  public void should_not_update_fields_when_values_are_null() {
    User user = new User("old@example.com", "oldname", "oldpw", "oldbio", "oldimage");
    user.update(null, null, null, null, null);
    assertThat(user.getEmail()).isEqualTo("old@example.com");
    assertThat(user.getUsername()).isEqualTo("oldname");
    assertThat(user.getPassword()).isEqualTo("oldpw");
    assertThat(user.getBio()).isEqualTo("oldbio");
    assertThat(user.getImage()).isEqualTo("oldimage");
  }

  @Test
  public void should_not_update_fields_when_values_are_empty() {
    User user = new User("old@example.com", "oldname", "oldpw", "oldbio", "oldimage");
    user.update("", "", "", "", "");
    assertThat(user.getEmail()).isEqualTo("old@example.com");
    assertThat(user.getUsername()).isEqualTo("oldname");
    assertThat(user.getPassword()).isEqualTo("oldpw");
    assertThat(user.getBio()).isEqualTo("oldbio");
    assertThat(user.getImage()).isEqualTo("oldimage");
  }

  @Test
  public void should_partially_update_only_present_fields() {
    User user = new User("old@example.com", "oldname", "oldpw", "oldbio", "oldimage");
    user.update("new@example.com", "", null, "newbio", "");
    assertThat(user.getEmail()).isEqualTo("new@example.com");
    assertThat(user.getUsername()).isEqualTo("oldname");
    assertThat(user.getPassword()).isEqualTo("oldpw");
    assertThat(user.getBio()).isEqualTo("newbio");
    assertThat(user.getImage()).isEqualTo("oldimage");
  }

  @Test
  public void should_keep_id_stable_after_update() {
    User user = new User("old@example.com", "oldname", "oldpw", "oldbio", "oldimage");
    String originalId = user.getId();
    user.update("new@example.com", "newname", "newpw", "newbio", "newimage");
    assertThat(user.getId()).isEqualTo(originalId);
  }

  @Test
  public void should_consider_users_equal_when_ids_match() {
    User user = new User("user@example.com", "username", "password", "bio", "image");
    User same = new User("other@example.com", "other", "otherpw", "otherbio", "otherimage");
    setId(same, user.getId());
    assertThat(user).isEqualTo(same);
    assertThat(user.hashCode()).isEqualTo(same.hashCode());
  }

  @Test
  public void should_consider_users_unequal_when_ids_differ() {
    User first = new User("user@example.com", "username", "password", "bio", "image");
    User second = new User("user@example.com", "username", "password", "bio", "image");
    assertThat(first).isNotEqualTo(second);
  }

  private void setId(User user, String id) {
    try {
      java.lang.reflect.Field field = User.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(user, id);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}
