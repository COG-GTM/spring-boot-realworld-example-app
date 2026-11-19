package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private GraphQLCustomizeExceptionHandler handler;
  private Validator validator;

  static class Bean {
    @NotBlank(message = "can't be empty")
    @Size(min = 3, message = "too short")
    String username;

    @NotBlank(message = "can't be empty")
    String password;

    Bean(String username, String password) {
      this.username = username;
      this.password = password;
    }
  }

  static class Inner {
    @NotBlank(message = "can't be empty")
    String email;

    Inner(String email) {
      this.email = email;
    }
  }

  static class Middle {
    @Valid Inner inner;

    Middle(Inner inner) {
      this.inner = inner;
    }
  }

  static class Outer {
    @Valid Middle middle;

    Outer(Middle middle) {
      this.middle = middle;
    }
  }

  @BeforeEach
  public void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  private DataFetcherExceptionHandlerParameters params(Throwable exception) {
    DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
    ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);
    when(stepInfo.getPath()).thenReturn(ResultPath.parse("/createUser"));
    when(env.getExecutionStepInfo()).thenReturn(stepInfo);
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(env)
        .exception(exception)
        .build();
  }

  private static Map<String, List<String>> asMap(Error error) {
    return error.getErrors().stream()
        .collect(Collectors.toMap(ErrorItem::getKey, ErrorItem::getValue));
  }

  @Test
  public void should_map_invalid_authentication_to_unauthenticated_error() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(params(new InvalidAuthenticationException()));

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals("invalid email or password", error.getMessage());
    assertEquals(Collections.singletonList("createUser"), error.getPath());
    assertEquals("UNAUTHENTICATED", error.getExtensions().get("errorType"));
  }

  @Test
  public void should_map_constraint_violation_to_bad_request_with_field_extensions() {
    ConstraintViolationException cve =
        new ConstraintViolationException(validator.validate(new Bean("ab", "")));

    DataFetcherExceptionHandlerResult result = handler.onException(params(cve));

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals(Collections.singletonList("createUser"), error.getPath());
    Map<String, Object> extensions = error.getExtensions();
    assertEquals("BAD_REQUEST", extensions.get("errorType"));
    assertEquals(Collections.singletonList("too short"), extensions.get("username"));
    assertEquals(Collections.singletonList("can't be empty"), extensions.get("password"));
  }

  @Test
  public void should_delegate_other_exceptions_to_default_handler() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(params(new IllegalStateException("boom")));

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertTrue(error.getMessage().contains("IllegalStateException"));
    assertTrue(error.getMessage().contains("boom"));
    assertEquals(Collections.singletonList("createUser"), error.getPath());
  }

  @Test
  public void get_errors_as_data_should_group_messages_by_field() {
    ConstraintViolationException cve =
        new ConstraintViolationException(validator.validate(new Bean("", "")));

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertEquals("BAD_REQUEST", error.getMessage());
    Map<String, List<String>> byField = asMap(error);
    assertEquals(2, byField.size());
    List<String> usernameMessages = byField.get("username");
    Collections.sort(usernameMessages);
    assertEquals(Arrays.asList("can't be empty", "too short"), usernameMessages);
    assertEquals(Collections.singletonList("can't be empty"), byField.get("password"));
  }

  @Test
  public void get_errors_as_data_should_strip_first_two_segments_of_nested_property_path() {
    ConstraintViolationException cve =
        new ConstraintViolationException(validator.validate(new Outer(new Middle(new Inner("")))));

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertEquals(1, error.getErrors().size());
    assertEquals("email", error.getErrors().get(0).getKey());
  }

  @Test
  public void get_errors_as_data_should_return_empty_errors_for_no_violations() {
    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(
            new ConstraintViolationException(Collections.emptySet()));

    assertEquals("BAD_REQUEST", error.getMessage());
    assertTrue(error.getErrors().isEmpty());
  }
}
