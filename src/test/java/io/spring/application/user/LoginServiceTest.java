package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class LoginServiceTest {
  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private LoginService loginService;
  private User user;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = spy(new BCryptPasswordEncoder(4));
    LoginAttemptLimiter limiter =
        new LoginAttemptLimiter(3, 10, Duration.ofMinutes(15), 1000, Clock.systemUTC());
    loginService = new LoginService(userRepository, passwordEncoder, limiter);
    user = new User("john@jacob.com", "john", passwordEncoder.encode("secret"), "", "");
    when(userRepository.findByEmail(eq("john@jacob.com"))).thenReturn(Optional.of(user));
    when(userRepository.findByEmail(eq("ghost@jacob.com"))).thenReturn(Optional.empty());
  }

  @Test
  public void should_return_user_for_valid_credentials() {
    assertEquals(
        Optional.of(user), loginService.authenticate("john@jacob.com", "secret", "1.1.1.1"));
  }

  @Test
  public void should_run_password_hash_for_unknown_email() {
    assertFalse(loginService.authenticate("ghost@jacob.com", "secret", "1.1.1.1").isPresent());
    verify(passwordEncoder).matches(eq("secret"), anyString());
  }

  @Test
  public void should_run_password_hash_for_null_input() {
    assertFalse(loginService.authenticate(null, null, "1.1.1.1").isPresent());
    verify(passwordEncoder).matches(eq(""), anyString());
  }

  @Test
  public void should_throttle_unknown_and_known_accounts_identically() {
    for (String email : new String[] {"john@jacob.com", "ghost@jacob.com"}) {
      for (int i = 0; i < 3; i++) {
        assertFalse(loginService.authenticate(email, "wrong", "2.2.2." + i).isPresent());
      }
      TooManyLoginAttemptsException e =
          assertThrows(
              TooManyLoginAttemptsException.class,
              () -> loginService.authenticate(email, "secret", "3.3.3.3"));
      assertTrue(e.getRetryAfterSeconds() > 0);
    }
  }

  @Test
  public void should_not_touch_repository_when_throttled() {
    for (int i = 0; i < 3; i++) {
      loginService.authenticate("ghost@jacob.com", "wrong", "1.1.1.1");
    }
    assertThrows(
        TooManyLoginAttemptsException.class,
        () -> loginService.authenticate("ghost@jacob.com", "wrong", "1.1.1.1"));
    verify(userRepository, times(3)).findByEmail("ghost@jacob.com");
  }
}
