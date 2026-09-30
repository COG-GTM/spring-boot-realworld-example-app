package io.spring.api.security;

import io.spring.api.exception.TooManyLoginAttemptsException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Locks out login attempts per client IP and per account after too many failures within a window.
 * Accounts are keyed by the submitted email, whether or not it is registered, so lockout behaviour
 * does not reveal which emails exist. State is held in memory per application instance.
 */
@Component
public class LoginAttemptLimiter {
  private static final int SWEEP_THRESHOLD = 10_000;

  private final int maxFailuresPerAccount;
  private final int maxFailuresPerIp;
  private final long windowNanos;
  private final long lockoutNanos;
  private final LongSupplier nanoTime;
  private final ConcurrentHashMap<String, Failures> failures = new ConcurrentHashMap<>();

  @Autowired
  public LoginAttemptLimiter(
      @Value("${security.login.max-failures-per-account:5}") int maxFailuresPerAccount,
      @Value("${security.login.max-failures-per-ip:20}") int maxFailuresPerIp,
      @Value("${security.login.failure-window-seconds:900}") long windowSeconds,
      @Value("${security.login.lockout-seconds:900}") long lockoutSeconds) {
    this(maxFailuresPerAccount, maxFailuresPerIp, windowSeconds, lockoutSeconds, System::nanoTime);
  }

  LoginAttemptLimiter(
      int maxFailuresPerAccount,
      int maxFailuresPerIp,
      long windowSeconds,
      long lockoutSeconds,
      LongSupplier nanoTime) {
    if (maxFailuresPerAccount <= 0 || maxFailuresPerIp <= 0) {
      throw new IllegalArgumentException("login failure limits must be positive");
    }
    if (windowSeconds <= 0 || lockoutSeconds <= 0) {
      throw new IllegalArgumentException("login failure window and lockout must be positive");
    }
    this.maxFailuresPerAccount = maxFailuresPerAccount;
    this.maxFailuresPerIp = maxFailuresPerIp;
    this.windowNanos = TimeUnit.SECONDS.toNanos(windowSeconds);
    this.lockoutNanos = TimeUnit.SECONDS.toNanos(lockoutSeconds);
    this.nanoTime = nanoTime;
  }

  public void checkAllowed(String clientIp, String email) {
    long now = nanoTime.getAsLong();
    long remaining = Math.max(lockedFor(ipKey(clientIp), now), lockedFor(accountKey(email), now));
    if (remaining > 0) {
      throw new TooManyLoginAttemptsException(toSecondsRoundedUp(remaining));
    }
  }

  public void recordFailure(String clientIp, String email) {
    long now = nanoTime.getAsLong();
    sweepIfNeeded(now);
    increment(ipKey(clientIp), maxFailuresPerIp, now);
    increment(accountKey(email), maxFailuresPerAccount, now);
  }

  public void recordSuccess(String email) {
    failures.remove(accountKey(email));
  }

  private long lockedFor(String key, long now) {
    Failures f = failures.get(key);
    if (f == null || f.lockedUntil == 0) {
      return 0;
    }
    return Math.max(0, f.lockedUntil - now);
  }

  private void increment(String key, int max, long now) {
    failures.compute(
        key,
        (k, existing) -> {
          if (existing == null || existing.isExpired(now)) {
            return Failures.first(now, windowNanos, max, lockoutNanos);
          }
          int count = existing.count + 1;
          long lockedUntil = count >= max ? now + lockoutNanos : existing.lockedUntil;
          return new Failures(count, existing.windowEnd, lockedUntil);
        });
  }

  private void sweepIfNeeded(long now) {
    if (failures.size() > SWEEP_THRESHOLD) {
      failures.values().removeIf(f -> f.isExpired(now));
    }
  }

  private static String ipKey(String clientIp) {
    return "ip:" + (clientIp == null ? "unknown" : clientIp);
  }

  private static String accountKey(String email) {
    return "account:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
  }

  private static long toSecondsRoundedUp(long nanos) {
    return Math.max(1, (nanos + TimeUnit.SECONDS.toNanos(1) - 1) / TimeUnit.SECONDS.toNanos(1));
  }

  private static final class Failures {
    private final int count;
    private final long windowEnd;
    private final long lockedUntil;

    private Failures(int count, long windowEnd, long lockedUntil) {
      this.count = count;
      this.windowEnd = windowEnd;
      this.lockedUntil = lockedUntil;
    }

    private static Failures first(long now, long windowNanos, int max, long lockoutNanos) {
      return new Failures(1, now + windowNanos, max <= 1 ? now + lockoutNanos : 0);
    }

    private boolean isExpired(long now) {
      return now - windowEnd >= 0 && (lockedUntil == 0 || now - lockedUntil >= 0);
    }
  }
}
