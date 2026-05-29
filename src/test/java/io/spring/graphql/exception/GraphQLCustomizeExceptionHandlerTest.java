package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.InvalidAuthenticationException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private GraphQLCustomizeExceptionHandler handler;
  private DataFetchingEnvironment dfe;

  @BeforeEach
  void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
    dfe = mock(DataFetchingEnvironment.class);
    ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);
    when(stepInfo.getPath()).thenReturn(ResultPath.rootPath());
    when(dfe.getExecutionStepInfo()).thenReturn(stepInfo);
  }

  @Test
  void should_handle_authentication_exception() {
    InvalidAuthenticationException exception = new InvalidAuthenticationException();
    DataFetcherExceptionHandlerParameters params =
        DataFetcherExceptionHandlerParameters.newExceptionParameters()
            .dataFetchingEnvironment(dfe)
            .exception(exception)
            .build();

    DataFetcherExceptionHandlerResult result = handler.onException(params);

    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }

  @Test
  @SuppressWarnings("unchecked")
  void should_handle_constraint_violation_exception() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    ConstraintViolation violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    when(path.toString()).thenReturn("createUser.param.email");
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn("must not be blank");
    when(violation.getRootBeanClass()).thenReturn(Object.class);

    Annotation annotation = mock(Annotation.class);
    when(annotation.annotationType())
        .thenReturn((Class) javax.validation.constraints.NotBlank.class);
    ConstraintDescriptor descriptor = mock(ConstraintDescriptor.class);
    when(descriptor.getAnnotation()).thenReturn(annotation);
    when(violation.getConstraintDescriptor()).thenReturn(descriptor);

    violations.add(violation);
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    DataFetcherExceptionHandlerParameters params =
        DataFetcherExceptionHandlerParameters.newExceptionParameters()
            .dataFetchingEnvironment(dfe)
            .exception(exception)
            .build();

    DataFetcherExceptionHandlerResult result = handler.onException(params);

    assertNotNull(result);
    assertFalse(result.getErrors().isEmpty());
  }

  @Test
  void should_handle_generic_exception() {
    RuntimeException exception = new RuntimeException("generic error");
    DataFetcherExceptionHandlerParameters params =
        DataFetcherExceptionHandlerParameters.newExceptionParameters()
            .dataFetchingEnvironment(dfe)
            .exception(exception)
            .build();

    DataFetcherExceptionHandlerResult result = handler.onException(params);

    assertNotNull(result);
  }

  @Test
  @SuppressWarnings("unchecked")
  void should_get_errors_as_data() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    ConstraintViolation violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    when(path.toString()).thenReturn("register.param.email");
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn("must not be blank");
    when(violation.getRootBeanClass()).thenReturn(Object.class);

    Annotation annotation = mock(Annotation.class);
    when(annotation.annotationType())
        .thenReturn((Class) javax.validation.constraints.NotBlank.class);
    ConstraintDescriptor descriptor = mock(ConstraintDescriptor.class);
    when(descriptor.getAnnotation()).thenReturn(annotation);
    when(violation.getConstraintDescriptor()).thenReturn(descriptor);

    violations.add(violation);
    ConstraintViolationException cve = new ConstraintViolationException(violations);

    var error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertNotNull(error);
    assertEquals("BAD_REQUEST", error.getMessage());
    assertFalse(error.getErrors().isEmpty());
    assertTrue(
        error.getErrors().stream()
            .anyMatch(e -> e.getKey().equals("email")));
  }
}
