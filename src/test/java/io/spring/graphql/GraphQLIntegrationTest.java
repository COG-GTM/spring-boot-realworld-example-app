package io.spring.graphql;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class GraphQLIntegrationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @Test
  public void should_resolve_tags_query() {
    ExecutionResult result = dgsQueryExecutor.execute("{ tags }");
    Assertions.assertTrue(result.getErrors().isEmpty(), result.getErrors().toString());
    Map<String, Object> data = result.getData();
    Assertions.assertTrue(data.get("tags") instanceof List);
  }

  @Test
  public void should_resolve_articles_connection_page_info() {
    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ articles(first: 1) { edges { cursor } pageInfo { hasNextPage hasPreviousPage } } }");
    Assertions.assertTrue(result.getErrors().isEmpty(), result.getErrors().toString());
    Map<String, Object> articles =
        (Map<String, Object>) ((Map<String, Object>) result.getData()).get("articles");
    Map<String, Object> pageInfo = (Map<String, Object>) articles.get("pageInfo");
    Assertions.assertTrue(pageInfo.get("hasNextPage") instanceof Boolean);
    Assertions.assertTrue(pageInfo.get("hasPreviousPage") instanceof Boolean);
  }

  @Test
  public void should_return_unauthenticated_error_for_bad_login() {
    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { login(email: \"nobody@example.invalid\", password: \"x\") { user { email"
                + " } } }");
    Assertions.assertEquals(1, result.getErrors().size());
    GraphQLError error = result.getErrors().get(0);
    Assertions.assertEquals("invalid email or password", error.getMessage());
    Assertions.assertEquals(List.of("login"), error.getPath());
    Assertions.assertEquals(
        "UNAUTHENTICATED", String.valueOf(error.getExtensions().get("errorType")));
  }

  @Test
  public void should_return_validation_errors_as_data_for_invalid_create_user() {
    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { createUser(input: {email: \"\", username: \"\", password: \"\"}) {"
                + " __typename ... on Error { message errors { key value } } } }");
    Assertions.assertTrue(result.getErrors().isEmpty(), result.getErrors().toString());
    Map<String, Object> createUser =
        (Map<String, Object>) ((Map<String, Object>) result.getData()).get("createUser");
    Assertions.assertEquals("Error", createUser.get("__typename"));
    Assertions.assertEquals("BAD_REQUEST", createUser.get("message"));
    List<Map<String, Object>> errors = (List<Map<String, Object>>) createUser.get("errors");
    Assertions.assertTrue(
        errors.stream().map(e -> e.get("key")).toList().containsAll(List.of("email", "username")),
        errors.toString());
  }
}
