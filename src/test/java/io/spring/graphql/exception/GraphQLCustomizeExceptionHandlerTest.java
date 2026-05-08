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
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private final GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

  private DataFetcherExceptionHandlerParameters buildParams(Throwable exception) {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);
    when(dfe.getExecutionStepInfo()).thenReturn(stepInfo);
    when(stepInfo.getPath()).thenReturn(ResultPath.rootPath());
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(dfe)
        .exception(exception)
        .build();
  }

  @Test
  public void should_return_unauthenticated_error_for_invalid_authentication() {
    DataFetcherExceptionHandlerParameters params =
        buildParams(new InvalidAuthenticationException());

    DataFetcherExceptionHandlerResult result = handler.onException(params);

    assertNotNull(result);
    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals(
        ErrorType.UNAUTHENTICATED.toString(),
        error.toSpecification().get("extensions").toString().contains("UNAUTHENTICATED")
            ? ErrorType.UNAUTHENTICATED.toString()
            : "MISSING");
  }

  @Test
  public void should_return_bad_request_error_for_constraint_violation() {
    Set<ConstraintViolation<?>> violations = Collections.singleton(buildViolation());
    ConstraintViolationException exception = new ConstraintViolationException(violations);
    DataFetcherExceptionHandlerParameters params = buildParams(exception);

    DataFetcherExceptionHandlerResult result = handler.onException(params);

    assertNotNull(result);
    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertTrue(
        error.toSpecification().get("extensions").toString().contains("BAD_REQUEST"),
        "expected BAD_REQUEST in extensions but got: " + error.toSpecification());
    assertNotNull(error.getExtensions());
  }

  @Test
  public void should_delegate_other_exceptions_to_default_handler() {
    DataFetcherExceptionHandlerParameters params = buildParams(new RuntimeException("boom"));

    DataFetcherExceptionHandlerResult result = handler.onException(params);

    assertNotNull(result);
    assertTrue(result.getErrors().size() >= 1);
  }

  @Test
  public void should_convert_constraint_violations_to_error_data() {
    Set<ConstraintViolation<?>> violations = Collections.singleton(buildViolation());
    ConstraintViolationException cve = new ConstraintViolationException(violations);

    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(cve);

    assertNotNull(error);
    assertEquals("BAD_REQUEST", error.getMessage());
    assertEquals(1, error.getErrors().size());
    assertEquals("title", error.getErrors().get(0).getKey());
    List<String> messages = error.getErrors().get(0).getValue();
    assertEquals("must not be blank", messages.get(0));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private ConstraintViolation<?> buildViolation() {
    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    when(path.toString()).thenReturn("createArticle.params.title");
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn("must not be blank");
    when(violation.getRootBeanClass()).thenAnswer(i -> Object.class);
    ConstraintDescriptor<?> descriptor = mock(ConstraintDescriptor.class);
    java.lang.annotation.Annotation annotation =
        new java.lang.annotation.Annotation() {
          @Override
          public Class<? extends java.lang.annotation.Annotation> annotationType() {
            return Override.class;
          }
        };
    when(descriptor.getAnnotation()).thenAnswer(i -> annotation);
    when(violation.getConstraintDescriptor()).thenAnswer(i -> descriptor);
    return violation;
  }
}
