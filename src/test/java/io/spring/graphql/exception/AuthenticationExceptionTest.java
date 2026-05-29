package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class AuthenticationExceptionTest {

  @Test
  void should_be_runtime_exception() {
    AuthenticationException exception = new AuthenticationException();
    assertTrue(exception instanceof RuntimeException);
  }
}
