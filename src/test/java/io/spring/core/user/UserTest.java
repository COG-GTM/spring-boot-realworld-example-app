package io.spring.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class UserTest {

  @Test
  public void should_generate_id_and_keep_all_fields_from_constructor() {
    User user = new User("jake@jake.jake", "jake", "123", "bio", "image");

    assertThat(user.getId()).isNotNull();
    assertThat(UUID.fromString(user.getId()).toString()).isEqualTo(user.getId());
    assertThat(user.getEmail()).isEqualTo("jake@jake.jake");
    assertThat(user.getUsername()).isEqualTo("jake");
    assertThat(user.getPassword()).isEqualTo("123");
    assertThat(user.getBio()).isEqualTo("bio");
    assertThat(user.getImage()).isEqualTo("image");
  }

  @Test
  public void should_generate_different_id_for_each_user() {
    User one = new User("jake@jake.jake", "jake", "123", "", "");
    User another = new User("jake@jake.jake", "jake", "123", "", "");

    assertThat(one.getId()).isNotEqualTo(another.getId());
  }

  @Test
  public void should_leave_all_fields_null_with_no_args_constructor() {
    User user = new User();

    assertThat(user.getId()).isNull();
    assertThat(user.getEmail()).isNull();
    assertThat(user.getUsername()).isNull();
    assertThat(user.getPassword()).isNull();
    assertThat(user.getBio()).isNull();
    assertThat(user.getImage()).isNull();
  }

  @Test
  public void should_update_all_fields_when_all_values_are_present() {
    User user = new User("jake@jake.jake", "jake", "123", "bio", "image");
    String id = user.getId();

    user.update("new@email.com", "newname", "newpassword", "new bio", "new image");

    assertThat(user.getId()).isEqualTo(id);
    assertThat(user.getEmail()).isEqualTo("new@email.com");
    assertThat(user.getUsername()).isEqualTo("newname");
    assertThat(user.getPassword()).isEqualTo("newpassword");
    assertThat(user.getBio()).isEqualTo("new bio");
    assertThat(user.getImage()).isEqualTo("new image");
  }

  @Test
  public void should_keep_original_values_when_updating_with_null() {
    User user = new User("jake@jake.jake", "jake", "123", "bio", "image");

    user.update(null, null, null, null, null);

    assertThat(user.getEmail()).isEqualTo("jake@jake.jake");
    assertThat(user.getUsername()).isEqualTo("jake");
    assertThat(user.getPassword()).isEqualTo("123");
    assertThat(user.getBio()).isEqualTo("bio");
    assertThat(user.getImage()).isEqualTo("image");
  }

  @Test
  public void should_keep_original_values_when_updating_with_empty_string() {
    User user = new User("jake@jake.jake", "jake", "123", "bio", "image");

    user.update("", "", "", "", "");

    assertThat(user.getEmail()).isEqualTo("jake@jake.jake");
    assertThat(user.getUsername()).isEqualTo("jake");
    assertThat(user.getPassword()).isEqualTo("123");
    assertThat(user.getBio()).isEqualTo("bio");
    assertThat(user.getImage()).isEqualTo("image");
  }

  @Test
  public void should_update_only_the_fields_with_a_value() {
    User user = new User("jake@jake.jake", "jake", "123", "bio", "image");

    user.update("", "newname", null, "", "new image");

    assertThat(user.getEmail()).isEqualTo("jake@jake.jake");
    assertThat(user.getUsername()).isEqualTo("newname");
    assertThat(user.getPassword()).isEqualTo("123");
    assertThat(user.getBio()).isEqualTo("bio");
    assertThat(user.getImage()).isEqualTo("new image");
  }

  @Test
  public void should_use_only_id_for_equals_and_hashcode() {
    User user = new User("jake@jake.jake", "jake", "123", "bio", "image");
    User sameIdDifferentContent = new User();
    ReflectionTestUtils.setField(sameIdDifferentContent, "id", user.getId());

    assertThat(sameIdDifferentContent).isEqualTo(user);
    assertThat(sameIdDifferentContent.hashCode()).isEqualTo(user.hashCode());
  }

  @Test
  public void should_not_be_equal_when_ids_are_different() {
    User one = new User("jake@jake.jake", "jake", "123", "bio", "image");
    User another = new User("jake@jake.jake", "jake", "123", "bio", "image");

    assertThat(one).isNotEqualTo(another);
  }

  @Test
  public void should_not_be_equal_to_null_or_other_types() {
    User user = new User("jake@jake.jake", "jake", "123", "bio", "image");

    assertThat(user).isNotEqualTo(null);
    assertThat(user).isNotEqualTo("jake");
    assertThat(user).isEqualTo(user);
  }

  @Test
  public void should_be_equal_when_both_ids_are_null() {
    assertThat(new User()).isEqualTo(new User());
    assertThat(new User().hashCode()).isEqualTo(new User().hashCode());
  }
}
