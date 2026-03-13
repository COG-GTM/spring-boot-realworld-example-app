package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

class CustomizeExceptionHandlerTest {

  private CustomizeExceptionHandler handler;
  private WebRequest webRequest;

  @BeforeEach
  void setUp() {
    handler = new CustomizeExceptionHandler();
    webRequest = mock(WebRequest.class);
  }

  @Test
  void handleInvalidRequest_returnsUnprocessableEntity() {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult("target", "target");
    bindingResult.addError(new FieldError("target", "username", "can't be empty"));

    InvalidRequestException exception = new InvalidRequestException(bindingResult);

    ResponseEntity<Object> response = handler.handleInvalidRequest(exception, webRequest);

    assertNotNull(response);
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  void handleInvalidRequest_withMultipleFieldErrors_returnsAllErrors() {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult("target", "target");
    bindingResult.addError(new FieldError("target", "username", "can't be empty"));
    bindingResult.addError(new FieldError("target", "email", "must be a valid email"));

    InvalidRequestException exception = new InvalidRequestException(bindingResult);

    ResponseEntity<Object> response = handler.handleInvalidRequest(exception, webRequest);

    assertNotNull(response);
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody() instanceof ErrorResource);
    ErrorResource errorResource = (ErrorResource) response.getBody();
    assertEquals(2, errorResource.getFieldErrors().size());
  }

  @Test
  void handleInvalidAuthentication_returnsUnprocessableEntityWithMessage() {
    InvalidAuthenticationException exception = new InvalidAuthenticationException();

    ResponseEntity<Object> response = handler.handleInvalidAuthentication(exception, webRequest);

    assertNotNull(response);
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  void handleMethodArgumentNotValid_returnsUnprocessableEntity() throws Exception {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult("target", "target");
    bindingResult.addError(new FieldError("target", "title", "can't be empty"));

    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(null, bindingResult);

    ResponseEntity<Object> response =
        handler.handleMethodArgumentNotValid(
            exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

    assertNotNull(response);
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody() instanceof ErrorResource);
  }

  @Test
  void handleMethodArgumentNotValid_withMultipleErrors_returnsAllErrors() throws Exception {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult("target", "target");
    bindingResult.addError(new FieldError("target", "title", "can't be empty"));
    bindingResult.addError(new FieldError("target", "body", "can't be empty"));

    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(null, bindingResult);

    ResponseEntity<Object> response =
        handler.handleMethodArgumentNotValid(
            exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

    assertNotNull(response);
    assertTrue(response.getBody() instanceof ErrorResource);
    ErrorResource errorResource = (ErrorResource) response.getBody();
    assertEquals(2, errorResource.getFieldErrors().size());
  }

  @Test
  void handleConstraintViolation_returnsErrorResource() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolation("email", "must be a valid email"));

    ConstraintViolationException exception = new ConstraintViolationException(violations);

    ErrorResource result = handler.handleConstraintViolation(exception, webRequest);

    assertNotNull(result);
    assertNotNull(result.getFieldErrors());
    assertEquals(1, result.getFieldErrors().size());
    assertEquals("email", result.getFieldErrors().get(0).getField());
    assertEquals("must be a valid email", result.getFieldErrors().get(0).getMessage());
  }

  @Test
  void handleConstraintViolation_withMultipleViolations_returnsAllErrors() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolation("email", "must be a valid email"));
    violations.add(createMockViolation("username", "can't be empty"));

    ConstraintViolationException exception = new ConstraintViolationException(violations);

    ErrorResource result = handler.handleConstraintViolation(exception, webRequest);

    assertNotNull(result);
    assertEquals(2, result.getFieldErrors().size());
  }

  @Test
  void handleConstraintViolation_withNestedPropertyPath_extractsCorrectParam() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolationWithPath("root.sub.fieldName", "bad value"));

    ConstraintViolationException exception = new ConstraintViolationException(violations);

    ErrorResource result = handler.handleConstraintViolation(exception, webRequest);

    assertNotNull(result);
    assertEquals(1, result.getFieldErrors().size());
    assertEquals("fieldName", result.getFieldErrors().get(0).getField());
  }

  @Test
  void handleConstraintViolation_withSingleSegmentPath_returnsFullPath() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolationWithPath("simplefield", "bad value"));

    ConstraintViolationException exception = new ConstraintViolationException(violations);

    ErrorResource result = handler.handleConstraintViolation(exception, webRequest);

    assertNotNull(result);
    assertEquals(1, result.getFieldErrors().size());
    assertEquals("simplefield", result.getFieldErrors().get(0).getField());
  }

  private ConstraintViolation<?> createMockViolation(String field, String message) {
    return createMockViolationWithPath(field, message);
  }

  @SuppressWarnings("unchecked")
  private ConstraintViolation<?> createMockViolationWithPath(String propertyPath, String message) {
    ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    when(path.toString()).thenReturn(propertyPath);
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn(message);
    when(violation.getRootBeanClass()).thenReturn(Object.class);

    ConstraintDescriptor<Annotation> descriptor = mock(ConstraintDescriptor.class);
    Annotation annotation = mock(Annotation.class);
    when(annotation.annotationType()).thenReturn((Class) Override.class);
    when(descriptor.getAnnotation()).thenReturn(annotation);
    doReturn(descriptor).when(violation).getConstraintDescriptor();

    return violation;
  }
}
