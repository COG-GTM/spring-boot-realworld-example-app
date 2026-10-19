package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

public class LoginAttemptLimiterTest {
  private final MutableClock clock = new MutableClock();

  private LoginAttemptLimiter limiter(int perAccount, int perClient, int maxKeys) {
    return new LoginAttemptLimiter(perAccount, perClient, Duration.ofMinutes(15), maxKeys, clock);
  }

  @Test
  public void should_block_account_after_max_attempts_from_any_client() {
    LoginAttemptLimiter limiter = limiter(3, 100, 1000);
    for (int i = 0; i < 3; i++) {
      limiter.acquire("a@b.com", "10.0.0." + i);
    }

    TooManyLoginAttemptsException e =
        assertThrows(
            TooManyLoginAttemptsException.class, () -> limiter.acquire("a@b.com", "10.9.9.9"));
    assertEquals(900, e.getRetryAfterSeconds());
  }

  @Test
  public void should_key_accounts_by_the_exact_email_the_repository_looks_up() {
    LoginAttemptLimiter limiter = limiter(2, 100, 1000);
    limiter.acquire("a@b.com", "10.0.0.1");
    limiter.acquire("a@b.com", "10.0.0.2");

    assertThrows(TooManyLoginAttemptsException.class, () -> limiter.acquire("a@b.com", "10.0.0.3"));
    limiter.acquire("A@b.com", "10.0.0.3");
  }

  @Test
  public void should_block_client_after_max_attempts_across_accounts() {
    LoginAttemptLimiter limiter = limiter(100, 3, 1000);
    for (int i = 0; i < 3; i++) {
      limiter.acquire("user" + i + "@b.com", "10.0.0.1");
    }

    assertThrows(
        TooManyLoginAttemptsException.class, () -> limiter.acquire("other@b.com", "10.0.0.1"));
    limiter.acquire("other@b.com", "10.0.0.2");
  }

  @Test
  public void should_allow_attempts_again_once_the_window_expires() {
    LoginAttemptLimiter limiter = limiter(2, 100, 1000);
    limiter.acquire("a@b.com", "10.0.0.1");
    clock.advance(Duration.ofMinutes(5));
    limiter.acquire("a@b.com", "10.0.0.1");

    TooManyLoginAttemptsException e =
        assertThrows(
            TooManyLoginAttemptsException.class, () -> limiter.acquire("a@b.com", "10.0.0.1"));
    assertEquals(600, e.getRetryAfterSeconds());

    clock.advance(Duration.ofMinutes(10));
    limiter.acquire("a@b.com", "10.0.0.1");
  }

  @Test
  public void should_clear_account_and_refund_client_on_success() {
    LoginAttemptLimiter limiter = limiter(2, 2, 1000);
    limiter.failed(limiter.acquire("a@b.com", "10.0.0.1"));
    limiter.succeeded(limiter.acquire("a@b.com", "10.0.0.1"));
    assertEquals(1, limiter.trackedKeys());

    limiter.acquire("a@b.com", "10.0.0.1");
    assertThrows(TooManyLoginAttemptsException.class, () -> limiter.acquire("a@b.com", "10.0.0.1"));
  }

  @Test
  public void should_keep_in_flight_attempts_counted_when_another_attempt_succeeds() {
    LoginAttemptLimiter limiter = limiter(3, 100, 1000);
    limiter.acquire("a@b.com", "10.6.6.1");
    limiter.acquire("a@b.com", "10.6.6.2");
    limiter.succeeded(limiter.acquire("a@b.com", "10.0.0.1"));

    limiter.acquire("a@b.com", "10.6.6.3");
    assertThrows(TooManyLoginAttemptsException.class, () -> limiter.acquire("a@b.com", "10.6.6.4"));
  }

  @Test
  public void should_refund_both_keys_when_aborted() {
    LoginAttemptLimiter limiter = limiter(1, 1, 1000);
    limiter.aborted(limiter.acquire("a@b.com", "10.0.0.1"));

    limiter.acquire("a@b.com", "10.0.0.1");
    assertEquals(2, limiter.trackedKeys());
  }

  @Test
  public void should_not_refund_a_newer_window_from_a_stale_attempt() {
    LoginAttemptLimiter limiter = limiter(1, 100, 1000);
    LoginAttemptLimiter.Attempt stale = limiter.acquire("a@b.com", "10.0.0.1");
    clock.advance(Duration.ofMinutes(15));
    limiter.acquire("a@b.com", "10.0.0.1");

    limiter.aborted(stale);
    limiter.succeeded(stale);

    assertThrows(TooManyLoginAttemptsException.class, () -> limiter.acquire("a@b.com", "10.0.0.1"));
  }

  @Test
  public void should_bound_tracked_keys_by_forgetting_the_oldest_windows() {
    LoginAttemptLimiter limiter = limiter(1, 100, 4);
    limiter.acquire("a@b.com", "10.0.0.1");
    clock.advance(Duration.ofMinutes(10));
    limiter.acquire("b@b.com", "10.0.0.1");
    assertEquals(3, limiter.trackedKeys());

    limiter.acquire("c@b.com", "10.0.0.2");
    assertEquals(4, limiter.trackedKeys());
    limiter.acquire("a@b.com", "10.0.0.2");

    assertThrows(TooManyLoginAttemptsException.class, () -> limiter.acquire("c@b.com", "10.0.0.9"));
    assertEquals(4, limiter.trackedKeys());

    clock.advance(Duration.ofMinutes(15));
    limiter.acquire("c@b.com", "10.0.0.9");
    assertEquals(2, limiter.trackedKeys());
  }

  @Test
  public void should_not_let_concurrent_attempts_exceed_the_account_limit() throws Exception {
    LoginAttemptLimiter limiter = limiter(3, 1000, 1000);
    int threads = 32;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Boolean>> results = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      String client = "10.0.0." + i;
      Callable<Boolean> task =
          () -> {
            start.await();
            try {
              limiter.acquire("a@b.com", client);
              return true;
            } catch (TooManyLoginAttemptsException e) {
              return false;
            }
          };
      results.add(pool.submit(task));
    }
    start.countDown();
    int allowed = 0;
    for (Future<Boolean> result : results) {
      allowed += result.get() ? 1 : 0;
    }
    pool.shutdown();

    assertEquals(3, allowed);
  }

  private static final class MutableClock extends Clock {
    private Instant now = Instant.parse("2026-01-01T00:00:00Z");

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
