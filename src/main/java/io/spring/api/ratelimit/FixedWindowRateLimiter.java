package io.spring.api.ratelimit;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * In-memory fixed-window counter keyed by an arbitrary client key. State is per JVM instance, so
 * limits apply per node when the app is horizontally scaled. Time is read from a monotonic
 * nanosecond source so wall-clock adjustments do not shorten or extend windows.
 */
public class FixedWindowRateLimiter {
  private static final int EVICTION_THRESHOLD = 10_000;
  private static final long NANOS_PER_SECOND = 1_000_000_000L;

  private final int limit;
  private final long windowNanos;
  private final LongSupplier nanoTime;
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
  private final AtomicLong nextSweepAt;

  public FixedWindowRateLimiter(int limit, Duration window, LongSupplier nanoTime) {
    if (limit <= 0) {
      throw new IllegalArgumentException("limit must be positive");
    }
    if (window == null || window.isZero() || window.isNegative()) {
      throw new IllegalArgumentException("window must be positive");
    }
    this.limit = limit;
    this.windowNanos = window.toNanos();
    this.nanoTime = nanoTime;
    this.nextSweepAt = new AtomicLong(nanoTime.getAsLong());
  }

  public RateLimitResult tryAcquire(String key) {
    long now = nanoTime.getAsLong();
    evictExpiredIfNeeded(now);
    Window window =
        windows.compute(
            key,
            (k, existing) -> {
              if (existing == null || existing.isExpired(now)) {
                return new Window(now + windowNanos, 1);
              }
              return new Window(existing.resetAt, Math.min(existing.count + 1, limit + 1L));
            });
    long remainingNanos = window.resetAt - now;
    long resetAfterSeconds =
        Math.max(1, (remainingNanos + NANOS_PER_SECOND - 1) / NANOS_PER_SECOND);
    return new RateLimitResult(
        window.count <= limit, limit, (int) Math.max(0, limit - window.count), resetAfterSeconds);
  }

  /** Sweeps expired windows at most once per window length so per-request cost stays bounded. */
  private void evictExpiredIfNeeded(long now) {
    if (windows.size() <= EVICTION_THRESHOLD) {
      return;
    }
    long sweepAt = nextSweepAt.get();
    if (now - sweepAt >= 0 && nextSweepAt.compareAndSet(sweepAt, now + windowNanos)) {
      windows.values().removeIf(w -> w.isExpired(now));
    }
  }

  int size() {
    return windows.size();
  }

  private static final class Window {
    private final long resetAt;
    private final long count;

    private Window(long resetAt, long count) {
      this.resetAt = resetAt;
      this.count = count;
    }

    private boolean isExpired(long now) {
      return now - resetAt >= 0;
    }
  }
}
