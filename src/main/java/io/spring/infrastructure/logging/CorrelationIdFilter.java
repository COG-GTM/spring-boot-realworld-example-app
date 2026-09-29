package io.spring.infrastructure.logging;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class CorrelationIdFilter extends OncePerRequestFilter {
  private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9._:-]{1,64}$");

  private final String headerName;
  private final boolean acceptIncoming;

  public CorrelationIdFilter(LoggingProperties.Correlation properties) {
    this.headerName = properties.getHeaderName();
    this.acceptIncoming = properties.isAcceptIncoming();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String correlationId = resolve(request.getHeader(headerName));
    MDC.put(CorrelationId.MDC_KEY, correlationId);
    request.setAttribute(CorrelationId.REQUEST_ATTRIBUTE, correlationId);
    response.setHeader(headerName, correlationId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(CorrelationId.MDC_KEY);
    }
  }

  private String resolve(String incoming) {
    if (acceptIncoming && incoming != null && VALID_ID.matcher(incoming).matches()) {
      return incoming;
    }
    return UUID.randomUUID().toString();
  }
}
