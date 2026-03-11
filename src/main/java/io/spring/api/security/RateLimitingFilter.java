package io.spring.api.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final int capacity;
  private final int refillTokens;
  private final int refillMinutes;

  public RateLimitingFilter(
      @Value("${rate.limit.login.capacity:10}") int capacity,
      @Value("${rate.limit.login.refill-tokens:10}") int refillTokens,
      @Value("${rate.limit.login.refill-minutes:1}") int refillMinutes) {
    this.capacity = capacity;
    this.refillTokens = refillTokens;
    this.refillMinutes = refillMinutes;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    if ("POST".equalsIgnoreCase(request.getMethod())
        && "/users/login".equals(request.getRequestURI())) {
      String clientIp = getClientIp(request);
      Bucket bucket = buckets.computeIfAbsent(clientIp, k -> createNewBucket());

      if (bucket.tryConsume(1)) {
        filterChain.doFilter(request, response);
      } else {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Too many login attempts. Please try again later.\"}");
      }
    } else {
      filterChain.doFilter(request, response);
    }
  }

  private Bucket createNewBucket() {
    Bandwidth limit =
        Bandwidth.classic(
            capacity, Refill.greedy(refillTokens, Duration.ofMinutes(refillMinutes)));
    return Bucket.builder().addLimit(limit).build();
  }

  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
