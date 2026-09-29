package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

public class CustomizeExceptionHandlerTest {
  private CustomizeExceptionHandler handler;
  private WebRequest request;

  static class Target {
    @NotBlank(message = "can't be empty")
    String name;
  }

  @BeforeEach
  public void setUp() {
    handler = new CustomizeExceptionHandler();
    request = new ServletWebRequest(new MockHttpServletRequest());
  }

  @Test
  public void should_convert_invalid_request_errors_to_422() {
    DirectFieldBindingResult errors = new DirectFieldBindingResult(new Target(), "target");
    errors.rejectValue("name", "DUPLICATED", "duplicated name");

    ResponseEntity<Object> response =
        handler.handleInvalidRequest(new InvalidRequestException(errors), request);

    assertEquals(422, response.getStatusCodeValue());
    List<FieldErrorResource> fieldErrors = ((ErrorResource) response.getBody()).getFieldErrors();
    assertEquals(1, fieldErrors.size());
    FieldErrorResource fieldError = fieldErrors.get(0);
    assertEquals("target", fieldError.getResource());
    assertEquals("name", fieldError.getField());
    assertEquals("DUPLICATED", fieldError.getCode());
    assertEquals("duplicated name", fieldError.getMessage());
  }

  @Test
  public void should_keep_single_segment_property_path() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<Target>> violations = validator.validate(new Target());

    ErrorResource resource =
        handler.handleConstraintViolation(new ConstraintViolationException(violations), request);

    assertEquals(1, resource.getFieldErrors().size());
    FieldErrorResource error = resource.getFieldErrors().get(0);
    assertEquals("name", error.getField());
    assertEquals("NotBlank", error.getCode());
    assertEquals(Target.class.getName(), error.getResource());
  }

  @Test
  public void should_handle_empty_constraint_violations() {
    ErrorResource resource =
        handler.handleConstraintViolation(
            new ConstraintViolationException(Collections.emptySet()), request);

    assertEquals(0, resource.getFieldErrors().size());
  }
}
