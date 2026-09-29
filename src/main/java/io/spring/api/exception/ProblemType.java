package io.spring.api.exception;

import java.net.URI;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ProblemType {
  VALIDATION_FAILED("validation-failed", "Validation Failed", HttpStatus.UNPROCESSABLE_ENTITY),
  INVALID_CREDENTIALS("invalid-credentials", "Invalid Credentials", HttpStatus.UNAUTHORIZED),
  AUTHENTICATION_REQUIRED(
      "authentication-required", "Authentication Required", HttpStatus.UNAUTHORIZED),
  FORBIDDEN("forbidden", "Forbidden", HttpStatus.FORBIDDEN),
  RESOURCE_NOT_FOUND("resource-not-found", "Resource Not Found", HttpStatus.NOT_FOUND);

  private static final String TYPE_PREFIX = "urn:problem-type:realworld:";

  private final URI type;
  private final String title;
  private final HttpStatus status;

  ProblemType(String slug, String title, HttpStatus status) {
    this.type = URI.create(TYPE_PREFIX + slug);
    this.title = title;
    this.status = status;
  }
}
