package io.spring.api.ratelimit;

import io.spring.api.exception.RateLimitExceededException;
import io.spring.core.user.User;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class RateLimitInterceptor implements HandlerInterceptor {
  public static final String LIMIT_HEADER = "X-RateLimit-Limit";
  public static final String REMAINING_HEADER = "X-RateLimit-Remaining";
  public static final String RESET_HEADER = "X-RateLimit-Reset";

  private final Map<RateLimitPolicy, FixedWindowRateLimiter> limiters;

  public RateLimitInterceptor(Map<RateLimitPolicy, FixedWindowRateLimiter> limiters) {
    this.limiters = limiters;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!(handler instanceof HandlerMethod)) {
      return true;
    }
    RateLimited rateLimited = ((HandlerMethod) handler).getMethodAnnotation(RateLimited.class);
    if (rateLimited == null) {
      return true;
    }
    RateLimitPolicy policy = rateLimited.value();
    RateLimitResult result = limiters.get(policy).tryAcquire(clientKey(policy, request));
    if (!result.isAllowed()) {
      throw new RateLimitExceededException(result);
    }
    writeHeaders(response, result);
    return true;
  }

  public static void writeHeaders(HttpServletResponse response, RateLimitResult result) {
    response.setHeader(LIMIT_HEADER, String.valueOf(result.getLimit()));
    response.setHeader(REMAINING_HEADER, String.valueOf(result.getRemaining()));
    response.setHeader(RESET_HEADER, String.valueOf(result.getResetAfterSeconds()));
  }

  private String clientKey(RateLimitPolicy policy, HttpServletRequest request) {
    if (policy == RateLimitPolicy.ARTICLE_CREATION) {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.getPrincipal() instanceof User) {
        return "user:" + ((User) authentication.getPrincipal()).getId();
      }
    }
    return "ip:" + request.getRemoteAddr();
  }
}
