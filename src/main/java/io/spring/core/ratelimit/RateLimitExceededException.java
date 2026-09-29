package io.spring.core.ratelimit;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {
  private final RateLimitAction action;
  private final RateLimitDecision decision;

  public RateLimitExceededException(RateLimitAction action, RateLimitDecision decision) {
    super("too many requests, retry after " + decision.getRetryAfterSeconds() + " seconds");
    this.action = action;
    this.decision = decision;
  }
}
