package io.spring.graphql.exception;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ResultPath;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.application.article.NewArticleParam;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {
  private GraphQLCustomizeExceptionHandler handler;
  private Validator validator;

  @BeforeEach
  public void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  private DataFetcherExceptionHandlerParameters params(Throwable exception, String path) {
    DataFetcherExceptionHandlerParameters parameters =
        mock(DataFetcherExceptionHandlerParameters.class);
    when(parameters.getException()).thenReturn(exception);
    when(parameters.getPath()).thenReturn(ResultPath.parse(path));
    return parameters;
  }

  @Test
  public void should_return_completed_unauthenticated_error_for_invalid_authentication() {
    CompletableFuture<DataFetcherExceptionHandlerResult> future =
        handler.handleException(params(new InvalidAuthenticationException(), "/login"));

    Assertions.assertTrue(future.isDone());
    List<GraphQLError> errors = future.join().getErrors();
    Assertions.assertEquals(1, errors.size());
    GraphQLError error = errors.get(0);
    Assertions.assertEquals("invalid email or password", error.getMessage());
    Assertions.assertEquals(Arrays.asList("login"), error.getPath());
    Assertions.assertEquals("UNAUTHENTICATED", error.getExtensions().get("errorType"));
  }

  @Test
  public void should_return_completed_bad_request_with_field_errors_for_constraint_violation() {
    Set<ConstraintViolation<NewArticleParam>> violations =
        validator.validate(new NewArticleParam(null, "desc", "body", null));
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    CompletableFuture<DataFetcherExceptionHandlerResult> future =
        handler.handleException(params(exception, "/createArticle"));

    Assertions.assertTrue(future.isDone());
    List<GraphQLError> errors = future.join().getErrors();
    Assertions.assertEquals(1, errors.size());
    Map<String, Object> extensions = errors.get(0).getExtensions();
    Assertions.assertEquals("BAD_REQUEST", extensions.get("errorType"));
    Assertions.assertEquals(Arrays.asList("can't be empty"), extensions.get("title"));
    Assertions.assertEquals(Arrays.asList("createArticle"), errors.get(0).getPath());
  }

  @Test
  public void should_build_error_data_from_constraint_violation() {
    Set<ConstraintViolation<NewArticleParam>> violations =
        validator.validate(new NewArticleParam("", "", "body", null));

    io.spring.graphql.types.Error error =
        GraphQLCustomizeExceptionHandler.getErrorsAsData(
            new ConstraintViolationException(violations));

    Assertions.assertEquals("BAD_REQUEST", error.getMessage());
    Assertions.assertEquals(2, error.getErrors().size());
  }
}
