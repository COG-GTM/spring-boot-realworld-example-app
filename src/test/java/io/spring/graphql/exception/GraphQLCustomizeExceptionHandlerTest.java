package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.lang.reflect.Method;
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

  @Test
  public void should_map_invalid_authentication_to_unauthenticated_error() throws Exception {
    DataFetcherExceptionHandlerResult result =
        handler.handleException(params(new InvalidAuthenticationException(), "/login")).get();

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals("invalid email or password", error.getMessage());
    assertEquals(ErrorType.UNAUTHENTICATED, error.getErrorType());
    assertEquals(List.of("login"), error.getPath());
  }

  @Test
  public void should_map_constraint_violation_to_bad_request_with_field_errors() throws Exception {
    ConstraintViolationException exception =
        new ConstraintViolationException(validator.validate(new Param("not-an-email", "")));

    DataFetcherExceptionHandlerResult result =
        handler.handleException(params(exception, "/createUser")).get();

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals(ErrorType.BAD_REQUEST, error.getErrorType());
    assertEquals(List.of("createUser"), error.getPath());
    Map<String, Object> extensions = error.getExtensions();
    assertEquals(List.of("must be a well-formed email address"), extensions.get("email"));
    assertEquals(List.of("can't be empty"), extensions.get("username"));
  }

  @Test
  public void should_strip_method_and_argument_from_method_validation_path() throws Exception {
    Method method = Service.class.getMethod("create", Param.class);
    Set<ConstraintViolation<Service>> violations =
        validator
            .forExecutables()
            .validateParameters(
                new Service(), method, new Object[] {new Param("john@jacob.com", "")});
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    DataFetcherExceptionHandlerResult result =
        handler.handleException(params(exception, "/createUser")).get();

    Map<String, Object> extensions = result.getErrors().get(0).getExtensions();
    assertEquals(List.of("can't be empty"), extensions.get("username"));
    assertFalse(extensions.containsKey("email"));
  }

  @Test
  public void should_delegate_other_exceptions_to_default_handler() throws Exception {
    DataFetcherExceptionHandlerResult result =
        handler.handleException(params(new RuntimeException("boom"), "/article")).get();

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertTrue(error.getMessage().contains("boom"));
    assertEquals(List.of("article"), error.getPath());
  }

  @Test
  public void should_convert_constraint_violations_to_error_data() {
    ConstraintViolationException exception =
        new ConstraintViolationException(validator.validate(new Param("not-an-email", "")));

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(exception);

    assertEquals("BAD_REQUEST", error.getMessage());
    Map<String, List<String>> errors =
        error.getErrors().stream()
            .collect(Collectors.toMap(ErrorItem::getKey, ErrorItem::getValue));
    assertEquals(2, errors.size());
    assertEquals(Arrays.asList("must be a well-formed email address"), errors.get("email"));
    assertEquals(Arrays.asList("can't be empty"), errors.get("username"));
  }

  private DataFetcherExceptionHandlerParameters params(Throwable exception, String path) {
    DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
    ExecutionStepInfo stepInfo =
        ExecutionStepInfo.newExecutionStepInfo()
            .type(Scalars.GraphQLString)
            .path(ResultPath.parse(path))
            .build();
    when(environment.getExecutionStepInfo()).thenReturn(stepInfo);
    when(environment.getMergedField())
        .thenReturn(MergedField.newMergedField(Field.newField(path.substring(1)).build()).build());
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(environment)
        .exception(exception)
        .build();
  }

  static class Param {
    @Email private final String email;

    @NotBlank(message = "can't be empty")
    private final String username;

    Param(String email, String username) {
      this.email = email;
      this.username = username;
    }
  }

  static class Service {
    public void create(@Valid Param param) {}
  }
}
