package io.spring.api.ratelimit;

import io.spring.core.ratelimit.RateLimitDecision;
import org.springframework.http.HttpHeaders;

public final class RateLimitHeaders {
  public static final String LIMIT = "X-RateLimit-Limit";
  public static final String REMAINING = "X-RateLimit-Remaining";

  private RateLimitHeaders() {}

  public static HttpHeaders from(RateLimitDecision decision) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(LIMIT, String.valueOf(decision.getLimit()));
    headers.set(REMAINING, String.valueOf(decision.getRemaining()));
    if (!decision.isAllowed()) {
      headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(decision.getRetryAfterSeconds()));
    }
    return headers;
  }
}
