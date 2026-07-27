package io.spring.graphql.exception;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private final GraphQLCustomizeExceptionHandler handler = new GraphQLCustomizeExceptionHandler();

  private DataFetcherExceptionHandlerParameters paramsFor(Throwable throwable) {
    DataFetchingEnvironment dfe = mock(DataFetchingEnvironment.class);
    ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);
    when(dfe.getExecutionStepInfo()).thenReturn(stepInfo);
    when(stepInfo.getPath()).thenReturn(ResultPath.rootPath());
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(dfe)
        .exception(throwable)
        .build();
  }

  static class SampleBean {
    @NotBlank(message = "can't be empty")
    private final String name;

    SampleBean(String name) {
      this.name = name;
    }
  }

  private ConstraintViolationException invalidRegisterException() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<SampleBean>> violations = validator.validate(new SampleBean(""));
    return new ConstraintViolationException(violations);
  }

  @Test
  public void should_handle_invalid_authentication_as_unauthenticated() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(paramsFor(new InvalidAuthenticationException()));

    Assertions.assertEquals(1, result.getErrors().size());
  }

  @Test
  public void should_handle_constraint_violation_as_bad_request() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(paramsFor(invalidRegisterException()));

    Assertions.assertEquals(1, result.getErrors().size());
  }

  @Test
  public void should_delegate_unknown_exception_to_default_handler() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(paramsFor(new RuntimeException("boom")));

    Assertions.assertFalse(result.getErrors().isEmpty());
  }

  @Test
  public void should_convert_constraint_violations_to_error_data() {
    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(invalidRegisterException());

    Assertions.assertEquals("BAD_REQUEST", error.getMessage());
    Assertions.assertFalse(error.getErrors().isEmpty());
  }
}
