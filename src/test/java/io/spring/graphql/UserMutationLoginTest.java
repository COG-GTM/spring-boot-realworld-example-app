package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.context.DgsContext;
import com.netflix.graphql.dgs.internal.DgsWebMvcRequestData;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.api.exception.TooManyLoginAttemptsException;
import io.spring.api.security.LoginThrottle;
import io.spring.application.user.LoginService;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

public class UserMutationLoginTest {
  private LoginService loginService;
  private UserMutation userMutation;

  @BeforeEach
  public void setUp() {
    loginService = mock(LoginService.class);
    LoginThrottle throttle = new LoginThrottle(2, 3, Duration.ofMinutes(15), 1000);
    userMutation = new UserMutation(loginService, throttle, mock(UserService.class));
    when(loginService.authenticate(anyString(), anyString())).thenReturn(Optional.empty());
  }

  private static DgsDataFetchingEnvironment env(String remoteAddr) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr(remoteAddr);
    DgsContext context =
        new DgsContext(
            null,
            new DgsWebMvcRequestData(
                Collections.emptyMap(), new HttpHeaders(), new ServletWebRequest(request)));
    return new DgsDataFetchingEnvironment(
        DataFetchingEnvironmentImpl.newDataFetchingEnvironment().context(context).build());
  }

  @Test
  public void should_login_with_valid_credentials() {
    User user = new User("a@b.com", "a", "hash", "", "");
    when(loginService.authenticate(eq("a@b.com"), eq("secret"))).thenReturn(Optional.of(user));

    assertSame(user, userMutation.login("secret", "a@b.com", env("1.1.1.1")).getLocalContext());
  }

  @Test
  public void should_throttle_failed_logins_per_email() {
    for (int i = 0; i < 2; i++) {
      assertThrows(
          InvalidAuthenticationException.class,
          () -> userMutation.login("wrong", "a@b.com", env("1.1.1.1")));
    }
    assertThrows(
        TooManyLoginAttemptsException.class,
        () -> userMutation.login("wrong", "a@b.com", env("2.2.2.2")));
    verify(loginService, never()).authenticate(eq("a@b.com"), eq("secret"));
  }

  @Test
  public void should_throttle_failed_logins_per_client_address() {
    for (int i = 0; i < 3; i++) {
      String email = "user" + i + "@b.com";
      assertThrows(
          InvalidAuthenticationException.class,
          () -> userMutation.login("wrong", email, env("1.1.1.1")));
    }
    assertThrows(
        TooManyLoginAttemptsException.class,
        () -> userMutation.login("wrong", "other@b.com", env("1.1.1.1")));
    assertThrows(
        InvalidAuthenticationException.class,
        () -> userMutation.login("wrong", "other@b.com", env("3.3.3.3")));
  }
}
