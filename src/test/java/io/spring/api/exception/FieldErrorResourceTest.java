package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class FieldErrorResourceTest {

  @Test
  void should_create_field_error_resource() {
    FieldErrorResource resource = new FieldErrorResource("User", "email", "NotBlank", "must not be blank");
    assertEquals("User", resource.getResource());
    assertEquals("email", resource.getField());
    assertEquals("NotBlank", resource.getCode());
    assertEquals("must not be blank", resource.getMessage());
  }
}
