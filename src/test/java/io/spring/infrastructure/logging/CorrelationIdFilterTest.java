package io.spring.infrastructure.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class CorrelationIdFilterTest {
  private static final Pattern UUID_PATTERN =
      Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

  private final LoggingProperties.Correlation properties = new LoggingProperties.Correlation();

  @Test
  public void should_generate_id_and_expose_it_in_mdc_and_response() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tags");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> seenInChain = new AtomicReference<>();

    new CorrelationIdFilter(properties)
        .doFilter(
            request,
            response,
            new MockFilterChain(
                new javax.servlet.http.HttpServlet() {},
                (req, res, chain) -> seenInChain.set(CorrelationId.current().orElse(null))));

    String header = response.getHeader("X-Correlation-Id");
    assertNotNull(header);
    assertEquals(true, UUID_PATTERN.matcher(header).matches());
    assertEquals(header, seenInChain.get());
    assertEquals(header, request.getAttribute(CorrelationId.REQUEST_ATTRIBUTE));
    assertNull(MDC.get(CorrelationId.MDC_KEY));
  }

  @Test
  public void should_reuse_valid_incoming_id() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tags");
    request.addHeader("X-Correlation-Id", "upstream-123");
    MockHttpServletResponse response = new MockHttpServletResponse();

    new CorrelationIdFilter(properties).doFilter(request, response, new MockFilterChain());

    assertEquals("upstream-123", response.getHeader("X-Correlation-Id"));
  }

  @Test
  public void should_replace_invalid_incoming_id() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tags");
    request.addHeader("X-Correlation-Id", "bad\nvalue");
    MockHttpServletResponse response = new MockHttpServletResponse();

    new CorrelationIdFilter(properties).doFilter(request, response, new MockFilterChain());

    String header = response.getHeader("X-Correlation-Id");
    assertEquals(true, UUID_PATTERN.matcher(header).matches());
  }

  @Test
  public void should_ignore_incoming_id_when_not_accepted() throws Exception {
    properties.setAcceptIncoming(false);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tags");
    request.addHeader("X-Correlation-Id", "upstream-123");
    MockHttpServletResponse response = new MockHttpServletResponse();

    new CorrelationIdFilter(properties).doFilter(request, response, new MockFilterChain());

    assertNotEquals("upstream-123", response.getHeader("X-Correlation-Id"));
  }
}
