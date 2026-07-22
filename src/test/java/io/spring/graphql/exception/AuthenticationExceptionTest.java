package io.spring.graphql.exception;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class AuthenticationExceptionTest {

  @Test
  public void should_be_a_runtime_exception() {
    AuthenticationException exception = new AuthenticationException();
    assertThat(exception, instanceOf(RuntimeException.class));
  }

  @Test
  public void should_be_throwable() {
    assertThrows(
        AuthenticationException.class,
        () -> {
          throw new AuthenticationException();
        });
  }
}
