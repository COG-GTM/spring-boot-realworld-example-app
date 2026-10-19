package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.UnExecutableSchemaGenerator;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphQLQueryLimitsTest {

  private GraphQL graphQL;

  @BeforeEach
  public void setUp() throws Exception {
    GraphQLSchema schema;
    try (Reader reader =
        new InputStreamReader(
            getClass().getResourceAsStream("/schema/schema.graphqls"), StandardCharsets.UTF_8)) {
      schema = UnExecutableSchemaGenerator.makeUnExecutableSchema(new SchemaParser().parse(reader));
    }
    GraphQLQueryLimits limits = new GraphQLQueryLimits();
    graphQL =
        GraphQL.newGraphQL(schema)
            .instrumentation(
                new ChainedInstrumentation(
                    Arrays.asList(
                        limits.maxQueryDepthInstrumentation(10),
                        limits.maxQueryComplexityInstrumentation(5000))))
            .build();
  }

  @Test
  public void allows_ordinary_paginated_query() {
    assertWithinLimits(
        execute(
            "{ articles(first: 100) { edges { node { slug title author { username following } } }"
                + " pageInfo { hasNextPage endCursor } } }"));
  }

  @Test
  public void allows_profile_and_comments_queries_used_by_clients() {
    assertWithinLimits(
        execute(
            "{ profile(username: \"jake\") { profile { username articles(first: 20) {"
                + " edges { node { slug author { username } } } } } } }"));
    assertWithinLimits(
        execute(
            "{ article(slug: \"a\") { slug comments(first: 20) {"
                + " edges { node { id body author { username } } } } } }"));
  }

  @Test
  public void rejects_cyclic_query_deeper_than_limit() {
    assertLimitError(
        execute(
            "{ articles(first: 1) { edges { node { author { articles(first: 1) {"
                + " edges { node { comments(first: 1) { edges { node { article {"
                + " author { username } } } } } } } } } } } } }"),
        "maximum query depth exceeded");
  }

  @Test
  public void rejects_nested_large_pages_exceeding_complexity() {
    assertLimitError(
        execute(
            "{ articles(first: 100) { edges { node {"
                + " comments(first: 100) { edges { node { body } } } } } } }"),
        "maximum query complexity exceeded");
  }

  @Test
  public void complexity_uses_page_size_from_variables() {
    String query =
        "query($n: Int) { articles(first: $n) { edges { node {"
            + " comments(last: $n) { edges { node { body } } } } } } }";

    assertWithinLimits(execute(query, 5));
    assertLimitError(execute(query, 1000), "maximum query complexity exceeded");
  }

  @Test
  public void page_size_above_limit_is_costed_at_clamped_limit() {
    assertWithinLimits(execute("{ articles(first: 1000000) { edges { node { slug } } } }"));
  }

  private ExecutionResult execute(String query) {
    return graphQL.execute(query);
  }

  private ExecutionResult execute(String query, int n) {
    return graphQL.execute(
        ExecutionInput.newExecutionInput(query).variables(Collections.singletonMap("n", n)));
  }

  private static void assertWithinLimits(ExecutionResult result) {
    assertFalse(
        result.getErrors().stream().anyMatch(e -> e.getMessage().startsWith("maximum query")),
        () -> result.getErrors().toString());
    assertNotNull(result.getData());
  }

  private static void assertLimitError(ExecutionResult result, String prefix) {
    assertEquals(1, result.getErrors().size(), () -> result.getErrors().toString());
    assertTrue(
        result.getErrors().get(0).getMessage().startsWith(prefix),
        () -> result.getErrors().toString());
    assertNull(result.getData());
  }
}
