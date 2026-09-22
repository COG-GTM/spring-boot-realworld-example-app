package io.spring.api.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {
  static final Duration WINDOW = Duration.ofMinutes(1);

  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();
  private volatile long lastEvictionWindowStart = Long.MIN_VALUE;

  public RateLimiter() {
    this(Clock.systemUTC());
  }

  public RateLimiter(Clock clock) {
    this.clock = clock;
  }

  public Decision tryAcquire(String key, int limit) {
    return tryAcquireAt(key, limit, clock.millis());
  }

  Decision tryAcquireAt(String key, int limit, long now) {
    long windowStart = now - (now % WINDOW.toMillis());
    evictExpired(windowStart);
    Window window =
        windows.compute(
            key,
            (k, existing) ->
                existing == null || existing.start < windowStart
                    ? new Window(windowStart)
                    : existing);
    int count = window.count.incrementAndGet();
    if (count <= limit) {
      return Decision.allowed();
    }
    long retryAfterMillis = Math.min(WINDOW.toMillis(), window.start + WINDOW.toMillis() - now);
    return Decision.rejected(Math.max(1, (int) Math.ceil(retryAfterMillis / 1000.0)));
  }

  int size() {
    return windows.size();
  }

  private void evictExpired(long currentWindowStart) {
    if (lastEvictionWindowStart >= currentWindowStart) {
      return;
    }
    lastEvictionWindowStart = currentWindowStart;
    windows.values().removeIf(window -> window.start < currentWindowStart);
  }

  public void reset() {
    windows.clear();
  }

  private static final class Window {
    private final long start;
    private final AtomicInteger count = new AtomicInteger();

    private Window(long start) {
      this.start = start;
    }
  }

  public static final class Decision {
    private final boolean allowed;
    private final int retryAfterSeconds;

    private Decision(boolean allowed, int retryAfterSeconds) {
      this.allowed = allowed;
      this.retryAfterSeconds = retryAfterSeconds;
    }

    static Decision allowed() {
      return new Decision(true, 0);
    }

    static Decision rejected(int retryAfterSeconds) {
      return new Decision(false, retryAfterSeconds);
    }

    public boolean isAllowed() {
      return allowed;
    }

    public int getRetryAfterSeconds() {
      return retryAfterSeconds;
    }
  }
}
