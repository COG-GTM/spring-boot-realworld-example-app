package io.spring.api.security;

import io.spring.api.exception.TooManyLoginAttemptsException;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * In-memory, fixed-window limiter for password logins, keyed both by the submitted email and by the
 * client address. A slot is taken before the password is checked, so concurrent requests cannot
 * exceed the limit; it is given back when the login succeeds or the check fails with an error, so
 * only wrong-password attempts count. Unknown and known emails are limited the same way.
 */
@Component
public class LoginThrottle {
  private final int maxFailuresPerEmail;
  private final int maxFailuresPerClient;
  private final long windowMillis;
  private final int maxTrackedKeys;
  private final Clock clock;
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  @Autowired
  public LoginThrottle(
      @Value("${security.login.max-failures-per-email:5}") int maxFailuresPerEmail,
      @Value("${security.login.max-failures-per-client:20}") int maxFailuresPerClient,
      @Value("${security.login.window:PT15M}") Duration window,
      @Value("${security.login.max-tracked-keys:100000}") int maxTrackedKeys) {
    this(maxFailuresPerEmail, maxFailuresPerClient, window, maxTrackedKeys, Clock.systemUTC());
  }

  LoginThrottle(
      int maxFailuresPerEmail,
      int maxFailuresPerClient,
      Duration window,
      int maxTrackedKeys,
      Clock clock) {
    if (maxFailuresPerEmail < 1 || maxFailuresPerClient < 1 || maxTrackedKeys < 2) {
      throw new IllegalArgumentException("login throttle limits must be positive");
    }
    if (window == null || window.toMillis() < 1) {
      throw new IllegalArgumentException("login throttle window must be at least 1ms");
    }
    this.maxFailuresPerEmail = maxFailuresPerEmail;
    this.maxFailuresPerClient = maxFailuresPerClient;
    this.windowMillis = window.toMillis();
    this.maxTrackedKeys = maxTrackedKeys;
    this.clock = clock;
  }

  /**
   * Runs {@code authenticator} if neither the email nor the client is over its limit.
   *
   * @throws TooManyLoginAttemptsException if a limit has been reached
   */
  public <T> Optional<T> attempt(
      String clientAddress, String email, Supplier<Optional<T>> authenticator) {
    // Emails are matched exactly, like the user lookup, so distinct accounts never share a limit.
    String emailKey = "email:" + (email == null ? "" : email);
    // Without a client address only the per-email limit applies, rather than one limit shared by
    // every such request.
    String clientKey = clientAddress == null ? null : "client:" + clientAddress;

    rejectIfBlocked(emailKey, clientKey);
    long clientWindow = clientKey == null ? 0 : acquire(clientKey, maxFailuresPerClient);
    long emailWindow;
    try {
      emailWindow = acquire(emailKey, maxFailuresPerEmail);
    } catch (RuntimeException e) {
      release(clientKey, clientWindow);
      throw e;
    }

    boolean failed = false;
    try {
      Optional<T> result = authenticator.get();
      failed = !result.isPresent();
      return result;
    } finally {
      if (!failed) {
        release(emailKey, emailWindow);
        release(clientKey, clientWindow);
      }
    }
  }

  private long acquire(String key, int limit) {
    long now = clock.millis();
    ensureCapacity(key, now);
    long[] retryAfterMillis = {0};
    Window window =
        windows.compute(
            key,
            (k, current) -> {
              if (current == null || current.isExpired(now, windowMillis)) {
                return new Window(now, 1);
              }
              if (current.count >= limit) {
                retryAfterMillis[0] = current.start + windowMillis - now;
                return current;
              }
              return new Window(current.start, current.count + 1);
            });
    if (retryAfterMillis[0] > 0) {
      throw new TooManyLoginAttemptsException(toSeconds(retryAfterMillis[0]));
    }
    return window.start;
  }

  private void rejectIfBlocked(String emailKey, String clientKey) {
    long now = clock.millis();
    long retryAfterMillis =
        Math.max(
            remainingBlockMillis(emailKey, maxFailuresPerEmail, now),
            remainingBlockMillis(clientKey, maxFailuresPerClient, now));
    if (retryAfterMillis > 0) {
      throw new TooManyLoginAttemptsException(toSeconds(retryAfterMillis));
    }
  }

  private long remainingBlockMillis(String key, int limit, long now) {
    Window current = key == null ? null : windows.get(key);
    if (current == null || current.isExpired(now, windowMillis) || current.count < limit) {
      return 0;
    }
    return current.start + windowMillis - now;
  }

  private void release(String key, long windowStart) {
    if (key == null) {
      return;
    }
    windows.computeIfPresent(
        key,
        (k, current) -> {
          if (current.start != windowStart) {
            return current;
          }
          return current.count <= 1 ? null : new Window(current.start, current.count - 1);
        });
  }

  private void ensureCapacity(String key, long now) {
    if (windows.size() < maxTrackedKeys || windows.containsKey(key)) {
      return;
    }
    windows.entrySet().removeIf(e -> e.getValue().isExpired(now, windowMillis));
    if (windows.size() >= maxTrackedKeys) {
      throw new TooManyLoginAttemptsException(toSeconds(windowMillis));
    }
  }

  private static long toSeconds(long millis) {
    return Math.max(1, (millis + 999) / 1000);
  }

  private static final class Window {
    private final long start;
    private final int count;

    private Window(long start, int count) {
      this.start = start;
      this.count = count;
    }

    private boolean isExpired(long now, long windowMillis) {
      return now - start >= windowMillis;
    }
  }
}
