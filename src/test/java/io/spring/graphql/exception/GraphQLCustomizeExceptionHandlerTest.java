package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ResultPath;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;

class GraphQLCustomizeExceptionHandlerTest {

  private final GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

  static class SimpleBean {
    @NotBlank private String name;
  }

  static class Inner {
    @NotBlank private String field;
  }

  static class Outer {
    @Valid private final Inner inner = new Inner();
  }

  private ConstraintViolationException simpleViolation() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<SimpleBean>> violations = validator.validate(new SimpleBean());
    return new ConstraintViolationException(violations);
  }

  private ConstraintViolationException nestedViolation() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<Outer>> violations = validator.validate(new Outer());
    return new ConstraintViolationException(violations);
  }

  private DataFetcherExceptionHandlerParameters paramsFor(Throwable t) {
    DataFetcherExceptionHandlerParameters params =
        mock(DataFetcherExceptionHandlerParameters.class);
    when(params.getException()).thenReturn(t);
    when(params.getPath()).thenReturn(ResultPath.rootPath());
    return params;
  }

  @Test
  void onException_maps_invalid_authentication_to_unauthenticated_error() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(paramsFor(new InvalidAuthenticationException()));

    assertEquals(1, result.getErrors().size());
    assertEquals("invalid email or password", result.getErrors().get(0).getMessage());
  }

  @Test
  void onException_maps_constraint_violation_to_bad_request_with_extensions() {
    DataFetcherExceptionHandlerResult result = handler.onException(paramsFor(simpleViolation()));

    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertTrue(error.getExtensions().containsKey("name"));
  }

  @Test
  void onException_delegates_other_exceptions_to_default_handler() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(paramsFor(new RuntimeException("boom")));

    assertFalse(result.getErrors().isEmpty());
  }

  @Test
  void getErrorsAsData_builds_error_payload_from_violations() {
    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(simpleViolation());

    assertEquals("BAD_REQUEST", error.getMessage());
    assertEquals(1, error.getErrors().size());
    assertEquals("name", error.getErrors().get(0).getKey());
    assertFalse(error.getErrors().get(0).getValue().isEmpty());
  }

  @Test
  void getErrorsAsData_handles_nested_property_paths() {
    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(nestedViolation());

    assertEquals("BAD_REQUEST", error.getMessage());
    assertEquals(1, error.getErrors().size());
  }
}
