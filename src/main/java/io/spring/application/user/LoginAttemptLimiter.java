package io.spring.application.user;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Counts failed logins per account (email) and per client (IP) in a fixed window and blocks further
 * attempts once either limit is reached. Unknown emails are tracked exactly like registered ones so
 * throttling never reveals whether an account exists.
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
  private final ConcurrentMap<String, FailureWindow> failures = new ConcurrentHashMap<>();

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
    if (maxFailuresPerAccount < 1 || maxFailuresPerClient < 1 || window.isNegative()) {
      throw new IllegalArgumentException("login throttling limits must be positive");
    }
    this.maxFailuresPerAccount = maxFailuresPerAccount;
    this.maxFailuresPerClient = maxFailuresPerClient;
    this.window = window;
    this.maxTrackedKeys = maxTrackedKeys;
    this.clock = clock;
  }

  /** Returns 0 when a login attempt is allowed, otherwise the seconds until it will be. */
  public long retryAfterSeconds(String email, String client) {
    Instant now = clock.instant();
    return Math.max(
        retryAfter(accountKey(email), maxFailuresPerAccount, now),
        retryAfter(clientKey(client), maxFailuresPerClient, now));
  }

  public void recordFailure(String email, String client) {
    Instant now = clock.instant();
    if (failures.size() >= maxTrackedKeys) {
      failures.values().removeIf(w -> w.isExpired(now, window));
    }
    increment(accountKey(email), now);
    increment(clientKey(client), now);
  }

  public void recordSuccess(String email) {
    failures.remove(accountKey(email));
  }

  int trackedKeys() {
    return failures.size();
  }

  private long retryAfter(String key, int limit, Instant now) {
    FailureWindow current = failures.get(key);
    if (current == null || current.isExpired(now, window) || current.count < limit) {
      return 0;
    }
    long seconds = Duration.between(now, current.start.plus(window)).getSeconds();
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
