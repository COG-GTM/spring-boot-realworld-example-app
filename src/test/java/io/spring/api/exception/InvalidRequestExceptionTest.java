package io.spring.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

public class InvalidRequestExceptionTest {

  @Test
  public void should_carry_the_binding_errors_with_an_empty_message() {
    Errors errors = new BeanPropertyBindingResult(new Object(), "article");
    errors.rejectValue(null, "EMPTY", "can't be empty");

    InvalidRequestException exception = new InvalidRequestException(errors);

    assertThat(exception.getErrors()).isSameAs(errors);
    assertThat(exception.getMessage()).isEmpty();
  }
}
