package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

public class InvalidRequestExceptionTest {

  @Test
  public void should_expose_errors_passed_to_constructor() {
    Errors errors = new BeanPropertyBindingResult(new Object(), "param");
    errors.rejectValue(null, "INVALID", "invalid");

    InvalidRequestException exception = new InvalidRequestException(errors);

    assertSame(errors, exception.getErrors());
    assertEquals("", exception.getMessage());
  }

  @Test
  public void should_mark_errors_field_as_transient() throws Exception {
    int modifiers = InvalidRequestException.class.getDeclaredField("errors").getModifiers();

    assertTrue(Modifier.isTransient(modifiers));
    assertTrue(Modifier.isFinal(modifiers));
  }
}
