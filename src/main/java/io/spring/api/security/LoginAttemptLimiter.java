package io.spring.api.security;

import io.spring.api.exception.TooManyLoginAttemptsException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Locks out login attempts per client IP and per account after too many failed attempts within a
 * window.
 *
 * <p>In-flight attempts count towards the limit, so concurrent requests cannot exceed it. Accounts
 * are keyed by the exact submitted email (matching the case-sensitive lookup), whether or not it is
 * registered, so lockout behaviour does not reveal which emails exist. State is held in memory per
 * application instance.
 */
@Component
public class LoginAttemptLimiter {
  private static final int SWEEP_THRESHOLD = 10_000;
  private static final long SWEEP_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(60);

  private enum Outcome {
    SUCCESS,
    FAILURE,
    ABORTED
  }

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
   * Runs {@code credentialCheck} if neither the client IP nor the account is locked, otherwise
   * throws {@link TooManyLoginAttemptsException}. An empty result counts as a failed attempt; a
   * present result clears the account's failures. If the check throws, the attempt is not counted.
   */
  public <T> Optional<T> attempt(
      String clientIp, String email, Supplier<Optional<T>> credentialCheck) {
    String ipKey = ipKey(clientIp);
    String accountKey = accountKey(email);
    begin(ipKey, accountKey);
    Optional<T> result;
    try {
      result = credentialCheck.get();
    } catch (RuntimeException | Error e) {
      finish(ipKey, maxFailuresPerIp, Outcome.ABORTED, false);
      finish(accountKey, maxFailuresPerAccount, Outcome.ABORTED, true);
      throw e;
    }
    Outcome outcome = result.isPresent() ? Outcome.SUCCESS : Outcome.FAILURE;
    finish(ipKey, maxFailuresPerIp, outcome, false);
    finish(accountKey, maxFailuresPerAccount, outcome, true);
    return result;
  }

  private void begin(String ipKey, String accountKey) {
    long now = nanoTime.getAsLong();
    sweepIfNeeded(now);
    acquire(ipKey, maxFailuresPerIp, now);
    try {
      acquire(accountKey, maxFailuresPerAccount, now);
    } catch (TooManyLoginAttemptsException e) {
      finish(ipKey, maxFailuresPerIp, Outcome.ABORTED, false);
      throw e;
    }
  }

  private void acquire(String key, int max, long now) {
    long[] retryAfterNanos = new long[1];
    attempts.compute(
        key,
        (k, existing) -> {
          Attempts current = existing == null ? Attempts.EMPTY : existing.refresh(now);
          if (current.lockedUntil != 0) {
            retryAfterNanos[0] = current.lockedUntil - now;
            return current;
          }
          if (current.failures + current.inFlight >= max) {
            retryAfterNanos[0] = TimeUnit.SECONDS.toNanos(1);
            return current;
          }
          return current.withInFlight(current.inFlight + 1);
        });
    if (retryAfterNanos[0] > 0) {
      throw new TooManyLoginAttemptsException(toSecondsRoundedUp(retryAfterNanos[0]));
    }
  }

  private void finish(String key, int max, Outcome outcome, boolean clearFailuresOnSuccess) {
    long now = nanoTime.getAsLong();
    attempts.computeIfPresent(
        key,
        (k, existing) -> {
          Attempts current = existing.refresh(now).withInFlight(Math.max(0, existing.inFlight - 1));
          if (outcome == Outcome.FAILURE) {
            current = current.withFailure(now, max, windowNanos, lockoutNanos);
          } else if (outcome == Outcome.SUCCESS && clearFailuresOnSuccess) {
            current = current.withoutFailures();
          }
          return current.isIdle() ? null : current;
        });
  }

  private void sweepIfNeeded(long now) {
    long scheduled = nextSweep.get();
    if (attempts.size() > SWEEP_THRESHOLD
        && now - scheduled >= 0
        && nextSweep.compareAndSet(scheduled, now + SWEEP_INTERVAL_NANOS)) {
      attempts.replaceAll((k, a) -> a.refresh(now));
      attempts.values().removeIf(Attempts::isIdle);
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
    private static final Attempts EMPTY = new Attempts(0, 0, 0, 0);

    private final int failures;
    private final int inFlight;
    private final long windowEnd;
    private final long lockedUntil;

    private Attempts(int failures, int inFlight, long windowEnd, long lockedUntil) {
      this.failures = failures;
      this.inFlight = inFlight;
      this.windowEnd = windowEnd;
      this.lockedUntil = lockedUntil;
    }

    private Attempts refresh(long now) {
      boolean lockExpired = lockedUntil != 0 && now - lockedUntil >= 0;
      boolean windowExpired = lockedUntil == 0 && failures > 0 && now - windowEnd >= 0;
      return lockExpired || windowExpired ? new Attempts(0, inFlight, 0, 0) : this;
    }

    private Attempts withInFlight(int inFlight) {
      return new Attempts(failures, inFlight, windowEnd, lockedUntil);
    }

    private Attempts withFailure(long now, int max, long windowNanos, long lockoutNanos) {
      if (lockedUntil != 0) {
        return this;
      }
      int count = failures + 1;
      long end = failures == 0 ? now + windowNanos : windowEnd;
      return new Attempts(count, inFlight, end, count >= max ? now + lockoutNanos : 0);
    }

    private Attempts withoutFailures() {
      return new Attempts(0, inFlight, 0, 0);
    }

    private boolean isIdle() {
      return failures == 0 && inFlight == 0 && lockedUntil == 0;
    }
  }
}
