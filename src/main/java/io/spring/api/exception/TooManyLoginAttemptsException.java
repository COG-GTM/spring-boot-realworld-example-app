package io.spring.api.exception;

import lombok.Getter;

@Getter
public class TooManyLoginAttemptsException extends RuntimeException {
  private final long retryAfterSeconds;

  public TooManyLoginAttemptsException(long retryAfterSeconds) {
    super("too many login attempts, try again later");
    this.retryAfterSeconds = retryAfterSeconds;
  }
}
