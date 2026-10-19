package io.spring.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.api.exception.TooManyLoginAttemptsException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LoginThrottleTest {
  private MutableClock clock;
  private LoginThrottle throttle;

  @BeforeEach
  public void setUp() {
    clock = new MutableClock();
    throttle = new LoginThrottle(3, 5, Duration.ofMinutes(15), 1000, clock);
  }

  private Optional<String> fail(String client, String email) {
    return throttle.attempt(client, email, Optional::empty);
  }

  private Optional<String> succeed(String client, String email) {
    return throttle.attempt(client, email, () -> Optional.of("user"));
  }

  @Test
  public void should_block_email_after_max_failures_even_with_correct_password() {
    for (int i = 0; i < 3; i++) {
      assertFalse(fail("1.1.1.1", "a@b.com").isPresent());
    }
    TooManyLoginAttemptsException e =
        assertThrows(TooManyLoginAttemptsException.class, () -> succeed("2.2.2.2", "a@b.com"));
    assertEquals(900, e.getRetryAfterSeconds());
    assertThrows(TooManyLoginAttemptsException.class, () -> fail("3.3.3.3", "a@b.com"));
  }

  @Test
  public void should_track_emails_that_differ_only_by_case_separately() {
    for (int i = 0; i < 3; i++) {
      fail("1.1.1.1", "alice@b.com");
    }
    assertThrows(TooManyLoginAttemptsException.class, () -> succeed("2.2.2.2", "alice@b.com"));
    assertTrue(succeed("2.2.2.2", "Alice@b.com").isPresent());
  }

  @Test
  public void should_only_apply_email_limit_when_client_address_is_unknown() {
    for (int i = 0; i < 10; i++) {
      assertFalse(fail(null, "user" + i + "@b.com").isPresent());
    }
    for (int i = 0; i < 3; i++) {
      fail(null, "a@b.com");
    }
    assertThrows(TooManyLoginAttemptsException.class, () -> succeed(null, "a@b.com"));
  }

  @Test
  public void should_report_blocked_email_retry_time_even_when_tracking_is_full() {
    throttle = new LoginThrottle(3, 100, Duration.ofMinutes(15), 2, clock);
    for (int i = 0; i < 3; i++) {
      fail("1.1.1.1", "a@b.com");
    }
    clock.advance(Duration.ofMinutes(10));
    TooManyLoginAttemptsException e =
        assertThrows(TooManyLoginAttemptsException.class, () -> succeed("2.2.2.2", "a@b.com"));
    assertEquals(300, e.getRetryAfterSeconds());
  }

  @Test
  public void should_report_the_longer_delay_when_email_and_client_are_both_blocked() {
    for (int i = 0; i < 3; i++) {
      fail("9.9.9.9", "a@b.com");
    }
    clock.advance(Duration.ofMinutes(5));
    for (int i = 0; i < 5; i++) {
      fail("1.1.1.1", "user" + i + "@b.com");
    }
    clock.advance(Duration.ofMinutes(5));
    TooManyLoginAttemptsException e =
        assertThrows(TooManyLoginAttemptsException.class, () -> succeed("1.1.1.1", "a@b.com"));
    assertEquals(600, e.getRetryAfterSeconds());
  }

  @Test
  public void should_reject_windows_shorter_than_one_millisecond() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new LoginThrottle(3, 5, Duration.ofNanos(500_000), 1000, clock));
  }

  @Test
  public void should_not_run_authenticator_when_blocked() {
    for (int i = 0; i < 3; i++) {
      fail("1.1.1.1", "a@b.com");
    }
    AtomicInteger calls = new AtomicInteger();
    assertThrows(
        TooManyLoginAttemptsException.class,
        () ->
            throttle.attempt(
                "1.1.1.1",
                "a@b.com",
                () -> {
                  calls.incrementAndGet();
                  return Optional.empty();
                }));
    assertEquals(0, calls.get());
  }

  @Test
  public void should_block_client_after_max_failures_across_emails() {
    for (int i = 0; i < 5; i++) {
      fail("1.1.1.1", "user" + i + "@b.com");
    }
    assertThrows(TooManyLoginAttemptsException.class, () -> succeed("1.1.1.1", "new@b.com"));
    assertTrue(succeed("9.9.9.9", "new@b.com").isPresent());
  }

  @Test
  public void should_not_count_successful_logins_or_errors() {
    for (int i = 0; i < 10; i++) {
      assertTrue(succeed("1.1.1.1", "a@b.com").isPresent());
      assertThrows(
          IllegalStateException.class,
          () ->
              throttle.attempt(
                  "1.1.1.1",
                  "a@b.com",
                  () -> {
                    throw new IllegalStateException("db down");
                  }));
    }
    for (int i = 0; i < 3; i++) {
      fail("1.1.1.1", "a@b.com");
    }
    assertThrows(TooManyLoginAttemptsException.class, () -> fail("1.1.1.1", "a@b.com"));
  }

  @Test
  public void should_not_charge_email_when_client_is_blocked() {
    for (int i = 0; i < 5; i++) {
      fail("1.1.1.1", "user" + i + "@b.com");
    }
    for (int i = 0; i < 5; i++) {
      assertThrows(TooManyLoginAttemptsException.class, () -> fail("1.1.1.1", "victim@b.com"));
    }
    assertTrue(succeed("2.2.2.2", "victim@b.com").isPresent());
  }

  @Test
  public void should_allow_attempts_again_after_window_and_report_remaining_time() {
    for (int i = 0; i < 3; i++) {
      fail("1.1.1.1", "a@b.com");
    }
    clock.advance(Duration.ofMinutes(10).plusMillis(1));
    TooManyLoginAttemptsException e =
        assertThrows(TooManyLoginAttemptsException.class, () -> fail("1.1.1.1", "a@b.com"));
    assertEquals(300, e.getRetryAfterSeconds());

    clock.advance(Duration.ofMinutes(5));
    assertTrue(succeed("1.1.1.1", "a@b.com").isPresent());
    assertFalse(fail("1.1.1.1", "a@b.com").isPresent());
  }

  @Test
  public void should_fail_closed_when_tracking_capacity_is_full() {
    throttle = new LoginThrottle(3, 100, Duration.ofMinutes(15), 4, clock);
    fail("1.1.1.1", "a@b.com");
    fail("1.1.1.1", "b@b.com");
    fail("1.1.1.1", "c@b.com");
    assertThrows(TooManyLoginAttemptsException.class, () -> fail("1.1.1.1", "d@b.com"));

    clock.advance(Duration.ofMinutes(15));
    assertFalse(fail("1.1.1.1", "d@b.com").isPresent());
  }

  @Test
  public void should_not_exceed_limit_under_concurrent_attempts() throws Exception {
    throttle = new LoginThrottle(3, 1000, Duration.ofMinutes(15), 1000, clock);
    int threads = 32;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch inside = new CountDownLatch(1);
    AtomicInteger authenticatorCalls = new AtomicInteger();
    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      String client = "10.0.0." + i;
      futures.add(
          pool.submit(
              () -> {
                start.await();
                try {
                  throttle.attempt(
                      client,
                      "a@b.com",
                      () -> {
                        authenticatorCalls.incrementAndGet();
                        try {
                          inside.await(200, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                          Thread.currentThread().interrupt();
                        }
                        return Optional.empty();
                      });
                } catch (TooManyLoginAttemptsException ignored) {
                  // expected for all but three threads
                }
                return null;
              }));
    }
    start.countDown();
    for (Future<?> f : futures) {
      f.get(10, TimeUnit.SECONDS);
    }
    pool.shutdown();
    assertEquals(3, authenticatorCalls.get());
  }

  private static class MutableClock extends Clock {
    private Instant now = Instant.parse("2026-01-01T00:00:00Z");

    void advance(Duration d) {
      now = now.plus(d);
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
