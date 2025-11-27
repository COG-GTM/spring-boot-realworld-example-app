package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class ResourceNotFoundExceptionTest {

  @Test
  public void should_create_exception() {
    ResourceNotFoundException exception = new ResourceNotFoundException();

    assertNotNull(exception);
  }

  @Test
  public void should_be_runtime_exception() {
    ResourceNotFoundException exception = new ResourceNotFoundException();

    assertTrue(exception instanceof RuntimeException);
  }

  @Test
  public void should_have_not_found_response_status_annotation() {
    ResponseStatus annotation = ResourceNotFoundException.class.getAnnotation(ResponseStatus.class);

    assertNotNull(annotation);
    assertEquals(HttpStatus.NOT_FOUND, annotation.value());
  }
}
