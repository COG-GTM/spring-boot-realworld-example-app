package io.spring.graphql.exception;

import static org.assertj.core.api.Assertions.assertThat;

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
import io.spring.application.user.UpdateUserParam;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import org.junit.jupiter.api.Test;

class GraphQLCustomizeExceptionHandlerTest {

  private final GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

  @Test
  void should_map_invalid_authentication_to_unauthenticated_error() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parametersFor(new InvalidAuthenticationException()));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getExtensions()).containsEntry("errorType", ErrorType.UNAUTHENTICATED.name());
    assertThat(error.getPath()).containsExactly("me");
  }

  @Test
  @SuppressWarnings("unchecked")
  void should_map_constraint_violation_to_bad_request_error_with_field_extensions() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parametersFor(invalidEmailViolation()));

    assertThat(result.getErrors()).hasSize(1);
    Map<String, Object> extensions = result.getErrors().get(0).getExtensions();
    assertThat(extensions).containsKey("email");
    assertThat((List<Object>) extensions.get("email")).containsExactly("should be an email");
  }

  @Test
  void should_delegate_unknown_exception_to_default_handler() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parametersFor(new RuntimeException("boom")));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("boom");
  }

  @Test
  void should_convert_constraint_violations_to_error_data() {
    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(invalidEmailViolation());

    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).hasSize(1);
    ErrorItem errorItem = error.getErrors().get(0);
    assertThat(errorItem.getKey()).isEqualTo("email");
    assertThat(errorItem.getValue()).containsExactly("should be an email");
  }

  private ConstraintViolationException invalidEmailViolation() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<UpdateUserParam>> violations =
        validator.validate(UpdateUserParam.builder().email("invalid").build());
    return new ConstraintViolationException(violations);
  }

  private DataFetcherExceptionHandlerParameters parametersFor(Throwable exception) {
    DataFetchingEnvironment dataFetchingEnvironment =
        DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .mergedField(MergedField.newMergedField(Field.newField("me").build()).build())
            .executionStepInfo(
                ExecutionStepInfo.newExecutionStepInfo()
                    .type(Scalars.GraphQLString)
                    .path(ResultPath.parse("/me"))
                    .build())
            .build();
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(dataFetchingEnvironment)
        .exception(exception)
        .build();
  }
}
