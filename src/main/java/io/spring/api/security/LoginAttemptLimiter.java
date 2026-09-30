package io.spring.api.security;

import io.spring.api.exception.TooManyLoginAttemptsException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Locks out login attempts per client IP and per account after too many failures within a window.
 *
 * <p>Each attempt is counted atomically in {@link #beginAttempt} before credentials are checked, so
 * concurrent requests cannot exceed the limit; {@link #recordSuccess} gives the attempt back.
 * Accounts are keyed by the exact submitted email (matching the case-sensitive lookup), whether or
 * not it is registered, so lockout behaviour does not reveal which emails exist. State is held in
 * memory per application instance.
 */
@Component
public class LoginAttemptLimiter {
  private static final int SWEEP_THRESHOLD = 10_000;
  private static final long SWEEP_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(60);

  private final int maxFailuresPerAccount;
  private final int maxFailuresPerIp;
  private final long windowNanos;
  private final long lockoutNanos;
  private final LongSupplier nanoTime;
  private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();
  private final AtomicLong nextSweep;

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
    this.nextSweep = new AtomicLong(nanoTime.getAsLong());
  }

  /**
   * Counts a login attempt against the client IP and the account. Throws {@link
   * TooManyLoginAttemptsException} without counting if either is locked. The attempt stays counted
   * as a failure unless {@link #recordSuccess} is called.
   */
  public void beginAttempt(String clientIp, String email) {
    long now = nanoTime.getAsLong();
    sweepIfNeeded(now);
    String ipKey = ipKey(clientIp);
    acquire(ipKey, maxFailuresPerIp, now);
    try {
      acquire(accountKey(email), maxFailuresPerAccount, now);
    } catch (TooManyLoginAttemptsException e) {
      release(ipKey, maxFailuresPerIp);
      throw e;
    }
  }

  public void recordSuccess(String clientIp, String email) {
    release(ipKey(clientIp), maxFailuresPerIp);
    attempts.remove(accountKey(email));
  }

  private void acquire(String key, int max, long now) {
    long[] lockedFor = new long[1];
    attempts.compute(
        key,
        (k, existing) -> {
          if (existing == null || existing.isExpired(now)) {
            return new Attempts(1, now + windowNanos, 1 >= max ? now + lockoutNanos : 0);
          }
          if (existing.lockedUntil != 0) {
            lockedFor[0] = existing.lockedUntil - now;
            return existing;
          }
          int count = existing.count + 1;
          return new Attempts(count, existing.windowEnd, count >= max ? now + lockoutNanos : 0);
        });
    if (lockedFor[0] > 0) {
      throw new TooManyLoginAttemptsException(toSecondsRoundedUp(lockedFor[0]));
    }
  }

  private void release(String key, int max) {
    attempts.computeIfPresent(
        key,
        (k, existing) -> {
          int count = existing.count - 1;
          if (count <= 0) {
            return null;
          }
          return new Attempts(count, existing.windowEnd, count >= max ? existing.lockedUntil : 0);
        });
  }

  private void sweepIfNeeded(long now) {
    long scheduled = nextSweep.get();
    if (attempts.size() > SWEEP_THRESHOLD
        && now - scheduled >= 0
        && nextSweep.compareAndSet(scheduled, now + SWEEP_INTERVAL_NANOS)) {
      attempts.values().removeIf(a -> a.isExpired(now));
    }
  }

  private static String ipKey(String clientIp) {
    return "ip:" + (clientIp == null ? "unknown" : clientIp);
  }

  private static String accountKey(String email) {
    return "account:" + (email == null ? "" : email);
  }

  private static long toSecondsRoundedUp(long nanos) {
    return Math.max(1, (nanos + TimeUnit.SECONDS.toNanos(1) - 1) / TimeUnit.SECONDS.toNanos(1));
  }

  private static final class Attempts {
    private final int count;
    private final long windowEnd;
    private final long lockedUntil;

    private Attempts(int count, long windowEnd, long lockedUntil) {
      this.count = count;
      this.windowEnd = windowEnd;
      this.lockedUntil = lockedUntil;
    }

    private boolean isExpired(long now) {
      return lockedUntil != 0 ? now - lockedUntil >= 0 : now - windowEnd >= 0;
    }
  }
}
