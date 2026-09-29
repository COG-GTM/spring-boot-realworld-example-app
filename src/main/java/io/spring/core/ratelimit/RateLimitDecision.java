package io.spring.core.ratelimit;

import lombok.Value;

@Value
public class RateLimitDecision {
  private static final RateLimitDecision UNLIMITED = new RateLimitDecision(true, -1, -1, 0);

  boolean allowed;
  int limit;
  int remaining;
  long retryAfterSeconds;

  public static RateLimitDecision unlimited() {
    return UNLIMITED;
  }

  public boolean isLimited() {
    return limit >= 0;
  }
}
