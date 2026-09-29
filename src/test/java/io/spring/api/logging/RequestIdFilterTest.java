package io.spring.api.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class RequestIdFilterTest {
  private final RequestIdFilter filter = new RequestIdFilter();

  @Test
  public void should_propagate_inbound_request_id_to_mdc_and_response() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tags");
    request.addHeader(RequestIdFilter.HEADER, "abc-123");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> seenInChain = new AtomicReference<>();

    filter.doFilter(request, response, (req, res) -> seenInChain.set(MDC.get("requestId")));

    assertEquals("abc-123", seenInChain.get());
    assertEquals("abc-123", response.getHeader(RequestIdFilter.HEADER));
    assertNull(MDC.get("requestId"));
  }

  @Test
  public void should_generate_request_id_when_header_missing() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tags");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> seenInChain = new AtomicReference<>();

    filter.doFilter(request, response, (req, res) -> seenInChain.set(MDC.get("requestId")));

    assertNotNull(seenInChain.get());
    assertEquals(seenInChain.get(), response.getHeader(RequestIdFilter.HEADER));
    assertNull(MDC.get("requestId"));
  }

  @Test
  public void should_replace_malformed_request_id() {
    String malicious = "bad\r\ninjected-log-line";
    assertNotEquals(malicious, RequestIdFilter.resolveRequestId(malicious));
    assertNotEquals("", RequestIdFilter.resolveRequestId(""));
    String tooLong = new String(new char[129]).replace('\0', 'a');
    assertNotEquals(tooLong, RequestIdFilter.resolveRequestId(tooLong));
  }
}
