package io.spring.api.ratelimit;

import io.spring.core.service.JwtService;
import java.io.IOException;
import java.time.Clock;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
  private final RateLimiter rateLimiter;
  private final RateLimitProperties properties;
  private final JwtService jwtService;

  public RateLimitFilter(
      ObjectProvider<RateLimiter> rateLimiter,
      ObjectProvider<RateLimitProperties> properties,
      JwtService jwtService) {
    this.rateLimiter = rateLimiter.getIfAvailable(() -> new RateLimiter(Clock.systemUTC()));
    this.properties = properties.getIfAvailable(RateLimitProperties::new);
    this.jwtService = jwtService;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
      uri = uri.substring(contextPath.length());
    }
    return "/health".equals(uri);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Optional<String> subject =
        getTokenString(request.getHeader("Authorization")).flatMap(jwtService::getSubFromToken);
    String key;
    int limit;
    if (subject.isPresent()) {
      key = "user:" + subject.get();
      limit = properties.getAuthenticatedPerMinute();
    } else {
      key = "ip:" + clientIp(request);
      limit = properties.getUnauthenticatedPerMinute();
    }
    RateLimiter.Result result = rateLimiter.tryAcquire(key, limit);
    if (!result.isAllowed()) {
      response.setStatus(429);
      response.setHeader("Retry-After", String.valueOf(result.getRetryAfterSeconds()));
      response.setContentType("application/json");
      response
          .getWriter()
          .write(
              String.format(
                  "{\"error\":\"rate_limited\",\"retry_after_seconds\":%d}",
                  result.getRetryAfterSeconds()));
      return;
    }
    filterChain.doFilter(request, response);
  }

  private Optional<String> getTokenString(String header) {
    if (header == null) {
      return Optional.empty();
    }
    String[] split = header.split(" ");
    if (split.length < 2) {
      return Optional.empty();
    }
    return Optional.ofNullable(split[1]);
  }

  private String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isEmpty()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
