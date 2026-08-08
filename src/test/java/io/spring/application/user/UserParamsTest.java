package io.spring.application.user;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.core.user.User;
import org.junit.jupiter.api.Test;

public class UserParamsTest {

  @Test
  public void should_expose_register_param_fields() {
    RegisterParam param = new RegisterParam("john@example.com", "john", "123");

    assertThat(param.getEmail()).isEqualTo("john@example.com");
    assertThat(param.getUsername()).isEqualTo("john");
    assertThat(param.getPassword()).isEqualTo("123");
  }

  @Test
  public void should_have_null_fields_for_default_register_param() {
    RegisterParam param = new RegisterParam();

    assertThat(param.getEmail()).isNull();
    assertThat(param.getUsername()).isNull();
    assertThat(param.getPassword()).isNull();
  }

  @Test
  public void should_default_update_user_param_fields_to_empty_string() {
    UpdateUserParam param = new UpdateUserParam();

    assertThat(param.getEmail()).isEmpty();
    assertThat(param.getPassword()).isEmpty();
    assertThat(param.getUsername()).isEmpty();
    assertThat(param.getBio()).isEmpty();
    assertThat(param.getImage()).isEmpty();
  }

  @Test
  public void should_build_update_user_param_with_builder_defaults() {
    UpdateUserParam param = UpdateUserParam.builder().email("john@example.com").build();

    assertThat(param.getEmail()).isEqualTo("john@example.com");
    assertThat(param.getPassword()).isEmpty();
    assertThat(param.getUsername()).isEmpty();
    assertThat(param.getBio()).isEmpty();
    assertThat(param.getImage()).isEmpty();
  }

  @Test
  public void should_build_update_user_param_with_all_args_constructor() {
    UpdateUserParam param = new UpdateUserParam("e", "p", "u", "b", "i");

    assertThat(param.getEmail()).isEqualTo("e");
    assertThat(param.getPassword()).isEqualTo("p");
    assertThat(param.getUsername()).isEqualTo("u");
    assertThat(param.getBio()).isEqualTo("b");
    assertThat(param.getImage()).isEqualTo("i");
  }

  @Test
  public void should_expose_target_user_and_param_from_update_user_command() {
    User user = new User("john@example.com", "john", "123", "bio", "image");
    UpdateUserParam param = UpdateUserParam.builder().username("new").build();

    UpdateUserCommand command = new UpdateUserCommand(user, param);

    assertThat(command.getTargetUser()).isSameAs(user);
    assertThat(command.getParam()).isSameAs(param);
  }
}
