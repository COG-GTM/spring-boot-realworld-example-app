package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class FieldErrorResourceTest {

  @Test
  public void should_create_field_error_resource_with_all_fields() {
    String resource = "user";
    String field = "email";
    String code = "NotBlank";
    String message = "must not be blank";

    FieldErrorResource fieldErrorResource = new FieldErrorResource(resource, field, code, message);

    assertNotNull(fieldErrorResource);
    assertEquals(resource, fieldErrorResource.getResource());
    assertEquals(field, fieldErrorResource.getField());
    assertEquals(code, fieldErrorResource.getCode());
    assertEquals(message, fieldErrorResource.getMessage());
  }

  @Test
  public void should_handle_null_values() {
    FieldErrorResource fieldErrorResource = new FieldErrorResource(null, null, null, null);

    assertNotNull(fieldErrorResource);
    assertEquals(null, fieldErrorResource.getResource());
    assertEquals(null, fieldErrorResource.getField());
    assertEquals(null, fieldErrorResource.getCode());
    assertEquals(null, fieldErrorResource.getMessage());
  }

  @Test
  public void should_handle_empty_strings() {
    FieldErrorResource fieldErrorResource = new FieldErrorResource("", "", "", "");

    assertNotNull(fieldErrorResource);
    assertEquals("", fieldErrorResource.getResource());
    assertEquals("", fieldErrorResource.getField());
    assertEquals("", fieldErrorResource.getCode());
    assertEquals("", fieldErrorResource.getMessage());
  }

  @Test
  public void should_preserve_special_characters_in_message() {
    String message = "Field 'email' is invalid: must contain @";

    FieldErrorResource fieldErrorResource =
        new FieldErrorResource("user", "email", "Email", message);

    assertEquals(message, fieldErrorResource.getMessage());
  }
}
