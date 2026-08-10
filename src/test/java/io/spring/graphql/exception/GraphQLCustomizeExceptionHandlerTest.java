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
import io.spring.graphql.types.ErrorItem;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  private GraphQLCustomizeExceptionHandler handler;

  static class SampleBean {
    @NotBlank(message = "can't be empty")
    private String title;

    SampleBean(String title) {
      this.title = title;
    }
  }

  @BeforeAll
  public static void setUpValidator() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  public static void tearDownValidator() {
    validatorFactory.close();
  }

  @BeforeEach
  public void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
  }

  private ConstraintViolationException violationException() {
    Set<ConstraintViolation<SampleBean>> violations = validator.validate(new SampleBean(""));
    Assertions.assertFalse(violations.isEmpty());
    return new ConstraintViolationException(violations);
  }

  private DataFetcherExceptionHandlerParameters params(Throwable exception) {
    DataFetchingEnvironment environment = mock(DataFetchingEnvironment.class);
    ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);
    when(environment.getExecutionStepInfo()).thenReturn(stepInfo);
    when(stepInfo.getPath()).thenReturn(ResultPath.rootPath());
    return DataFetcherExceptionHandlerParameters.newExceptionParameters()
        .dataFetchingEnvironment(environment)
        .exception(exception)
        .build();
  }

  @Test
  public void should_handle_invalid_authentication_exception() {
    DataFetcherExceptionHandlerResult result =
        handler.onException(params(new InvalidAuthenticationException()));

    Assertions.assertEquals(1, result.getErrors().size());
    Assertions.assertEquals("invalid email or password", result.getErrors().get(0).getMessage());
  }

  @Test
  public void should_handle_constraint_violation_exception() {
    DataFetcherExceptionHandlerResult result = handler.onException(params(violationException()));

    Assertions.assertEquals(1, result.getErrors().size());
    Assertions.assertTrue(result.getErrors().get(0).getExtensions().containsKey("title"));
  }

  @Test
  public void should_convert_constraint_violations_to_error_data() {
    Error error = GraphQLCustomizeExceptionHandler.getErrorsAsData(violationException());

    Assertions.assertEquals("BAD_REQUEST", error.getMessage());
    Assertions.assertEquals(1, error.getErrors().size());
    ErrorItem item = error.getErrors().get(0);
    Assertions.assertEquals("title", item.getKey());
    Assertions.assertEquals("can't be empty", item.getValue().get(0));
  }
}
