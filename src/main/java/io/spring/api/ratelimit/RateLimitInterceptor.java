package io.spring.api.ratelimit;

import io.spring.core.ratelimit.RateLimitDecision;
import io.spring.core.ratelimit.RateLimitSubject;
import io.spring.core.ratelimit.RateLimiter;
import io.spring.core.user.User;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class RateLimitInterceptor implements HandlerInterceptor {
  private final RateLimiter rateLimiter;

  public RateLimitInterceptor(RateLimiter rateLimiter) {
    this.rateLimiter = rateLimiter;
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
    RateLimitDecision decision =
        rateLimiter.acquireOrThrow(rateLimited.value(), resolveSubject(request));
    if (decision.isLimited()) {
      RateLimitHeaders.from(decision)
          .forEach((name, values) -> response.setHeader(name, values.get(0)));
    }
    return true;
  }

  private String resolveSubject(HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof User) {
      return RateLimitSubject.of((User) authentication.getPrincipal());
    }
    return RateLimitSubject.ofAddress(request.getRemoteAddr());
  }
}
