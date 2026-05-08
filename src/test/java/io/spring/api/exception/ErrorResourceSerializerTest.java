package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class ErrorResourceSerializerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void should_serialize_to_errors_envelope_with_field_arrays() throws Exception {
    ErrorResource error =
        new ErrorResource(
            Arrays.asList(
                new FieldErrorResource("Resource", "title", "NotBlank", "can't be empty"),
                new FieldErrorResource("Resource", "title", "Pattern", "invalid format"),
                new FieldErrorResource("Resource", "body", "NotBlank", "can't be empty")));

    String json = objectMapper.writeValueAsString(error);

    assertTrue(json.startsWith("{\"errors\":{"));
    assertTrue(json.contains("\"title\":[\"can't be empty\",\"invalid format\"]"));
    assertTrue(json.contains("\"body\":[\"can't be empty\"]"));
    assertTrue(json.endsWith("}}"));
  }

  @Test
  public void should_serialize_empty_errors() throws Exception {
    ErrorResource error = new ErrorResource(Collections.emptyList());

    String json = objectMapper.writeValueAsString(error);

    assertTrue(json.equals("{\"errors\":{}}"));
  }
}
