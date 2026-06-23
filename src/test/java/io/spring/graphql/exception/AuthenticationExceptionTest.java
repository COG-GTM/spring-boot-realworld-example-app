package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuthenticationExceptionTest {

  @Test
  void is_a_runtime_exception() {
    AuthenticationException exception = new AuthenticationException();
    assertTrue(exception instanceof RuntimeException);
  }

  @Test
  void can_be_thrown() {
    assertThrows(
        AuthenticationException.class,
        () -> {
          throw new AuthenticationException();
        });
  }
}
