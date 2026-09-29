package io.spring.api.ratelimit;

import io.spring.api.exception.RateLimitExceededException;
import io.spring.core.user.User;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Applies a {@link RateLimitPolicy} to a request; shared by the REST and GraphQL entry points. */
public class RateLimitService {
  private final boolean enabled;
  private final Map<RateLimitPolicy, FixedWindowRateLimiter> limiters =
      new EnumMap<>(RateLimitPolicy.class);

  public RateLimitService(RateLimitProperties properties, LongSupplier nanoTime) {
    this.enabled = properties.isEnabled();
    for (RateLimitPolicy policy : RateLimitPolicy.values()) {
      RateLimitProperties.Limit limit = properties.forPolicy(policy);
      limiters.put(
          policy, new FixedWindowRateLimiter(limit.getLimit(), limit.getWindow(), nanoTime));
    }
  }

  /**
   * Consumes one unit of quota for the caller.
   *
   * @return the post-acquire quota state, or empty when rate limiting is disabled
   * @throws RateLimitExceededException when the caller has exhausted the policy's quota
   */
  public Optional<RateLimitResult> acquire(RateLimitPolicy policy, HttpServletRequest request) {
    if (!enabled) {
      return Optional.empty();
    }
    RateLimitResult result = limiters.get(policy).tryAcquire(clientKey(policy, request));
    if (!result.isAllowed()) {
      throw new RateLimitExceededException(result);
    }
    return Optional.of(result);
  }

  private static String clientKey(RateLimitPolicy policy, HttpServletRequest request) {
    if (policy == RateLimitPolicy.ARTICLE_CREATION) {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.getPrincipal() instanceof User) {
        return "user:" + ((User) authentication.getPrincipal()).getId();
      }
    }
    return "ip:" + (request == null ? "unknown" : request.getRemoteAddr());
  }
}
