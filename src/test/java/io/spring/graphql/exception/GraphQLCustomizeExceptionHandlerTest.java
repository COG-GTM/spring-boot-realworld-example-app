package io.spring.graphql.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.netflix.graphql.types.errors.ErrorType;
import com.netflix.graphql.types.errors.TypedGraphQLError;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ResultPath;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GraphQLCustomizeExceptionHandlerTest {

  private GraphQLCustomizeExceptionHandler handler;
  private Validator validator;

  @BeforeEach
  void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  private DataFetcherExceptionHandlerParameters paramsWith(Throwable t) {
    DataFetcherExceptionHandlerParameters params =
        mock(DataFetcherExceptionHandlerParameters.class);
    when(params.getException()).thenReturn(t);
    when(params.getPath()).thenReturn(ResultPath.rootPath());
    return params;
  }

  private Set<ConstraintViolation<Bean>> violations() {
    Set<ConstraintViolation<Bean>> violations = validator.validate(new Bean());
    assertThat(violations).isNotEmpty();
    return violations;
  }

  private static class Bean {
    @NotBlank(message = "can't be empty")
    private String email = "";
  }

  @Test
  void should_map_invalid_authentication_to_unauthenticated() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(paramsWith(new InvalidAuthenticationException()));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error).isInstanceOf(TypedGraphQLError.class);
    assertThat(error.getMessage()).isEqualTo("invalid email or password");
    assertThat(error.toSpecification().toString()).contains(ErrorType.UNAUTHENTICATED.name());
  }

  @Test
  void should_map_constraint_violation_to_bad_request() {
    ConstraintViolationException cve = new ConstraintViolationException(violations());

    DataFetcherExceptionHandlerResult result = handler.onException(paramsWith(cve));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getExtensions()).isNotEmpty();
  }

  @Test
  void should_delegate_other_exceptions_to_default_handler() {
    DataFetcherExceptionHandlerParameters params = mock(DataFetcherExceptionHandlerParameters.class);
    when(params.getException()).thenReturn(new RuntimeException("boom"));
    when(params.getPath()).thenReturn(ResultPath.rootPath());

    DataFetcherExceptionHandlerResult result = handler.onException(params);

    assertThat(result.getErrors()).isNotEmpty();
  }

  @Test
  void should_build_errors_as_data() {
    ConstraintViolationException cve = new ConstraintViolationException(violations());

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).isNotEmpty();
  }
}
