package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import java.lang.annotation.Annotation;
import java.util.*;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GraphQLCustomizeExceptionHandlerTest {

  private GraphQLCustomizeExceptionHandler handler;

  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  @BeforeEach
  void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();

    ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);
    when(stepInfo.getPath()).thenReturn(ResultPath.rootPath());
    when(dataFetchingEnvironment.getExecutionStepInfo()).thenReturn(stepInfo);
  }

  private DataFetcherExceptionHandlerParameters buildParams(Throwable exception) {
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(dataFetchingEnvironment)
        .exception(exception)
        .build();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private ConstraintViolation<?> createMockViolation(
      String propertyPath, String message, Class<?> rootBeanClass) {
    ConstraintViolation violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    when(path.toString()).thenReturn(propertyPath);
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn(message);
    when(violation.getRootBeanClass()).thenReturn(rootBeanClass);

    ConstraintDescriptor descriptor = mock(ConstraintDescriptor.class);
    Annotation annotation = mock(Annotation.class);
    doReturn(javax.validation.constraints.NotBlank.class).when(annotation).annotationType();
    when(descriptor.getAnnotation()).thenReturn(annotation);
    when(violation.getConstraintDescriptor()).thenReturn(descriptor);

    return violation;
  }

  // --- InvalidAuthenticationException tests ---

  @Test
  void onException_invalidAuthentication_returnsUnauthenticatedError() {
    InvalidAuthenticationException exception = new InvalidAuthenticationException();

    DataFetcherExceptionHandlerResult result = handler.onException(buildParams(exception));

    assertNotNull(result);
    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertTrue(error.getMessage().contains("invalid email or password"));
  }

  @Test
  void onException_invalidAuthentication_hasCorrectErrorType() {
    InvalidAuthenticationException exception = new InvalidAuthenticationException();

    DataFetcherExceptionHandlerResult result = handler.onException(buildParams(exception));

    GraphQLError error = result.getErrors().get(0);
    assertNotNull(error.getExtensions());
    assertEquals("UNAUTHENTICATED", error.getExtensions().get("errorType"));
  }

  // --- ConstraintViolationException tests ---

  @Test
  void onException_constraintViolation_returnsBadRequestError() {
    ConstraintViolation<?> violation =
        createMockViolation("createUser.input.email", "must not be blank", String.class);
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);
    ConstraintViolationException cve =
        new ConstraintViolationException("validation failed", violations);

    DataFetcherExceptionHandlerResult result = handler.onException(buildParams(cve));

    assertNotNull(result);
    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertNotNull(error.getExtensions());
  }

  @Test
  void onException_multipleConstraintViolations_returnsError() {
    ConstraintViolation<?> v1 =
        createMockViolation("createUser.input.email", "must not be blank", String.class);
    ConstraintViolation<?> v2 =
        createMockViolation("createUser.input.username", "too short", String.class);
    Set<ConstraintViolation<?>> violations = new LinkedHashSet<>();
    violations.add(v1);
    violations.add(v2);
    ConstraintViolationException cve =
        new ConstraintViolationException("validation failed", violations);

    DataFetcherExceptionHandlerResult result = handler.onException(buildParams(cve));

    assertNotNull(result);
    assertEquals(1, result.getErrors().size());
  }

  // --- Default exception handling tests ---

  @Test
  void onException_genericException_delegatesToDefaultHandler() {
    RuntimeException exception = new RuntimeException("something went wrong");

    DataFetcherExceptionHandlerResult result = handler.onException(buildParams(exception));

    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }

  // --- getErrorsAsData tests ---

  @Test
  void getErrorsAsData_returnsErrorWithFieldMessages() {
    ConstraintViolation<?> violation =
        createMockViolation("createUser.input.email", "must not be blank", String.class);
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);
    ConstraintViolationException cve =
        new ConstraintViolationException("validation failed", violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertNotNull(error);
    assertEquals("BAD_REQUEST", error.getMessage());
    assertNotNull(error.getErrors());
    assertFalse(error.getErrors().isEmpty());
  }

  @Test
  void getErrorsAsData_multipleViolationsSameField_groupsMessages() {
    ConstraintViolation<?> v1 =
        createMockViolation("createUser.input.email", "must not be blank", String.class);
    ConstraintViolation<?> v2 =
        createMockViolation("createUser.input.email", "invalid format", String.class);
    Set<ConstraintViolation<?>> violations = new LinkedHashSet<>();
    violations.add(v1);
    violations.add(v2);
    ConstraintViolationException cve =
        new ConstraintViolationException("validation failed", violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertNotNull(error);
    boolean foundEmailItem = false;
    for (ErrorItem item : error.getErrors()) {
      if ("email".equals(item.getKey())) {
        foundEmailItem = true;
        assertEquals(2, item.getValue().size());
      }
    }
    assertTrue(foundEmailItem);
  }

  @Test
  void getErrorsAsData_singlePathProperty_usesFullPath() {
    ConstraintViolation<?> violation =
        createMockViolation("email", "must not be blank", String.class);
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);
    ConstraintViolationException cve =
        new ConstraintViolationException("validation failed", violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertNotNull(error);
    boolean foundEmailItem = false;
    for (ErrorItem item : error.getErrors()) {
      if ("email".equals(item.getKey())) {
        foundEmailItem = true;
      }
    }
    assertTrue(foundEmailItem);
  }

  @Test
  void getErrorsAsData_returnsBAD_REQUEST_message() {
    ConstraintViolation<?> violation =
        createMockViolation("field", "error msg", String.class);
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);
    ConstraintViolationException cve =
        new ConstraintViolationException("validation failed", violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertEquals("BAD_REQUEST", error.getMessage());
  }
}
