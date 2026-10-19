package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

public class LoginServiceTest {
  private UserRepository userRepository;
  private LoginService loginService;
  private User user;

  @BeforeEach
  public void setUp() {
    userRepository = mock(UserRepository.class);
    LoginAttemptLimiter limiter = new LoginAttemptLimiter(2, 100, Duration.ofMinutes(15), 1000);
    loginService = new LoginService(userRepository, NoOpPasswordEncoder.getInstance(), limiter);
    user = new User("a@b.com", "a", "secret", "", "");
    when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
  }

  @Test
  public void should_return_user_for_matching_credentials() {
    assertEquals(Optional.of(user), loginService.authenticate("a@b.com", "secret", "10.0.0.1"));
  }

  @Test
  public void should_throttle_before_touching_the_repository() {
    assertTrue(loginService.authenticate("a@b.com", "bad", "10.0.0.1").isEmpty());
    assertTrue(loginService.authenticate("a@b.com", "bad", "10.0.0.1").isEmpty());

    assertThrows(
        TooManyLoginAttemptsException.class,
        () -> loginService.authenticate("a@b.com", "secret", "10.0.0.1"));
    verify(userRepository, times(2)).findByEmail("a@b.com");
  }

  @Test
  public void should_count_unknown_emails_like_known_ones() {
    loginService.authenticate("nobody@b.com", "x", "10.0.0.1");
    loginService.authenticate("nobody@b.com", "x", "10.0.0.2");

    assertThrows(
        TooManyLoginAttemptsException.class,
        () -> loginService.authenticate("nobody@b.com", "x", "10.0.0.3"));
  }

  @Test
  public void should_reset_account_failures_after_success() {
    loginService.authenticate("a@b.com", "bad", "10.0.0.1");
    loginService.authenticate("a@b.com", "secret", "10.0.0.1");

    assertTrue(loginService.authenticate("a@b.com", "bad", "10.0.0.1").isEmpty());
    assertTrue(loginService.authenticate("a@b.com", "bad", "10.0.0.1").isEmpty());
  }

  @Test
  public void should_not_count_attempts_that_fail_with_a_backend_error() {
    UserRepository failing = mock(UserRepository.class);
    when(failing.findByEmail("a@b.com")).thenThrow(new IllegalStateException("db down"));
    LoginAttemptLimiter limiter = new LoginAttemptLimiter(1, 100, Duration.ofMinutes(15), 1000);
    LoginService service = new LoginService(failing, NoOpPasswordEncoder.getInstance(), limiter);

    assertThrows(
        IllegalStateException.class, () -> service.authenticate("a@b.com", "x", "10.0.0.1"));
    assertThrows(
        IllegalStateException.class, () -> service.authenticate("a@b.com", "x", "10.0.0.1"));
    verify(failing, times(2)).findByEmail("a@b.com");
  }
}
