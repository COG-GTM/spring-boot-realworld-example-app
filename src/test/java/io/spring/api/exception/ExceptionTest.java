package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ExceptionTest {

  @Test
  public void should_create_invalid_authentication_exception() {
    InvalidAuthenticationException ex = new InvalidAuthenticationException();
    assertEquals("invalid email or password", ex.getMessage());
  }

  @Test
  public void should_create_resource_not_found_exception() {
    ResourceNotFoundException ex = new ResourceNotFoundException();
    assertNotNull(ex);
  }

  @Test
  public void should_create_no_authorization_exception() {
    NoAuthorizationException ex = new NoAuthorizationException();
    assertNotNull(ex);
  }

  @Test
  public void should_create_field_error_resource() {
    FieldErrorResource fer = new FieldErrorResource("User", "email", "NotBlank", "can't be empty");
    assertEquals("User", fer.getResource());
    assertEquals("email", fer.getField());
    assertEquals("NotBlank", fer.getCode());
    assertEquals("can't be empty", fer.getMessage());
  }
}
