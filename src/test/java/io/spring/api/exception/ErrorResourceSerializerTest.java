package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

public class ErrorResourceSerializerTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    objectMapper = new ObjectMapper();
  }

  @Test
  public void should_serialize_single_field_error() throws Exception {
    ErrorResource errorResource =
        new ErrorResource(
            Collections.singletonList(
                new FieldErrorResource("registerParam", "email", "Email", "should be an email")));

    assertJsonEquals("{\"errors\":{\"email\":[\"should be an email\"]}}", errorResource);
  }

  @Test
  public void should_group_multiple_messages_of_the_same_field_into_one_list() throws Exception {
    ErrorResource errorResource =
        new ErrorResource(
            Arrays.asList(
                new FieldErrorResource("loginParam", "email", "NotBlank", "can't be empty"),
                new FieldErrorResource("loginParam", "email", "Email", "should be an email")));

    assertJsonEquals(
        "{\"errors\":{\"email\":[\"can't be empty\",\"should be an email\"]}}", errorResource);
  }

  @Test
  public void should_keep_errors_of_different_fields_separated() throws Exception {
    ErrorResource errorResource =
        new ErrorResource(
            Arrays.asList(
                new FieldErrorResource("loginParam", "email", "NotBlank", "can't be empty"),
                new FieldErrorResource("loginParam", "password", "NotBlank", "can't be empty"),
                new FieldErrorResource("loginParam", "email", "Email", "should be an email")));

    assertJsonEquals(
        "{\"errors\":{"
            + "\"email\":[\"can't be empty\",\"should be an email\"],"
            + "\"password\":[\"can't be empty\"]}}",
        errorResource);
  }

  @Test
  public void should_serialize_empty_errors_object_when_no_field_error() throws Exception {
    assertJsonEquals("{\"errors\":{}}", new ErrorResource(Collections.emptyList()));
  }

  @Test
  public void should_finish_the_json_document_when_writing_a_message_fails() throws Exception {
    JsonGenerator generator = mock(JsonGenerator.class);
    doThrow(new IOException("boom")).when(generator).writeString(anyString());

    new ErrorResourceSerializer()
        .serialize(
            new ErrorResource(
                Collections.singletonList(
                    new FieldErrorResource("loginParam", "email", "NotBlank", "can't be empty"))),
            generator,
            null);

    InOrder inOrder = inOrder(generator);
    inOrder.verify(generator).writeStartObject();
    inOrder.verify(generator).writeObjectFieldStart("errors");
    inOrder.verify(generator).writeArrayFieldStart("email");
    inOrder.verify(generator).writeString("can't be empty");
    inOrder.verify(generator).writeEndArray();
    inOrder.verify(generator, times(2)).writeEndObject();
  }

  private void assertJsonEquals(String expected, ErrorResource actual) throws Exception {
    assertEquals(objectMapper.readTree(expected), objectMapper.readTree(writeAsString(actual)));
  }

  private String writeAsString(ErrorResource errorResource) throws Exception {
    return objectMapper.writeValueAsString(errorResource);
  }
}
