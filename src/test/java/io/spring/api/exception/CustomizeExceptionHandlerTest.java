package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.context.request.WebRequest;

public class CustomizeExceptionHandlerTest {

  private CustomizeExceptionHandler handler;
  private WebRequest webRequest;

  @BeforeEach
  void setUp() {
    handler = new CustomizeExceptionHandler();
    webRequest = Mockito.mock(WebRequest.class);
  }

  @Test
  void should_handle_invalid_request_exception() {
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new Object(), "user");
    errors.addError(new FieldError("user", "email", "must not be blank"));
    InvalidRequestException exception = new InvalidRequestException(errors);

    ResponseEntity<Object> response = handler.handleInvalidRequest(exception, webRequest);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  void should_handle_invalid_authentication_exception() {
    InvalidAuthenticationException exception = new InvalidAuthenticationException();

    ResponseEntity<Object> response =
        handler.handleInvalidAuthentication(exception, webRequest);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
  }

  @Test
  @SuppressWarnings("unchecked")
  void should_handle_constraint_violation_exception() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    ConstraintViolation violation = Mockito.mock(ConstraintViolation.class);
    Path path = Mockito.mock(Path.class);
    Mockito.when(path.toString()).thenReturn("createUser.param.email");
    Mockito.when(violation.getPropertyPath()).thenReturn(path);
    Mockito.when(violation.getMessage()).thenReturn("must not be blank");
    Mockito.when(violation.getRootBeanClass()).thenReturn(Object.class);

    Annotation annotation = Mockito.mock(Annotation.class);
    Mockito.when(annotation.annotationType())
        .thenReturn((Class) javax.validation.constraints.NotBlank.class);
    ConstraintDescriptor descriptor = Mockito.mock(ConstraintDescriptor.class);
    Mockito.when(descriptor.getAnnotation()).thenReturn(annotation);
    Mockito.when(violation.getConstraintDescriptor()).thenReturn(descriptor);

    violations.add(violation);
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    ErrorResource result = handler.handleConstraintViolation(exception, webRequest);

    assertNotNull(result);
    assertFalse(result.getFieldErrors().isEmpty());
  }
}
