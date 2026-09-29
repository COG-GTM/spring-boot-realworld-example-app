package io.spring.core.ratelimit;

public interface RateLimiter {
  /** Records an attempt of {@code action} by {@code subject} and reports whether it may proceed. */
  RateLimitDecision tryAcquire(RateLimitAction action, String subject);

  /** Like {@link #tryAcquire} but throws {@link RateLimitExceededException} when rejected. */
  default RateLimitDecision acquireOrThrow(RateLimitAction action, String subject) {
    RateLimitDecision decision = tryAcquire(action, subject);
    if (!decision.isAllowed()) {
      throw new RateLimitExceededException(action, decision);
    }
    return decision;
  }
}
