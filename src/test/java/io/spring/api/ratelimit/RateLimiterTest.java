package io.spring.api.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

public class RateLimiterTest {
  private final AtomicLong now =
      new AtomicLong(Instant.parse("2024-01-01T00:00:10Z").toEpochMilli());
  private final Clock clock =
      new Clock() {
        @Override
        public ZoneOffset getZone() {
          return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
          return this;
        }

        @Override
        public Instant instant() {
          return Instant.ofEpochMilli(now.get());
        }
      };
  private final RateLimiter limiter = new RateLimiter(clock);

  @Test
  public void should_allow_requests_up_to_limit_then_reject() {
    for (int i = 0; i < 3; i++) {
      assertTrue(limiter.tryAcquire("ip:1.2.3.4", 3).isAllowed());
    }
    RateLimiter.Decision rejected = limiter.tryAcquire("ip:1.2.3.4", 3);
    assertFalse(rejected.isAllowed());
    assertEquals(50, rejected.getRetryAfterSeconds());
  }

  @Test
  public void should_track_keys_independently() {
    assertTrue(limiter.tryAcquire("ip:a", 1).isAllowed());
    assertFalse(limiter.tryAcquire("ip:a", 1).isAllowed());
    assertTrue(limiter.tryAcquire("ip:b", 1).isAllowed());
    assertTrue(limiter.tryAcquire("key:a", 1).isAllowed());
  }

  @Test
  public void should_reset_when_window_rolls_over() {
    assertTrue(limiter.tryAcquire("ip:a", 1).isAllowed());
    assertFalse(limiter.tryAcquire("ip:a", 1).isAllowed());
    now.addAndGet(60_000);
    assertTrue(limiter.tryAcquire("ip:a", 1).isAllowed());
  }

  @Test
  public void should_round_retry_after_up_to_at_least_one_second() {
    now.set(Instant.parse("2024-01-01T00:00:59.900Z").toEpochMilli());
    limiter.tryAcquire("ip:a", 1);
    assertEquals(1, limiter.tryAcquire("ip:a", 1).getRetryAfterSeconds());
  }

  @Test
  public void should_evict_expired_windows_when_window_rolls_over() {
    limiter.tryAcquire("ip:a", 1);
    limiter.tryAcquire("ip:b", 1);
    assertEquals(2, limiter.size());
    now.addAndGet(60_000);
    limiter.tryAcquire("ip:c", 1);
    assertEquals(1, limiter.size());
  }

  @Test
  public void should_not_replace_newer_window_with_older_one() throws Exception {
    // request B observes the new minute and is admitted before a delayed request A (which read
    // the clock in the previous minute) reaches the map; A must not reset B's count.
    long previousMinute = now.get();
    now.addAndGet(60_000);
    assertTrue(limiter.tryAcquire("ip:a", 1).isAllowed());
    RateLimiter.Decision lateFromPreviousMinute = limiter.tryAcquireAt("ip:a", 1, previousMinute);
    assertFalse(lateFromPreviousMinute.isAllowed());
    assertEquals(60, lateFromPreviousMinute.getRetryAfterSeconds());
    assertFalse(limiter.tryAcquire("ip:a", 1).isAllowed());
  }

  @Test
  public void should_clear_state_on_reset() {
    limiter.tryAcquire("ip:a", 1);
    limiter.reset();
    assertTrue(limiter.tryAcquire("ip:a", 1).isAllowed());
  }
}
