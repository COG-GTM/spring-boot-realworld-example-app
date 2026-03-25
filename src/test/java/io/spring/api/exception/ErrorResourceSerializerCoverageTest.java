package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class ErrorResourceSerializerCoverageTest {

  @Test
  public void should_serialize_error_resource() throws JsonProcessingException {
    FieldErrorResource fieldError =
        new FieldErrorResource("object", "title", "NotBlank", "can't be empty");
    ErrorResource errorResource = new ErrorResource(Arrays.asList(fieldError));

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(errorResource);

    assertNotNull(json);
    assertTrue(json.contains("errors"));
    assertTrue(json.contains("title"));
    assertTrue(json.contains("can't be empty"));
  }

  @Test
  public void should_serialize_empty_errors() throws JsonProcessingException {
    ErrorResource errorResource = new ErrorResource(Collections.emptyList());

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(errorResource);

    assertNotNull(json);
    assertTrue(json.contains("errors"));
  }

  @Test
  public void should_serialize_multiple_errors_same_field() throws JsonProcessingException {
    FieldErrorResource error1 =
        new FieldErrorResource("obj", "email", "NotBlank", "can't be empty");
    FieldErrorResource error2 =
        new FieldErrorResource("obj", "email", "Email", "should be an email");

    ErrorResource errorResource = new ErrorResource(Arrays.asList(error1, error2));

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(errorResource);

    assertNotNull(json);
    assertTrue(json.contains("email"));
    assertTrue(json.contains("can't be empty"));
    assertTrue(json.contains("should be an email"));
  }

  @Test
  public void should_serialize_multiple_fields() throws JsonProcessingException {
    FieldErrorResource error1 =
        new FieldErrorResource("obj", "email", "NotBlank", "can't be empty");
    FieldErrorResource error2 =
        new FieldErrorResource("obj", "username", "NotBlank", "can't be empty");

    ErrorResource errorResource = new ErrorResource(Arrays.asList(error1, error2));

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(errorResource);

    assertNotNull(json);
    assertTrue(json.contains("email"));
    assertTrue(json.contains("username"));
  }
}
