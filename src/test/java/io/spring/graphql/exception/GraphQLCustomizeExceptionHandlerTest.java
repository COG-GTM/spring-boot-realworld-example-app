package io.spring.graphql.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.GraphQLError;
import graphql.Scalars;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.ConstraintViolationFixture;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import java.util.List;
import org.junit.jupiter.api.Test;

class GraphQLCustomizeExceptionHandlerTest {

  private final GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

  private DataFetcherExceptionHandlerParameters parametersOf(Throwable exception) {
    DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
    when(environment.getExecutionStepInfo())
        .thenReturn(
            ExecutionStepInfo.newExecutionStepInfo()
                .type(Scalars.GraphQLString)
                .path(ResultPath.parse("/user"))
                .build());
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(environment)
        .exception(exception)
        .build();
  }

  @Test
  void should_map_invalid_authentication_to_unauthenticated_error() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parametersOf(new InvalidAuthenticationException()));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getMessage()).isEqualTo("invalid email or password");
    assertThat(error.getPath()).containsExactly("user");
    assertThat(error.getExtensions()).containsEntry("errorType", "UNAUTHENTICATED");
  }

  @Test
  void should_map_constraint_violations_to_bad_request_error() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parametersOf(ConstraintViolationFixture.beanViolations()));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getPath()).containsExactly("user");
    assertThat(error.getExtensions()).containsEntry("errorType", "BAD_REQUEST");
    assertThat(error.getExtensions().get("email")).isEqualTo(List.of("should be an email"));
    assertThat((List<String>) error.getExtensions().get("username"))
        .containsExactlyInAnyOrder("can't be empty", "too short");
  }

  @Test
  void should_strip_the_method_prefix_of_nested_violation_paths() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parametersOf(ConstraintViolationFixture.methodParameterViolations()));

    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getExtensions()).containsKeys("username", "email");
  }

  @Test
  void should_delegate_unknown_exceptions_to_the_default_handler() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parametersOf(new RuntimeException("boom")));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("boom");
  }

  @Test
  void should_convert_violations_into_error_data() {
    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(
            ConstraintViolationFixture.beanViolations());

    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).hasSize(2);
    ErrorItem username =
        error.getErrors().stream()
            .filter(item -> item.getKey().equals("username"))
            .findFirst()
            .orElseThrow(AssertionError::new);
    assertThat(username.getValue()).containsExactlyInAnyOrder("can't be empty", "too short");
  }
}
