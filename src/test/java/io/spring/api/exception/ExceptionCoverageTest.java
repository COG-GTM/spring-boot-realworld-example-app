package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

public class ExceptionCoverageTest {

  @Test
  public void should_create_invalid_request_exception() {
    Errors errors = new BeanPropertyBindingResult(new Object(), "test");
    errors.rejectValue(null, "error", "test error");

    InvalidRequestException exception = new InvalidRequestException(errors);

    assertNotNull(exception);
    assertNotNull(exception.getErrors());
    assertEquals(errors, exception.getErrors());
  }

  @Test
  public void should_create_invalid_authentication_exception() {
    InvalidAuthenticationException exception = new InvalidAuthenticationException();

    assertNotNull(exception);
    assertEquals("invalid email or password", exception.getMessage());
  }

  @Test
  public void should_create_resource_not_found_exception() {
    ResourceNotFoundException exception = new ResourceNotFoundException();
    assertNotNull(exception);
  }

  @Test
  public void should_create_no_authorization_exception() {
    NoAuthorizationException exception = new NoAuthorizationException();
    assertNotNull(exception);
  }

  @Test
  public void should_create_field_error_resource() {
    FieldErrorResource resource =
        new FieldErrorResource("object", "field", "NotBlank", "can't be empty");

    assertEquals("object", resource.getResource());
    assertEquals("field", resource.getField());
    assertEquals("NotBlank", resource.getCode());
    assertEquals("can't be empty", resource.getMessage());
  }

  @Test
  public void should_create_error_resource() {
    FieldErrorResource fieldError =
        new FieldErrorResource("object", "field", "NotBlank", "can't be empty");
    List<FieldErrorResource> fieldErrors = Arrays.asList(fieldError);

    ErrorResource errorResource = new ErrorResource(fieldErrors);

    assertNotNull(errorResource);
    assertEquals(1, errorResource.getFieldErrors().size());
    assertEquals("field", errorResource.getFieldErrors().get(0).getField());
  }

  @Test
  public void should_create_error_resource_with_multiple_errors() {
    FieldErrorResource error1 =
        new FieldErrorResource("obj", "email", "NotBlank", "can't be empty");
    FieldErrorResource error2 =
        new FieldErrorResource("obj", "username", "NotBlank", "can't be empty");

    ErrorResource errorResource = new ErrorResource(Arrays.asList(error1, error2));

    assertEquals(2, errorResource.getFieldErrors().size());
  }
}
