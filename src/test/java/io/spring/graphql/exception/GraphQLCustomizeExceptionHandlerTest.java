package io.spring.graphql.exception;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.graphql.types.Error;
import java.util.Collections;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GraphQLCustomizeExceptionHandlerTest {

  @Mock private DataFetchingEnvironment dataFetchingEnvironment;
  @Mock private ExecutionStepInfo executionStepInfo;

  private GraphQLCustomizeExceptionHandler handler;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    handler = new GraphQLCustomizeExceptionHandler();
    when(dataFetchingEnvironment.getExecutionStepInfo()).thenReturn(executionStepInfo);
    when(executionStepInfo.getPath()).thenReturn(ResultPath.rootPath());
  }

  private DataFetcherExceptionHandlerParameters params(Throwable t) {
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(dataFetchingEnvironment)
        .exception(t)
        .build();
  }

  @Test
  public void should_handle_invalid_authentication_exception() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(params(new InvalidAuthenticationException()));
    assertThat(result.getErrors().size(), is(1));
  }

  @Test
  public void should_handle_constraint_violation_exception() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(params(new ConstraintViolationException(Collections.emptySet())));
    assertThat(result.getErrors().size(), is(1));
  }

  @Test
  public void should_delegate_other_exceptions_to_default_handler() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(params(new RuntimeException("boom")));
    assertThat(result.getErrors().size(), is(1));
  }

  @Test
  public void should_convert_constraint_violation_to_error_data() {
    Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(
            new ConstraintViolationException(Collections.emptySet()));
    assertThat(error.getMessage(), is("BAD_REQUEST"));
    assertThat(error.getErrors().isEmpty(), is(true));
  }
}
