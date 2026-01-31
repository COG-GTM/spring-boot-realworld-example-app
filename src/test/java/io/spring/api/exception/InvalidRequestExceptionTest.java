package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.validation.Errors;

public class InvalidRequestExceptionTest {

  @Test
  public void should_create_exception_with_errors() {
    Errors errors = mock(Errors.class);

    InvalidRequestException exception = new InvalidRequestException(errors);

    assertNotNull(exception);
    assertSame(errors, exception.getErrors());
  }

  @Test
  public void should_have_empty_message() {
    Errors errors = mock(Errors.class);

    InvalidRequestException exception = new InvalidRequestException(errors);

    assertEquals("", exception.getMessage());
  }

  @Test
  public void should_be_runtime_exception() {
    Errors errors = mock(Errors.class);

    InvalidRequestException exception = new InvalidRequestException(errors);

    assertTrue(exception instanceof RuntimeException);
  }

  @Test
  public void should_return_same_errors_object() {
    Errors errors = mock(Errors.class);

    InvalidRequestException exception = new InvalidRequestException(errors);

    Errors returnedErrors = exception.getErrors();
    assertSame(errors, returnedErrors);
  }
}
