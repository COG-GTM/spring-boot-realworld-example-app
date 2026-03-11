package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.hibernate.validator.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

public class CustomizeExceptionHandlerTest {

  private CustomizeExceptionHandler handler;
  private WebRequest webRequest;

  @BeforeEach
  public void setUp() {
    handler = new CustomizeExceptionHandler();
    webRequest = mock(WebRequest.class);
  }

  @Test
  public void should_handle_invalid_request_exception_with_single_field_error() {
    BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "user");
    bindingResult.addError(new FieldError("user", "email", "must not be blank"));

    InvalidRequestException exception = new InvalidRequestException(bindingResult);

    ResponseEntity<Object> response = handler.handleInvalidRequest(exception, webRequest);

    assertNotNull(response);
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    assertTrue(response.getBody() instanceof ErrorResource);

    ErrorResource errorResource = (ErrorResource) response.getBody();
    assertEquals(1, errorResource.getFieldErrors().size());
    assertEquals("email", errorResource.getFieldErrors().get(0).getField());
    assertEquals("must not be blank", errorResource.getFieldErrors().get(0).getMessage());
  }

  @Test
  public void should_handle_invalid_request_exception_with_multiple_field_errors() {
    BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "user");
    bindingResult.addError(new FieldError("user", "email", "must not be blank"));
    bindingResult.addError(new FieldError("user", "username", "size must be between 1 and 50"));

    InvalidRequestException exception = new InvalidRequestException(bindingResult);

    ResponseEntity<Object> response = handler.handleInvalidRequest(exception, webRequest);

    assertNotNull(response);
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());

    ErrorResource errorResource = (ErrorResource) response.getBody();
    assertEquals(2, errorResource.getFieldErrors().size());
  }

  @Test
  public void should_handle_invalid_request_exception_with_empty_errors() {
    BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "user");

    InvalidRequestException exception = new InvalidRequestException(bindingResult);

    ResponseEntity<Object> response = handler.handleInvalidRequest(exception, webRequest);

    assertNotNull(response);
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());

    ErrorResource errorResource = (ErrorResource) response.getBody();
    assertTrue(errorResource.getFieldErrors().isEmpty());
  }

  @Test
  public void should_handle_invalid_authentication_exception() {
    InvalidAuthenticationException exception = new InvalidAuthenticationException();

    ResponseEntity<Object> response = handler.handleInvalidAuthentication(exception, webRequest);

    assertNotNull(response);
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertTrue(response.getBody() instanceof Map);

    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertEquals("invalid email or password", body.get("message"));
  }

  @Test
  public void should_handle_method_argument_not_valid_exception() throws NoSuchMethodException {
    BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "user");
    bindingResult.addError(new FieldError("user", "email", "must be a valid email"));

    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(null, bindingResult);

    ResponseEntity<Object> response =
        handler.handleMethodArgumentNotValid(
            exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

    assertNotNull(response);
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertTrue(response.getBody() instanceof ErrorResource);

    ErrorResource errorResource = (ErrorResource) response.getBody();
    assertEquals(1, errorResource.getFieldErrors().size());
    assertEquals("email", errorResource.getFieldErrors().get(0).getField());
    assertEquals("must be a valid email", errorResource.getFieldErrors().get(0).getMessage());
  }

  @Test
  public void should_handle_method_argument_not_valid_exception_with_multiple_errors()
      throws NoSuchMethodException {
    BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "user");
    bindingResult.addError(new FieldError("user", "email", "must be a valid email"));
    bindingResult.addError(new FieldError("user", "password", "must not be blank"));

    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(null, bindingResult);

    ResponseEntity<Object> response =
        handler.handleMethodArgumentNotValid(
            exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

    assertNotNull(response);
    ErrorResource errorResource = (ErrorResource) response.getBody();
    assertEquals(2, errorResource.getFieldErrors().size());
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void should_handle_constraint_violation_exception() {
    ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    ConstraintDescriptor descriptor = mock(ConstraintDescriptor.class);
    NotBlank annotation = mock(NotBlank.class);

    doReturn(Object.class).when(violation).getRootBeanClass();
    when(violation.getPropertyPath()).thenReturn(path);
    when(path.toString()).thenReturn("method.param.field");
    doReturn(descriptor).when(violation).getConstraintDescriptor();
    when(descriptor.getAnnotation()).thenReturn(annotation);
    when(annotation.annotationType()).thenReturn((Class) NotBlank.class);
    when(violation.getMessage()).thenReturn("must not be blank");

    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);

    ConstraintViolationException exception = new ConstraintViolationException(violations);

    ErrorResource errorResource = handler.handleConstraintViolation(exception, webRequest);

    assertNotNull(errorResource);
    assertEquals(1, errorResource.getFieldErrors().size());
    assertEquals("field", errorResource.getFieldErrors().get(0).getField());
    assertEquals("must not be blank", errorResource.getFieldErrors().get(0).getMessage());
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void should_handle_constraint_violation_exception_with_simple_path() {
    ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    ConstraintDescriptor descriptor = mock(ConstraintDescriptor.class);
    NotBlank annotation = mock(NotBlank.class);

    doReturn(Object.class).when(violation).getRootBeanClass();
    when(violation.getPropertyPath()).thenReturn(path);
    when(path.toString()).thenReturn("field");
    doReturn(descriptor).when(violation).getConstraintDescriptor();
    when(descriptor.getAnnotation()).thenReturn(annotation);
    when(annotation.annotationType()).thenReturn((Class) NotBlank.class);
    when(violation.getMessage()).thenReturn("must not be blank");

    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);

    ConstraintViolationException exception = new ConstraintViolationException(violations);

    ErrorResource errorResource = handler.handleConstraintViolation(exception, webRequest);

    assertNotNull(errorResource);
    assertEquals(1, errorResource.getFieldErrors().size());
    assertEquals("field", errorResource.getFieldErrors().get(0).getField());
  }

  @Test
  public void should_handle_constraint_violation_exception_with_empty_violations() {
    Set<ConstraintViolation<?>> violations = Collections.emptySet();

    ConstraintViolationException exception = new ConstraintViolationException(violations);

    ErrorResource errorResource = handler.handleConstraintViolation(exception, webRequest);

    assertNotNull(errorResource);
    assertTrue(errorResource.getFieldErrors().isEmpty());
  }

  @Test
  public void should_set_json_content_type_header_for_invalid_request() {
    BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "user");
    bindingResult.addError(new FieldError("user", "email", "must not be blank"));

    InvalidRequestException exception = new InvalidRequestException(bindingResult);

    ResponseEntity<Object> response = handler.handleInvalidRequest(exception, webRequest);

    assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
  }

  @Test
  public void should_preserve_field_error_code() {
    BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "user");
    FieldError fieldError = new FieldError("user", "email", null, false, new String[] {"NotBlank"}, null, "must not be blank");
    bindingResult.addError(fieldError);

    InvalidRequestException exception = new InvalidRequestException(bindingResult);

    ResponseEntity<Object> response = handler.handleInvalidRequest(exception, webRequest);

    ErrorResource errorResource = (ErrorResource) response.getBody();
    assertEquals("NotBlank", errorResource.getFieldErrors().get(0).getCode());
  }
}
