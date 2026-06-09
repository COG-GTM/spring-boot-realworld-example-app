package io.spring.graphql.exception;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private GraphQLCustomizeExceptionHandler handler;
  private DataFetchingEnvironment dfe;

  @BeforeEach
  public void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
    dfe = mock(DataFetchingEnvironment.class);
    ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);
    when(stepInfo.getPath()).thenReturn(ResultPath.rootPath());
    when(dfe.getExecutionStepInfo()).thenReturn(stepInfo);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private ConstraintViolation<?> buildMockViolation(String pathStr, String message) {
    ConstraintViolation violation = mock(ConstraintViolation.class);
    when(violation.getRootBeanClass()).thenReturn((Class) String.class);
    when(violation.getMessage()).thenReturn(message);

    Path path = mock(Path.class);
    when(path.toString()).thenReturn(pathStr);
    when(violation.getPropertyPath()).thenReturn(path);

    ConstraintDescriptor descriptor = mock(ConstraintDescriptor.class);
    Annotation annotation = mock(Annotation.class);
    when(annotation.annotationType()).thenReturn((Class) Override.class);
    when(descriptor.getAnnotation()).thenReturn(annotation);
    when(violation.getConstraintDescriptor()).thenReturn(descriptor);
    return violation;
  }

  private DataFetcherExceptionHandlerParameters buildParams(Throwable ex) {
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(dfe)
        .exception(ex)
        .build();
  }

  @Test
  public void should_handle_invalid_authentication_exception() {
    InvalidAuthenticationException ex = new InvalidAuthenticationException();
    DataFetcherExceptionHandlerResult result = handler.onException(buildParams(ex));
    assertThat(result, notNullValue());
    assertThat(result.getErrors().size(), is(1));
  }

  @Test
  public void should_handle_constraint_violation_exception() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(buildMockViolation("registerParam.email", "can't be empty"));

    ConstraintViolationException cve = new ConstraintViolationException("validation", violations);
    DataFetcherExceptionHandlerResult result = handler.onException(buildParams(cve));
    assertThat(result, notNullValue());
    assertThat(result.getErrors().size(), is(1));
  }

  @Test
  public void should_delegate_unknown_exceptions_to_default_handler() {
    RuntimeException ex = new RuntimeException("some error");
    DataFetcherExceptionHandlerResult result = handler.onException(buildParams(ex));
    assertThat(result, notNullValue());
  }

  @Test
  public void should_get_errors_as_data() {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    violations.add(buildMockViolation("registerParam.email", "can't be empty"));

    ConstraintViolationException cve = new ConstraintViolationException("validation", violations);
    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);
    assertThat(error, notNullValue());
    assertThat(error.getMessage(), is("BAD_REQUEST"));
    assertThat(error.getErrors().size(), is(1));
  }
}
