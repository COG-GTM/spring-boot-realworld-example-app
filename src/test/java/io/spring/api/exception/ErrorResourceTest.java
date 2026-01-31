package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ErrorResourceTest {

  @Test
  public void should_create_error_resource_with_field_errors() {
    List<FieldErrorResource> fieldErrors =
        Arrays.asList(
            new FieldErrorResource("user", "email", "NotBlank", "must not be blank"),
            new FieldErrorResource("user", "username", "Size", "size must be between 1 and 50"));

    ErrorResource errorResource = new ErrorResource(fieldErrors);

    assertNotNull(errorResource);
    assertSame(fieldErrors, errorResource.getFieldErrors());
    assertEquals(2, errorResource.getFieldErrors().size());
  }

  @Test
  public void should_create_error_resource_with_empty_list() {
    List<FieldErrorResource> fieldErrors = new ArrayList<>();

    ErrorResource errorResource = new ErrorResource(fieldErrors);

    assertNotNull(errorResource);
    assertTrue(errorResource.getFieldErrors().isEmpty());
  }

  @Test
  public void should_create_error_resource_with_single_error() {
    FieldErrorResource fieldError =
        new FieldErrorResource("user", "email", "Email", "must be a valid email");
    List<FieldErrorResource> fieldErrors = Arrays.asList(fieldError);

    ErrorResource errorResource = new ErrorResource(fieldErrors);

    assertNotNull(errorResource);
    assertEquals(1, errorResource.getFieldErrors().size());
    assertEquals("email", errorResource.getFieldErrors().get(0).getField());
  }

  @Test
  public void should_preserve_field_error_order() {
    FieldErrorResource error1 = new FieldErrorResource("user", "email", "NotBlank", "error1");
    FieldErrorResource error2 = new FieldErrorResource("user", "username", "NotBlank", "error2");
    FieldErrorResource error3 = new FieldErrorResource("user", "password", "NotBlank", "error3");
    List<FieldErrorResource> fieldErrors = Arrays.asList(error1, error2, error3);

    ErrorResource errorResource = new ErrorResource(fieldErrors);

    assertEquals("email", errorResource.getFieldErrors().get(0).getField());
    assertEquals("username", errorResource.getFieldErrors().get(1).getField());
    assertEquals("password", errorResource.getFieldErrors().get(2).getField());
  }
}
