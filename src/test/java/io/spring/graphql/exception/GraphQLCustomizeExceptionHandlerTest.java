package io.spring.graphql.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.constraints.NotBlank;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GraphQLCustomizeExceptionHandlerTest {

  private GraphQLCustomizeExceptionHandler handler;
  private DataFetchingEnvironment dataFetchingEnvironment;

  @BeforeEach
  void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
    dataFetchingEnvironment = mock(DataFetchingEnvironment.class);
    ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);
    when(stepInfo.getPath()).thenReturn(ResultPath.rootPath());
    when(dataFetchingEnvironment.getExecutionStepInfo()).thenReturn(stepInfo);
  }

  private DataFetcherExceptionHandlerParameters parametersFor(Throwable throwable) {
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(dataFetchingEnvironment)
        .exception(throwable)
        .build();
  }

  private ConstraintViolationException constraintViolation(String field, String message) {
    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    when(path.toString()).thenReturn(field);
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn(message);
    when(violation.getRootBeanClass()).thenReturn((Class) String.class);
    ConstraintDescriptor<?> descriptor = mock(ConstraintDescriptor.class);
    Annotation annotation = mock(Annotation.class);
    when(annotation.annotationType()).thenReturn((Class) NotBlank.class);
    when(descriptor.getAnnotation()).thenReturn((Annotation) annotation);
    when(violation.getConstraintDescriptor()).thenReturn((ConstraintDescriptor) descriptor);
    Set<ConstraintViolation<?>> violations = Collections.singleton(violation);
    return new ConstraintViolationException(violations);
  }

  @Test
  void should_map_invalid_authentication_to_unauthenticated_error() {
    InvalidAuthenticationException exception = new InvalidAuthenticationException();

    DataFetcherExceptionHandlerResult result = handler.onException(parametersFor(exception));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getMessage()).isEqualTo("invalid email or password");
    assertThat(error.getExtensions().get("errorType").toString()).isEqualTo("UNAUTHENTICATED");
  }

  @Test
  void should_map_constraint_violation_to_bad_request_error() {
    ConstraintViolationException exception = constraintViolation("email", "should be an email");

    DataFetcherExceptionHandlerResult result = handler.onException(parametersFor(exception));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getExtensions().get("errorType").toString()).isEqualTo("BAD_REQUEST");
    assertThat(error.getExtensions()).containsKey("email");
  }

  @Test
  void should_delegate_unhandled_exception_to_default_handler() {
    RuntimeException exception = new RuntimeException("boom");

    DataFetcherExceptionHandlerResult result = handler.onException(parametersFor(exception));

    assertThat(result.getErrors()).isNotEmpty();
    assertThat(result.getErrors().get(0).getMessage()).contains("boom");
  }

  @Test
  void should_build_error_data_from_constraint_violation() {
    ConstraintViolationException exception = constraintViolation("username", "can't be empty");

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(exception);

    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).hasSize(1);
    assertThat(error.getErrors().get(0).getKey()).isEqualTo("username");
    assertThat(error.getErrors().get(0).getValue()).contains("can't be empty");
  }
}
