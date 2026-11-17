package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import graphql.GraphQLError;
import graphql.Scalars;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.spring.api.exception.InvalidAuthenticationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private GraphQLCustomizeExceptionHandler handler;

  @BeforeEach
  public void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
  }

  @Test
  public void should_map_invalid_authentication_to_unauthenticated_error() throws Exception {
    DataFetcherExceptionHandlerResult result =
        handler.handleException(params(new InvalidAuthenticationException())).get();

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals("invalid email or password", error.getMessage());
    assertEquals("UNAUTHENTICATED", error.getExtensions().get("errorType"));
    assertEquals(Arrays.asList("login"), error.getPath());
  }

  @Test
  public void should_map_constraint_violation_to_bad_request_with_field_errors() throws Exception {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<Input>> violations = validator.validate(new Input(""));
    ConstraintViolationException cve = new ConstraintViolationException(violations);

    DataFetcherExceptionHandlerResult result = handler.handleException(params(cve)).get();

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals("BAD_REQUEST", error.getExtensions().get("errorType"));
    assertEquals(List.of("can't be empty"), error.getExtensions().get("username"));
    assertEquals(Arrays.asList("login"), error.getPath());
  }

  @Test
  public void should_delegate_other_exceptions_to_default_handler() throws Exception {
    DataFetcherExceptionHandlerResult result =
        handler.handleException(params(new IllegalStateException("boom"))).get();

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertTrue(error.getMessage().contains("boom"));
    assertEquals("INTERNAL", error.getExtensions().get("errorType"));
  }

  private static DataFetcherExceptionHandlerParameters params(Throwable exception) {
    ExecutionStepInfo stepInfo =
        ExecutionStepInfo.newExecutionStepInfo()
            .type(Scalars.GraphQLString)
            .path(ResultPath.parse("/login"))
            .build();
    DataFetchingEnvironment env =
        DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .executionStepInfo(stepInfo)
            .build();
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(env)
        .exception(exception)
        .build();
  }

  static class Input {
    @NotBlank(message = "can't be empty")
    private final String username;

    Input(String username) {
      this.username = username;
    }
  }
}
