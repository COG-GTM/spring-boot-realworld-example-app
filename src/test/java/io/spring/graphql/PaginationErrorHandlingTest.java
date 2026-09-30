package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;

@SpringBootTest
@WithAnonymousUser
public class PaginationErrorHandlingTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @Test
  public void anonymous_feed_query_returns_unauthenticated_error() {
    ExecutionResult result = dgsQueryExecutor.execute("{ feed(first: 1) { edges { cursor } } }");

    assertSingleError(result, "UNAUTHENTICATED", "Authentication required");
    assertNull(((Map<?, ?>) result.getData()).get("feed"));
  }

  @Test
  public void non_numeric_after_cursor_returns_bad_request_error() {
    ExecutionResult result =
        dgsQueryExecutor.execute("{ articles(first: 1, after: \"x\") { edges { cursor } } }");

    assertSingleError(result, "BAD_REQUEST", "Invalid pagination cursor");
  }

  @Test
  public void non_numeric_before_cursor_returns_bad_request_error() {
    ExecutionResult result =
        dgsQueryExecutor.execute("{ articles(last: 1, before: \"1e3\") { edges { cursor } } }");

    assertSingleError(result, "BAD_REQUEST", "Invalid pagination cursor");
  }

  @Test
  public void numeric_cursor_is_accepted() {
    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ articles(first: 1, after: \"1600000000000\") { edges { cursor } } }");

    assertEquals(0, result.getErrors().size());
  }

  private static void assertSingleError(ExecutionResult result, String errorType, String message) {
    assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    assertEquals(message, error.getMessage());
    assertEquals(errorType, error.getExtensions().get("errorType"));
  }
}
