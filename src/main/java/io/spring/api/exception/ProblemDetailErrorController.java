package io.spring.api.exception;

import java.net.URI;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Replaces Spring Boot's default {@code /error} endpoint so that errors raised outside Spring MVC
 * (unmapped routes, servlet container errors) also render as {@code application/problem+json}.
 */
@RestController
@RequestMapping("${server.error.path:${error.path:/error}}")
public class ProblemDetailErrorController implements ErrorController {

  @RequestMapping
  public ResponseEntity<ProblemDetail> error(HttpServletRequest request) {
    HttpStatus status = resolveStatus(request);
    ProblemDetail problem =
        ProblemDetail.forStatus(status)
            .detail(resolveDetail(request, status))
            .instance(resolveInstance(request))
            .build();
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  private static HttpStatus resolveStatus(HttpServletRequest request) {
    Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    if (code instanceof Integer) {
      HttpStatus status = HttpStatus.resolve((Integer) code);
      if (status != null) {
        return status;
      }
    }
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  private static String resolveDetail(HttpServletRequest request, HttpStatus status) {
    if (status.is5xxServerError()) {
      return CustomizeExceptionHandler.INTERNAL_ERROR_DETAIL;
    }
    Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
    return message instanceof String && StringUtils.hasText((String) message)
        ? (String) message
        : null;
  }

  private static URI resolveInstance(HttpServletRequest request) {
    Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    return uri instanceof String ? URI.create((String) uri) : null;
  }
}
