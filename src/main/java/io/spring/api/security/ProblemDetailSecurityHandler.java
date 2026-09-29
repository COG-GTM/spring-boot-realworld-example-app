package io.spring.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.api.exception.ProblemDetail;
import io.spring.api.exception.ProblemType;
import java.io.IOException;
import java.net.URI;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Renders Spring Security 401/403 rejections as {@code application/problem+json}. */
public class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
  public static final String AUTHENTICATION_SCHEME = "Token";

  private final ObjectMapper objectMapper;

  public ProblemDetailSecurityHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, AUTHENTICATION_SCHEME);
    write(
        request,
        response,
        ProblemDetail.forType(ProblemType.AUTHENTICATION_REQUIRED)
            .detail("A valid token is required to access this resource"));
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    write(
        request,
        response,
        ProblemDetail.forType(ProblemType.FORBIDDEN)
            .detail("You are not allowed to perform this action"));
  }

  private void write(
      HttpServletRequest request,
      HttpServletResponse response,
      ProblemDetail.ProblemDetailBuilder problem)
      throws IOException {
    ProblemDetail body = problem.instance(URI.create(request.getRequestURI())).build();
    response.setStatus(body.getStatus());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), body);
  }
}
