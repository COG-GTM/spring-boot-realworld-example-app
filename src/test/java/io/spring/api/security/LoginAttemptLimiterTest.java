package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.spring.api.exception.TooManyLoginAttemptsException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LoginAttemptLimiterTest {
  private AtomicLong now;
  private LoginAttemptLimiter limiter;
  private ExecutorService pool;

  @BeforeEach
  public void setUp() {
    now = new AtomicLong(0);
    limiter = new LoginAttemptLimiter(3, 5, 60, 120, now::get);
    pool = Executors.newCachedThreadPool();
  }

  @AfterEach
  public void tearDown() {
    pool.shutdownNow();
  }

  private void advanceSeconds(long seconds) {
    now.addAndGet(TimeUnit.SECONDS.toNanos(seconds));
  }

  private void fail(String ip, String email) {
    limiter.attempt(ip, email, Optional::empty);
  }

  private void succeed(String ip, String email) {
    limiter.attempt(ip, email, () -> Optional.of("user"));
  }

  private void assertLocked(String ip, String email) {
    assertThrows(TooManyLoginAttemptsException.class, () -> fail(ip, email));
  }

  private void assertAllowed(String ip, String email) {
    assertDoesNotThrow(() -> fail(ip, email));
  }

  private Future<?> startPending(String ip, String email, boolean valid, CountDownLatch release)
      throws InterruptedException {
    CountDownLatch started = new CountDownLatch(1);
    Future<?> future =
        pool.submit(
            () ->
                limiter.attempt(
                    ip,
                    email,
                    () -> {
                      started.countDown();
                      try {
                        release.await();
                      } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                      }
                      return valid ? Optional.of("user") : Optional.empty();
                    }));
    started.await(10, TimeUnit.SECONDS);
    return future;
  }

  @Test
  public void should_lock_account_after_max_failures() {
    for (int i = 0; i < 3; i++) {
      fail("1.1.1." + i, "a@b.com");
    }

    TooManyLoginAttemptsException e =
        assertThrows(TooManyLoginAttemptsException.class, () -> fail("9.9.9.9", "a@b.com"));
    assertEquals(120, e.getRetryAfterSeconds());
    assertAllowed("9.9.9.9", "other@b.com");
  }

  @Test
  public void should_not_run_credential_check_when_locked() {
    for (int i = 0; i < 3; i++) {
      fail("1.1.1." + i, "a@b.com");
    }
    AtomicInteger checks = new AtomicInteger();

    assertThrows(
        TooManyLoginAttemptsException.class,
        () ->
            limiter.attempt(
                "9.9.9.9",
                "a@b.com",
                () -> {
                  checks.incrementAndGet();
                  return Optional.empty();
                }));
    assertEquals(0, checks.get());
  }

  @Test
  public void should_key_accounts_by_exact_email() {
    for (int i = 0; i < 3; i++) {
      fail("1.1.1." + i, "Alex@b.com");
    }

    assertLocked("1.1.1.9", "Alex@b.com");
    assertAllowed("1.1.1.9", "alex@b.com");
  }

  @Test
  public void should_lock_ip_after_max_failures_across_accounts() {
    for (int i = 0; i < 5; i++) {
      fail("1.1.1.1", "user" + i + "@b.com");
    }

    assertLocked("1.1.1.1", "fresh@b.com");
    assertAllowed("2.2.2.2", "fresh@b.com");
  }

  @Test
  public void should_not_count_rejected_attempts() {
    for (int i = 0; i < 3; i++) {
      fail("1.1.1.1", "a@b.com");
    }
    for (int i = 0; i < 10; i++) {
      assertLocked("1.1.1.1", "a@b.com");
    }

    assertAllowed("1.1.1.1", "b@b.com");
    assertAllowed("1.1.1.1", "c@b.com");
  }

  @Test
  public void should_not_count_attempts_whose_check_throws() {
    for (int i = 0; i < 10; i++) {
      assertThrows(
          IllegalStateException.class,
          () ->
              limiter.attempt(
                  "1.1.1.1",
                  "a@b.com",
                  () -> {
                    throw new IllegalStateException("db down");
                  }));
    }

    assertAllowed("1.1.1.1", "a@b.com");
  }

  @Test
  public void should_unlock_after_lockout_expires() {
    for (int i = 0; i < 3; i++) {
      fail("1.1.1." + i, "a@b.com");
    }
    advanceSeconds(119);
    TooManyLoginAttemptsException e =
        assertThrows(TooManyLoginAttemptsException.class, () -> fail("9.9.9.9", "a@b.com"));
    assertEquals(1, e.getRetryAfterSeconds());

    advanceSeconds(1);
    assertAllowed("9.9.9.9", "a@b.com");
  }

  @Test
  public void should_give_fresh_allowance_after_lockout_shorter_than_window() {
    limiter = new LoginAttemptLimiter(3, 50, 60, 10, now::get);
    for (int i = 0; i < 3; i++) {
      fail("1.1.1.1", "a@b.com");
    }
    advanceSeconds(11);

    fail("1.1.1.1", "a@b.com");
    fail("1.1.1.1", "a@b.com");
    assertAllowed("1.1.1.1", "a@b.com");
    assertLocked("1.1.1.1", "a@b.com");
  }

  @Test
  public void should_forget_failures_after_window_expires() {
    fail("1.1.1.1", "a@b.com");
    fail("1.1.1.1", "a@b.com");
    advanceSeconds(60);
    fail("1.1.1.1", "a@b.com");
    fail("1.1.1.1", "a@b.com");

    assertAllowed("1.1.1.1", "a@b.com");
  }

  @Test
  public void should_clear_account_failures_on_success_but_keep_ip_failures() {
    fail("1.1.1.1", "a@b.com");
    fail("1.1.1.1", "a@b.com");
    succeed("1.1.1.1", "a@b.com");
    fail("2.2.2.2", "a@b.com");
    fail("2.2.2.2", "a@b.com");
    assertAllowed("3.3.3.3", "a@b.com");

    fail("1.1.1.1", "c@b.com");
    fail("1.1.1.1", "d@b.com");
    assertAllowed("1.1.1.1", "e@b.com");
    assertLocked("1.1.1.1", "f@b.com");
  }

  @Test
  public void should_keep_pending_failure_when_concurrent_login_succeeds() throws Exception {
    fail("1.1.1.1", "a@b.com");
    CountDownLatch release = new CountDownLatch(1);
    Future<?> pendingFailure = startPending("2.2.2.2", "a@b.com", false, release);

    succeed("3.3.3.3", "a@b.com");
    release.countDown();
    pendingFailure.get(10, TimeUnit.SECONDS);

    fail("4.4.4.4", "a@b.com");
    assertAllowed("4.4.4.4", "a@b.com");
    assertLocked("4.4.4.4", "a@b.com");
  }

  @Test
  public void should_not_let_late_success_erase_newer_ip_failures() throws Exception {
    limiter = new LoginAttemptLimiter(50, 2, 1, 120, now::get);
    CountDownLatch release = new CountDownLatch(1);
    Future<?> pendingSuccess = startPending("1.1.1.1", "slow@b.com", true, release);
    advanceSeconds(2);
    fail("1.1.1.1", "x@b.com");

    release.countDown();
    pendingSuccess.get(10, TimeUnit.SECONDS);

    assertAllowed("1.1.1.1", "y@b.com");
    assertLocked("1.1.1.1", "z@b.com");
  }

  @Test
  public void should_admit_at_most_max_concurrent_attempts() throws Exception {
    limiter = new LoginAttemptLimiter(3, 1000, 60, 120, now::get);
    int threads = 32;
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger admitted = new AtomicInteger();
    List<Future<?>> results = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      String ip = "10.0.0." + i;
      results.add(
          pool.submit(
              () -> {
                start.await();
                try {
                  limiter.attempt(
                      ip,
                      "a@b.com",
                      () -> {
                        admitted.incrementAndGet();
                        try {
                          release.await();
                        } catch (InterruptedException e) {
                          throw new IllegalStateException(e);
                        }
                        return Optional.empty();
                      });
                } catch (TooManyLoginAttemptsException e) {
                  // expected for requests beyond the limit
                }
                return null;
              }));
    }
    start.countDown();
    Thread.sleep(500);
    release.countDown();
    for (Future<?> result : results) {
      result.get(10, TimeUnit.SECONDS);
    }

    assertEquals(3, admitted.get());
    assertLocked("10.0.0.99", "a@b.com");
  }
}
