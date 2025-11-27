package io.spring.graphql.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class AuthenticationExceptionTest {

  @Test
  void constructor_createsException() {
    AuthenticationException exception = new AuthenticationException();

    assertNotNull(exception);
    assertTrue(exception instanceof RuntimeException);
  }

  @Test
  void exception_canBeThrown() {
    assertThrows(AuthenticationException.class, () -> {
      throw new AuthenticationException();
    });
  }

  @Test
  void exception_hasNoMessage() {
    AuthenticationException exception = new AuthenticationException();

    assertNull(exception.getMessage());
  }

  @Test
  void exception_hasNoCause() {
    AuthenticationException exception = new AuthenticationException();

    assertNull(exception.getCause());
  }

  @Test
  void exception_isRuntimeException() {
    AuthenticationException exception = new AuthenticationException();

    assertTrue(exception instanceof RuntimeException);
    assertFalse(exception instanceof Exception && !(exception instanceof RuntimeException));
  }

  @Test
  void exception_canBeCaughtAsRuntimeException() {
    try {
      throw new AuthenticationException();
    } catch (RuntimeException e) {
      assertTrue(e instanceof AuthenticationException);
    }
  }

  @Test
  void exception_stackTraceIsAvailable() {
    AuthenticationException exception = new AuthenticationException();

    assertNotNull(exception.getStackTrace());
    assertTrue(exception.getStackTrace().length > 0);
  }

  @Test
  void multipleInstances_areIndependent() {
    AuthenticationException exception1 = new AuthenticationException();
    AuthenticationException exception2 = new AuthenticationException();

    assertNotSame(exception1, exception2);
  }
}
