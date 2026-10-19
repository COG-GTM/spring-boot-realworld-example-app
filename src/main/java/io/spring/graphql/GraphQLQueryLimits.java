package io.spring.graphql;

import graphql.analysis.FieldComplexityCalculator;
import graphql.analysis.FieldComplexityEnvironment;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import io.spring.application.CursorPageParameter;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphQLQueryLimits {

  @Bean
  public Instrumentation maxQueryDepthInstrumentation(
      @Value("${graphql.limits.max-depth:10}") int maxDepth) {
    return new MaxQueryDepthInstrumentation(maxDepth);
  }

  @Bean
  public Instrumentation maxQueryComplexityInstrumentation(
      @Value("${graphql.limits.max-complexity:5000}") int maxComplexity) {
    return new MaxQueryComplexityInstrumentation(maxComplexity, new PageSizeComplexity());
  }

  /** Each field costs 1; paginated fields multiply their selection by the requested page size. */
  static class PageSizeComplexity implements FieldComplexityCalculator {
    @Override
    public int calculate(FieldComplexityEnvironment environment, int childComplexity) {
      long cost = 1L + (long) childComplexity * pageSize(environment.getArguments());
      return (int) Math.min(cost, Integer.MAX_VALUE);
    }

    private static int pageSize(Map<String, Object> arguments) {
      Object first = arguments.get("first");
      Object last = arguments.get("last");
      Object requested = first != null ? first : last;
      if (!(requested instanceof Number)) {
        return 1;
      }
      int size = ((Number) requested).intValue();
      return Math.max(1, Math.min(size, CursorPageParameter.MAX_LIMIT));
    }
  }
}
