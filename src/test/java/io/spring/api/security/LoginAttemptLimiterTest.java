package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.spring.api.exception.TooManyLoginAttemptsException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LoginAttemptLimiterTest {
  private AtomicLong now;
  private LoginAttemptLimiter limiter;

  @BeforeEach
  public void setUp() {
    now = new AtomicLong(0);
    limiter = new LoginAttemptLimiter(3, 5, 60, 120, now::get);
  }

  private void advanceSeconds(long seconds) {
    now.addAndGet(TimeUnit.SECONDS.toNanos(seconds));
  }

  @Test
  public void should_lock_account_after_max_failures() {
    for (int i = 0; i < 3; i++) {
      limiter.checkAllowed("1.1.1." + i, "a@b.com");
      limiter.recordFailure("1.1.1." + i, "a@b.com");
    }

    TooManyLoginAttemptsException e =
        assertThrows(
            TooManyLoginAttemptsException.class, () -> limiter.checkAllowed("9.9.9.9", "a@b.com"));
    assertEquals(120, e.getRetryAfterSeconds());
    assertDoesNotThrow(() -> limiter.checkAllowed("9.9.9.9", "other@b.com"));
  }

  @Test
  public void should_treat_email_case_and_whitespace_as_same_account() {
    limiter.recordFailure("1.1.1.1", "A@B.com");
    limiter.recordFailure("1.1.1.2", " a@b.COM ");
    limiter.recordFailure("1.1.1.3", "a@b.com");

    assertThrows(
        TooManyLoginAttemptsException.class, () -> limiter.checkAllowed("1.1.1.4", "a@B.com"));
  }

  @Test
  public void should_lock_ip_after_max_failures_across_accounts() {
    for (int i = 0; i < 5; i++) {
      limiter.recordFailure("1.1.1.1", "user" + i + "@b.com");
    }

    assertThrows(
        TooManyLoginAttemptsException.class, () -> limiter.checkAllowed("1.1.1.1", "fresh@b.com"));
    assertDoesNotThrow(() -> limiter.checkAllowed("2.2.2.2", "fresh@b.com"));
  }

  @Test
  public void should_unlock_after_lockout_expires() {
    for (int i = 0; i < 3; i++) {
      limiter.recordFailure("1.1.1.1", "a@b.com");
    }
    advanceSeconds(119);
    TooManyLoginAttemptsException e =
        assertThrows(
            TooManyLoginAttemptsException.class, () -> limiter.checkAllowed("1.1.1.1", "a@b.com"));
    assertEquals(1, e.getRetryAfterSeconds());

    advanceSeconds(1);
    assertDoesNotThrow(() -> limiter.checkAllowed("1.1.1.1", "a@b.com"));
    limiter.recordFailure("1.1.1.1", "a@b.com");
    assertDoesNotThrow(() -> limiter.checkAllowed("1.1.1.1", "a@b.com"));
  }

  @Test
  public void should_forget_failures_after_window_expires() {
    limiter.recordFailure("1.1.1.1", "a@b.com");
    limiter.recordFailure("1.1.1.1", "a@b.com");
    advanceSeconds(60);
    limiter.recordFailure("1.1.1.1", "a@b.com");
    limiter.recordFailure("1.1.1.1", "a@b.com");

    assertDoesNotThrow(() -> limiter.checkAllowed("1.1.1.1", "a@b.com"));
  }

  @Test
  public void should_clear_account_failures_on_success_but_keep_ip_failures() {
    limiter.recordFailure("1.1.1.1", "a@b.com");
    limiter.recordFailure("1.1.1.1", "a@b.com");
    limiter.recordSuccess("a@b.com");
    limiter.recordFailure("1.1.1.1", "a@b.com");
    limiter.recordFailure("1.1.1.1", "a@b.com");
    assertDoesNotThrow(() -> limiter.checkAllowed("2.2.2.2", "a@b.com"));

    limiter.recordFailure("1.1.1.1", "c@b.com");
    assertThrows(
        TooManyLoginAttemptsException.class, () -> limiter.checkAllowed("1.1.1.1", "d@b.com"));
  }
}
