package io.spring.api.exception;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class InvalidAuthenticationExceptionTest {

  @Test
  public void should_have_correct_message() {
    InvalidAuthenticationException exception = new InvalidAuthenticationException();
    
    assertThat(exception.getMessage(), is("invalid email or password"));
  }
}
