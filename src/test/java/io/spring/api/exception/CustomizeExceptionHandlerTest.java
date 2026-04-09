package io.spring.api.exception;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

public class CustomizeExceptionHandlerTest {

  private CustomizeExceptionHandler handler;
  private WebRequest webRequest;

  @BeforeEach
  public void setUp() {
    handler = new CustomizeExceptionHandler();
    webRequest = mock(WebRequest.class);
  }

  @Test
  public void should_handle_invalid_request_exception() {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult("target", "objectName");
    bindingResult.addError(new FieldError("objectName", "email", "can't be empty"));

    InvalidRequestException exception = new InvalidRequestException(bindingResult);

    ResponseEntity<Object> response = handler.handleInvalidRequest(exception, webRequest);

    assertThat(response.getStatusCode(), is(HttpStatus.UNPROCESSABLE_ENTITY));
    assertThat(response.getHeaders().getContentType().toString(), is("application/json"));
  }

  @Test
  public void should_handle_invalid_request_exception_with_multiple_errors() {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult("target", "objectName");
    bindingResult.addError(new FieldError("objectName", "email", "can't be empty"));
    bindingResult.addError(new FieldError("objectName", "username", "can't be empty"));

    InvalidRequestException exception = new InvalidRequestException(bindingResult);

    ResponseEntity<Object> response = handler.handleInvalidRequest(exception, webRequest);

    assertThat(response.getStatusCode(), is(HttpStatus.UNPROCESSABLE_ENTITY));
  }

  @Test
  public void should_handle_invalid_authentication_exception() {
    InvalidAuthenticationException exception = new InvalidAuthenticationException();

    ResponseEntity<Object> response = handler.handleInvalidAuthentication(exception, webRequest);

    assertThat(response.getStatusCode(), is(HttpStatus.UNPROCESSABLE_ENTITY));
  }

  @Test
  public void should_handle_constraint_violation_exception() {
    ConstraintViolation<?> violation =
        mockViolation("SomeBean", "arg0.field.email", "NotBlank", "must not be blank");
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);
    ConstraintViolationException exception =
        new ConstraintViolationException("Validation failed", violations);

    ErrorResource errorResource = handler.handleConstraintViolation(exception, webRequest);

    assertThat(errorResource.getFieldErrors(), hasSize(1));
    assertThat(errorResource.getFieldErrors().get(0).getField(), is("email"));
    assertThat(errorResource.getFieldErrors().get(0).getMessage(), is("must not be blank"));
  }

  @Test
  public void should_handle_constraint_violation_with_single_segment_path() {
    ConstraintViolation<?> violation =
        mockViolation("SomeBean", "fieldName", "NotBlank", "must not be blank");
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);
    ConstraintViolationException exception =
        new ConstraintViolationException("Validation failed", violations);

    ErrorResource errorResource = handler.handleConstraintViolation(exception, webRequest);

    assertThat(errorResource.getFieldErrors(), hasSize(1));
    assertThat(errorResource.getFieldErrors().get(0).getField(), is("fieldName"));
  }

  @Test
  public void should_handle_constraint_violation_with_multi_segment_path() {
    ConstraintViolation<?> violation =
        mockViolation("SomeBean", "arg0.field.nested.deep", "NotBlank", "must not be blank");
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);
    ConstraintViolationException exception =
        new ConstraintViolationException("Validation failed", violations);

    ErrorResource errorResource = handler.handleConstraintViolation(exception, webRequest);

    assertThat(errorResource.getFieldErrors(), hasSize(1));
    assertThat(errorResource.getFieldErrors().get(0).getField(), is("nested.deep"));
  }

  @Test
  public void should_handle_method_argument_not_valid_exception() throws Exception {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult("target", "objectName");
    bindingResult.addError(new FieldError("objectName", "title", "can't be empty"));
    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(null, bindingResult);

    ResponseEntity<Object> response =
        handler.handleMethodArgumentNotValid(
            exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

    assertThat(response.getStatusCode(), is(HttpStatus.UNPROCESSABLE_ENTITY));
  }

  @SuppressWarnings("unchecked")
  private ConstraintViolation<?> mockViolation(
      String rootBeanClassName, String propertyPath, String annotationSimpleName, String message) {
    ConstraintViolation violation = mock(ConstraintViolation.class);

    doReturn(String.class).when(violation).getRootBeanClass();

    Path path = mock(Path.class);
    when(path.toString()).thenReturn(propertyPath);
    doReturn(path).when(violation).getPropertyPath();

    ConstraintDescriptor descriptor = mock(ConstraintDescriptor.class);
    javax.validation.constraints.NotBlank mockAnnotation =
        mock(javax.validation.constraints.NotBlank.class);
    doReturn(mockAnnotation).when(descriptor).getAnnotation();
    doReturn(javax.validation.constraints.NotBlank.class).when(mockAnnotation).annotationType();
    doReturn(descriptor).when(violation).getConstraintDescriptor();

    doReturn(message).when(violation).getMessage();

    return violation;
  }
}
