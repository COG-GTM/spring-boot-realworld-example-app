package io.spring.graphql.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.netflix.graphql.types.errors.ErrorType;
import graphql.GraphQLError;
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
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;

class GraphQLCustomizeExceptionHandlerTest {

  private final GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

  static class Payload {
    @NotBlank(message = "can't be empty")
    private final String username;

    @NotBlank(message = "can't be empty")
    @Email(message = "should be an email")
    private final String email;

    Payload(String username, String email) {
      this.username = username;
      this.email = email;
    }
  }

  private static ConstraintViolationException violationException(String username, String email) {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<Payload>> violations = validator.validate(new Payload(username, email));
    return new ConstraintViolationException(violations);
  }

  private static DataFetcherExceptionHandlerParameters parameters(Throwable exception) {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);
    when(stepInfo.getPath()).thenReturn(ResultPath.parse("/createUser"));
    when(dfe.getExecutionStepInfo()).thenReturn(stepInfo);
    when(dfe.getMergedField())
        .thenReturn(MergedField.newMergedField(Field.newField("createUser").build()).build());
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(dfe)
        .exception(exception)
        .build();
  }

  @Test
  void should_map_invalid_authentication_to_unauthenticated_error() {
    InvalidAuthenticationException exception = new InvalidAuthenticationException();

    DataFetcherExceptionHandlerResult result = handler.onException(parameters(exception));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getMessage()).isEqualTo(exception.getMessage());
    assertThat(error.getExtensions()).containsEntry("errorType", ErrorType.UNAUTHENTICATED.name());
    assertThat(error.getPath()).containsExactly("createUser");
  }

  @Test
  @SuppressWarnings("unchecked")
  void should_map_constraint_violations_to_bad_request_error_with_extensions() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parameters(violationException("", "not-an-email")));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getExtensions()).containsEntry("errorType", ErrorType.BAD_REQUEST.name());
    assertThat(error.getExtensions()).containsKeys("username", "email");
    assertThat((List<Object>) error.getExtensions().get("username"))
        .containsExactly("can't be empty");
    assertThat((List<Object>) error.getExtensions().get("email"))
        .containsExactly("should be an email");
  }

  @Test
  void should_delegate_unknown_exceptions_to_default_handler() {
    RuntimeException exception = new RuntimeException("boom");

    DataFetcherExceptionHandlerResult result = handler.onException(parameters(exception));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("boom");
  }

  @Test
  void should_convert_violations_to_error_data() {
    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(violationException("", "not-an-email"));

    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).hasSize(2);
    assertThat(error.getErrors())
        .extracting(ErrorItem::getKey)
        .containsExactlyInAnyOrder("username", "email");
    ErrorItem usernameError =
        error.getErrors().stream()
            .filter(item -> item.getKey().equals("username"))
            .findFirst()
            .orElseThrow(AssertionError::new);
    assertThat(usernameError.getValue()).containsExactly("can't be empty");
  }

  @Test
  void should_group_multiple_messages_for_the_same_field() {
    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(violationException("jake", ""));

    assertThat(error.getErrors()).hasSize(1);
    assertThat(error.getErrors().get(0).getKey()).isEqualTo("email");
    assertThat(error.getErrors().get(0).getValue()).containsExactly("can't be empty");
  }
}
