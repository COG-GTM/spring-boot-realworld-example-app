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
import graphql.schema.DataFetchingEnvironmentImpl;
import io.spring.api.exception.InvalidAuthenticationException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  private final GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

  @BeforeAll
  public static void setUpValidator() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  public static void closeValidator() {
    validatorFactory.close();
  }

  @Test
  public void should_map_invalid_authentication_to_unauthenticated_error() throws Exception {
    CompletableFuture<DataFetcherExceptionHandlerResult> future =
        handler.handleException(parameters("login", new InvalidAuthenticationException()));

    assertTrue(future.isDone());
    List<GraphQLError> errors = future.get().getErrors();
    assertEquals(1, errors.size());
    GraphQLError error = errors.get(0);
    assertEquals("invalid email or password", error.getMessage());
    assertEquals(List.of("login"), error.getPath());
    assertEquals(ErrorType.UNAUTHENTICATED.name(), error.getExtensions().get("errorType"));
  }

  @Test
  public void should_map_constraint_violation_to_bad_request_with_field_errors() throws Exception {
    ConstraintViolationException exception =
        new ConstraintViolationException(validator.validate(new Payload("")));

    CompletableFuture<DataFetcherExceptionHandlerResult> future =
        handler.handleException(parameters("createArticle", exception));

    assertTrue(future.isDone());
    List<GraphQLError> errors = future.get().getErrors();
    assertEquals(1, errors.size());
    GraphQLError error = errors.get(0);
    assertEquals(List.of("createArticle"), error.getPath());
    Map<String, Object> extensions = error.getExtensions();
    assertEquals(ErrorType.BAD_REQUEST.name(), extensions.get("errorType"));
    assertEquals(List.of("can't be empty"), extensions.get("title"));
  }

  @Test
  public void should_delegate_other_exceptions_to_default_handler() throws Exception {
    CompletableFuture<DataFetcherExceptionHandlerResult> future =
        handler.handleException(parameters("article", new IllegalStateException("boom")));

    List<GraphQLError> errors = future.get().getErrors();
    assertEquals(1, errors.size());
    assertEquals(List.of("article"), errors.get(0).getPath());
    assertTrue(errors.get(0).getMessage().contains("boom"));
  }

  private static DataFetcherExceptionHandlerParameters parameters(
      String fieldName, Throwable exception) {
    DataFetchingEnvironmentImpl environment =
        DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .executionStepInfo(
                ExecutionStepInfo.newExecutionStepInfo()
                    .type(Scalars.GraphQLString)
                    .path(ResultPath.rootPath().segment(fieldName))
                    .build())
            .mergedField(MergedField.newMergedField(new Field(fieldName)).build())
            .build();
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(environment)
        .exception(exception)
        .build();
  }

  static class Payload {
    @NotBlank(message = "can't be empty")
    private final String title;

    Payload(String title) {
      this.title = title;
    }
  }
}
