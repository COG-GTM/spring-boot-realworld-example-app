package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class NoAuthorizationExceptionTest {

  @Test
  public void should_create_exception() {
    NoAuthorizationException exception = new NoAuthorizationException();

    assertNotNull(exception);
  }

  @Test
  public void should_be_runtime_exception() {
    NoAuthorizationException exception = new NoAuthorizationException();

    assertTrue(exception instanceof RuntimeException);
  }

  @Test
  public void should_have_forbidden_response_status_annotation() {
    ResponseStatus annotation = NoAuthorizationException.class.getAnnotation(ResponseStatus.class);

    assertNotNull(annotation);
    assertEquals(HttpStatus.FORBIDDEN, annotation.value());
  }
}
