package io.spring.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class ErrorResourceSerializerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void should_group_messages_of_the_same_field() throws Exception {
    ErrorResource errorResource =
        new ErrorResource(
            Arrays.asList(
                new FieldErrorResource("article", "title", "EMPTY", "can't be empty"),
                new FieldErrorResource("article", "title", "LENGTH", "is too short"),
                new FieldErrorResource("article", "body", "EMPTY", "can't be empty")));

    String json = objectMapper.writeValueAsString(errorResource);

    assertThat(objectMapper.readTree(json).get("errors").get("title"))
        .hasToString("[\"can't be empty\",\"is too short\"]");
    assertThat(objectMapper.readTree(json).get("errors").get("body"))
        .hasToString("[\"can't be empty\"]");
  }

  @Test
  public void should_serialize_empty_errors_object_when_there_is_no_field_error() throws Exception {
    String json = objectMapper.writeValueAsString(new ErrorResource(Collections.emptyList()));

    assertThat(json).isEqualTo("{\"errors\":{}}");
  }
}
