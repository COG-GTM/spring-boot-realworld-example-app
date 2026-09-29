package io.spring.api.ratelimit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class RateLimitInterceptor implements HandlerInterceptor {
  public static final String LIMIT_HEADER = "X-RateLimit-Limit";
  public static final String REMAINING_HEADER = "X-RateLimit-Remaining";
  public static final String RESET_HEADER = "X-RateLimit-Reset";

  private final RateLimitService rateLimitService;

  public RateLimitInterceptor(RateLimitService rateLimitService) {
    this.rateLimitService = rateLimitService;
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
    rateLimitService
        .acquire(rateLimited.value(), request)
        .ifPresent(result -> writeHeaders(response, result));
    return true;
  }

  public static void writeHeaders(HttpServletResponse response, RateLimitResult result) {
    response.setHeader(LIMIT_HEADER, String.valueOf(result.getLimit()));
    response.setHeader(REMAINING_HEADER, String.valueOf(result.getRemaining()));
    response.setHeader(RESET_HEADER, String.valueOf(result.getResetAfterSeconds()));
  }
}
