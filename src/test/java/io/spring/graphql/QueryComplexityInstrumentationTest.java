package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class QueryComplexityInstrumentationTest {
  private static final String SDL =
      "type Query { node(first: Int, last: Int): Node }\n"
          + "type Node { id: ID next(first: Int, last: Int): Node }";

  private static final GraphQLSchema SCHEMA =
      new SchemaGenerator()
          .makeExecutableSchema(
              new SchemaParser().parse(SDL), RuntimeWiring.newRuntimeWiring().build());

  private ExecutionResult execute(long maxComplexity, String query) {
    return GraphQL.newGraphQL(SCHEMA)
        .instrumentation(new QueryComplexityInstrumentation(maxComplexity))
        .build()
        .execute(query);
  }

  private static String nested(int levels, String pageArgs) {
    StringBuilder query = new StringBuilder("{ node" + pageArgs + " { id ");
    for (int i = 0; i < levels; i++) {
      query.append("next").append(pageArgs).append(" { id ");
    }
    for (int i = 0; i <= levels; i++) {
      query.append("} ");
    }
    return query.append("}").toString();
  }

  @Test
  public void should_multiply_child_cost_by_page_size() {
    // node(first: 10) { id next(first: 10) { id } } = 1 + 10 * (1 + (1 + 10 * 1))
    assertThat(execute(121, nested(1, "(first: 10)")).getErrors()).isEmpty();
    assertThat(execute(120, nested(1, "(first: 10)")).getErrors())
        .singleElement()
        .satisfies(
            e ->
                assertThat(e.getMessage())
                    .isEqualTo("maximum query complexity exceeded 121 > 120"));
  }

  @Test
  public void should_clamp_page_size_to_the_cursor_page_limit() {
    // node(first: N) { id } = 1 + min(N, 100) * 1
    assertThat(execute(101, nested(0, "(first: 2147483647)")).getErrors()).isEmpty();
    assertThat(execute(101, nested(0, "(last: 100)")).getErrors()).isEmpty();
    assertThat(execute(100, nested(0, "(first: 1000)")).getErrors())
        .singleElement()
        .satisfies(
            e ->
                assertThat(e.getMessage())
                    .isEqualTo("maximum query complexity exceeded 101 > 100"));
  }

  @Test
  public void should_saturate_instead_of_overflowing() {
    ExecutionResult result = execute(10000, nested(20, "(first: 2147483647)"));

    assertThat(result.getErrors())
        .singleElement()
        .satisfies(
            e ->
                assertThat(e.getMessage())
                    .isEqualTo("maximum query complexity exceeded " + Long.MAX_VALUE + " > 10000"));
  }

  @Test
  public void should_count_page_sizes_passed_as_variables() {
    ExecutionResult result =
        GraphQL.newGraphQL(SCHEMA)
            .instrumentation(new QueryComplexityInstrumentation(10000))
            .build()
            .execute(
                graphql.ExecutionInput.newExecutionInput()
                    .query(
                        "query($n: Int) { node(first: $n) { next(first: $n) { next(first: $n) { id"
                            + " } } } }")
                    .variables(Collections.singletonMap("n", 1000))
                    .build());

    assertThat(result.getErrors()).isNotEmpty();
  }
}
