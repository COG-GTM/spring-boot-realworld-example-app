package io.spring.graphql.types;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ErrorTypeTest {
  private static List<ErrorItem> items() {
    return Arrays.asList(new ErrorItem("email", Collections.singletonList("is invalid")));
  }

  private static Error full() {
    return new Error("validation failed", items());
  }

  @Test
  public void should_build_with_builder() {
    Error error = Error.newBuilder().message("validation failed").errors(items()).build();

    assertThat(error.getMessage()).isEqualTo("validation failed");
    assertThat(error.getErrors()).hasSize(1);
    assertThat(error.getErrors().get(0).getKey()).isEqualTo("email");
    assertThat(error).isEqualTo(full());
  }

  @Test
  public void should_be_a_user_result() {
    assertThat(full()).isInstanceOf(UserResult.class);
  }

  @Test
  public void should_default_all_fields_to_null_with_no_args_constructor() {
    Error error = new Error();

    assertThat(error.getMessage()).isNull();
    assertThat(error.getErrors()).isNull();
  }

  @Test
  public void should_apply_setters() {
    Error error = new Error();
    error.setMessage("validation failed");
    error.setErrors(items());

    assertThat(error).isEqualTo(full());
  }

  @Test
  public void should_construct_with_all_args_constructor() {
    Error error = full();

    assertThat(error.getMessage()).isEqualTo("validation failed");
    assertThat(error.getErrors()).isEqualTo(items());
  }

  @Test
  public void should_implement_equals_and_hash_code() {
    Error error = full();

    assertThat(error).isEqualTo(error).isEqualTo(full()).isNotEqualTo(null);
    assertThat(error.equals("not an error")).isFalse();
    assertThat(error.hashCode()).isEqualTo(full().hashCode());
    assertThat(error).isNotEqualTo(new Error("other", items()));
    assertThat(error).isNotEqualTo(new Error("validation failed", Collections.emptyList()));
  }

  @Test
  public void should_render_all_fields_in_to_string() {
    assertThat(full().toString())
        .startsWith("Error{")
        .contains("message='validation failed'")
        .contains("key='email'")
        .endsWith("}");
  }
}
