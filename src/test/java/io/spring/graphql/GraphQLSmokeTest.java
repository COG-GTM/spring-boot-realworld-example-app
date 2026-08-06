package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;

@SpringBootTest(
    properties = "spring.datasource.url=jdbc:sqlite:file:graphqlsmoketest?mode=memory&cache=shared")
@AutoConfigureGraphQlTester
public class GraphQLSmokeTest {

  @Autowired private GraphQlTester graphQlTester;

  @Test
  public void should_execute_query() {
    graphQlTester.document("{ tags }").execute().path("tags").hasValue();
  }

  @Test
  public void should_execute_mutation_and_resolve_union_payload() {
    graphQlTester
        .document(
            "mutation { createUser(input: {username: \"smoketest\", email:"
                + " \"smoketest@example.com\", password: \"password\"}) { ... on UserPayload { user"
                + " { username email } } } }")
        .execute()
        .path("createUser.user.username")
        .matchesJson("\"smoketest\"");
  }

  @Test
  public void should_map_invalid_input_to_error_payload() {
    graphQlTester
        .document(
            "mutation { createUser(input: {username: \"\", email: \"not-an-email\", password:"
                + " \"password\"}) { ... on Error { message } } }")
        .execute()
        .path("createUser.message")
        .matchesJson("\"BAD_REQUEST\"");
  }

  @Test
  public void should_map_authentication_failure_to_unauthenticated_error() {
    graphQlTester
        .document(
            "mutation { login(email: \"nobody@example.com\", password: \"wrong\") { user {"
                + " token } } }")
        .execute()
        .errors()
        .satisfy(
            errors ->
                assertThat(errors)
                    .singleElement()
                    .satisfies(
                        error ->
                            assertThat(error.getExtensions())
                                .containsEntry("errorType", "UNAUTHENTICATED")));
  }
}
