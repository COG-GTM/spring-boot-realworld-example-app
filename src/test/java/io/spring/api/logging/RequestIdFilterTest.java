package io.spring.api.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.DispatcherType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
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

  @Test
  public void should_log_uncaught_exception_as_server_error(CapturedOutput output) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/articles");
    request.addHeader(RequestIdFilter.HEADER, "boom-1");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThrows(
        IllegalStateException.class,
        () ->
            filter.doFilter(
                request,
                response,
                (req, res) -> {
                  throw new IllegalStateException("boom");
                }));

    assertTrue(output.getOut().contains("GET /articles -> 500"), output.getOut());
    assertNull(MDC.get("requestId"));
  }

  @Test
  public void should_keep_request_id_on_error_dispatch() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/articles");
    request.addHeader(RequestIdFilter.HEADER, "err-1");
    filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});

    request.setDispatcherType(DispatcherType.ERROR);
    request.removeHeader(RequestIdFilter.HEADER);
    AtomicReference<String> seenInErrorDispatch = new AtomicReference<>();
    filter.doFilter(
        request,
        new MockHttpServletResponse(),
        (req, res) -> seenInErrorDispatch.set(MDC.get("requestId")));

    assertEquals("err-1", seenInErrorDispatch.get());
    assertNull(MDC.get("requestId"));
  }
}
