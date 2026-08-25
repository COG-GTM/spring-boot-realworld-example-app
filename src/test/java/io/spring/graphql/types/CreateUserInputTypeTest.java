package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class CreateUserInputTypeTest {
  private static CreateUserInput full() {
    return new CreateUserInput("jack@example.com", "jack", "secret");
  }

  @Test
  public void should_build_with_builder() {
    CreateUserInput input =
        CreateUserInput.newBuilder()
            .email("jack@example.com")
            .username("jack")
            .password("secret")
            .build();

    assertThat(input.getEmail()).isEqualTo("jack@example.com");
    assertThat(input.getUsername()).isEqualTo("jack");
    assertThat(input.getPassword()).isEqualTo("secret");
    assertThat(input).isEqualTo(full());
  }

  @Test
  public void should_default_all_fields_to_null_with_no_args_constructor() {
    CreateUserInput input = new CreateUserInput();

    assertThat(input.getEmail()).isNull();
    assertThat(input.getUsername()).isNull();
    assertThat(input.getPassword()).isNull();
  }

  @Test
  public void should_apply_setters() {
    CreateUserInput input = new CreateUserInput();
    input.setEmail("jack@example.com");
    input.setUsername("jack");
    input.setPassword("secret");

    assertThat(input).isEqualTo(full());
  }

  @Test
  public void should_construct_with_all_args_constructor() {
    CreateUserInput input = full();

    assertThat(input.getEmail()).isEqualTo("jack@example.com");
    assertThat(input.getUsername()).isEqualTo("jack");
    assertThat(input.getPassword()).isEqualTo("secret");
  }

  @Test
  public void should_implement_equals_and_hash_code() {
    CreateUserInput input = full();

    assertThat(input).isEqualTo(input).isEqualTo(full()).isNotEqualTo(null);
    assertThat(input.equals("not an input")).isFalse();
    assertThat(input.hashCode()).isEqualTo(full().hashCode());
    assertThat(input).isNotEqualTo(new CreateUserInput("other@example.com", "jack", "secret"));
    assertThat(input).isNotEqualTo(new CreateUserInput("jack@example.com", "john", "secret"));
    assertThat(input).isNotEqualTo(new CreateUserInput("jack@example.com", "jack", "other"));
  }

  @Test
  public void should_render_all_fields_in_to_string() {
    assertThat(full().toString())
        .startsWith("CreateUserInput{")
        .contains("email='jack@example.com'")
        .contains("username='jack'")
        .contains("password='secret'")
        .endsWith("}");
  }
}
