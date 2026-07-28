package io.spring.graphql.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.netflix.graphql.types.errors.ErrorType;
import graphql.Scalars;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import java.util.HashSet;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;

class GraphQLCustomizeExceptionHandlerTest {

  private final GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

  @Test
  void should_map_invalid_authentication_to_unauthenticated_error() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parameters(new InvalidAuthenticationException()));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getExtensions())
        .containsEntry("errorType", ErrorType.UNAUTHENTICATED.toString());
  }

  @Test
  void should_map_constraint_violation_to_bad_request_error() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parameters(new ConstraintViolationException(violations())));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getExtensions()).containsKey("name");
  }

  @Test
  void should_convert_constraint_violations_as_data() {
    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(
            new ConstraintViolationException(violations()));

    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).hasSize(1);
    assertThat(error.getErrors().get(0).getKey()).isEqualTo("name");
  }

  private DataFetcherExceptionHandlerParameters parameters(Throwable throwable) {
    ExecutionStepInfo executionStepInfo =
        ExecutionStepInfo.newExecutionStepInfo()
            .type(Scalars.GraphQLString)
            .path(ResultPath.rootPath())
            .build();
    DataFetchingEnvironment environment =
        DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .executionStepInfo(executionStepInfo)
            .build();
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(environment)
        .exception(throwable)
        .build();
  }

  private Set<ConstraintViolation<?>> violations() {
    return new HashSet<>(
        Validation.buildDefaultValidatorFactory().getValidator().validate(new Named()));
  }

  private static class Named {
    @NotBlank(message = "can't be empty")
    private String name = "";

    public String getName() {
      return name;
    }
  }
}
