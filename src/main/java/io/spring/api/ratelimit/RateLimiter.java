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

  public RateLimiter() {
    this(Clock.systemUTC());
  }

  public RateLimiter(Clock clock) {
    this.clock = clock;
  }

  public Decision tryAcquire(String key, int limit) {
    long now = clock.millis();
    long windowStart = now - (now % WINDOW.toMillis());
    Window window =
        windows.compute(
            key,
            (k, existing) ->
                existing == null || existing.start != windowStart
                    ? new Window(windowStart)
                    : existing);
    int count = window.count.incrementAndGet();
    if (count <= limit) {
      return Decision.allowed();
    }
    long retryAfterMillis = windowStart + WINDOW.toMillis() - now;
    return Decision.rejected(Math.max(1, (int) Math.ceil(retryAfterMillis / 1000.0)));
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
