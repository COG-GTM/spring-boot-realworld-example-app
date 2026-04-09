package io.spring.api.exception;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class CustomizeExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(CustomizeExceptionHandler.class);

  @ExceptionHandler({InvalidRequestException.class})
  public ResponseEntity<Object> handleInvalidRequest(RuntimeException e, WebRequest request) {
    InvalidRequestException ire = (InvalidRequestException) e;

    List<FieldErrorResource> errorResources =
        ire.getErrors().getFieldErrors().stream()
            .map(
                fieldError ->
                    new FieldErrorResource(
                        fieldError.getObjectName(),
                        fieldError.getField(),
                        fieldError.getCode(),
                        fieldError.getDefaultMessage()))
            .collect(Collectors.toList());

    ErrorResource error = new ErrorResource(errorResources);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    return handleExceptionInternal(e, error, headers, UNPROCESSABLE_ENTITY, request);
  }

  @ExceptionHandler(InvalidAuthenticationException.class)
  public ResponseEntity<Object> handleInvalidAuthentication(
      InvalidAuthenticationException e, WebRequest request) {
    return ResponseEntity.status(UNPROCESSABLE_ENTITY)
        .body(
            new HashMap<String, Object>() {
              {
                put("message", e.getMessage());
              }
            });
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException e,
      HttpHeaders headers,
      HttpStatus status,
      WebRequest request) {
    List<FieldErrorResource> errorResources =
        e.getBindingResult().getFieldErrors().stream()
            .map(
                fieldError ->
                    new FieldErrorResource(
                        fieldError.getObjectName(),
                        fieldError.getField(),
                        fieldError.getCode(),
                        fieldError.getDefaultMessage()))
            .collect(Collectors.toList());

    return ResponseEntity.status(UNPROCESSABLE_ENTITY).body(new ErrorResource(errorResources));
  }

  @ExceptionHandler({ConstraintViolationException.class})
  @ResponseStatus(UNPROCESSABLE_ENTITY)
  @ResponseBody
  public ErrorResource handleConstraintViolation(
      ConstraintViolationException ex, WebRequest request) {
    List<FieldErrorResource> errors = new ArrayList<>();
    for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
      FieldErrorResource fieldErrorResource =
          new FieldErrorResource(
              violation.getRootBeanClass().getName(),
              getParam(violation.getPropertyPath().toString()),
              violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
              violation.getMessage());
      errors.add(fieldErrorResource);
    }

    return new ErrorResource(errors);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Object> handleResourceNotFound(
      ResourceNotFoundException ex, WebRequest request) {
    HashMap<String, Object> body = new HashMap<>();
    body.put(
        "errors",
        new HashMap<String, Object>() {
          {
            put("resource", Arrays.asList("not found"));
          }
        });
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  @ExceptionHandler(NoAuthorizationException.class)
  public ResponseEntity<Object> handleNoAuthorization(
      NoAuthorizationException ex, WebRequest request) {
    HashMap<String, Object> body = new HashMap<>();
    body.put(
        "errors",
        new HashMap<String, Object>() {
          {
            put("authorization", Arrays.asList("forbidden"));
          }
        });
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatus status,
      WebRequest request) {
    HashMap<String, Object> body = new HashMap<>();
    body.put(
        "errors",
        new HashMap<String, Object>() {
          {
            put("body", Arrays.asList("malformed request body"));
          }
        });
    return ResponseEntity.status(UNPROCESSABLE_ENTITY).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleGenericException(Exception ex, WebRequest request) {
    logger.error("Unhandled exception", ex);
    HashMap<String, Object> body = new HashMap<>();
    body.put(
        "errors",
        new HashMap<String, Object>() {
          {
            put("server", Arrays.asList("internal server error"));
          }
        });
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  private String getParam(String s) {
    String[] splits = s.split("\\.");
    if (splits.length <= 2) {
      return splits[splits.length - 1];
    } else {
      return String.join(".", Arrays.copyOfRange(splits, 2, splits.length));
    }
  }
}
