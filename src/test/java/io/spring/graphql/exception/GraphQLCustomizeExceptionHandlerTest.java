package io.spring.graphql.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ResultPath;
import io.spring.api.exception.InvalidAuthenticationException;
import java.util.HashSet;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private GraphQLCustomizeExceptionHandler exceptionHandler;

  @BeforeEach
  void setUp() {
    exceptionHandler = new GraphQLCustomizeExceptionHandler();
  }

  @Test
  void shouldCreateExceptionHandler() {
    assertThat(exceptionHandler).isNotNull();
  }

  @Test
  void shouldReturnResultForInvalidAuthenticationException() {
    InvalidAuthenticationException exception = new InvalidAuthenticationException();
    
    DataFetcherExceptionHandlerParameters params = mock(DataFetcherExceptionHandlerParameters.class);
    when(params.getException()).thenReturn(exception);
    when(params.getPath()).thenReturn(ResultPath.rootPath());
    
    DataFetcherExceptionHandlerResult result = exceptionHandler.onException(params);
    
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).hasSize(1);
  }

  @Test
  void shouldReturnResultForConstraintViolationException() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    ConstraintViolation<?> violation = createMockViolation("field.name", "must not be blank");
    violations.add(violation);
    
    ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);
    
    DataFetcherExceptionHandlerParameters params = mock(DataFetcherExceptionHandlerParameters.class);
    when(params.getException()).thenReturn(exception);
    when(params.getPath()).thenReturn(ResultPath.rootPath());
    
    DataFetcherExceptionHandlerResult result = exceptionHandler.onException(params);
    
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).hasSize(1);
  }

  @Test
  void shouldReturnSingleErrorForMultipleViolationsOnSameField() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolation("input.field", "error message 1"));
    violations.add(createMockViolation("input.field", "error message 2"));
    
    ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);
    
    DataFetcherExceptionHandlerParameters params = mock(DataFetcherExceptionHandlerParameters.class);
    when(params.getException()).thenReturn(exception);
    when(params.getPath()).thenReturn(ResultPath.rootPath());
    
    DataFetcherExceptionHandlerResult result = exceptionHandler.onException(params);
    
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).hasSize(1);
  }

  @Test
  void shouldReturnErrorsForMultipleViolationsOnDifferentFields() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(createMockViolation("input.field1", "error message 1"));
    violations.add(createMockViolation("input.field2", "error message 2"));
    
    ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);
    
    DataFetcherExceptionHandlerParameters params = mock(DataFetcherExceptionHandlerParameters.class);
    when(params.getException()).thenReturn(exception);
    when(params.getPath()).thenReturn(ResultPath.rootPath());
    
    DataFetcherExceptionHandlerResult result = exceptionHandler.onException(params);
    
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).hasSize(1);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private ConstraintViolation<?> createMockViolation(String propertyPath, String message) {
    ConstraintViolation violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    ConstraintDescriptor descriptor = mock(ConstraintDescriptor.class);
    javax.validation.constraints.NotBlank annotation = mock(javax.validation.constraints.NotBlank.class);
    
    when(path.toString()).thenReturn(propertyPath);
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn(message);
    when(violation.getRootBeanClass()).thenReturn((Class) Object.class);
    when(violation.getConstraintDescriptor()).thenReturn(descriptor);
    when(descriptor.getAnnotation()).thenReturn(annotation);
    when(annotation.annotationType()).thenReturn((Class) javax.validation.constraints.NotBlank.class);
    
    return violation;
  }
}
