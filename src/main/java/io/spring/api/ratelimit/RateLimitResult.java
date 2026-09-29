package io.spring.api.ratelimit;

import lombok.Value;

@Value
public class RateLimitResult {
  boolean allowed;
  int limit;
  int remaining;
  long resetAfterSeconds;
}
