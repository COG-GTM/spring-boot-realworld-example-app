package io.spring.application.user;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Limits login attempts per account (email) and per client (IP) in a fixed window. An attempt is
 * counted atomically before the password is checked, so concurrent guesses cannot exceed the
 * limits; a successful login refunds it. Unknown emails are tracked exactly like registered ones so
 * throttling never reveals whether an account exists.
 *
 * <p>At most {@code maxTrackedKeys} windows are stored. When storage is full of live windows,
 * attempts that would need a new key are rejected until the oldest window expires.
 */
@Component
public class LoginAttemptLimiter {
  private static final String ACCOUNT_PREFIX = "account:";
  private static final String CLIENT_PREFIX = "client:";

  private final int maxFailuresPerAccount;
  private final int maxFailuresPerClient;
  private final Duration window;
  private final int maxTrackedKeys;
  private final Clock clock;
  private final Map<String, FailureWindow> failures = new HashMap<>();
  private Instant nextPruneAt = Instant.MIN;

  @Autowired
  public LoginAttemptLimiter(
      @Value("${security.login.max-failures-per-account:5}") int maxFailuresPerAccount,
      @Value("${security.login.max-failures-per-client:20}") int maxFailuresPerClient,
      @Value("${security.login.failure-window-seconds:900}") long windowSeconds,
      @Value("${security.login.max-tracked-keys:100000}") int maxTrackedKeys) {
    this(
        maxFailuresPerAccount,
        maxFailuresPerClient,
        Duration.ofSeconds(windowSeconds),
        maxTrackedKeys,
        Clock.systemUTC());
  }

  LoginAttemptLimiter(
      int maxFailuresPerAccount,
      int maxFailuresPerClient,
      Duration window,
      int maxTrackedKeys,
      Clock clock) {
    if (maxFailuresPerAccount < 1
        || maxFailuresPerClient < 1
        || maxTrackedKeys < 2
        || window.isNegative()
        || window.isZero()) {
      throw new IllegalArgumentException("login throttling limits must be positive");
    }
    this.maxFailuresPerAccount = maxFailuresPerAccount;
    this.maxFailuresPerClient = maxFailuresPerClient;
    this.window = window;
    this.maxTrackedKeys = maxTrackedKeys;
    this.clock = clock;
  }

  /**
   * Counts a login attempt if the account and client are under their limits.
   *
   * @return 0 when the attempt is allowed, otherwise the seconds until it will be
   */
  public synchronized long tryAcquire(String email, String client) {
    Instant now = clock.instant();
    String account = accountKey(email);
    String clientKey = clientKey(client);
    long wait =
        Math.max(
            retryAfter(account, maxFailuresPerAccount, now),
            retryAfter(clientKey, maxFailuresPerClient, now));
    if (wait > 0) {
      return wait;
    }
    int newKeys =
        (failures.containsKey(account) ? 0 : 1) + (failures.containsKey(clientKey) ? 0 : 1);
    if (!hasCapacity(newKeys, now)) {
      return secondsUntil(now, nextPruneAt);
    }
    increment(account, now);
    increment(clientKey, now);
    return 0;
  }

  /** Clears the account's window and refunds the client's attempt after a successful login. */
  public synchronized void recordSuccess(String email, String client) {
    failures.remove(accountKey(email));
    failures.computeIfPresent(
        clientKey(client),
        (k, current) ->
            current.count <= 1 ? null : new FailureWindow(current.start, current.count - 1));
  }

  synchronized int trackedKeys() {
    return failures.size();
  }

  private boolean hasCapacity(int newKeys, Instant now) {
    if (failures.size() + newKeys <= maxTrackedKeys) {
      return true;
    }
    if (now.isBefore(nextPruneAt)) {
      return false;
    }
    Instant oldestLiveStart = null;
    for (Iterator<FailureWindow> it = failures.values().iterator(); it.hasNext(); ) {
      FailureWindow w = it.next();
      if (w.isExpired(now, window)) {
        it.remove();
      } else if (oldestLiveStart == null || w.start.isBefore(oldestLiveStart)) {
        oldestLiveStart = w.start;
      }
    }
    nextPruneAt = oldestLiveStart == null ? now : oldestLiveStart.plus(window);
    return failures.size() + newKeys <= maxTrackedKeys;
  }

  private long retryAfter(String key, int limit, Instant now) {
    FailureWindow current = failures.get(key);
    if (current == null || current.isExpired(now, window) || current.count < limit) {
      return 0;
    }
    return secondsUntil(now, current.start.plus(window));
  }

  private static long secondsUntil(Instant now, Instant until) {
    Duration remaining = Duration.between(now, until);
    long seconds = remaining.getSeconds() + (remaining.getNano() > 0 ? 1 : 0);
    return Math.max(1, seconds);
  }

  private void increment(String key, Instant now) {
    failures.compute(
        key,
        (k, current) ->
            current == null || current.isExpired(now, window)
                ? new FailureWindow(now, 1)
                : new FailureWindow(current.start, current.count + 1));
  }

  private static String accountKey(String email) {
    return ACCOUNT_PREFIX + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
  }

  private static String clientKey(String client) {
    return CLIENT_PREFIX + (client == null ? "unknown" : client);
  }

  private static final class FailureWindow {
    private final Instant start;
    private final int count;

    private FailureWindow(Instant start, int count) {
      this.start = start;
      this.count = count;
    }

    private boolean isExpired(Instant now, Duration window) {
      return !now.isBefore(start.plus(window));
    }
  }
}
