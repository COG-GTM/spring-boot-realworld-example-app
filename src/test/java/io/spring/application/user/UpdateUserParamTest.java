package io.spring.application.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UpdateUserParamTest {

  @Test
  public void should_default_all_builder_fields_to_empty_string() {
    UpdateUserParam param = UpdateUserParam.builder().build();

    assertThat(param.getEmail()).isEmpty();
    assertThat(param.getPassword()).isEmpty();
    assertThat(param.getUsername()).isEmpty();
    assertThat(param.getBio()).isEmpty();
    assertThat(param.getImage()).isEmpty();
  }

  @Test
  public void should_build_param_with_all_values() {
    UpdateUserParam param =
        UpdateUserParam.builder()
            .email("john@example.com")
            .password("123")
            .username("john")
            .bio("bio")
            .image("image")
            .build();

    assertThat(param.getEmail()).isEqualTo("john@example.com");
    assertThat(param.getPassword()).isEqualTo("123");
    assertThat(param.getUsername()).isEqualTo("john");
    assertThat(param.getBio()).isEqualTo("bio");
    assertThat(param.getImage()).isEqualTo("image");
    assertThat(UpdateUserParam.builder().email("john@example.com").toString())
        .contains("john@example.com");
  }

  @Test
  public void should_expose_values_passed_to_all_args_constructor() {
    UpdateUserParam param = new UpdateUserParam("john@example.com", "123", "john", "bio", "image");

    assertThat(param.getEmail()).isEqualTo("john@example.com");
    assertThat(param.getPassword()).isEqualTo("123");
    assertThat(param.getUsername()).isEqualTo("john");
    assertThat(param.getBio()).isEqualTo("bio");
    assertThat(param.getImage()).isEqualTo("image");
  }
}
