package io.spring.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidPasswordResetTokenException extends RuntimeException {

  public InvalidPasswordResetTokenException() {
    super("invalid or expired password reset token");
  }
}
