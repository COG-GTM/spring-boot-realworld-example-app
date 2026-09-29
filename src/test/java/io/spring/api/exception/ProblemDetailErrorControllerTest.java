package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import javax.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

public class ProblemDetailErrorControllerTest {
  private final ProblemDetailErrorController controller = new ProblemDetailErrorController();

  @Test
  public void should_render_not_found_as_problem_detail() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
    request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
    request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/no-such-route");

    ResponseEntity<ProblemDetail> response = controller.error(request);

    assertEquals(404, response.getStatusCodeValue());
    assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
    ProblemDetail body = response.getBody();
    assertEquals(ProblemDetail.BLANK_TYPE, body.getType());
    assertEquals("Not Found", body.getTitle());
    assertEquals(404, body.getStatus());
    assertNull(body.getDetail());
    assertEquals(URI.create("/no-such-route"), body.getInstance());
  }

  @Test
  public void should_hide_server_error_message() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
    request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
    request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "NullPointerException at Foo.java:42");

    ResponseEntity<ProblemDetail> response = controller.error(request);

    assertEquals(500, response.getStatusCodeValue());
    assertEquals("An unexpected error occurred", response.getBody().getDetail());
  }

  @Test
  public void should_default_to_internal_server_error_without_status() {
    ResponseEntity<ProblemDetail> response =
        controller.error(new MockHttpServletRequest("GET", "/error"));

    assertEquals(500, response.getStatusCodeValue());
    assertEquals("Internal Server Error", response.getBody().getTitle());
  }
}
