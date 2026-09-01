package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class GraphQLQueryLimitConfigTest {

  private static final String SDL =
      String.join(
          "\n",
          "type Query { articles(first: Int, after: String, last: Int, before: String): "
              + "ArticlesConnection }",
          "type ArticlesConnection { edges: [ArticleEdge] }",
          "type ArticleEdge { cursor: String!, node: Article }",
          "type Article { slug: String!, title: String!, comments(first: Int, after: String, "
              + "last: Int, before: String): CommentsConnection }",
          "type CommentsConnection { edges: [CommentEdge] }",
          "type CommentEdge { cursor: String!, node: Comment }",
          "type Comment { id: ID!, body: String!, article: Article! }");

  @Test
  public void should_reject_queries_exceeding_maximum_depth() {
    GraphQLQueryLimitConfig config = new GraphQLQueryLimitConfig();
    ExecutionResult executionResult =
        graphWith(config.maxQueryDepthInstrumentation(10)).execute(depthQuery());

    assertThat(executionResult.getErrors()).isNotEmpty();
    assertThat(errorMessages(executionResult)).contains("maximum query depth exceeded");
  }

  @Test
  public void should_reject_queries_exceeding_maximum_complexity() {
    GraphQLQueryLimitConfig config = new GraphQLQueryLimitConfig();
    ExecutionResult executionResult =
        graphWith(config.maxQueryComplexityInstrumentation(50000))
            .execute(
                "{ articles(first: 1000) { edges { node { comments(first: 1000) { "
                    + "edges { node { body } } } } } } }");

    assertThat(executionResult.getErrors()).isNotEmpty();
    assertThat(errorMessages(executionResult)).contains("maximum query complexity exceeded");
  }

  @Test
  public void should_allow_realistic_queries_within_both_limits() {
    String query =
        "{ articles(first: 20) { edges { node { title comments(first: 20) { "
            + "edges { node { body } } } } } } }";
    GraphQLQueryLimitConfig config = new GraphQLQueryLimitConfig();

    assertThat(graphWith(config.maxQueryDepthInstrumentation(10)).execute(query).getErrors())
        .isEmpty();
    ExecutionResult executionResult =
        graphWith(config.maxQueryComplexityInstrumentation(50000)).execute(query);
    assertThat(executionResult.getErrors()).isEmpty();
  }

  private static GraphQL graphWith(Instrumentation instrumentation) {
    TypeDefinitionRegistry registry = new SchemaParser().parse(SDL);
    GraphQLSchema schema =
        new SchemaGenerator()
            .makeExecutableSchema(registry, RuntimeWiring.newRuntimeWiring().build());
    return GraphQL.newGraphQL(schema).instrumentation(instrumentation).build();
  }

  private static String depthQuery() {
    return "{ articles { edges { node { " + cycle(5) + " } } } }";
  }

  private static String cycle(int remaining) {
    if (remaining == 0) {
      return "slug";
    }
    return "comments { edges { node { article { " + cycle(remaining - 1) + " } } } }";
  }

  private static String errorMessages(ExecutionResult executionResult) {
    return executionResult.getErrors().stream()
        .map(error -> error.getMessage())
        .collect(Collectors.joining("\n"));
  }
}
