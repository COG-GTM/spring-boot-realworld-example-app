package io.spring.graphql;

import graphql.analysis.QueryTraverser;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.execution.AbortExecutionException;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.parameters.InstrumentationValidationParameters;
import graphql.validation.ValidationError;
import io.spring.application.CursorPageParameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rejects operations whose estimated cost exceeds {@code maxComplexity}. Every field costs 1, and a
 * paginated field multiplies the cost of its selection by the number of rows it can return, so the
 * estimate grows with the fan-out of nested connections. Arithmetic saturates, so huge page sizes
 * or very wide queries cannot overflow below the limit.
 */
public class QueryComplexityInstrumentation extends SimpleInstrumentation {
  private final long maxComplexity;

  public QueryComplexityInstrumentation(long maxComplexity) {
    if (maxComplexity < 1) {
      throw new IllegalArgumentException("maxComplexity must be positive");
    }
    this.maxComplexity = maxComplexity;
  }

  @Override
  public InstrumentationContext<List<ValidationError>> beginValidation(
      InstrumentationValidationParameters parameters) {
    return SimpleInstrumentationContext.whenCompleted(
        (errors, throwable) -> {
          if ((errors != null && !errors.isEmpty()) || throwable != null) {
            return;
          }
          long complexity = complexity(parameters);
          if (complexity > maxComplexity) {
            throw new AbortExecutionException(
                "maximum query complexity exceeded " + complexity + " > " + maxComplexity);
          }
        });
  }

  long complexity(InstrumentationValidationParameters parameters) {
    Map<QueryVisitorFieldEnvironment, Long> childCost = new HashMap<>();
    long[] total = {0};
    QueryTraverser.newQueryTraverser()
        .schema(parameters.getSchema())
        .document(parameters.getDocument())
        .operationName(parameters.getOperation())
        .variables(parameters.getVariables())
        .build()
        .visitPostOrder(
            new QueryVisitorStub() {
              @Override
              public void visitField(QueryVisitorFieldEnvironment env) {
                if (env.isTypeNameIntrospectionField()) {
                  return;
                }
                long children = childCost.getOrDefault(env, 0L);
                long cost = saturatedAdd(1, saturatedMultiply(rows(env.getArguments()), children));
                QueryVisitorFieldEnvironment parent = env.getParentEnvironment();
                if (parent == null) {
                  total[0] = saturatedAdd(total[0], cost);
                } else {
                  childCost.merge(parent, cost, QueryComplexityInstrumentation::saturatedAdd);
                }
              }
            });
    return total[0];
  }

  static long rows(Map<String, Object> arguments) {
    Object requested = arguments.get("first");
    if (requested == null) {
      requested = arguments.get("last");
    }
    if (!(requested instanceof Number)) {
      return 1;
    }
    return CursorPageParameter.effectiveLimit(((Number) requested).intValue());
  }

  private static long saturatedAdd(long a, long b) {
    long sum = a + b;
    return sum < a ? Long.MAX_VALUE : sum;
  }

  private static long saturatedMultiply(long a, long b) {
    if (a != 0 && b > Long.MAX_VALUE / a) {
      return Long.MAX_VALUE;
    }
    return a * b;
  }
}
