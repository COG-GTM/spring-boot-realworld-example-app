package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.DocumentContext;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import graphql.ExecutionResult;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.core.service.JwtService;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;

@SpringBootTest(classes = {DgsAutoConfiguration.class, MeDatafetcher.class})
public class MeDatafetcherTest extends GraphQLTestBase {

  private static final String ME_QUERY = "{ me { email username token } }";

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @MockBean private UserQueryService userQueryService;

  @MockBean private JwtService jwtService;

  @Test
  void should_return_current_user_with_token_from_authorization_header() {
    when(userQueryService.findById(eq(user.getId())))
        .thenReturn(
            Optional.of(
                new UserData(
                    user.getId(), user.getEmail(), user.getUsername(), "bio", DEFAULT_AVATAR)));

    DocumentContext context =
        dgsQueryExecutor.executeAndGetDocumentContext(
            ME_QUERY, Collections.emptyMap(), authorizationHeaders("jwt-token"));

    assertThat(context.read("$.data.me.email", String.class)).isEqualTo(user.getEmail());
    assertThat(context.read("$.data.me.username", String.class)).isEqualTo(user.getUsername());
    assertThat(context.read("$.data.me.token", String.class)).isEqualTo("jwt-token");
  }

  @Test
  void should_return_null_for_anonymous_user() {
    logout();

    ExecutionResult result =
        dgsQueryExecutor.execute(
            ME_QUERY,
            Collections.emptyMap(),
            Collections.emptyMap(),
            authorizationHeaders("jwt-token"));

    assertThat(result.getErrors()).isEmpty();
    assertThat(result.<java.util.Map<String, Object>>getData()).containsEntry("me", null);
  }

  @Test
  void should_fail_when_current_user_is_not_found() {
    when(userQueryService.findById(eq(user.getId()))).thenReturn(Optional.empty());

    ExecutionResult result =
        dgsQueryExecutor.execute(
            ME_QUERY,
            Collections.emptyMap(),
            Collections.emptyMap(),
            authorizationHeaders("jwt-token"));

    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getMessage()).contains("ResourceNotFoundException");
  }

  private HttpHeaders authorizationHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token " + token);
    return headers;
  }
}
