package io.spring.infrastructure.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.spring.core.ratelimit.RateLimitAction;
import io.spring.core.ratelimit.RateLimitDecision;
import io.spring.core.ratelimit.RateLimitExceededException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FixedWindowRateLimiterTest {
  private MutableClock clock;
  private RateLimitProperties properties;
  private FixedWindowRateLimiter limiter;

  @BeforeEach
  public void setUp() {
    clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    properties = new RateLimitProperties();
    properties
        .getLimits()
        .put(
            RateLimitAction.ARTICLE_CREATE,
            new RateLimitProperties.Limit(3, Duration.ofMinutes(1)));
    properties
        .getLimits()
        .put(RateLimitAction.FOLLOW, new RateLimitProperties.Limit(1, Duration.ofSeconds(10)));
    limiter = new FixedWindowRateLimiter(properties, clock);
  }

  @Test
  public void should_allow_requests_up_to_limit_and_reject_after() {
    for (int i = 2; i >= 0; i--) {
      RateLimitDecision decision = limiter.tryAcquire(RateLimitAction.ARTICLE_CREATE, "user:a");
      assertTrue(decision.isAllowed());
      assertEquals(3, decision.getLimit());
      assertEquals(i, decision.getRemaining());
    }

    RateLimitDecision rejected = limiter.tryAcquire(RateLimitAction.ARTICLE_CREATE, "user:a");
    assertFalse(rejected.isAllowed());
    assertEquals(0, rejected.getRemaining());
    assertEquals(60, rejected.getRetryAfterSeconds());
  }

  @Test
  public void should_report_retry_after_relative_to_window_end() {
    limiter.tryAcquire(RateLimitAction.ARTICLE_CREATE, "user:a");
    limiter.tryAcquire(RateLimitAction.ARTICLE_CREATE, "user:a");
    limiter.tryAcquire(RateLimitAction.ARTICLE_CREATE, "user:a");

    clock.advance(Duration.ofMillis(45_500));

    assertEquals(
        15, limiter.tryAcquire(RateLimitAction.ARTICLE_CREATE, "user:a").getRetryAfterSeconds());
  }

  @Test
  public void should_reset_after_window_elapses() {
    assertTrue(limiter.tryAcquire(RateLimitAction.FOLLOW, "user:a").isAllowed());
    assertFalse(limiter.tryAcquire(RateLimitAction.FOLLOW, "user:a").isAllowed());

    clock.advance(Duration.ofSeconds(10));

    assertTrue(limiter.tryAcquire(RateLimitAction.FOLLOW, "user:a").isAllowed());
  }

  @Test
  public void should_track_subjects_and_actions_independently() {
    assertTrue(limiter.tryAcquire(RateLimitAction.FOLLOW, "user:a").isAllowed());
    assertFalse(limiter.tryAcquire(RateLimitAction.FOLLOW, "user:a").isAllowed());

    assertTrue(limiter.tryAcquire(RateLimitAction.FOLLOW, "user:b").isAllowed());
    assertTrue(limiter.tryAcquire(RateLimitAction.ARTICLE_CREATE, "user:a").isAllowed());
  }

  @Test
  public void should_not_limit_unconfigured_actions() {
    for (int i = 0; i < 100; i++) {
      RateLimitDecision decision = limiter.tryAcquire(RateLimitAction.COMMENT_CREATE, "user:a");
      assertTrue(decision.isAllowed());
      assertFalse(decision.isLimited());
    }
  }

  @Test
  public void should_not_limit_when_disabled() {
    properties.setEnabled(false);
    for (int i = 0; i < 10; i++) {
      assertTrue(limiter.tryAcquire(RateLimitAction.FOLLOW, "user:a").isAllowed());
    }
  }

  @Test
  public void should_reject_everything_when_limit_is_zero() {
    properties
        .getLimits()
        .put(
            RateLimitAction.COMMENT_CREATE,
            new RateLimitProperties.Limit(0, Duration.ofMinutes(1)));
    assertFalse(limiter.tryAcquire(RateLimitAction.COMMENT_CREATE, "user:a").isAllowed());
  }

  @Test
  public void should_throw_from_acquire_or_throw_when_rejected() {
    limiter.acquireOrThrow(RateLimitAction.FOLLOW, "user:a");
    RateLimitExceededException e =
        assertThrows(
            RateLimitExceededException.class,
            () -> limiter.acquireOrThrow(RateLimitAction.FOLLOW, "user:a"));
    assertEquals(RateLimitAction.FOLLOW, e.getAction());
    assertEquals(10, e.getDecision().getRetryAfterSeconds());
  }

  @Test
  public void should_evict_expired_windows() {
    for (int i = 0; i < 10; i++) {
      limiter.tryAcquire(RateLimitAction.FOLLOW, "user:" + i);
    }
    assertEquals(10, limiter.trackedWindows());

    clock.advance(Duration.ofSeconds(11));
    for (int i = 10; i < FixedWindowRateLimiter.EVICTION_INTERVAL; i++) {
      limiter.tryAcquire(RateLimitAction.FOLLOW, "user:fresh");
    }

    assertEquals(1, limiter.trackedWindows());
  }

  private static class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant now) {
      this.now = now;
    }

    void advance(Duration duration) {
      now = now.plus(duration);
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
      return now;
    }
  }
}
