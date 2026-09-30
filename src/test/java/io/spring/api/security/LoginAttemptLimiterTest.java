package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.spring.api.exception.TooManyLoginAttemptsException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

  private void assertLocked(String ip, String email) {
    assertThrows(TooManyLoginAttemptsException.class, () -> limiter.beginAttempt(ip, email));
  }

  @Test
  public void should_lock_account_after_max_failures() {
    for (int i = 0; i < 3; i++) {
      limiter.beginAttempt("1.1.1." + i, "a@b.com");
    }

    TooManyLoginAttemptsException e =
        assertThrows(
            TooManyLoginAttemptsException.class, () -> limiter.beginAttempt("9.9.9.9", "a@b.com"));
    assertEquals(120, e.getRetryAfterSeconds());
    assertDoesNotThrow(() -> limiter.beginAttempt("9.9.9.9", "other@b.com"));
  }

  @Test
  public void should_key_accounts_by_exact_email() {
    for (int i = 0; i < 3; i++) {
      limiter.beginAttempt("1.1.1." + i, "Alex@b.com");
    }

    assertLocked("1.1.1.9", "Alex@b.com");
    assertDoesNotThrow(() -> limiter.beginAttempt("1.1.1.9", "alex@b.com"));
  }

  @Test
  public void should_lock_ip_after_max_failures_across_accounts() {
    for (int i = 0; i < 5; i++) {
      limiter.beginAttempt("1.1.1.1", "user" + i + "@b.com");
    }

    assertLocked("1.1.1.1", "fresh@b.com");
    assertDoesNotThrow(() -> limiter.beginAttempt("2.2.2.2", "fresh@b.com"));
  }

  @Test
  public void should_not_count_rejected_attempts() {
    for (int i = 0; i < 3; i++) {
      limiter.beginAttempt("1.1.1.1", "a@b.com");
    }
    for (int i = 0; i < 10; i++) {
      assertLocked("1.1.1.1", "a@b.com");
    }

    assertDoesNotThrow(() -> limiter.beginAttempt("1.1.1.1", "b@b.com"));
    assertDoesNotThrow(() -> limiter.beginAttempt("1.1.1.1", "c@b.com"));
  }

  @Test
  public void should_unlock_after_lockout_expires() {
    for (int i = 0; i < 3; i++) {
      limiter.beginAttempt("1.1.1." + i, "a@b.com");
    }
    advanceSeconds(119);
    TooManyLoginAttemptsException e =
        assertThrows(
            TooManyLoginAttemptsException.class, () -> limiter.beginAttempt("9.9.9.9", "a@b.com"));
    assertEquals(1, e.getRetryAfterSeconds());

    advanceSeconds(1);
    assertDoesNotThrow(() -> limiter.beginAttempt("9.9.9.9", "a@b.com"));
  }

  @Test
  public void should_give_fresh_allowance_after_lockout_shorter_than_window() {
    limiter = new LoginAttemptLimiter(3, 50, 60, 10, now::get);
    for (int i = 0; i < 3; i++) {
      limiter.beginAttempt("1.1.1.1", "a@b.com");
    }
    advanceSeconds(11);

    limiter.beginAttempt("1.1.1.1", "a@b.com");
    limiter.beginAttempt("1.1.1.1", "a@b.com");
    assertDoesNotThrow(() -> limiter.beginAttempt("1.1.1.1", "a@b.com"));
    assertLocked("1.1.1.1", "a@b.com");
  }

  @Test
  public void should_forget_failures_after_window_expires() {
    limiter.beginAttempt("1.1.1.1", "a@b.com");
    limiter.beginAttempt("1.1.1.1", "a@b.com");
    advanceSeconds(60);
    limiter.beginAttempt("1.1.1.1", "a@b.com");
    limiter.beginAttempt("1.1.1.1", "a@b.com");

    assertDoesNotThrow(() -> limiter.beginAttempt("1.1.1.1", "a@b.com"));
  }

  @Test
  public void should_clear_account_and_refund_ip_attempt_on_success() {
    limiter.beginAttempt("1.1.1.1", "a@b.com");
    limiter.beginAttempt("1.1.1.1", "a@b.com");
    limiter.beginAttempt("1.1.1.1", "a@b.com");
    limiter.recordSuccess("1.1.1.1", "a@b.com");
    limiter.beginAttempt("2.2.2.2", "a@b.com");
    limiter.beginAttempt("2.2.2.2", "a@b.com");
    assertDoesNotThrow(() -> limiter.beginAttempt("3.3.3.3", "a@b.com"));

    limiter.beginAttempt("1.1.1.1", "c@b.com");
    limiter.beginAttempt("1.1.1.1", "d@b.com");
    assertDoesNotThrow(() -> limiter.beginAttempt("1.1.1.1", "e@b.com"));
    assertLocked("1.1.1.1", "f@b.com");
  }

  @Test
  public void should_admit_at_most_max_concurrent_attempts() throws Exception {
    limiter = new LoginAttemptLimiter(3, 1000, 60, 120, now::get);
    int threads = 32;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Boolean>> results = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      String ip = "10.0.0." + i;
      results.add(
          pool.submit(
              () -> {
                start.await();
                try {
                  limiter.beginAttempt(ip, "a@b.com");
                  return true;
                } catch (TooManyLoginAttemptsException e) {
                  return false;
                }
              }));
    }
    start.countDown();
    int admitted = 0;
    for (Future<Boolean> result : results) {
      if (result.get(10, TimeUnit.SECONDS)) {
        admitted++;
      }
    }
    pool.shutdown();

    assertEquals(3, admitted);
  }
}
