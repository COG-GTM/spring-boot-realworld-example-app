package io.spring.graphql.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuthenticationExceptionTest {

  @Test
  void should_be_a_runtime_exception() {
    AuthenticationException exception = new AuthenticationException();

    assertThat(exception).isInstanceOf(RuntimeException.class);
  }
}
