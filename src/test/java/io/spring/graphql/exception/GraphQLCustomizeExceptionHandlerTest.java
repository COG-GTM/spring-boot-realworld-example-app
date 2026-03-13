package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ResultPath;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GraphQLCustomizeExceptionHandlerTest {

  private GraphQLCustomizeExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
  }

  private DataFetcherExceptionHandlerParameters buildParams(Throwable exception) {
    DataFetcherExceptionHandlerParameters params =
        mock(DataFetcherExceptionHandlerParameters.class);
    when(params.getException()).thenReturn(exception);
    when(params.getPath()).thenReturn(ResultPath.rootPath());
    return params;
  }

  @Test
  void onException_withInvalidAuthenticationException_returnsUnauthenticatedError() {
    InvalidAuthenticationException exception = new InvalidAuthenticationException();

    DataFetcherExceptionHandlerResult result = handler.onException(buildParams(exception));

    assertNotNull(result);
    assertEquals(1, result.getErrors().size());
    assertTrue(result.getErrors().get(0).getMessage().contains("invalid email or password"));
  }

  @Test
  void onException_withConstraintViolationException_returnsBadRequestError() {
    ConstraintViolationException exception =
        createConstraintViolationException("field", "must not be blank");

    DataFetcherExceptionHandlerResult result = handler.onException(buildParams(exception));

    assertNotNull(result);
    assertEquals(1, result.getErrors().size());
  }

  @Test
  void onException_withOtherException_delegatesToDefaultHandler() {
    RuntimeException exception = new RuntimeException("some other error");

    DataFetcherExceptionHandlerResult result = handler.onException(buildParams(exception));

    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }

  @Test
  void onException_withConstraintViolation_includesExtensionsWithFieldErrors() {
    ConstraintViolationException exception =
        createConstraintViolationException("email", "must be a valid email");

    DataFetcherExceptionHandlerResult result = handler.onException(buildParams(exception));

    assertNotNull(result);
    assertEquals(1, result.getErrors().size());
    assertNotNull(result.getErrors().get(0).getExtensions());
  }

  @Test
  void getErrorsAsData_withSingleViolation_returnsErrorWithCorrectStructure() {
    ConstraintViolationException exception =
        createConstraintViolationException("username", "can't be empty");

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(exception);

    assertNotNull(error);
    assertEquals("BAD_REQUEST", error.getMessage());
    assertNotNull(error.getErrors());
    assertEquals(1, error.getErrors().size());
    ErrorItem item = error.getErrors().get(0);
    assertEquals("username", item.getKey());
    assertTrue(item.getValue().contains("can't be empty"));
  }

  @Test
  void getErrorsAsData_withMultipleViolations_returnsAllErrors() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolation("username", "can't be empty"));
    violations.add(createMockViolation("email", "must be a valid email"));

    ConstraintViolationException exception = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(exception);

    assertNotNull(error);
    assertEquals("BAD_REQUEST", error.getMessage());
    assertEquals(2, error.getErrors().size());
  }

  @Test
  void getErrorsAsData_withMultipleViolationsOnSameField_groupsThem() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolation("email", "can't be empty"));
    violations.add(createMockViolation("email", "must be a valid email"));

    ConstraintViolationException exception = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(exception);

    assertNotNull(error);
    assertEquals("BAD_REQUEST", error.getMessage());
    assertEquals(1, error.getErrors().size());
    ErrorItem item = error.getErrors().get(0);
    assertEquals("email", item.getKey());
    assertEquals(2, item.getValue().size());
  }

  @Test
  void onException_withConstraintViolationHavingNestedPath_extractsCorrectParam() {
    ConstraintViolation<?> violation = createMockViolationWithPath("root.sub.field", "bad value");
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);

    ConstraintViolationException exception = new ConstraintViolationException(violations);

    DataFetcherExceptionHandlerResult result = handler.onException(buildParams(exception));

    assertNotNull(result);
    assertEquals(1, result.getErrors().size());
  }

  @Test
  void getErrorsAsData_withNestedPropertyPath_extractsCorrectField() {
    ConstraintViolation<?> violation = createMockViolationWithPath("root.sub.field", "bad value");
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);

    ConstraintViolationException exception = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(exception);

    assertNotNull(error);
    assertEquals(1, error.getErrors().size());
    assertEquals("field", error.getErrors().get(0).getKey());
  }

  @Test
  void getErrorsAsData_withSingleSegmentPath_returnsFullPath() {
    ConstraintViolation<?> violation = createMockViolationWithPath("simplefield", "bad value");
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);

    ConstraintViolationException exception = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(exception);

    assertNotNull(error);
    assertEquals(1, error.getErrors().size());
    assertEquals("simplefield", error.getErrors().get(0).getKey());
  }

  private ConstraintViolationException createConstraintViolationException(
      String field, String message) {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolation(field, message));
    return new ConstraintViolationException(violations);
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
