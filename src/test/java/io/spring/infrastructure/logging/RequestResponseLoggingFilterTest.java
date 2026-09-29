package io.spring.infrastructure.logging;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.StreamUtils;

@ExtendWith(OutputCaptureExtension.class)
public class RequestResponseLoggingFilterTest {

  private static final HttpServlet ECHO =
      new HttpServlet() {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws java.io.IOException {
          String body = StreamUtils.copyToString(req.getInputStream(), StandardCharsets.UTF_8);
          resp.setStatus(201);
          resp.setContentType("application/json");
          resp.setHeader("Set-Cookie", "session=secret");
          resp.getWriter().write(body.replace("}}", ",\"token\":\"jwt-value\"}}"));
        }
      };

  private MockHttpServletRequest loginRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/users/login");
    request.setContentType("application/json");
    request.addHeader("Authorization", "Token abc");
    request.setContent(
        "{\"user\":{\"email\":\"a@b.c\",\"password\":\"hunter2\"}}"
            .getBytes(StandardCharsets.UTF_8));
    return request;
  }

  @Test
  public void should_log_summary_only_by_default(CapturedOutput output) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RequestResponseLoggingFilter(new LoggingProperties.Http())
        .doFilter(loginRequest(), response, new MockFilterChain(ECHO));

    assertThat(output.getOut(), containsString("POST /users/login -> 201 ("));
    assertThat(output.getOut(), not(containsString("requestBody")));
    assertThat(output.getOut(), not(containsString("requestHeaders")));
  }

  @Test
  public void should_log_masked_headers_and_payloads_when_enabled(CapturedOutput output)
      throws Exception {
    LoggingProperties.Http properties = new LoggingProperties.Http();
    properties.setIncludeHeaders(true);
    properties.setIncludePayload(true);
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RequestResponseLoggingFilter(properties)
        .doFilter(loginRequest(), response, new MockFilterChain(ECHO));

    String out = output.getOut();
    assertThat(out, containsString("Authorization=***"));
    assertThat(out, containsString("Set-Cookie=***"));
    assertThat(out, containsString("\"email\":\"a@b.c\""));
    assertThat(out, containsString("\"password\":\"***\""));
    assertThat(out, containsString("\"token\":\"***\""));
    assertThat(out, not(containsString("hunter2")));
    assertThat(out, not(containsString("jwt-value")));
    assertThat(out, not(containsString("Token abc")));
    assertThat(response.getContentAsString(), containsString("jwt-value"));
  }

  @Test
  public void should_truncate_long_payloads(CapturedOutput output) throws Exception {
    LoggingProperties.Http properties = new LoggingProperties.Http();
    properties.setIncludePayload(true);
    properties.setMaxPayloadLength(10);
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RequestResponseLoggingFilter(properties)
        .doFilter(loginRequest(), response, new MockFilterChain(ECHO));

    assertThat(output.getOut(), containsString("...[truncated"));
    assertThat(output.getOut(), not(containsString("hunter2")));
  }

  @Test
  public void should_skip_excluded_paths(CapturedOutput output) throws Exception {
    LoggingProperties.Http properties = new LoggingProperties.Http();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/graphiql/index.html");
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RequestResponseLoggingFilter(properties).doFilter(request, response, new MockFilterChain());

    assertThat(output.getOut(), not(containsString("/graphiql")));
  }

  @Test
  public void should_mask_configured_fields() {
    RequestResponseLoggingFilter filter =
        new RequestResponseLoggingFilter(new LoggingProperties.Http());

    assertEquals(
        "{\"Password\": \"***\",\"name\":\"x\"}",
        filter.maskFields("{\"Password\": \"p\\\"w\",\"name\":\"x\"}"));
  }
}
