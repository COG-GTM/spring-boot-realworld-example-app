package io.spring.api.exception;

import io.spring.api.ratelimit.RateLimitResult;
import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {
  private final RateLimitResult result;

  public RateLimitExceededException(RateLimitResult result) {
    super("too many requests");
    this.result = result;
  }
}
