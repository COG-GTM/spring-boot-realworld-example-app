package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.netflix.graphql.types.errors.ErrorType;
import graphql.GraphQLError;
import graphql.Scalars;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.application.user.RegisterParam;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {
  private final GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

  private DataFetcherExceptionHandlerParameters parameters(Throwable exception) {
    DataFetchingEnvironment env =
        DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .mergedField(MergedField.newMergedField(Field.newField("login").build()).build())
            .executionStepInfo(
                ExecutionStepInfo.newExecutionStepInfo()
                    .type(Scalars.GraphQLString)
                    .path(ResultPath.rootPath().segment("login"))
                    .build())
            .build();
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(env)
        .exception(exception)
        .build();
  }

  @Test
  public void should_map_invalid_authentication_to_unauthenticated_error() throws Exception {
    CompletableFuture<DataFetcherExceptionHandlerResult> future =
        handler.handleException(parameters(new InvalidAuthenticationException()));

    assertTrue(future.isDone());
    List<GraphQLError> errors = future.get().getErrors();
    assertEquals(1, errors.size());
    GraphQLError error = errors.get(0);
    assertEquals("invalid email or password", error.getMessage());
    assertEquals(Arrays.asList("login"), error.getPath());
    assertEquals(ErrorType.UNAUTHENTICATED.name(), error.getExtensions().get("errorType"));
  }

  @Test
  public void should_map_constraint_violation_to_bad_request_with_field_errors() throws Exception {
    ConstraintViolationException cve;
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      cve =
          new ConstraintViolationException(
              factory
                  .getValidator()
                  .validateProperty(
                      new RegisterParam("john@jacob.com", "johnjacob", "a".repeat(73)),
                      "password"));
    }

    CompletableFuture<DataFetcherExceptionHandlerResult> future =
        handler.handleException(parameters(cve));

    assertTrue(future.isDone());
    List<GraphQLError> errors = future.get().getErrors();
    assertEquals(1, errors.size());
    Map<String, Object> extensions = errors.get(0).getExtensions();
    assertEquals(ErrorType.BAD_REQUEST.name(), extensions.get("errorType"));
    assertEquals(Arrays.asList("is too long (maximum is 72 bytes)"), extensions.get("password"));
    assertEquals(Arrays.asList("login"), errors.get(0).getPath());
  }

  @Test
  public void should_delegate_other_exceptions_to_default_handler() throws Exception {
    CompletableFuture<DataFetcherExceptionHandlerResult> future =
        handler.handleException(parameters(new IllegalStateException("boom")));

    List<GraphQLError> errors = future.get().getErrors();
    assertEquals(1, errors.size());
    assertTrue(errors.get(0).getMessage().contains("boom"));
    assertEquals(Arrays.asList("login"), errors.get(0).getPath());
  }
}
