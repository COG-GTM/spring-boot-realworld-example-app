package io.spring.api.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FixedWindowRateLimiterTest {
  private FakeTicker clock;
  private FixedWindowRateLimiter limiter;

  @BeforeEach
  public void setUp() {
    clock = new FakeTicker(Long.MAX_VALUE - Duration.ofSeconds(30).toNanos());
    limiter = new FixedWindowRateLimiter(3, Duration.ofSeconds(60), clock::nanos);
  }

  @Test
  public void should_allow_requests_up_to_limit_then_reject() {
    RateLimitResult first = limiter.tryAcquire("a");
    assertTrue(first.isAllowed());
    assertEquals(3, first.getLimit());
    assertEquals(2, first.getRemaining());
    assertEquals(60, first.getResetAfterSeconds());

    assertEquals(1, limiter.tryAcquire("a").getRemaining());
    RateLimitResult third = limiter.tryAcquire("a");
    assertTrue(third.isAllowed());
    assertEquals(0, third.getRemaining());

    RateLimitResult rejected = limiter.tryAcquire("a");
    assertFalse(rejected.isAllowed());
    assertEquals(0, rejected.getRemaining());
  }

  @Test
  public void should_report_seconds_until_window_reset_rounded_up() {
    limiter.tryAcquire("a");
    clock.advance(Duration.ofMillis(20_500));

    assertEquals(40, limiter.tryAcquire("a").getResetAfterSeconds());
  }

  @Test
  public void should_reset_after_window_elapses() {
    for (int i = 0; i < 4; i++) {
      limiter.tryAcquire("a");
    }
    assertFalse(limiter.tryAcquire("a").isAllowed());

    clock.advance(Duration.ofSeconds(60));

    RateLimitResult result = limiter.tryAcquire("a");
    assertTrue(result.isAllowed());
    assertEquals(2, result.getRemaining());
    assertEquals(60, result.getResetAfterSeconds());
  }

  @Test
  public void should_track_keys_independently() {
    for (int i = 0; i < 3; i++) {
      limiter.tryAcquire("a");
    }
    assertFalse(limiter.tryAcquire("a").isAllowed());
    assertTrue(limiter.tryAcquire("b").isAllowed());
  }

  @Test
  public void should_reject_invalid_configuration() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new FixedWindowRateLimiter(0, Duration.ofSeconds(1), clock::nanos));
    assertThrows(
        IllegalArgumentException.class,
        () -> new FixedWindowRateLimiter(1, Duration.ZERO, clock::nanos));
  }

  @Test
  public void should_enforce_submillisecond_windows() {
    FixedWindowRateLimiter tiny =
        new FixedWindowRateLimiter(1, Duration.ofNanos(500), clock::nanos);

    assertTrue(tiny.tryAcquire("a").isAllowed());
    assertFalse(tiny.tryAcquire("a").isAllowed());

    clock.advance(Duration.ofNanos(500));
    assertTrue(tiny.tryAcquire("a").isAllowed());
  }

  @Test
  public void should_report_remaining_correctly_at_max_limit() {
    FixedWindowRateLimiter max =
        new FixedWindowRateLimiter(Integer.MAX_VALUE, Duration.ofSeconds(1), clock::nanos);

    assertEquals(Integer.MAX_VALUE - 1, max.tryAcquire("a").getRemaining());
    RateLimitResult second = max.tryAcquire("a");
    assertTrue(second.isAllowed());
    assertEquals(Integer.MAX_VALUE - 2, second.getRemaining());
  }

  @Test
  public void should_sweep_expired_windows_at_most_once_per_window() {
    fill("a");
    clock.advance(Duration.ofSeconds(60));
    limiter.tryAcquire("x");
    assertEquals(1, limiter.size());

    fill("b");
    clock.advance(Duration.ofSeconds(30));
    fill("c");
    clock.advance(Duration.ofSeconds(30));
    limiter.tryAcquire("y");
    assertEquals(10_002, limiter.size());

    clock.advance(Duration.ofSeconds(30));
    limiter.tryAcquire("z");
    assertEquals(10_003, limiter.size());

    clock.advance(Duration.ofSeconds(30));
    limiter.tryAcquire("w");
    assertEquals(2, limiter.size());
  }

  private void fill(String prefix) {
    for (int i = 0; i <= 10_000; i++) {
      limiter.tryAcquire(prefix + i);
    }
  }

  private static class FakeTicker {
    private final AtomicLong nanos;

    FakeTicker(long start) {
      nanos = new AtomicLong(start);
    }

    long nanos() {
      return nanos.get();
    }

    void advance(Duration duration) {
      nanos.addAndGet(duration.toNanos());
    }
  }
}
