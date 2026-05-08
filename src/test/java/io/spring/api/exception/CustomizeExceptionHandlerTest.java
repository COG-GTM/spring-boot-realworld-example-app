package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

public class CustomizeExceptionHandlerTest {

  private final CustomizeExceptionHandler handler = new CustomizeExceptionHandler();

  @Test
  public void should_handle_invalid_request_exception() {
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new Object(), "article");
    bindingResult.addError(new FieldError("article", "title", "can't be empty"));
    InvalidRequestException ex = new InvalidRequestException(bindingResult);
    WebRequest request = mock(WebRequest.class);

    ResponseEntity<Object> response = handler.handleInvalidRequest(ex, request);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody() instanceof ErrorResource);
    ErrorResource body = (ErrorResource) response.getBody();
    assertEquals(1, body.getFieldErrors().size());
    assertEquals("title", body.getFieldErrors().get(0).getField());
  }

  @Test
  public void should_handle_invalid_authentication_exception() {
    InvalidAuthenticationException ex = new InvalidAuthenticationException();
    WebRequest request = mock(WebRequest.class);

    ResponseEntity<Object> response = handler.handleInvalidAuthentication(ex, request);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    assertTrue(response.getBody() instanceof Map);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertEquals(ex.getMessage(), body.get("message"));
  }

  @Test
  public void should_handle_constraint_violation_exception() {
    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    when(path.toString()).thenReturn("createArticle.params.title");
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn("must not be blank");
    when(violation.getRootBeanClass()).thenAnswer(i -> Object.class);
    ConstraintDescriptor<?> descriptor = mock(ConstraintDescriptor.class);
    java.lang.annotation.Annotation annotation =
        new java.lang.annotation.Annotation() {
          @Override
          public Class<? extends java.lang.annotation.Annotation> annotationType() {
            return Override.class;
          }
        };
    when(descriptor.getAnnotation()).thenAnswer(i -> annotation);
    when(violation.getConstraintDescriptor()).thenAnswer(i -> descriptor);

    Set<ConstraintViolation<?>> violations = Collections.singleton(violation);
    ConstraintViolationException ex = new ConstraintViolationException(violations);
    WebRequest request = mock(WebRequest.class);

    ErrorResource resource = handler.handleConstraintViolation(ex, request);

    assertNotNull(resource);
    assertEquals(1, resource.getFieldErrors().size());
    assertEquals("title", resource.getFieldErrors().get(0).getField());
    assertEquals("must not be blank", resource.getFieldErrors().get(0).getMessage());
  }

  @Test
  public void should_format_method_argument_not_valid_exception() throws Exception {
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new HashMap<>(), "article");
    bindingResult.addError(new FieldError("article", "title", "can't be empty"));
    bindingResult.addError(new FieldError("article", "body", "can't be empty"));

    java.lang.reflect.Method method = Sample.class.getDeclaredMethod("doIt", String.class);
    org.springframework.core.MethodParameter parameter =
        new org.springframework.core.MethodParameter(method, 0);

    MethodArgumentNotValidException ex =
        new MethodArgumentNotValidException(parameter, bindingResult);
    HttpHeaders headers = new HttpHeaders();
    WebRequest request = mock(WebRequest.class);

    ResponseEntity<Object> response =
        invokeHandleMethodArgumentNotValid(ex, headers, HttpStatus.BAD_REQUEST, request);

    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    ErrorResource body = (ErrorResource) response.getBody();
    assertNotNull(body);
    assertEquals(2, body.getFieldErrors().size());
    assertTrue(
        Arrays.asList(
                body.getFieldErrors().get(0).getField(), body.getFieldErrors().get(1).getField())
            .containsAll(Arrays.asList("title", "body")));
  }

  @SuppressWarnings("unchecked")
  private ResponseEntity<Object> invokeHandleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatus status,
      WebRequest request)
      throws Exception {
    java.lang.reflect.Method m =
        CustomizeExceptionHandler.class.getDeclaredMethod(
            "handleMethodArgumentNotValid",
            MethodArgumentNotValidException.class,
            HttpHeaders.class,
            HttpStatus.class,
            WebRequest.class);
    m.setAccessible(true);
    return (ResponseEntity<Object>) m.invoke(handler, ex, headers, status, request);
  }

  static class Sample {
    public void doIt(String value) {}
  }
}
