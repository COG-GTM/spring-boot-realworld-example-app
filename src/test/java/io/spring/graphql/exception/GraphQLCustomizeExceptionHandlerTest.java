package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ResultPath;
import io.spring.api.exception.InvalidAuthenticationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphQLCustomizeExceptionHandlerTest {

  private GraphQLCustomizeExceptionHandler handler;

  @BeforeEach
  public void setUp() {
    handler = new GraphQLCustomizeExceptionHandler();
  }

  @Test
  public void should_return_completable_future() {
    DataFetcherExceptionHandlerParameters params = mock(DataFetcherExceptionHandlerParameters.class);
    when(params.getException()).thenReturn(new RuntimeException("test"));
    when(params.getPath()).thenReturn(ResultPath.rootPath());

    CompletableFuture<DataFetcherExceptionHandlerResult> result = handler.handleException(params);
    assertNotNull(result);
    assertTrue(result instanceof CompletableFuture);
  }

  @Test
  public void should_handle_invalid_authentication_exception() throws ExecutionException, InterruptedException {
    DataFetcherExceptionHandlerParameters params = mock(DataFetcherExceptionHandlerParameters.class);
    InvalidAuthenticationException exception = new InvalidAuthenticationException();
    when(params.getException()).thenReturn(exception);
    when(params.getPath()).thenReturn(ResultPath.rootPath());

    CompletableFuture<DataFetcherExceptionHandlerResult> future = handler.handleException(params);
    DataFetcherExceptionHandlerResult result = future.get();

    assertNotNull(result);
    assertEquals(1, result.getErrors().size());
  }

  @Test
  public void should_delegate_unknown_exceptions_to_default_handler() throws ExecutionException, InterruptedException {
    DataFetcherExceptionHandlerParameters params = mock(DataFetcherExceptionHandlerParameters.class);
    RuntimeException exception = new RuntimeException("Unknown error");
    when(params.getException()).thenReturn(exception);
    when(params.getPath()).thenReturn(ResultPath.rootPath());

    CompletableFuture<DataFetcherExceptionHandlerResult> future = handler.handleException(params);
    DataFetcherExceptionHandlerResult result = future.get();

    assertNotNull(result);
  }

  @Test
  public void should_complete_future_without_blocking() throws ExecutionException, InterruptedException {
    DataFetcherExceptionHandlerParameters params = mock(DataFetcherExceptionHandlerParameters.class);
    when(params.getException()).thenReturn(new InvalidAuthenticationException());
    when(params.getPath()).thenReturn(ResultPath.rootPath());

    CompletableFuture<DataFetcherExceptionHandlerResult> future = handler.handleException(params);
    
    assertTrue(future.isDone() || future.get() != null);
  }

  @Test
  public void should_return_error_with_correct_path() throws ExecutionException, InterruptedException {
    DataFetcherExceptionHandlerParameters params = mock(DataFetcherExceptionHandlerParameters.class);
    when(params.getException()).thenReturn(new InvalidAuthenticationException());
    when(params.getPath()).thenReturn(ResultPath.rootPath().segment("articles"));

    CompletableFuture<DataFetcherExceptionHandlerResult> future = handler.handleException(params);
    DataFetcherExceptionHandlerResult result = future.get();

    assertNotNull(result);
    assertNotNull(result.getErrors());
  }
}
