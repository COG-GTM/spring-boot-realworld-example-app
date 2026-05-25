package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;

@ExtendWith(MockitoExtension.class)
class CustomizeExceptionHandlerTest {

  private CustomizeExceptionHandler handler;
  private WebRequest webRequest;

  @BeforeEach
  void setUp() {
    handler = new CustomizeExceptionHandler();
    webRequest = mock(WebRequest.class);
  }

  @Test
  void handleInvalidRequest_with_field_errors() {
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new Object(), "article");
    errors.addError(new FieldError("article", "title", "can't be empty"));
    errors.addError(new FieldError("article", "body", "can't be empty"));

    InvalidRequestException ex = new InvalidRequestException(errors);

    ResponseEntity<Object> response = handler.handleInvalidRequest(ex, webRequest);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertNotNull(response.getBody());
    ErrorResource errorResource = (ErrorResource) response.getBody();
    assertEquals(2, errorResource.getFieldErrors().size());
  }

  @Test
  @SuppressWarnings("unchecked")
  void handleInvalidAuthentication_returns_422_with_message() {
    InvalidAuthenticationException ex = new InvalidAuthenticationException();

    ResponseEntity<Object> response = handler.handleInvalidAuthentication(ex, webRequest);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertNotNull(body);
    assertEquals("invalid email or password", body.get("message"));
  }

  @Test
  void handleMethodArgumentNotValid_returns_422() throws Exception {
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new Object(), "user");
    bindingResult.addError(new FieldError("user", "email", "must not be blank"));

    MethodArgumentNotValidException ex =
        new MethodArgumentNotValidException(null, bindingResult);

    ResponseEntity<Object> response =
        handler.handleMethodArgumentNotValid(ex, null, HttpStatus.BAD_REQUEST, webRequest);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertNotNull(response.getBody());
    ErrorResource errorResource = (ErrorResource) response.getBody();
    assertEquals(1, errorResource.getFieldErrors().size());
    assertEquals("email", errorResource.getFieldErrors().get(0).getField());
  }

  @Test
  @SuppressWarnings("unchecked")
  void handleConstraintViolation_with_multiple_violations() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();

    ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
    Path path1 = mock(Path.class);
    when(path1.toString()).thenReturn("method.arg.title");
    when(violation1.getPropertyPath()).thenReturn(path1);
    when(violation1.getMessage()).thenReturn("must not be blank");
    when(violation1.getRootBeanClass()).thenReturn((Class) Object.class);
    ConstraintDescriptor<?> descriptor1 = mock(ConstraintDescriptor.class);
    Annotation annotation1 = mock(Annotation.class);
    when(annotation1.annotationType()).thenReturn((Class) Override.class);
    when(descriptor1.getAnnotation()).thenReturn(annotation1);
    doReturn(descriptor1).when(violation1).getConstraintDescriptor();

    ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);
    Path path2 = mock(Path.class);
    when(path2.toString()).thenReturn("method.arg.body");
    when(violation2.getPropertyPath()).thenReturn(path2);
    when(violation2.getMessage()).thenReturn("must not be empty");
    when(violation2.getRootBeanClass()).thenReturn((Class) Object.class);
    ConstraintDescriptor<?> descriptor2 = mock(ConstraintDescriptor.class);
    Annotation annotation2 = mock(Annotation.class);
    when(annotation2.annotationType()).thenReturn((Class) Override.class);
    when(descriptor2.getAnnotation()).thenReturn(annotation2);
    doReturn(descriptor2).when(violation2).getConstraintDescriptor();

    violations.add(violation1);
    violations.add(violation2);

    ConstraintViolationException ex = new ConstraintViolationException(violations);

    ErrorResource result = handler.handleConstraintViolation(ex, webRequest);

    assertNotNull(result);
    assertEquals(2, result.getFieldErrors().size());
  }
}
