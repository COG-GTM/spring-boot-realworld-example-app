package io.spring.api.exception;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import io.spring.api.security.ProblemDetailSecurityHandler;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class CustomizeExceptionHandler extends ResponseEntityExceptionHandler {

  static final String INTERNAL_ERROR_DETAIL = "An unexpected error occurred";

  @ExceptionHandler(InvalidRequestException.class)
  public ResponseEntity<Object> handleInvalidRequest(
      InvalidRequestException e, WebRequest request) {
    return validationProblem(e, e.getErrors().getFieldErrors().stream(), request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> handleConstraintViolation(
      ConstraintViolationException e, WebRequest request) {
    Map<String, List<String>> errors =
        e.getConstraintViolations().stream()
            .collect(
                Collectors.groupingBy(
                    violation -> getParam(violation.getPropertyPath().toString()),
                    LinkedHashMap::new,
                    Collectors.mapping(ConstraintViolation::getMessage, Collectors.toList())));
    return problem(e, ProblemDetail.forType(ProblemType.VALIDATION_FAILED).errors(errors), request);
  }

  @ExceptionHandler(InvalidAuthenticationException.class)
  public ResponseEntity<Object> handleInvalidAuthentication(
      InvalidAuthenticationException e, WebRequest request) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.WWW_AUTHENTICATE, ProblemDetailSecurityHandler.AUTHENTICATION_SCHEME);
    return problem(
        e,
        ProblemDetail.forType(ProblemType.INVALID_CREDENTIALS).detail(e.getMessage()),
        headers,
        request);
  }

  @ExceptionHandler(NoAuthorizationException.class)
  public ResponseEntity<Object> handleNoAuthorization(
      NoAuthorizationException e, WebRequest request) {
    return problem(e, ProblemDetail.forType(ProblemType.FORBIDDEN).detail(e.getMessage()), request);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Object> handleResourceNotFound(
      ResourceNotFoundException e, WebRequest request) {
    return problem(
        e, ProblemDetail.forType(ProblemType.RESOURCE_NOT_FOUND).detail(e.getMessage()), request);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Object> handleResponseStatus(
      ResponseStatusException e, WebRequest request) {
    HttpHeaders headers = new HttpHeaders();
    headers.putAll(e.getResponseHeaders());
    return handleExceptionInternal(e, null, headers, e.getStatus(), request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUnexpected(Exception e, WebRequest request) {
    log.error("Unhandled exception while processing {}", describe(request), e);
    return handleExceptionInternal(e, null, new HttpHeaders(), INTERNAL_SERVER_ERROR, request);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException e,
      HttpHeaders headers,
      HttpStatus status,
      WebRequest request) {
    return validationProblem(e, e.getBindingResult().getFieldErrors().stream(), request);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException e,
      HttpHeaders headers,
      HttpStatus status,
      WebRequest request) {
    ProblemDetail body =
        ProblemDetail.forStatus(status)
            .detail("Request body is missing or malformed")
            .instance(instance(request))
            .build();
    return handleExceptionInternal(e, body, headers, status, request);
  }

  /**
   * Every Spring MVC error handled by {@link ResponseEntityExceptionHandler} funnels through here,
   * so this is where bodies default to a problem detail and the problem media type is applied.
   */
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception e, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
    HttpHeaders problemHeaders = new HttpHeaders();
    problemHeaders.putAll(headers);
    problemHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
    Object problemBody = body != null ? body : defaultProblem(e, status, request);
    return super.handleExceptionInternal(e, problemBody, problemHeaders, status, request);
  }

  private ProblemDetail defaultProblem(Exception e, HttpStatus status, WebRequest request) {
    String detail = status.is5xxServerError() ? INTERNAL_ERROR_DETAIL : reason(e);
    return ProblemDetail.forStatus(status).detail(detail).instance(instance(request)).build();
  }

  private ResponseEntity<Object> validationProblem(
      Exception e, Stream<FieldError> fieldErrors, WebRequest request) {
    Map<String, List<String>> errors =
        fieldErrors.collect(
            Collectors.groupingBy(
                FieldError::getField,
                LinkedHashMap::new,
                Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())));
    return problem(e, ProblemDetail.forType(ProblemType.VALIDATION_FAILED).errors(errors), request);
  }

  private ResponseEntity<Object> problem(
      Exception e, ProblemDetail.ProblemDetailBuilder problem, WebRequest request) {
    return problem(e, problem, new HttpHeaders(), request);
  }

  private ResponseEntity<Object> problem(
      Exception e,
      ProblemDetail.ProblemDetailBuilder problem,
      HttpHeaders headers,
      WebRequest request) {
    ProblemDetail body = problem.instance(instance(request)).build();
    return handleExceptionInternal(e, body, headers, HttpStatus.valueOf(body.getStatus()), request);
  }

  private static String reason(Exception e) {
    if (e instanceof ResponseStatusException) {
      return ((ResponseStatusException) e).getReason();
    }
    return e.getMessage();
  }

  private static URI instance(WebRequest request) {
    if (request instanceof ServletWebRequest) {
      return URI.create(((ServletWebRequest) request).getRequest().getRequestURI());
    }
    return null;
  }

  private static String describe(WebRequest request) {
    return request.getDescription(false);
  }

  private static String getParam(String s) {
    String[] splits = s.split("\\.");
    if (splits.length == 1) {
      return s;
    } else {
      return String.join(".", Arrays.copyOfRange(splits, 2, splits.length));
    }
  }
}
