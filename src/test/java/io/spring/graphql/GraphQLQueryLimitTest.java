package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class GraphQLQueryLimitTest {
  @Autowired private DgsQueryExecutor queryExecutor;

  @Test
  public void should_reject_deeply_nested_cyclic_query() {
    ExecutionResult result =
        queryExecutor.execute(
            "{ article(slug: \"x\") { comments(first: 1) { edges { node { article {"
                + " comments(first: 1) { edges { node { article {"
                + " comments(first: 1) { edges { node { id } } }"
                + " } } } } } } } } } }");

    assertThat(result.getErrors())
        .extracting(GraphQLError::getMessage)
        .containsExactly("maximum query depth exceeded 13 > 12");
  }

  @Test
  public void should_reject_nested_connection_amplification() {
    ExecutionResult result =
        queryExecutor.execute(
            "{ articles(first: 1000) { edges { node { author {"
                + " articles(first: 1000) { edges { node {"
                + " comments(first: 1000) { edges { node { id } } }"
                + " } } } } } } } }");

    assertThat(result.getErrors())
        .extracting(GraphQLError::getMessage)
        .singleElement()
        .asString()
        .startsWith("maximum query complexity exceeded");
  }

  @Test
  public void should_allow_realistic_client_queries() {
    ExecutionResult result =
        queryExecutor.execute(
            "{ articles(first: 20) { edges { cursor node { slug title favorited favoritesCount"
                + " author { username image following }"
                + " comments(first: 20) { edges { node { id body author { username } } } } } }"
                + " pageInfo { hasNextPage endCursor } } }");

    assertThat(result.getErrors()).isEmpty();
  }
}
