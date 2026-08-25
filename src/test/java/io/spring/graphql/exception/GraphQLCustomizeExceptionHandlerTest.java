package io.spring.graphql.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

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
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.graphql.types.Error;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GraphQLCustomizeExceptionHandlerTest {

  @Mock private DataFetchingEnvironment dataFetchingEnvironment;

  private GraphQLCustomizeExceptionHandler handler;

  static class Inner {
    @NotBlank(message = "can't be empty")
    private String email;
  }

  static class Outer {
    @NotBlank(message = "can't be empty")
    private String username;

    @Valid private Inner inner = new Inner();
  }

  @BeforeEach
  public void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
    lenient()
        .when(dataFetchingEnvironment.getExecutionStepInfo())
        .thenReturn(
            ExecutionStepInfo.newExecutionStepInfo()
                .type(Scalars.GraphQLString)
                .path(ResultPath.rootPath().segment("user"))
                .build());
    lenient()
        .when(dataFetchingEnvironment.getMergedField())
        .thenReturn(MergedField.newMergedField(Field.newField("user").build()).build());
  }

  private DataFetcherExceptionHandlerResult handle(Throwable throwable) {
    return handler.onException(
        DataFetcherExceptionHandlerParameters.newExceptionParameters()
            .dataFetchingEnvironment(dataFetchingEnvironment)
            .exception(throwable)
            .build());
  }

  private static ConstraintViolationException constraintViolationException() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<Outer>> violations = validator.validate(new Outer());
    return new ConstraintViolationException(violations);
  }

  @Test
  public void should_map_invalid_authentication_to_unauthenticated_error() {
    DataFetcherExceptionHandlerResult result = handle(new InvalidAuthenticationException());

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getMessage()).isEqualTo("invalid email or password");
    assertThat(error.getPath()).containsExactly("user");
    assertThat(error.getExtensions()).containsEntry("errorType", "UNAUTHENTICATED");
  }

  @Test
  public void should_map_constraint_violation_to_bad_request_error() {
    DataFetcherExceptionHandlerResult result = handle(constraintViolationException());

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getPath()).containsExactly("user");
    assertThat(error.getExtensions()).containsEntry("errorType", "BAD_REQUEST");
    assertThat(error.getExtensions().get("username")).isEqualTo(List.of("can't be empty"));
    assertThat(error.getExtensions().get("")).isEqualTo(List.of("can't be empty"));
  }

  @Test
  public void should_fallback_to_default_handler_for_authentication_exception() {
    DataFetcherExceptionHandlerResult result = handle(new AuthenticationException());

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage())
        .contains(AuthenticationException.class.getName());
    assertThat(result.getErrors().get(0).getPath()).containsExactly("user");
  }

  @Test
  public void should_fallback_to_default_handler_for_resource_not_found() {
    DataFetcherExceptionHandlerResult result = handle(new ResourceNotFoundException());

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage())
        .contains(ResourceNotFoundException.class.getName());
  }

  @Test
  public void should_fallback_to_default_handler_for_unknown_exception() {
    DataFetcherExceptionHandlerResult result = handle(new RuntimeException("boom"));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("boom");
  }

  @Test
  public void should_convert_violations_to_error_data() {
    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(constraintViolationException());

    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).hasSize(2);
    Map<String, List<String>> byKey =
        error.getErrors().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    io.spring.graphql.types.ErrorItem::getKey,
                    io.spring.graphql.types.ErrorItem::getValue));
    assertThat(byKey).containsKeys("username", "");
    assertThat(byKey.get("username")).containsExactly("can't be empty");
    assertThat(byKey.get("")).containsExactly("can't be empty");
  }
}
