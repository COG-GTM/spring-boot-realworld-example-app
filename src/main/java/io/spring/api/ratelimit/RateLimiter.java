package io.spring.api.ratelimit;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {
  private static final long WINDOW_SECONDS = 60;
  private static final int EVICTION_THRESHOLD = 10_000;

  private final Clock clock;
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  public RateLimiter(Clock clock) {
    this.clock = clock;
  }

  public Result tryAcquire(String key, int limit) {
    long now = clock.instant().getEpochSecond();
    long windowStart = (now / WINDOW_SECONDS) * WINDOW_SECONDS;
    if (windows.size() > EVICTION_THRESHOLD) {
      windows.entrySet().removeIf(e -> e.getValue().windowStartEpochSecond < windowStart);
    }
    Window window =
        windows.compute(
            key,
            (k, existing) -> {
              if (existing == null || existing.windowStartEpochSecond < windowStart) {
                return new Window(windowStart);
              }
              return existing;
            });
    int count = window.count.incrementAndGet();
    if (count <= limit) {
      return new Result(true, 0);
    }
    long retryAfter = Math.max(1, windowStart + WINDOW_SECONDS - now);
    return new Result(false, retryAfter);
  }

  public static class Result {
    private final boolean allowed;
    private final long retryAfterSeconds;

    public Result(boolean allowed, long retryAfterSeconds) {
      this.allowed = allowed;
      this.retryAfterSeconds = retryAfterSeconds;
    }

    public boolean isAllowed() {
      return allowed;
    }

    public long getRetryAfterSeconds() {
      return retryAfterSeconds;
    }
  }

  private static class Window {
    private final long windowStartEpochSecond;
    private final AtomicInteger count = new AtomicInteger(0);

    Window(long windowStartEpochSecond) {
      this.windowStartEpochSecond = windowStartEpochSecond;
    }
  }
}
