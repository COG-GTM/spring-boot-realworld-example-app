package io.spring.graphql;

import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphQLQueryLimitConfig {

  @Bean
  public Instrumentation maxQueryDepthInstrumentation(
      @Value("${graphql.query.max-depth:12}") int maxDepth) {
    return new MaxQueryDepthInstrumentation(maxDepth);
  }

  @Bean
  public Instrumentation queryComplexityInstrumentation(
      @Value("${graphql.query.max-complexity:10000}") long maxComplexity) {
    return new QueryComplexityInstrumentation(maxComplexity);
  }
}
