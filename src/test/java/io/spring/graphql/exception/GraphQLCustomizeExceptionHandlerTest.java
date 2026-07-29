package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.netflix.graphql.types.errors.ErrorType;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ResultPath;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.ConstraintViolationFixture;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private final GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

  @Test
  public void should_map_invalid_authentication_to_unauthenticated_error() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parametersFor(new InvalidAuthenticationException()));

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals("invalid email or password", error.getMessage());
    assertEquals(ErrorType.UNAUTHENTICATED.name(), errorTypeOf(error));
    assertEquals(List.of("me"), error.getPath());
  }

  @Test
  public void should_map_constraint_violations_to_bad_request_error_with_extensions() {
    ConstraintViolationException exception = ConstraintViolationFixture.propertyPathViolations();

    DataFetcherExceptionHandlerResult result = handler.onException(parametersFor(exception));

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals(ErrorType.BAD_REQUEST.name(), errorTypeOf(error));
    Map<String, Object> extensions = error.getExtensions();
    assertTrue(extensions.containsKey("email"));
    assertTrue(extensions.containsKey("username"));
    assertEquals(List.of("can't be empty"), extensions.get("username"));
    assertEquals(2, ((List<?>) extensions.get("email")).size());
  }

  @Test
  public void should_delegate_unknown_exceptions_to_the_default_handler() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parametersFor(new RuntimeException("boom")));

    assertEquals(1, result.getErrors().size());
    assertEquals(ErrorType.INTERNAL.name(), errorTypeOf(result.getErrors().get(0)));
  }

  @Test
  public void should_expose_property_path_violations_as_data() {
    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(
            ConstraintViolationFixture.propertyPathViolations());

    assertEquals("BAD_REQUEST", error.getMessage());
    Map<String, List<String>> byKey = byKey(error);
    assertEquals(List.of("can't be empty"), byKey.get("username"));
    assertEquals(2, byKey.get("email").size());
  }

  @Test
  public void should_strip_method_prefix_from_violation_paths() {
    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(
            ConstraintViolationFixture.methodPathViolations());

    Map<String, List<String>> byKey = byKey(error);
    assertEquals(List.of("can't be empty"), byKey.get("username"));
    assertNotNull(byKey.get("email"));
  }

  private Map<String, List<String>> byKey(Error error) {
    return error.getErrors().stream()
        .collect(Collectors.toMap(ErrorItem::getKey, ErrorItem::getValue));
  }

  private String errorTypeOf(GraphQLError error) {
    return String.valueOf(error.getExtensions().get("errorType"));
  }

  private DataFetcherExceptionHandlerParameters parametersFor(Throwable throwable) {
    DataFetcherExceptionHandlerParameters parameters =
        mock(DataFetcherExceptionHandlerParameters.class);
    when(parameters.getException()).thenReturn(throwable);
    when(parameters.getPath()).thenReturn(ResultPath.rootPath().segment("me"));
    return parameters;
  }
}
