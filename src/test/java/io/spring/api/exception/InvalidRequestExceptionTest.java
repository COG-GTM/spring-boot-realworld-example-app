package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

class InvalidRequestExceptionTest {

  @Test
  void constructor_setsErrors() {
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult("target", "target");
    errors.addError(new FieldError("target", "field", "message"));

    InvalidRequestException exception = new InvalidRequestException(errors);

    assertNotNull(exception.getErrors());
    assertEquals(errors, exception.getErrors());
  }

  @Test
  void getErrors_returnsBindingResult() {
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult("target", "target");

    InvalidRequestException exception = new InvalidRequestException(errors);

    Errors result = exception.getErrors();
    assertNotNull(result);
    assertFalse(result.hasErrors());
  }

  @Test
  void constructor_setsEmptyMessage() {
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult("target", "target");

    InvalidRequestException exception = new InvalidRequestException(errors);

    assertEquals("", exception.getMessage());
  }

  @Test
  void isRuntimeException() {
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult("target", "target");

    InvalidRequestException exception = new InvalidRequestException(errors);

    assertTrue(exception instanceof RuntimeException);
  }

  @Test
  void getErrors_withFieldErrors_returnsCorrectErrors() {
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult("target", "target");
    errors.addError(new FieldError("target", "username", "can't be empty"));
    errors.addError(new FieldError("target", "email", "must be a valid email"));

    InvalidRequestException exception = new InvalidRequestException(errors);

    assertTrue(exception.getErrors().hasErrors());
    assertEquals(2, exception.getErrors().getFieldErrors().size());
  }
}
