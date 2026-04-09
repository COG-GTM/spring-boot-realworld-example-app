package io.spring.graphql.exception;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import io.spring.graphql.types.ErrorItem;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private GraphQLCustomizeExceptionHandler handler;

  @BeforeEach
  public void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
  }

  @Test
  public void should_handle_invalid_authentication_exception() {
    InvalidAuthenticationException exception = new InvalidAuthenticationException();
    DataFetcherExceptionHandlerParameters params = buildHandlerParameters(exception);

    DataFetcherExceptionHandlerResult result = handler.onException(params);

    assertThat(result.getErrors(), hasSize(1));
    assertThat(result.getErrors().get(0).getMessage(), is(exception.getMessage()));
  }

  @Test
  public void should_handle_constraint_violation_exception() {
    ConstraintViolation<?> violation =
        mockViolation("SomeBean", "arg0.field.name", "NotBlank", "must not be blank");
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);
    ConstraintViolationException exception =
        new ConstraintViolationException("Validation failed", violations);

    DataFetcherExceptionHandlerParameters params = buildHandlerParameters(exception);

    DataFetcherExceptionHandlerResult result = handler.onException(params);

    assertThat(result.getErrors(), hasSize(1));
    assertThat(result.getErrors().get(0).getMessage(), is("Validation failed"));
  }

  @Test
  public void should_handle_constraint_violation_with_extensions() {
    ConstraintViolation<?> violation =
        mockViolation("SomeBean", "arg0.field.name", "NotBlank", "must not be blank");
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);
    ConstraintViolationException exception =
        new ConstraintViolationException("Validation failed", violations);

    DataFetcherExceptionHandlerParameters params = buildHandlerParameters(exception);

    DataFetcherExceptionHandlerResult result = handler.onException(params);

    Map<String, Object> extensions = result.getErrors().get(0).getExtensions();
    assertThat(extensions.containsKey("name"), is(true));
    List<String> nameErrors = (List<String>) extensions.get("name");
    assertThat(nameErrors, hasSize(1));
    assertThat(nameErrors.get(0), is("must not be blank"));
  }

  @Test
  public void should_handle_other_exception_with_default_handler() {
    RuntimeException exception = new RuntimeException("some error");
    DataFetcherExceptionHandlerParameters params = buildHandlerParameters(exception);

    DataFetcherExceptionHandlerResult result = handler.onException(params);

    assertThat(result.getErrors(), hasSize(1));
  }

  @Test
  public void should_get_errors_as_data_from_constraint_violation() {
    ConstraintViolation<?> violation =
        mockViolation("SomeBean", "arg0.field.email", "Email", "must be a valid email");
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(exception);

    assertThat(error.getMessage(), is("BAD_REQUEST"));
    assertThat(error.getErrors(), hasSize(1));
    ErrorItem item = error.getErrors().get(0);
    assertThat(item.getKey(), is("email"));
    assertThat(item.getValue(), hasSize(1));
    assertThat(item.getValue().get(0), is("must be a valid email"));
  }

  @Test
  public void should_get_errors_as_data_with_multiple_violations() {
    ConstraintViolation<?> violation1 =
        mockViolation("SomeBean", "arg0.field.email", "Email", "must be a valid email");
    ConstraintViolation<?> violation2 =
        mockViolation("SomeBean", "arg0.field.email", "NotBlank", "must not be blank");
    ConstraintViolation<?> violation3 =
        mockViolation("SomeBean", "arg0.field.username", "NotBlank", "must not be blank");

    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation1);
    violations.add(violation2);
    violations.add(violation3);
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(exception);

    assertThat(error.getMessage(), is("BAD_REQUEST"));
    assertThat(error.getErrors(), hasSize(2));

    List<String> keys =
        error.getErrors().stream()
            .map(ErrorItem::getKey)
            .collect(java.util.stream.Collectors.toList());
    assertThat(keys, containsInAnyOrder("email", "username"));
  }

  @Test
  public void should_handle_single_segment_property_path() {
    ConstraintViolation<?> violation =
        mockViolation("SomeBean", "fieldName", "NotBlank", "must not be blank");
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(exception);

    assertThat(error.getErrors(), hasSize(1));
    assertThat(error.getErrors().get(0).getKey(), is("fieldName"));
  }

  @Test
  public void should_handle_multi_segment_property_path() {
    ConstraintViolation<?> violation =
        mockViolation("SomeBean", "arg0.field.nested.deep", "NotBlank", "must not be blank");
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(violation);
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(exception);

    assertThat(error.getErrors(), hasSize(1));
    assertThat(error.getErrors().get(0).getKey(), is("nested.deep"));
  }

  @SuppressWarnings("unchecked")
  private ConstraintViolation<?> mockViolation(
      String rootBeanClassName, String propertyPath, String annotationSimpleName, String message) {
    ConstraintViolation violation = mock(ConstraintViolation.class);

    doReturn(String.class).when(violation).getRootBeanClass();

    Path path = mock(Path.class);
    when(path.toString()).thenReturn(propertyPath);
    doReturn(path).when(violation).getPropertyPath();

    ConstraintDescriptor descriptor = mock(ConstraintDescriptor.class);
    javax.validation.constraints.NotBlank mockAnnotation =
        mock(javax.validation.constraints.NotBlank.class);
    doReturn(mockAnnotation).when(descriptor).getAnnotation();
    doReturn(javax.validation.constraints.NotBlank.class).when(mockAnnotation).annotationType();
    doReturn(descriptor).when(violation).getConstraintDescriptor();

    doReturn(message).when(violation).getMessage();

    return violation;
  }

  private DataFetcherExceptionHandlerParameters buildHandlerParameters(Exception exception) {
    DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
    ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);
    when(stepInfo.getPath()).thenReturn(ResultPath.rootPath());
    when(environment.getExecutionStepInfo()).thenReturn(stepInfo);
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(environment)
        .exception(exception)
        .build();
  }
}
