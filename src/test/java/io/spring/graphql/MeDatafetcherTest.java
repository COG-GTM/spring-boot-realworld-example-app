package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.exceptions.QueryException;
import io.spring.application.data.UserData;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class MeDatafetcherTest extends DgsGraphQLTestBase {

  private HttpHeaders authHeader() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token jwt-token");
    return headers;
  }

  @Test
  void should_return_me_when_authenticated() {
    setAuthenticatedUser(user);
    UserData userData =
        new UserData(user.getId(), user.getEmail(), user.getUsername(), user.getBio(), "img");
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.of(userData));

    String token =
        dgsQueryExecutor.executeAndExtractJsonPath(
            "{ me { email username token } }", "data.me.token", authHeader());

    assertEquals("jwt-token", token);
  }

  @Test
  void should_return_null_me_when_anonymous() {
    setAnonymous();

    Object me =
        dgsQueryExecutor.executeAndExtractJsonPath("{ me { email } }", "data.me", authHeader());

    assertNull(me);
  }

  @Test
  void should_error_me_when_user_data_missing() {
    setAuthenticatedUser(user);
    when(userQueryService.findById(any())).thenReturn(Optional.empty());

    QueryException error =
        org.junit.jupiter.api.Assertions.assertThrows(
            QueryException.class,
            () ->
                dgsQueryExecutor.executeAndExtractJsonPath(
                    "{ me { email } }", "data.me", authHeader()));

    assertFalse(error.getErrors().isEmpty());
  }
}
