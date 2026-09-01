package io.spring.graphql;

import graphql.analysis.FieldComplexityCalculator;
import graphql.analysis.FieldComplexityEnvironment;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphQLQueryLimitConfig {

  @Bean
  public Instrumentation maxQueryDepthInstrumentation(
      @Value("${graphql.limit.max-query-depth:10}") int maxQueryDepth) {
    return new MaxQueryDepthInstrumentation(maxQueryDepth);
  }

  @Bean
  public Instrumentation maxQueryComplexityInstrumentation(
      @Value("${graphql.limit.max-query-complexity:50000}") int maxQueryComplexity) {
    return new MaxQueryComplexityInstrumentation(maxQueryComplexity, pageSizeAwareCalculator());
  }

  static FieldComplexityCalculator pageSizeAwareCalculator() {
    return (FieldComplexityEnvironment environment, int childComplexity) ->
        1 + pageSize(environment.getArguments()) * childComplexity;
  }

  private static int pageSize(Map<String, Object> arguments) {
    Object requested =
        arguments.get("first") != null ? arguments.get("first") : arguments.get("last");
    if (requested instanceof Number) {
      int size = ((Number) requested).intValue();
      return size > 0 ? size : 1;
    }
    return 1;
  }
}
