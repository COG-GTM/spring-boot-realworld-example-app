package io.spring.graphql;

import graphql.ExecutionResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

public class MeDatafetcherTest extends GraphQLTestBase {

  @Test
  public void should_return_current_user_when_authenticated() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token " + token);

    String returnedUsername =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ me { username email token } }", "data.me.username", headers);

    Assertions.assertEquals(username, returnedUsername);
  }

  @Test
  public void should_expose_token_from_authorization_header() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token " + token);

    String returnedToken =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ me { token } }", "data.me.token", headers);

    Assertions.assertEquals(token, returnedToken);
  }

  @Test
  public void should_return_null_me_when_anonymous() {
    logout();
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token " + token);

    ExecutionResult result =
        dgsQueryExecutor.execute("{ me { username } }", java.util.Collections.emptyMap(),
            java.util.Collections.emptyMap(), headers);

    Assertions.assertTrue(result.getErrors().isEmpty());
    java.util.Map<String, Object> data = result.getData();
    Assertions.assertNull(data.get("me"));
  }
}
