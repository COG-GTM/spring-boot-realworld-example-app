package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {
  private GraphQLCustomizeExceptionHandler handler;
  private Validator validator;

  @BeforeEach
  public void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  private DataFetcherExceptionHandlerParameters params(Throwable exception) {
    DataFetchingEnvironment env =
        DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .mergedField(MergedField.newMergedField(Field.newField("createUser").build()).build())
            .executionStepInfo(
                ExecutionStepInfo.newExecutionStepInfo()
                    .type(Scalars.GraphQLString)
                    .path(ResultPath.parse("/createUser"))
                    .build())
            .build();
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(env)
        .exception(exception)
        .build();
  }

  private static class SignUp {
    @Email(message = "should be an email")
    private final String email;

    @NotBlank(message = "can't be empty")
    private final String username;

    SignUp(String email, String username) {
      this.email = email;
      this.username = username;
    }
  }

  private static class SignUpService {
    @SuppressWarnings("unused")
    public void createUser(@Valid SignUp registerParam) {}
  }

  private ConstraintViolationException invalidMethodParam() throws Exception {
    SignUpService service = new SignUpService();
    Set<ConstraintViolation<SignUpService>> violations =
        validator
            .forExecutables()
            .validateParameters(
                service,
                SignUpService.class.getMethod("createUser", SignUp.class),
                new Object[] {new SignUp("not-an-email", "")});
    return new ConstraintViolationException(violations);
  }

  private ConstraintViolationException invalidRegisterParam() {
    Set<ConstraintViolation<SignUp>> violations =
        validator.validate(new SignUp("not-an-email", ""));
    return new ConstraintViolationException(violations);
  }

  @Test
  public void should_map_invalid_authentication_to_unauthenticated_error() throws Exception {
    DataFetcherExceptionHandlerResult result =
        handler.handleException(params(new InvalidAuthenticationException())).get();

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals("invalid email or password", error.getMessage());
    assertEquals(Arrays.asList("createUser"), error.getPath());
    assertEquals("UNAUTHENTICATED", error.getExtensions().get("errorType"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void should_map_constraint_violation_to_bad_request_with_field_errors() throws Exception {
    DataFetcherExceptionHandlerResult result =
        handler.handleException(params(invalidRegisterParam())).get();

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    Map<String, Object> extensions = error.getExtensions();
    assertEquals("BAD_REQUEST", extensions.get("errorType"));
    assertEquals(Arrays.asList("createUser"), error.getPath());
    assertEquals(Arrays.asList("should be an email"), extensions.get("email"));
    assertEquals(Arrays.asList("can't be empty"), extensions.get("username"));
  }

  @Test
  public void should_delegate_other_exceptions_to_default_handler() throws Exception {
    DataFetcherExceptionHandlerResult result =
        handler.handleException(params(new IllegalStateException("boom"))).get();

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals(Arrays.asList("createUser"), error.getPath());
    assertEquals("INTERNAL", error.getExtensions().get("errorType"));
    assertTrue(error.getMessage().contains("boom"));
  }

  @Test
  public void should_convert_constraint_violations_to_error_data() {
    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(invalidRegisterParam());

    assertEquals("BAD_REQUEST", error.getMessage());
    assertEquals(2, error.getErrors().size());
    assertFalse(
        error.getErrors().stream()
            .filter(item -> item.getKey().equals("email"))
            .map(item -> item.getValue())
            .findFirst()
            .orElse(List.of())
            .isEmpty());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void should_strip_method_and_parameter_prefix_from_field_names() throws Exception {
    ConstraintViolationException exception = invalidMethodParam();

    DataFetcherExceptionHandlerResult result = handler.handleException(params(exception)).get();
    Map<String, Object> extensions = result.getErrors().get(0).getExtensions();
    assertEquals(Arrays.asList("should be an email"), extensions.get("email"));
    assertEquals(Arrays.asList("can't be empty"), extensions.get("username"));

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(exception);
    assertEquals(
        Set.of("email", "username"),
        error.getErrors().stream().map(item -> item.getKey()).collect(Collectors.toSet()));
  }
}
