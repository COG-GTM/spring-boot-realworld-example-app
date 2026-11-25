package io.spring.graphql.exception;

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
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private GraphQLCustomizeExceptionHandler handler;
  private Validator validator;

  static class Input {
    @NotBlank @Email private final String email;
    @NotBlank private final String username;

    Input(String email, String username) {
      this.email = email;
      this.username = username;
    }
  }

  @BeforeEach
  public void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  private DataFetcherExceptionHandlerParameters params(Throwable exception) {
    DataFetchingEnvironment env =
        DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .executionStepInfo(
                ExecutionStepInfo.newExecutionStepInfo()
                    .type(Scalars.GraphQLString)
                    .path(ResultPath.parse("/login"))
                    .build())
            .mergedField(MergedField.newMergedField(Field.newField("login").build()).build())
            .build();
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(env)
        .exception(exception)
        .build();
  }

  private ConstraintViolationException violations(Input input) {
    return new ConstraintViolationException(validator.validate(input));
  }

  @Test
  public void should_map_invalid_authentication_to_unauthenticated_error() throws Exception {
    DataFetcherExceptionHandlerResult result =
        handler.handleException(params(new InvalidAuthenticationException())).get();

    Assertions.assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    Assertions.assertEquals("invalid email or password", error.getMessage());
    Assertions.assertEquals(List.of("login"), error.getPath());
    Assertions.assertEquals(
        ErrorType.UNAUTHENTICATED.name(), String.valueOf(error.getExtensions().get("errorType")));
  }

  @Test
  public void should_map_constraint_violation_to_bad_request_with_field_errors() throws Exception {
    DataFetcherExceptionHandlerResult result =
        handler.handleException(params(violations(new Input("not-an-email", "")))).get();

    Assertions.assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    Assertions.assertEquals(List.of("login"), error.getPath());
    Map<String, Object> extensions = error.getExtensions();
    Assertions.assertEquals(
        ErrorType.BAD_REQUEST.name(), String.valueOf(extensions.get("errorType")));
    Assertions.assertEquals(1, ((List<?>) extensions.get("email")).size());
    Assertions.assertEquals(1, ((List<?>) extensions.get("username")).size());
  }

  @Test
  public void should_delegate_other_exceptions_to_default_handler() throws Exception {
    DataFetcherExceptionHandlerResult result =
        handler.handleException(params(new IllegalStateException("boom"))).get();

    Assertions.assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    Assertions.assertTrue(error.getMessage().contains("boom"), error.getMessage());
    Assertions.assertEquals(List.of("login"), error.getPath());
    Assertions.assertEquals(
        ErrorType.INTERNAL.name(), String.valueOf(error.getExtensions().get("errorType")));
  }

  @Test
  public void should_group_constraint_violations_by_field_as_data() {
    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(violations(new Input("", "")));

    Assertions.assertEquals("BAD_REQUEST", error.getMessage());
    Map<String, List<String>> byKey =
        error.getErrors().stream()
            .collect(Collectors.toMap(ErrorItem::getKey, ErrorItem::getValue));
    Assertions.assertEquals(2, byKey.size());
    Assertions.assertFalse(byKey.get("email").isEmpty());
    Assertions.assertEquals(1, byKey.get("username").size());
  }
}
