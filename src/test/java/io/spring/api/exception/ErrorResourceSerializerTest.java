package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class ErrorResourceSerializerTest {

  @Test
  void should_serialize_error_resource() throws Exception {
    ErrorResourceSerializer serializer = new ErrorResourceSerializer();
    FieldErrorResource fieldError =
        new FieldErrorResource("User", "email", "NotBlank", "must not be blank");
    ErrorResource errorResource = new ErrorResource(Arrays.asList(fieldError));

    StringWriter writer = new StringWriter();
    JsonGenerator gen = new JsonFactory().createGenerator(writer);
    SerializerProvider provider = new ObjectMapper().getSerializerProvider();

    serializer.serialize(errorResource, gen, provider);
    gen.flush();

    String json = writer.toString();
    assertTrue(json.contains("errors"));
    assertTrue(json.contains("email"));
    assertTrue(json.contains("must not be blank"));
  }

  @Test
  void should_serialize_empty_error_resource() throws Exception {
    ErrorResourceSerializer serializer = new ErrorResourceSerializer();
    ErrorResource errorResource = new ErrorResource(Collections.emptyList());

    StringWriter writer = new StringWriter();
    JsonGenerator gen = new JsonFactory().createGenerator(writer);
    SerializerProvider provider = new ObjectMapper().getSerializerProvider();

    serializer.serialize(errorResource, gen, provider);
    gen.flush();

    String json = writer.toString();
    assertTrue(json.contains("errors"));
  }

  @Test
  void should_group_errors_by_field() throws Exception {
    ErrorResourceSerializer serializer = new ErrorResourceSerializer();
    ErrorResource errorResource =
        new ErrorResource(
            Arrays.asList(
                new FieldErrorResource("User", "email", "NotBlank", "must not be blank"),
                new FieldErrorResource("User", "email", "Email", "must be valid email")));

    StringWriter writer = new StringWriter();
    JsonGenerator gen = new JsonFactory().createGenerator(writer);
    SerializerProvider provider = new ObjectMapper().getSerializerProvider();

    serializer.serialize(errorResource, gen, provider);
    gen.flush();

    String json = writer.toString();
    assertTrue(json.contains("must not be blank"));
    assertTrue(json.contains("must be valid email"));
  }
}
