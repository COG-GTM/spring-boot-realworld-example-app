package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ErrorResourceSerializerTest {

  private ErrorResourceSerializer serializer;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    serializer = new ErrorResourceSerializer();
    objectMapper = new ObjectMapper();
  }

  @Test
  void serialize_single_field_error() throws Exception {
    FieldErrorResource fieldError =
        new FieldErrorResource("article", "title", "NotBlank", "can't be empty");
    ErrorResource errorResource = new ErrorResource(Collections.singletonList(fieldError));

    String json = serializeToJson(errorResource);

    assertEquals("{\"errors\":{\"title\":[\"can't be empty\"]}}", json);
  }

  @Test
  void serialize_multiple_errors_on_same_field_should_be_grouped() throws Exception {
    List<FieldErrorResource> fieldErrors =
        Arrays.asList(
            new FieldErrorResource("article", "title", "NotBlank", "can't be empty"),
            new FieldErrorResource("article", "title", "Size", "too short"));
    ErrorResource errorResource = new ErrorResource(fieldErrors);

    String json = serializeToJson(errorResource);

    assertTrue(json.contains("\"errors\""));
    assertTrue(json.contains("\"title\""));
    assertTrue(json.contains("can't be empty"));
    assertTrue(json.contains("too short"));
    int titleCount = json.split("\"title\"").length - 1;
    assertEquals(1, titleCount);
  }

  @Test
  void serialize_errors_on_different_fields() throws Exception {
    List<FieldErrorResource> fieldErrors =
        Arrays.asList(
            new FieldErrorResource("article", "title", "NotBlank", "can't be empty"),
            new FieldErrorResource("article", "body", "NotBlank", "can't be empty"));
    ErrorResource errorResource = new ErrorResource(fieldErrors);

    String json = serializeToJson(errorResource);

    assertTrue(json.contains("\"title\""));
    assertTrue(json.contains("\"body\""));
    assertTrue(json.contains("\"errors\""));
  }

  private String serializeToJson(ErrorResource errorResource) throws Exception {
    StringWriter writer = new StringWriter();
    JsonGenerator gen = new JsonFactory().createGenerator(writer);
    SerializerProvider provider = objectMapper.getSerializerProvider();
    serializer.serialize(errorResource, gen, provider);
    gen.flush();
    return writer.toString();
  }
}
