package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.context.DgsContext;
import com.netflix.graphql.dgs.internal.DgsWebMvcRequestData;
import com.netflix.graphql.types.errors.ErrorDetail;
import graphql.GraphQLError;
import graphql.Scalars;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.application.user.LoginService;
import io.spring.application.user.TooManyLoginAttemptsException;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import io.spring.graphql.exception.GraphQLCustomizeExceptionHandler;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

public class UserMutationLoginTest {
  private final LoginService loginService = mock(LoginService.class);
  private final UserMutation userMutation = new UserMutation(mock(UserService.class), loginService);

  @Test
  public void should_pass_the_http_client_address_to_the_login_service() {
    User user = new User("a@b.com", "a", "hash", "", "");
    when(loginService.authenticate("a@b.com", "secret", "203.0.113.7"))
        .thenReturn(Optional.of(user));

    assertEquals(
        user,
        userMutation.login("secret", "a@b.com", environmentFrom("203.0.113.7")).getLocalContext());
  }

  @Test
  public void should_reject_wrong_credentials() {
    when(loginService.authenticate(any(), any(), any())).thenReturn(Optional.empty());

    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("bad", "a@b.com", environmentFrom("203.0.113.7")));
    verify(loginService).authenticate("a@b.com", "bad", "203.0.113.7");
  }

  @Test
  public void should_map_throttling_to_enhance_your_calm_with_retry_after() {
    DataFetchingEnvironment env =
        DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .executionStepInfo(
                ExecutionStepInfo.newExecutionStepInfo()
                    .type(Scalars.GraphQLString)
                    .path(ResultPath.rootPath().segment("login"))
                    .build())
            .build();
    DataFetcherExceptionHandlerParameters params =
        DataFetcherExceptionHandlerParameters.newExceptionParameters()
            .dataFetchingEnvironment(env)
            .exception(new TooManyLoginAttemptsException(42))
            .build();

    GraphQLError error =
        new GraphQLCustomizeExceptionHandler().onException(params).getErrors().get(0);

    assertEquals("too many login attempts, try again later", error.getMessage());
    assertEquals(
        ErrorDetail.Common.ENHANCE_YOUR_CALM.name(), error.getExtensions().get("errorDetail"));
    assertEquals(42L, error.getExtensions().get("retryAfterSeconds"));
  }

  private static DgsDataFetchingEnvironment environmentFrom(String remoteAddress) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr(remoteAddress);
    DgsContext context =
        new DgsContext(
            null,
            new DgsWebMvcRequestData(
                Collections.emptyMap(), new HttpHeaders(), new ServletWebRequest(request)));
    return new DgsDataFetchingEnvironment(
        DataFetchingEnvironmentImpl.newDataFetchingEnvironment().context(context).build());
  }
}
