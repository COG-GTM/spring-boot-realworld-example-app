package io.spring.api.logging;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns a correlation ID to every request, exposes it in the logging MDC under {@link #MDC_KEY}
 * and echoes it back in the {@link #HEADER} response header. A well-formed inbound {@link #HEADER}
 * is reused so IDs can be traced across services; otherwise a new UUID is generated.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
  public static final String HEADER = "X-Request-Id";
  public static final String MDC_KEY = "requestId";
  static final String ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";

  private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
  private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

  @Override
  protected boolean shouldNotFilterErrorDispatch() {
    return false;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Object existingId = request.getAttribute(ATTRIBUTE);
    if (existingId != null) {
      MDC.put(MDC_KEY, existingId.toString());
      try {
        filterChain.doFilter(request, response);
      } finally {
        MDC.remove(MDC_KEY);
      }
      return;
    }

    String requestId = resolveRequestId(request.getHeader(HEADER));
    long start = System.nanoTime();
    request.setAttribute(ATTRIBUTE, requestId);
    MDC.put(MDC_KEY, requestId);
    response.setHeader(HEADER, requestId);
    boolean failed = true;
    try {
      filterChain.doFilter(request, response);
      failed = false;
    } finally {
      log.info(
          "{} {} -> {} ({} ms)",
          request.getMethod(),
          request.getRequestURI(),
          failed ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : response.getStatus(),
          (System.nanoTime() - start) / 1_000_000);
      MDC.remove(MDC_KEY);
    }
  }

  static String resolveRequestId(String candidate) {
    if (candidate != null && VALID_ID.matcher(candidate).matches()) {
      return candidate;
    }
    return UUID.randomUUID().toString();
  }
}
