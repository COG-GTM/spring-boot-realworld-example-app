package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ResultPath;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import java.util.HashSet;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GraphQLCustomizeExceptionHandlerTest {

  private GraphQLCustomizeExceptionHandler handler;

  @Mock private DataFetcherExceptionHandlerParameters handlerParameters;

  @BeforeEach
  void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
  }

  @Test
  void onException_withInvalidAuthenticationException_returnsUnauthenticatedError() {
    InvalidAuthenticationException exception = new InvalidAuthenticationException();
    when(handlerParameters.getException()).thenReturn(exception);
    when(handlerParameters.getPath()).thenReturn(ResultPath.rootPath());

    DataFetcherExceptionHandlerResult result = handler.onException(handlerParameters);

    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
    assertEquals(1, result.getErrors().size());
  }

  @Test
  void onException_withConstraintViolationException_returnsBadRequestError() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    ConstraintViolation<?> violation = createMockViolation("email", "must be a valid email");
    violations.add(violation);

    ConstraintViolationException cve = new ConstraintViolationException(violations);
    when(handlerParameters.getException()).thenReturn(cve);
    when(handlerParameters.getPath()).thenReturn(ResultPath.rootPath());

    DataFetcherExceptionHandlerResult result = handler.onException(handlerParameters);

    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }

  @Test
  void onException_withOtherException_delegatesToDefaultHandler() {
    RuntimeException exception = new RuntimeException("Some error");
    when(handlerParameters.getException()).thenReturn(exception);
    when(handlerParameters.getPath()).thenReturn(ResultPath.rootPath());

    DataFetcherExceptionHandlerResult result = handler.onException(handlerParameters);

    assertNotNull(result);
  }

  @Test
  void getErrorsAsData_withSingleViolation_returnsError() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    ConstraintViolation<?> violation = createMockViolation("email", "must be a valid email");
    violations.add(violation);

    ConstraintViolationException cve = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertNotNull(error);
    assertEquals("BAD_REQUEST", error.getMessage());
    assertNotNull(error.getErrors());
    assertFalse(error.getErrors().isEmpty());
  }

  @Test
  void getErrorsAsData_withMultipleViolations_returnsAllErrors() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolation("email", "must be a valid email"));
    violations.add(createMockViolation("username", "must not be blank"));

    ConstraintViolationException cve = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertNotNull(error);
    assertEquals("BAD_REQUEST", error.getMessage());
    assertNotNull(error.getErrors());
    assertEquals(2, error.getErrors().size());
  }

  @Test
  void getErrorsAsData_withMultipleViolationsOnSameField_groupsErrors() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolation("email", "must be a valid email"));
    violations.add(createMockViolation("email", "must not be blank"));

    ConstraintViolationException cve = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertNotNull(error);
    assertEquals("BAD_REQUEST", error.getMessage());
    assertNotNull(error.getErrors());
    assertEquals(1, error.getErrors().size());
    assertEquals(2, error.getErrors().get(0).getValue().size());
  }

  @Test
  void getErrorsAsData_withNestedPropertyPath_extractsCorrectField() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolationWithPath("createUser.param.email", "must be a valid email"));

    ConstraintViolationException cve = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertNotNull(error);
    assertNotNull(error.getErrors());
    assertEquals(1, error.getErrors().size());
    assertEquals("email", error.getErrors().get(0).getKey());
  }

  @Test
  void getErrorsAsData_withSimplePropertyPath_returnsFieldAsIs() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolation("email", "must be a valid email"));

    ConstraintViolationException cve = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertNotNull(error);
    assertNotNull(error.getErrors());
    assertEquals("email", error.getErrors().get(0).getKey());
  }

  @Test
  void onException_withInvalidAuthenticationException_includesPath() {
    InvalidAuthenticationException exception = new InvalidAuthenticationException();
    ResultPath path = ResultPath.rootPath().segment("login");
    when(handlerParameters.getException()).thenReturn(exception);
    when(handlerParameters.getPath()).thenReturn(path);

    DataFetcherExceptionHandlerResult result = handler.onException(handlerParameters);

    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }

  @Test
  void onException_withConstraintViolationException_includesExtensions() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolation("email", "must be a valid email"));

    ConstraintViolationException cve = new ConstraintViolationException(violations);
    when(handlerParameters.getException()).thenReturn(cve);
    when(handlerParameters.getPath()).thenReturn(ResultPath.rootPath());

    DataFetcherExceptionHandlerResult result = handler.onException(handlerParameters);

    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }

  private ConstraintViolation<?> createMockViolation(String field, String message) {
    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    ConstraintDescriptor<?> descriptor = mock(ConstraintDescriptor.class);
    java.lang.annotation.Annotation annotation = mock(java.lang.annotation.Annotation.class);

    when(violation.getPropertyPath()).thenReturn(path);
    when(path.toString()).thenReturn(field);
    doReturn(Object.class).when(violation).getRootBeanClass();
    doReturn(descriptor).when(violation).getConstraintDescriptor();
    doReturn(annotation).when(descriptor).getAnnotation();
    doReturn(javax.validation.constraints.Email.class).when(annotation).annotationType();
    when(violation.getMessage()).thenReturn(message);

    return violation;
  }

  private ConstraintViolation<?> createMockViolationWithPath(String fullPath, String message) {
    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    ConstraintDescriptor<?> descriptor = mock(ConstraintDescriptor.class);
    java.lang.annotation.Annotation annotation = mock(java.lang.annotation.Annotation.class);

    when(violation.getPropertyPath()).thenReturn(path);
    when(path.toString()).thenReturn(fullPath);
    doReturn(Object.class).when(violation).getRootBeanClass();
    doReturn(descriptor).when(violation).getConstraintDescriptor();
    doReturn(annotation).when(descriptor).getAnnotation();
    doReturn(javax.validation.constraints.Email.class).when(annotation).annotationType();
    when(violation.getMessage()).thenReturn(message);

    return violation;
  }
}
