package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class ErrorResourceSerializerTest {

  @Test
  public void should_serialize_error_resource_with_single_error() throws JsonProcessingException {
    FieldErrorResource fieldError =
        new FieldErrorResource("object", "email", "NotBlank", "can't be empty");
    ErrorResource errorResource = new ErrorResource(Collections.singletonList(fieldError));

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(errorResource);

    assertNotNull(json);
    assertTrue(json.contains("errors"));
    assertTrue(json.contains("email"));
    assertTrue(json.contains("can't be empty"));
  }

  @Test
  public void should_serialize_error_resource_with_multiple_errors()
      throws JsonProcessingException {
    FieldErrorResource error1 =
        new FieldErrorResource("object", "email", "NotBlank", "can't be empty");
    FieldErrorResource error2 =
        new FieldErrorResource("object", "username", "NotBlank", "can't be empty");
    ErrorResource errorResource = new ErrorResource(Arrays.asList(error1, error2));

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(errorResource);

    assertNotNull(json);
    assertTrue(json.contains("email"));
    assertTrue(json.contains("username"));
  }

  @Test
  public void should_serialize_error_resource_with_multiple_errors_on_same_field()
      throws JsonProcessingException {
    FieldErrorResource error1 =
        new FieldErrorResource("object", "email", "NotBlank", "can't be empty");
    FieldErrorResource error2 =
        new FieldErrorResource("object", "email", "Email", "invalid format");
    ErrorResource errorResource = new ErrorResource(Arrays.asList(error1, error2));

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(errorResource);

    assertNotNull(json);
    assertTrue(json.contains("email"));
    assertTrue(json.contains("can't be empty"));
    assertTrue(json.contains("invalid format"));
  }

  @Test
  public void should_serialize_empty_error_resource() throws JsonProcessingException {
    ErrorResource errorResource = new ErrorResource(Collections.emptyList());

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(errorResource);

    assertNotNull(json);
    assertTrue(json.contains("errors"));
  }
}
