package io.spring.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

public class InvalidRequestExceptionTest {

  @Test
  public void should_keep_errors_and_empty_message() {
    Errors errors = new BeanPropertyBindingResult(new Object(), "target");
    errors.rejectValue(null, "code", "the message");

    InvalidRequestException exception = new InvalidRequestException(errors);

    assertThat(exception.getMessage()).isEmpty();
    assertThat(exception.getErrors()).isSameAs(errors);
    assertThat(exception.getErrors().getAllErrors()).hasSize(1);
  }
}
