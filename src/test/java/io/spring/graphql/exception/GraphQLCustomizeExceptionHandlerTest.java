package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import java.util.Collections;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private final GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

  private DataFetcherExceptionHandlerResult handle(Throwable throwable) {
    DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
    ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);
    when(environment.getExecutionStepInfo()).thenReturn(stepInfo);
    when(stepInfo.getPath()).thenReturn(ResultPath.rootPath());
    DataFetcherExceptionHandlerParameters params =
        DataFetcherExceptionHandlerParameters.newExceptionParameters()
            .dataFetchingEnvironment(environment)
            .exception(throwable)
            .build();
    return handler.onException(params);
  }

  @Test
  void should_map_invalid_authentication_to_unauthenticated_error() {
    DataFetcherExceptionHandlerResult result = handle(new InvalidAuthenticationException());

    assertEquals(1, result.getErrors().size());
    assertEquals("invalid email or password", result.getErrors().get(0).getMessage());
  }

  @Test
  void should_map_constraint_violation_to_bad_request_error() {
    DataFetcherExceptionHandlerResult result =
        handle(new ConstraintViolationException(Collections.emptySet()));

    assertEquals(1, result.getErrors().size());
  }

  @Test
  void should_delegate_unknown_exception_to_default_handler() {
    DataFetcherExceptionHandlerResult result = handle(new RuntimeException("boom"));

    assertFalse(result.getErrors().isEmpty());
  }

  @Test
  void should_build_error_data_from_constraint_violation() {
    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(
            new ConstraintViolationException(Collections.emptySet()));

    assertEquals("BAD_REQUEST", error.getMessage());
    assertTrue(error.getErrors().isEmpty());
  }
}
