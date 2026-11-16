package io.spring.api.exception;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

public class InvalidRequestExceptionTest {

  private static class NotSerializableTarget {
    private String body;

    public String getBody() {
      return body;
    }
  }

  @Test
  public void should_expose_errors() {
    Errors errors = new BeanPropertyBindingResult(new NotSerializableTarget(), "target");
    errors.rejectValue("body", "NotBlank", "can't be empty");

    InvalidRequestException exception = new InvalidRequestException(errors);

    Assertions.assertSame(errors, exception.getErrors());
    Assertions.assertEquals("", exception.getMessage());
  }

  @Test
  public void should_serialize_without_errors_when_errors_target_is_not_serializable()
      throws Exception {
    Errors errors = new BeanPropertyBindingResult(new NotSerializableTarget(), "target");
    errors.rejectValue("body", "NotBlank", "can't be empty");
    InvalidRequestException exception = new InvalidRequestException(errors);

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(exception);
    }

    InvalidRequestException restored;
    try (ObjectInputStream in =
        new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      restored = (InvalidRequestException) in.readObject();
    }

    Assertions.assertEquals("", restored.getMessage());
    Assertions.assertNull(restored.getErrors());
  }
}
