package io.spring.graphql;

import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Records the exception a datafetcher raised and then delegates to the production handler, so the
 * GraphQL response is exactly what the application would return while tests can still assert on the
 * exception itself instead of on the DGS error message format.
 */
@Component
@Primary
public class RecordingExceptionHandler implements DataFetcherExceptionHandler {

  private final GraphQLCustomizeExceptionHandler delegate;
  private final List<Throwable> raised = new CopyOnWriteArrayList<>();

  public RecordingExceptionHandler(GraphQLCustomizeExceptionHandler delegate) {
    this.delegate = delegate;
  }

  @Override
  public DataFetcherExceptionHandlerResult onException(
      DataFetcherExceptionHandlerParameters handlerParameters) {
    raised.add(handlerParameters.getException());
    return delegate.onException(handlerParameters);
  }

  List<Throwable> raised() {
    return raised;
  }

  void clear() {
    raised.clear();
  }
}
