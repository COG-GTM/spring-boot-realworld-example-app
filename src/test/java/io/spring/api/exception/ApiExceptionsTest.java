package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ResponseStatus;

public class ApiExceptionsTest {

  @Test
  public void invalid_request_exception_should_expose_the_binding_errors() {
    Errors errors = new BeanPropertyBindingResult(new Object(), "target");
    errors.reject("invalid", "invalid target");

    InvalidRequestException exception = new InvalidRequestException(errors);

    assertSame(errors, exception.getErrors());
    assertEquals("", exception.getMessage());
    assertTrue(exception instanceof RuntimeException);
  }

  @Test
  public void resource_not_found_exception_should_be_mapped_to_404() {
    ResponseStatus responseStatus =
        ResourceNotFoundException.class.getAnnotation(ResponseStatus.class);

    assertEquals(HttpStatus.NOT_FOUND, responseStatus.value());
    assertTrue(new ResourceNotFoundException() instanceof RuntimeException);
  }

  @Test
  public void no_authorization_exception_should_be_mapped_to_403() {
    ResponseStatus responseStatus =
        NoAuthorizationException.class.getAnnotation(ResponseStatus.class);

    assertEquals(HttpStatus.FORBIDDEN, responseStatus.value());
    assertTrue(new NoAuthorizationException() instanceof RuntimeException);
  }

  @Test
  public void field_error_resource_should_expose_its_parts() {
    FieldErrorResource fieldErrorResource =
        new FieldErrorResource("loginParam", "email", "NotBlank", "can't be empty");

    assertEquals("loginParam", fieldErrorResource.getResource());
    assertEquals("email", fieldErrorResource.getField());
    assertEquals("NotBlank", fieldErrorResource.getCode());
    assertEquals("can't be empty", fieldErrorResource.getMessage());
  }

  @Test
  public void invalid_authentication_exception_should_carry_a_default_message() {
    assertEquals("invalid email or password", new InvalidAuthenticationException().getMessage());
  }
}
