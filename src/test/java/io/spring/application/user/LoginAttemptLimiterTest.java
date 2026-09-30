package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LoginAttemptLimiterTest {
  private MutableClock clock;
  private LoginAttemptLimiter limiter;

  @BeforeEach
  public void setUp() {
    clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    limiter = new LoginAttemptLimiter(3, 5, Duration.ofMinutes(15), 1000, clock);
  }

  @Test
  public void should_block_account_after_max_failures_regardless_of_client() {
    for (int i = 0; i < 3; i++) {
      assertEquals(0, limiter.retryAfterSeconds("a@b.com", "10.0.0." + i));
      limiter.recordFailure("a@b.com", "10.0.0." + i);
    }
    assertEquals(900, limiter.retryAfterSeconds("A@B.com ", "10.0.0.99"));
    assertEquals(0, limiter.retryAfterSeconds("other@b.com", "10.0.0.99"));
  }

  @Test
  public void should_block_client_after_max_failures_across_accounts() {
    for (int i = 0; i < 5; i++) {
      limiter.recordFailure("user" + i + "@b.com", "10.0.0.1");
    }
    assertTrue(limiter.retryAfterSeconds("new@b.com", "10.0.0.1") > 0);
    assertEquals(0, limiter.retryAfterSeconds("new@b.com", "10.0.0.2"));
  }

  @Test
  public void should_unblock_after_window_expires() {
    for (int i = 0; i < 3; i++) {
      limiter.recordFailure("a@b.com", "10.0.0.1");
    }
    clock.advance(Duration.ofMinutes(10));
    assertEquals(300, limiter.retryAfterSeconds("a@b.com", "10.0.0.2"));
    clock.advance(Duration.ofMinutes(5));
    assertEquals(0, limiter.retryAfterSeconds("a@b.com", "10.0.0.2"));
  }

  @Test
  public void should_reset_account_on_success() {
    limiter.recordFailure("a@b.com", "10.0.0.1");
    limiter.recordFailure("a@b.com", "10.0.0.1");
    limiter.recordSuccess("a@b.com");
    limiter.recordFailure("a@b.com", "10.0.0.2");
    limiter.recordFailure("a@b.com", "10.0.0.2");
    assertEquals(0, limiter.retryAfterSeconds("a@b.com", "10.0.0.3"));
  }

  @Test
  public void should_prune_expired_entries_when_full() {
    LoginAttemptLimiter small = new LoginAttemptLimiter(3, 5, Duration.ofMinutes(1), 4, clock);
    small.recordFailure("a@b.com", "10.0.0.1");
    small.recordFailure("b@b.com", "10.0.0.2");
    clock.advance(Duration.ofMinutes(2));
    small.recordFailure("c@b.com", "10.0.0.3");
    assertEquals(2, small.trackedKeys());
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
    public ZoneOffset getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
