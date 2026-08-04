package io.spring.graphql.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.GraphQLError;
import graphql.Scalars;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.core.user.User;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.constraints.NotBlank;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.Test;

class GraphQLCustomizeExceptionHandlerTest {

  private final GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

  @Test
  void should_map_invalid_authentication_to_unauthenticated_error() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parametersFor(new InvalidAuthenticationException()));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getMessage()).isEqualTo("invalid email or password");
    assertThat(error.getPath()).containsExactly("login");
    assertThat(error.getExtensions().get("errorType")).hasToString("UNAUTHENTICATED");
  }

  @Test
  void should_map_constraint_violations_to_bad_request_extensions() {
    ConstraintViolationException cve =
        new ConstraintViolationException(
            "invalid",
            violations(
                violation("createUser.arg0.email", "can't be empty"),
                violation("createUser.arg0.username", "already exist")));

    DataFetcherExceptionHandlerResult result = handler.onException(parametersFor(cve));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getMessage()).isEqualTo("invalid");
    // The field errors are attached under their (stripped) property path.
    Map<String, Object> extensions = error.getExtensions();
    assertThat(extensions.get("email")).isEqualTo(Arrays.asList("can't be empty"));
    assertThat(extensions.get("username")).isEqualTo(Arrays.asList("already exist"));
  }

  @Test
  void should_fall_through_to_default_handler_for_unknown_exceptions() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(parametersFor(new IllegalStateException("boom")));

    assertThat(result.getErrors()).hasSize(1);
    GraphQLError error = result.getErrors().get(0);
    assertThat(error.getMessage()).contains("boom");
    // The DGS default handler classifies anything it does not know about as INTERNAL.
    assertThat(error.getExtensions().get("errorType")).hasToString("INTERNAL");
  }

  @Test
  void should_build_error_payload_data_from_constraint_violations() {
    ConstraintViolationException cve =
        new ConstraintViolationException(
            violations(
                violation("createUser.arg0.email", "can't be empty"),
                violation("createUser.arg0.email", "should be an email")));

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertThat(error.getMessage()).isEqualTo("BAD_REQUEST");
    assertThat(error.getErrors()).hasSize(1);
    ErrorItem item = error.getErrors().get(0);
    assertThat(item.getKey()).isEqualTo("email");
    assertThat(item.getValue()).containsExactlyInAnyOrder("can't be empty", "should be an email");
  }

  @Test
  void should_keep_single_segment_property_path_as_is() {
    ConstraintViolationException cve =
        new ConstraintViolationException(violations(violation("password", "too short")));

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    List<ErrorItem> items = error.getErrors();
    assertThat(items).hasSize(1);
    assertThat(items.get(0).getKey()).isEqualTo("password");
    assertThat(items.get(0).getValue()).containsExactly("too short");
  }

  /**
   * Known defect, pinned here so a fix is a deliberate change: {@code getParam} drops the first two
   * segments of a multi-segment path, so a two-segment path leaves an empty key behind even though
   * the schema declares {@code ErrorItem.key} as non-null and clients cannot use it.
   */
  @Test
  void should_currently_produce_an_empty_key_for_a_two_segment_property_path() {
    ConstraintViolationException cve =
        new ConstraintViolationException(violations(violation("createUser.email", "too short")));

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertThat(error.getErrors()).hasSize(1);
    assertThat(error.getErrors().get(0).getKey()).isEmpty();
    assertThat(error.getErrors().get(0).getValue()).containsExactly("too short");
  }

  private DataFetcherExceptionHandlerParameters parametersFor(Throwable throwable) {
    ExecutionStepInfo stepInfo =
        ExecutionStepInfo.newExecutionStepInfo()
            .type(Scalars.GraphQLString)
            .path(ResultPath.rootPath().segment("login"))
            .build();
    DataFetchingEnvironment environment =
        DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .executionStepInfo(stepInfo)
            .build();
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(environment)
        .exception(throwable)
        .build();
  }

  private Set<ConstraintViolation<?>> violations(ConstraintViolation<?>... violations) {
    return new HashSet<>(Arrays.asList(violations));
  }

  private ConstraintViolation<?> violation(String propertyPath, String message) {
    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    when(path.toString()).thenReturn(propertyPath);
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn(message);
    org.mockito.Mockito.<Class<?>>when(violation.getRootBeanClass()).thenReturn(User.class);

    ConstraintDescriptor<?> descriptor = mock(ConstraintDescriptor.class);
    Annotation annotation = mock(NotBlank.class);
    org.mockito.Mockito.<Class<? extends Annotation>>when(annotation.annotationType())
        .thenReturn(NotBlank.class);
    when(descriptor.getAnnotation()).thenReturn((Annotation) annotation);
    org.mockito.Mockito.<ConstraintDescriptor<?>>when(violation.getConstraintDescriptor())
        .thenReturn(descriptor);
    return violation;
  }
}
