package io.spring.api.ratelimit;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {
  private final RateLimiter rateLimiter;
  private final RateLimitProperties properties;

  public RateLimitFilter(RateLimiter rateLimiter, RateLimitProperties properties) {
    this.rateLimiter = rateLimiter;
    this.properties = properties;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !properties.isEnabled() || request.getRequestURI().equals(properties.getHealthPath());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String apiKey = apiKey(request);
    RateLimiter.Decision decision =
        apiKey != null
            ? rateLimiter.tryAcquire("key:" + apiKey, properties.getAuthenticatedPerMinute())
            : rateLimiter.tryAcquire("ip:" + clientIp(request), properties.getAnonymousPerMinute());

    if (decision.isAllowed()) {
      filterChain.doFilter(request, response);
      return;
    }

    int retryAfter = decision.getRetryAfterSeconds();
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response
        .getWriter()
        .write("{\"error\":\"rate_limited\",\"retry_after_seconds\":" + retryAfter + "}");
  }

  private String apiKey(HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null) {
      return null;
    }
    String[] split = header.split(" ");
    return split.length < 2 ? null : split[1];
  }

  private String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (properties.isTrustForwardedHeaders() && forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
