package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import graphql.GraphQLError;
import graphql.Scalars;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private final GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

  public static class Input {
    @NotBlank(message = "can't be empty")
    @Email(message = "should be an email")
    private final String email;

    @NotBlank(message = "can't be empty")
    private final String username;

    public Input(String email, String username) {
      this.email = email;
      this.username = username;
    }

    public String getEmail() {
      return email;
    }

    public String getUsername() {
      return username;
    }
  }

  public static class Target {
    public void create(@Valid Input input) {}
  }

  @Test
  public void should_map_invalid_authentication_to_unauthenticated_error() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(params(new InvalidAuthenticationException(), "login"));

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals("invalid email or password", error.getMessage());
    assertEquals(Arrays.asList("login"), error.getPath());
    assertEquals("UNAUTHENTICATED", error.getExtensions().get("errorType"));
  }

  @Test
  public void should_map_constraint_violation_to_bad_request_with_field_errors() {
    ConstraintViolationException cve = methodViolations(new Input("not-an-email", ""));

    DataFetcherExceptionHandlerResult result = handler.onException(params(cve, "createUser"));

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    Map<String, Object> extensions = error.getExtensions();
    assertEquals("BAD_REQUEST", extensions.get("errorType"));
    assertEquals(Arrays.asList("should be an email"), extensions.get("email"));
    assertEquals(Arrays.asList("can't be empty"), extensions.get("username"));
    assertEquals(Arrays.asList("createUser"), error.getPath());
  }

  @Test
  public void should_delegate_other_exceptions_to_default_handler() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(params(new IllegalStateException("boom"), "article"));

    assertEquals(1, result.getErrors().size());
    assertTrue(result.getErrors().get(0).getMessage().contains("boom"));
  }

  @Test
  public void should_convert_constraint_violations_to_error_data() {
    ConstraintViolationException cve = methodViolations(new Input("", ""));

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertEquals("BAD_REQUEST", error.getMessage());
    Map<String, List<String>> items =
        error.getErrors().stream()
            .collect(Collectors.toMap(ErrorItem::getKey, ErrorItem::getValue));
    assertEquals(2, items.size());
    assertEquals(Arrays.asList("can't be empty"), items.get("email"));
    assertEquals(Arrays.asList("can't be empty"), items.get("username"));
  }

  @Test
  public void should_group_multiple_messages_for_same_field() {
    ConstraintViolationException cve =
        new ConstraintViolationException(
            Validation.buildDefaultValidatorFactory()
                .getValidator()
                .validate(new Input("not-an-email", "ok")));

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertEquals(1, error.getErrors().size());
    assertEquals("email", error.getErrors().get(0).getKey());
    assertEquals(Arrays.asList("should be an email"), error.getErrors().get(0).getValue());
  }

  private static ConstraintViolationException methodViolations(Input input) {
    try {
      Set<ConstraintViolation<Target>> violations =
          Validation.buildDefaultValidatorFactory()
              .getValidator()
              .forExecutables()
              .validateParameters(
                  new Target(),
                  Target.class.getMethod("create", Input.class),
                  new Object[] {input});
      return new ConstraintViolationException(violations);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }
  }

  private static DataFetcherExceptionHandlerParameters params(Throwable e, String field) {
    ExecutionStepInfo stepInfo =
        ExecutionStepInfo.newExecutionStepInfo()
            .type(Scalars.GraphQLString)
            .path(ResultPath.rootPath().segment(field))
            .build();
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(
            DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .executionStepInfo(stepInfo)
                .build())
        .exception(e)
        .build();
  }
}
