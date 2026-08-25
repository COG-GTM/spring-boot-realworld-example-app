package io.spring.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class FieldErrorResourceTest {

  @Test
  public void should_expose_all_error_attributes() {
    FieldErrorResource resource =
        new FieldErrorResource("article", "title", "EMPTY", "can't be empty");

    assertThat(resource.getResource()).isEqualTo("article");
    assertThat(resource.getField()).isEqualTo("title");
    assertThat(resource.getCode()).isEqualTo("EMPTY");
    assertThat(resource.getMessage()).isEqualTo("can't be empty");
  }

  @Test
  public void should_expose_field_errors_of_an_error_resource() {
    FieldErrorResource resource =
        new FieldErrorResource("article", "title", "EMPTY", "can't be empty");

    ErrorResource errorResource = new ErrorResource(Arrays.asList(resource));

    assertThat(errorResource.getFieldErrors()).containsExactly(resource);
  }
}
