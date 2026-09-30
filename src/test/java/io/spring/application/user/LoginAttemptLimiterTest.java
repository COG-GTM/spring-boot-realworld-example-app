package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
  public void should_block_account_after_max_attempts_regardless_of_client() {
    for (int i = 0; i < 3; i++) {
      assertEquals(0, limiter.tryAcquire("a@b.com", "10.0.0." + i));
    }
    assertEquals(900, limiter.tryAcquire("A@B.com ", "10.0.0.99"));
    assertEquals(0, limiter.tryAcquire("other@b.com", "10.0.0.99"));
  }

  @Test
  public void should_block_client_after_max_attempts_across_accounts() {
    for (int i = 0; i < 5; i++) {
      assertEquals(0, limiter.tryAcquire("user" + i + "@b.com", "10.0.0.1"));
    }
    assertTrue(limiter.tryAcquire("new@b.com", "10.0.0.1") > 0);
    assertEquals(0, limiter.tryAcquire("new@b.com", "10.0.0.2"));
  }

  @Test
  public void should_unblock_after_window_expires() {
    for (int i = 0; i < 3; i++) {
      limiter.tryAcquire("a@b.com", "10.0.0.1");
    }
    clock.advance(Duration.ofMinutes(10));
    assertEquals(300, limiter.tryAcquire("a@b.com", "10.0.0.2"));
    clock.advance(Duration.ofMinutes(5));
    assertEquals(0, limiter.tryAcquire("a@b.com", "10.0.0.2"));
  }

  @Test
  public void should_round_retry_after_up() {
    for (int i = 0; i < 3; i++) {
      limiter.tryAcquire("a@b.com", "10.0.0.1");
    }
    clock.advance(Duration.ofMinutes(10).plusMillis(500));
    assertEquals(300, limiter.tryAcquire("a@b.com", "10.0.0.2"));
  }

  @Test
  public void should_reset_account_and_refund_client_on_success() {
    for (int i = 0; i < 10; i++) {
      assertEquals(0, limiter.tryAcquire("a@b.com", "10.0.0.1"));
      limiter.recordSuccess("a@b.com", "10.0.0.1");
    }
    assertEquals(0, limiter.trackedKeys());
  }

  @Test
  public void should_not_allow_concurrent_attempts_beyond_limit() throws Exception {
    int threads = 32;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Long>> results = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      String client = "10.0.1." + i;
      results.add(
          pool.submit(
              () -> {
                start.await();
                return limiter.tryAcquire("a@b.com", client);
              }));
    }
    start.countDown();
    int allowed = 0;
    for (Future<Long> result : results) {
      if (result.get() == 0) {
        allowed++;
      }
    }
    pool.shutdown();
    assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
    assertEquals(3, allowed);
  }

  @Test
  public void should_cap_tracked_keys_and_prune_expired_entries() {
    LoginAttemptLimiter small = new LoginAttemptLimiter(3, 5, Duration.ofMinutes(1), 4, clock);
    assertEquals(0, small.tryAcquire("a@b.com", "10.0.0.1"));
    assertEquals(0, small.tryAcquire("b@b.com", "10.0.0.2"));
    assertEquals(60, small.tryAcquire("c@b.com", "10.0.0.3"));
    assertEquals(0, small.tryAcquire("a@b.com", "10.0.0.1"));
    assertEquals(4, small.trackedKeys());

    clock.advance(Duration.ofMinutes(1));
    assertEquals(0, small.tryAcquire("c@b.com", "10.0.0.3"));
    assertEquals(2, small.trackedKeys());
  }

  @Test
  public void should_report_retry_after_when_enough_slots_expire() {
    LoginAttemptLimiter small = new LoginAttemptLimiter(3, 5, Duration.ofSeconds(60), 3, clock);
    assertEquals(0, small.tryAcquire("a@b.com", "10.0.0.1"));
    clock.advance(Duration.ofSeconds(10));
    assertEquals(0, small.tryAcquire("b@b.com", "10.0.0.1"));
    small.recordSuccess("a@b.com", "10.0.0.1");
    clock.advance(Duration.ofSeconds(10));
    assertEquals(0, small.tryAcquire("d@b.com", "10.0.0.1"));

    // Stored: client (expires t=60), b@ (t=70), d@ (t=80). A new account from a new client needs
    // two free slots, so it must wait for the second expiry, not the first.
    assertEquals(50, small.tryAcquire("e@b.com", "10.0.0.2"));
    clock.advance(Duration.ofSeconds(40));
    assertEquals(10, small.tryAcquire("e@b.com", "10.0.0.2"));
    clock.advance(Duration.ofSeconds(10));
    assertEquals(0, small.tryAcquire("e@b.com", "10.0.0.2"));
  }

  @Test
  public void should_refund_released_attempts() {
    for (int i = 0; i < 10; i++) {
      assertEquals(0, limiter.tryAcquire("a@b.com", "10.0.0.1"));
      limiter.release("a@b.com", "10.0.0.1");
    }
    assertEquals(0, limiter.trackedKeys());
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
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
