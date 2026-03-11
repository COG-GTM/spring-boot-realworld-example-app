package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ErrorResourceSerializerTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    objectMapper = new ObjectMapper();
  }

  @Test
  public void should_serialize_single_field_error() throws JsonProcessingException {
    FieldErrorResource fieldError =
        new FieldErrorResource("user", "email", "NotBlank", "must not be blank");
    ErrorResource errorResource = new ErrorResource(Arrays.asList(fieldError));

    String json = objectMapper.writeValueAsString(errorResource);

    assertTrue(json.contains("\"errors\""));
    assertTrue(json.contains("\"email\""));
    assertTrue(json.contains("must not be blank"));
  }

  @Test
  public void should_serialize_multiple_field_errors() throws JsonProcessingException {
    List<FieldErrorResource> fieldErrors =
        Arrays.asList(
            new FieldErrorResource("user", "email", "NotBlank", "must not be blank"),
            new FieldErrorResource("user", "username", "Size", "size must be between 1 and 50"));
    ErrorResource errorResource = new ErrorResource(fieldErrors);

    String json = objectMapper.writeValueAsString(errorResource);

    assertTrue(json.contains("\"errors\""));
    assertTrue(json.contains("\"email\""));
    assertTrue(json.contains("\"username\""));
    assertTrue(json.contains("must not be blank"));
    assertTrue(json.contains("size must be between 1 and 50"));
  }

  @Test
  public void should_serialize_empty_field_errors() throws JsonProcessingException {
    ErrorResource errorResource = new ErrorResource(Collections.emptyList());

    String json = objectMapper.writeValueAsString(errorResource);

    assertTrue(json.contains("\"errors\""));
  }

  @Test
  public void should_group_multiple_errors_for_same_field() throws JsonProcessingException {
    List<FieldErrorResource> fieldErrors =
        Arrays.asList(
            new FieldErrorResource("user", "email", "NotBlank", "must not be blank"),
            new FieldErrorResource("user", "email", "Email", "must be a valid email"));
    ErrorResource errorResource = new ErrorResource(fieldErrors);

    String json = objectMapper.writeValueAsString(errorResource);

    assertTrue(json.contains("\"errors\""));
    assertTrue(json.contains("\"email\""));
    assertTrue(json.contains("must not be blank"));
    assertTrue(json.contains("must be a valid email"));
  }

  @Test
  public void should_serialize_field_error_with_special_characters() throws JsonProcessingException {
    FieldErrorResource fieldError =
        new FieldErrorResource("user", "email", "Pattern", "must match \"^[a-z]+$\"");
    ErrorResource errorResource = new ErrorResource(Arrays.asList(fieldError));

    String json = objectMapper.writeValueAsString(errorResource);

    assertTrue(json.contains("\"errors\""));
    assertTrue(json.contains("\"email\""));
  }

  @Test
  public void should_produce_valid_json_structure() throws JsonProcessingException {
    FieldErrorResource fieldError =
        new FieldErrorResource("user", "email", "NotBlank", "error message");
    ErrorResource errorResource = new ErrorResource(Arrays.asList(fieldError));

    String json = objectMapper.writeValueAsString(errorResource);

    assertTrue(json.startsWith("{"));
    assertTrue(json.endsWith("}"));
    assertTrue(json.contains("\"errors\":{"));
  }
}
