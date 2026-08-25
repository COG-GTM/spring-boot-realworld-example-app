package io.spring.application.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class RegisterParamTest {

  @Test
  public void should_expose_all_values_passed_to_all_args_constructor() {
    RegisterParam param = new RegisterParam("john@example.com", "john", "123");

    assertThat(param.getEmail()).isEqualTo("john@example.com");
    assertThat(param.getUsername()).isEqualTo("john");
    assertThat(param.getPassword()).isEqualTo("123");
  }

  @Test
  public void should_have_null_fields_when_created_with_no_args_constructor() {
    RegisterParam param = new RegisterParam();

    assertThat(param.getEmail()).isNull();
    assertThat(param.getUsername()).isNull();
    assertThat(param.getPassword()).isNull();
  }
}
