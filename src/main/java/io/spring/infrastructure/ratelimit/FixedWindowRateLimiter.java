package io.spring.infrastructure.ratelimit;

import io.spring.core.ratelimit.RateLimitAction;
import io.spring.core.ratelimit.RateLimitDecision;
import io.spring.core.ratelimit.RateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory, per-instance fixed-window limiter. Each (action, subject) pair gets a window that
 * opens on its first request and allows {@code requests} attempts until {@code window} elapses.
 */
public class FixedWindowRateLimiter implements RateLimiter {
  static final int EVICTION_INTERVAL = 1000;

  private final RateLimitProperties properties;
  private final Clock clock;
  private final Map<Key, Window> windows = new ConcurrentHashMap<>();
  private final AtomicLong acquisitions = new AtomicLong();

  public FixedWindowRateLimiter(RateLimitProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public RateLimitDecision tryAcquire(RateLimitAction action, String subject) {
    if (!properties.isEnabled()) {
      return RateLimitDecision.unlimited();
    }
    RateLimitProperties.Limit limit = properties.getLimits().get(action);
    if (limit == null || limit.getRequests() < 0) {
      return RateLimitDecision.unlimited();
    }

    Instant now = clock.instant();
    if (acquisitions.incrementAndGet() % EVICTION_INTERVAL == 0) {
      evictExpired(now);
    }

    Window window =
        windows.compute(
            new Key(action, subject),
            (k, current) -> {
              if (current == null || current.isExpired(now)) {
                return new Window(now.plus(limit.getWindow()), 1);
              }
              return new Window(current.resetAt, current.count + 1);
            });

    int max = limit.getRequests();
    if (window.count <= max) {
      return new RateLimitDecision(true, max, max - window.count, 0);
    }
    return new RateLimitDecision(false, max, 0, secondsUntil(now, window.resetAt));
  }

  int trackedWindows() {
    return windows.size();
  }

  private void evictExpired(Instant now) {
    windows.values().removeIf(w -> w.isExpired(now));
  }

  private static long secondsUntil(Instant now, Instant resetAt) {
    long millis = Duration.between(now, resetAt).toMillis();
    return Math.max(1, (millis + 999) / 1000);
  }

  private static final class Key {
    private final RateLimitAction action;
    private final String subject;

    private Key(RateLimitAction action, String subject) {
      this.action = action;
      this.subject = subject;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Key)) return false;
      Key key = (Key) o;
      return action == key.action && subject.equals(key.subject);
    }

    @Override
    public int hashCode() {
      return Objects.hash(action, subject);
    }
  }

  private static final class Window {
    private final Instant resetAt;
    private final int count;

    private Window(Instant resetAt, int count) {
      this.resetAt = resetAt;
      this.count = count;
    }

    private boolean isExpired(Instant now) {
      return !now.isBefore(resetAt);
    }
  }
}
