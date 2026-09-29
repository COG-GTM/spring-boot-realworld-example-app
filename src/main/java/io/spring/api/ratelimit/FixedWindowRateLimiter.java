package io.spring.api.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory fixed-window counter keyed by an arbitrary client key. State is per JVM instance, so
 * limits apply per node when the app is horizontally scaled.
 */
public class FixedWindowRateLimiter {
  private static final int EVICTION_THRESHOLD = 10_000;

  private final int limit;
  private final long windowMillis;
  private final Clock clock;
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  public FixedWindowRateLimiter(int limit, Duration window, Clock clock) {
    if (limit <= 0) {
      throw new IllegalArgumentException("limit must be positive");
    }
    if (window == null || window.isZero() || window.isNegative()) {
      throw new IllegalArgumentException("window must be positive");
    }
    this.limit = limit;
    this.windowMillis = window.toMillis();
    this.clock = clock;
  }

  public RateLimitResult tryAcquire(String key) {
    long now = clock.millis();
    evictExpiredIfNeeded(now);
    Window window =
        windows.compute(
            key,
            (k, existing) -> {
              if (existing == null || now >= existing.resetAt) {
                return new Window(now + windowMillis, 1);
              }
              return new Window(existing.resetAt, Math.min(existing.count + 1, limit + 1));
            });
    long resetAfterSeconds = Math.max(1, (window.resetAt - now + 999) / 1000);
    return new RateLimitResult(
        window.count <= limit, limit, Math.max(0, limit - window.count), resetAfterSeconds);
  }

  private void evictExpiredIfNeeded(long now) {
    if (windows.size() > EVICTION_THRESHOLD) {
      windows.values().removeIf(w -> now >= w.resetAt);
    }
  }

  private static final class Window {
    private final long resetAt;
    private final int count;

    private Window(long resetAt, int count) {
      this.resetAt = resetAt;
      this.count = count;
    }
  }
}
