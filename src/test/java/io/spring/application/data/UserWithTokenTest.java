package io.spring.application.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

public class UserWithTokenTest {

  @Test
  public void should_copy_fields_from_user_data_and_keep_token() {
    UserData userData = new UserData("user-id", "jane@example.com", "jane", "bio", "image");

    UserWithToken userWithToken = new UserWithToken(userData, "a-jwt-token");

    assertThat(userWithToken.getEmail()).isEqualTo("jane@example.com");
    assertThat(userWithToken.getUsername()).isEqualTo("jane");
    assertThat(userWithToken.getBio()).isEqualTo("bio");
    assertThat(userWithToken.getImage()).isEqualTo("image");
    assertThat(userWithToken.getToken()).isEqualTo("a-jwt-token");
  }

  @Test
  public void should_not_expose_the_user_id() {
    assertThat(UserWithToken.class.getDeclaredFields())
        .extracting(Field::getName)
        .doesNotContain("id");
    assertThat(UserWithToken.class.getMethods())
        .extracting(Method::getName)
        .doesNotContain("getId");
  }

  @Test
  public void should_snapshot_values_and_ignore_later_user_data_changes() {
    UserData userData = new UserData("user-id", "jane@example.com", "jane", "bio", "image");
    UserWithToken userWithToken = new UserWithToken(userData, "token");

    userData.setUsername("renamed");
    userData.setBio("new bio");

    assertThat(userWithToken.getUsername()).isEqualTo("jane");
    assertThat(userWithToken.getBio()).isEqualTo("bio");
  }

  @Test
  public void should_carry_null_fields_from_incomplete_user_data() {
    UserWithToken userWithToken = new UserWithToken(new UserData(), "token");

    assertThat(userWithToken.getEmail()).isNull();
    assertThat(userWithToken.getUsername()).isNull();
    assertThat(userWithToken.getBio()).isNull();
    assertThat(userWithToken.getImage()).isNull();
    assertThat(userWithToken.getToken()).isEqualTo("token");
  }
}
