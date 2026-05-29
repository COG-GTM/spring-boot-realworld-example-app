package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ErrorResourceTest {

  @Test
  void should_create_error_resource_with_field_errors() {
    List<FieldErrorResource> fieldErrors =
        Arrays.asList(
            new FieldErrorResource("User", "email", "NotBlank", "must not be blank"),
            new FieldErrorResource("User", "username", "NotBlank", "must not be blank"));
    ErrorResource resource = new ErrorResource(fieldErrors);
    assertEquals(2, resource.getFieldErrors().size());
    assertEquals("email", resource.getFieldErrors().get(0).getField());
    assertEquals("username", resource.getFieldErrors().get(1).getField());
  }
}
