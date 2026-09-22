package io.spring.api.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

public class RateLimiterTest {

  private static class MutableClock extends Clock {
    private Instant instant;

    MutableClock(Instant instant) {
      this.instant = instant;
    }

    void advanceSeconds(long seconds) {
      instant = instant.plusSeconds(seconds);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }

  @Test
  public void allows_up_to_limit_then_denies_with_retry_after() {
    MutableClock clock = new MutableClock(Instant.ofEpochSecond(1000));
    RateLimiter limiter = new RateLimiter(clock);
    for (int i = 0; i < 3; i++) {
      assertTrue(limiter.tryAcquire("k", 3).isAllowed());
    }
    RateLimiter.Result denied = limiter.tryAcquire("k", 3);
    assertFalse(denied.isAllowed());
    long retryAfter = denied.getRetryAfterSeconds();
    assertTrue(retryAfter >= 1 && retryAfter <= 60, "retryAfter=" + retryAfter);
    assertEquals(1020 - 1000, retryAfter);
  }

  @Test
  public void different_keys_have_independent_limits() {
    RateLimiter limiter = new RateLimiter(new MutableClock(Instant.ofEpochSecond(0)));
    assertTrue(limiter.tryAcquire("a", 1).isAllowed());
    assertFalse(limiter.tryAcquire("a", 1).isAllowed());
    assertTrue(limiter.tryAcquire("b", 1).isAllowed());
  }

  @Test
  public void window_resets_after_clock_advances() {
    MutableClock clock = new MutableClock(Instant.ofEpochSecond(5));
    RateLimiter limiter = new RateLimiter(clock);
    assertTrue(limiter.tryAcquire("k", 1).isAllowed());
    assertFalse(limiter.tryAcquire("k", 1).isAllowed());
    clock.advanceSeconds(60);
    assertTrue(limiter.tryAcquire("k", 1).isAllowed());
  }

  @Test
  public void honours_different_limit_values() {
    RateLimiter limiter = new RateLimiter(new MutableClock(Instant.ofEpochSecond(0)));
    for (int i = 0; i < 60; i++) {
      assertTrue(limiter.tryAcquire("unauth", 60).isAllowed());
    }
    assertFalse(limiter.tryAcquire("unauth", 60).isAllowed());
    for (int i = 0; i < 600; i++) {
      assertTrue(limiter.tryAcquire("auth", 600).isAllowed());
    }
    assertFalse(limiter.tryAcquire("auth", 600).isAllowed());
  }
}
