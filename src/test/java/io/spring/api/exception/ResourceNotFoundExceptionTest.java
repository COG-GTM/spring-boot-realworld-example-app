package io.spring.api.exception;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class ResourceNotFoundExceptionTest {

  @Test
  public void should_create_exception() {
    ResourceNotFoundException exception = new ResourceNotFoundException();
    
    assertThat(exception, notNullValue());
  }
}
