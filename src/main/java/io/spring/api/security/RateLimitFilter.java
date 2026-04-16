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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final int MAX_BUCKETS = 10_000;

  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (shouldRateLimit(request)) {
      String clientIp = getClientIp(request);
      evictIfNeeded();
      Bucket bucket = buckets.computeIfAbsent(clientIp, k -> createBucket());
      if (!bucket.tryConsume(1)) {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.getWriter().write("Rate limit exceeded. Try again later.");
        return;
      }
    }
    filterChain.doFilter(request, response);
  }

  private void evictIfNeeded() {
    if (buckets.size() > MAX_BUCKETS) {
      int toRemove = buckets.size() - MAX_BUCKETS + MAX_BUCKETS / 10;
      int removed = 0;
      for (Map.Entry<String, Bucket> entry : buckets.entrySet()) {
        if (removed >= toRemove) {
          break;
        }
        buckets.remove(entry.getKey());
        removed++;
      }
    }
  }

  private boolean shouldRateLimit(HttpServletRequest request) {
    String method = request.getMethod();
    String uri = request.getRequestURI();
    return HttpMethod.POST.matches(method) && ("/users".equals(uri) || "/users/login".equals(uri));
  }

  private String getClientIp(HttpServletRequest request) {
    return request.getRemoteAddr();
  }

  private Bucket createBucket() {
    Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
    return Bucket.builder().addLimit(limit).build();
  }
}
