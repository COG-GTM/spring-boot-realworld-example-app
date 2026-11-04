package io.spring.graphql.exception;

import com.netflix.graphql.types.errors.ErrorType;
import graphql.GraphQLError;
import graphql.Scalars;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.ConstraintViolationFixture;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {
  private final GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

  private static DataFetcherExceptionHandlerParameters params(Throwable exception) {
    DataFetchingEnvironment env =
        DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .executionStepInfo(
                ExecutionStepInfo.newExecutionStepInfo()
                    .type(Scalars.GraphQLString)
                    .path(ResultPath.rootPath().segment("createUser"))
                    .build())
            .build();
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(env)
        .exception(exception)
        .build();
  }

  @Test
  public void should_map_invalid_authentication_to_unauthenticated_error() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(params(new InvalidAuthenticationException()));

    Assertions.assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    Assertions.assertEquals(
        ErrorType.UNAUTHENTICATED.name(), error.getExtensions().get("errorType"));
    Assertions.assertEquals(List.of("createUser"), error.getPath());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void should_map_constraint_violations_to_bad_request_with_field_errors() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(params(ConstraintViolationFixture.blankRegistration()));

    Assertions.assertEquals(1, result.getErrors().size());
    Map<String, Object> extensions = result.getErrors().get(0).getExtensions();
    Assertions.assertEquals(ErrorType.BAD_REQUEST.name(), extensions.get("errorType"));
    Assertions.assertEquals(List.of("can't be empty"), extensions.get("email"));
    Assertions.assertEquals(List.of("can't be empty"), extensions.get("username"));
    Assertions.assertEquals(List.of("can't be empty"), extensions.get("password"));
  }

  @Test
  public void should_delegate_other_exceptions_to_default_handler() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(params(new IllegalStateException("boom")));

    Assertions.assertEquals(1, result.getErrors().size());
    Assertions.assertTrue(result.getErrors().get(0).getMessage().contains("boom"));
  }

  @Test
  public void should_convert_constraint_violations_to_error_data() {
    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(
            ConstraintViolationFixture.blankRegistration());

    Assertions.assertEquals("BAD_REQUEST", error.getMessage());
    Map<String, List<String>> items =
        error.getErrors().stream()
            .collect(Collectors.toMap(ErrorItem::getKey, ErrorItem::getValue));
    Assertions.assertEquals(Set.of("email", "username", "password"), items.keySet());
    Assertions.assertEquals(List.of("can't be empty"), items.get("email"));
  }
}
