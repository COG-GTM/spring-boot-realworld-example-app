package io.spring.graphql.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.netflix.graphql.types.errors.ErrorType;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private final GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

  @Test
  void should_map_invalid_authentication_to_unauthenticated_error() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parametersFor(new InvalidAuthenticationException()));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getMessage()).isEqualTo("invalid email or password");
    assertThat(error.getExtensions()).containsEntry("errorType", ErrorType.UNAUTHENTICATED.name());
    assertThat(error.getPath()).containsExactly("login");
  }

  @Test
  void should_map_constraint_violation_to_bad_request_error_with_field_extensions() {
    ConstraintViolationException cve = constraintViolationException();

    DataFetcherExceptionHandlerResult result = handler.onException(parametersFor(cve));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getExtensions()).containsEntry("errorType", ErrorType.BAD_REQUEST.name());
    assertThat(error.getPath()).containsExactly("login");
    @SuppressWarnings("unchecked")
    Map<String, Object> extensions = (Map<String, Object>) error.getExtensions();
    assertThat(extensions).containsKeys("email", "username");
    assertThat((List<String>) extensions.get("email"))
        .containsExactly("must be a well-formed email address");
    assertThat((List<String>) extensions.get("username")).containsExactly("can't be empty");
  }

  @Test
  void should_delegate_unhandled_exceptions_to_default_handler() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parametersFor(new RuntimeException("boom")));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("boom");
    assertThat(result.getErrors().get(0).getExtensions())
        .containsEntry("errorType", ErrorType.INTERNAL.name());
  }

  @Test
  void should_convert_constraint_violations_to_error_data() {
    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(constraintViolationException());

    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors())
        .extracting(ErrorItem::getKey)
        .containsExactlyInAnyOrder("email", "username");
    assertThat(error.getErrors())
        .filteredOn(item -> item.getKey().equals("username"))
        .flatExtracting(ErrorItem::getValue)
        .containsExactly("can't be empty");
  }

  @Test
  void should_convert_empty_constraint_violations_to_empty_error_data() {
    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(
            new ConstraintViolationException(java.util.Collections.emptySet()));

    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).isEmpty();
  }

  private ConstraintViolationException constraintViolationException() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<RegistrationForm>> violations =
        validator.validate(new RegistrationForm("not-an-email", ""));
    return new ConstraintViolationException(violations);
  }

  private DataFetcherExceptionHandlerParameters parametersFor(Throwable exception) {
    ExecutionStepInfo stepInfo =
        ExecutionStepInfo.newExecutionStepInfo()
            .type(GraphQLObjectType.newObject().name("Query").build())
            .path(ResultPath.parse("/login"))
            .build();
    DataFetchingEnvironment dataFetchingEnvironment = mock(DataFetchingEnvironment.class);
    when(dataFetchingEnvironment.getExecutionStepInfo()).thenReturn(stepInfo);
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(dataFetchingEnvironment)
        .exception(exception)
        .build();
  }

  private static class RegistrationForm {
    @Email private final String email;

    @NotBlank(message = "can't be empty")
    private final String username;

    RegistrationForm(String email, String username) {
      this.email = email;
      this.username = username;
    }
  }
}
