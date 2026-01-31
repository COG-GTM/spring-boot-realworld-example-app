package io.spring.api.exception;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class NoAuthorizationExceptionTest {

  @Test
  public void should_create_exception() {
    NoAuthorizationException exception = new NoAuthorizationException();
    
    assertThat(exception, notNullValue());
  }
}
