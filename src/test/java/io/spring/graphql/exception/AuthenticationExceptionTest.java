package io.spring.graphql.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

public class AuthenticationExceptionTest {

  @Test
  public void should_be_a_runtime_exception_without_message() {
    AuthenticationException exception = new AuthenticationException();

    assertThat(exception).isInstanceOf(RuntimeException.class);
    assertThat(exception.getMessage()).isNull();
    assertThat(exception.getCause()).isNull();
  }

  @Test
  public void should_be_throwable() {
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(
            () -> {
              throw new AuthenticationException();
            });
  }
}
