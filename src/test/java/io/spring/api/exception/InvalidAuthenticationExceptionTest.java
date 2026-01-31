package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class InvalidAuthenticationExceptionTest {

  @Test
  public void should_create_exception_with_default_message() {
    InvalidAuthenticationException exception = new InvalidAuthenticationException();

    assertNotNull(exception);
    assertEquals("invalid email or password", exception.getMessage());
  }

  @Test
  public void should_be_runtime_exception() {
    InvalidAuthenticationException exception = new InvalidAuthenticationException();

    assertTrue(exception instanceof RuntimeException);
  }
}
