package io.spring.api.exception;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

public class InvalidRequestExceptionTest {

  @Test
  public void should_create_exception_with_errors() {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult("target", "objectName");
    bindingResult.addError(new FieldError("objectName", "email", "can't be empty"));

    InvalidRequestException exception = new InvalidRequestException(bindingResult);

    assertThat(exception, notNullValue());
    assertThat(exception.getErrors(), is(notNullValue()));
    assertThat(exception.getErrors().hasErrors(), is(true));
    assertThat(exception.getErrors().getErrorCount(), is(1));
  }

  @Test
  public void should_return_errors_object() {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult("target", "objectName");
    bindingResult.addError(new FieldError("objectName", "username", "can't be empty"));
    bindingResult.addError(new FieldError("objectName", "email", "invalid format"));

    InvalidRequestException exception = new InvalidRequestException(bindingResult);

    Errors errors = exception.getErrors();
    assertThat(errors.getErrorCount(), is(2));
    assertThat(errors.getFieldErrors().get(0).getField(), is("username"));
    assertThat(errors.getFieldErrors().get(1).getField(), is("email"));
  }

  @Test
  public void should_extend_runtime_exception() {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult("target", "objectName");

    InvalidRequestException exception = new InvalidRequestException(bindingResult);

    assertThat(exception instanceof RuntimeException, is(true));
    assertThat(exception.getMessage(), is(""));
  }

  @Test
  public void should_create_exception_with_no_field_errors() {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult("target", "objectName");

    InvalidRequestException exception = new InvalidRequestException(bindingResult);

    assertThat(exception.getErrors().hasErrors(), is(false));
    assertThat(exception.getErrors().getErrorCount(), is(0));
  }
}
