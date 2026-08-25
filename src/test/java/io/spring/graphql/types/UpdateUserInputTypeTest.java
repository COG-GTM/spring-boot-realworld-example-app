package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UpdateUserInputTypeTest {
  private static UpdateUserInput full() {
    return new UpdateUserInput("jack@example.com", "jack", "secret", "image.png", "bio");
  }

  @Test
  public void should_build_with_builder() {
    UpdateUserInput input =
        UpdateUserInput.newBuilder()
            .email("jack@example.com")
            .username("jack")
            .password("secret")
            .image("image.png")
            .bio("bio")
            .build();

    assertThat(input.getEmail()).isEqualTo("jack@example.com");
    assertThat(input.getUsername()).isEqualTo("jack");
    assertThat(input.getPassword()).isEqualTo("secret");
    assertThat(input.getImage()).isEqualTo("image.png");
    assertThat(input.getBio()).isEqualTo("bio");
    assertThat(input).isEqualTo(full());
  }

  @Test
  public void should_default_all_fields_to_null_with_no_args_constructor() {
    UpdateUserInput input = new UpdateUserInput();

    assertThat(input.getEmail()).isNull();
    assertThat(input.getUsername()).isNull();
    assertThat(input.getPassword()).isNull();
    assertThat(input.getImage()).isNull();
    assertThat(input.getBio()).isNull();
  }

  @Test
  public void should_apply_setters() {
    UpdateUserInput input = new UpdateUserInput();
    input.setEmail("jack@example.com");
    input.setUsername("jack");
    input.setPassword("secret");
    input.setImage("image.png");
    input.setBio("bio");

    assertThat(input).isEqualTo(full());
  }

  @Test
  public void should_construct_with_all_args_constructor() {
    UpdateUserInput input = full();

    assertThat(input.getEmail()).isEqualTo("jack@example.com");
    assertThat(input.getUsername()).isEqualTo("jack");
    assertThat(input.getPassword()).isEqualTo("secret");
    assertThat(input.getImage()).isEqualTo("image.png");
    assertThat(input.getBio()).isEqualTo("bio");
  }

  @Test
  public void should_implement_equals_and_hash_code() {
    UpdateUserInput input = full();

    assertThat(input).isEqualTo(input).isEqualTo(full()).isNotEqualTo(null);
    assertThat(input.equals("not an input")).isFalse();
    assertThat(input.hashCode()).isEqualTo(full().hashCode());
    assertThat(input)
        .isNotEqualTo(
            new UpdateUserInput("other@example.com", "jack", "secret", "image.png", "bio"));
    assertThat(input)
        .isNotEqualTo(
            new UpdateUserInput("jack@example.com", "john", "secret", "image.png", "bio"));
    assertThat(input)
        .isNotEqualTo(new UpdateUserInput("jack@example.com", "jack", "other", "image.png", "bio"));
    assertThat(input)
        .isNotEqualTo(
            new UpdateUserInput("jack@example.com", "jack", "secret", "other.png", "bio"));
    assertThat(input)
        .isNotEqualTo(
            new UpdateUserInput("jack@example.com", "jack", "secret", "image.png", "other"));
  }

  @Test
  public void should_render_all_fields_in_to_string() {
    assertThat(full().toString())
        .startsWith("UpdateUserInput{")
        .contains("email='jack@example.com'")
        .contains("username='jack'")
        .contains("password='secret'")
        .contains("image='image.png'")
        .contains("bio='bio'")
        .endsWith("}");
  }
}
