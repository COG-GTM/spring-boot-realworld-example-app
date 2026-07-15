package io.spring.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class ErrorResourceSerializerTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void should_group_multiple_messages_by_field() throws Exception {
    ErrorResource errorResource =
        new ErrorResource(
            Arrays.asList(
                new FieldErrorResource("user", "email", "Email", "must be a valid email"),
                new FieldErrorResource("user", "email", "Unique", "has already been taken"),
                new FieldErrorResource("user", "username", "NotBlank", "can't be empty")));

    JsonNode actual = objectMapper.readTree(objectMapper.writeValueAsString(errorResource));
    JsonNode expected =
        objectMapper.readTree(
            "{\"errors\":{\"email\":[\"must be a valid email\",\"has already been taken\"],"
                + "\"username\":[\"can't be empty\"]}}");

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void should_serialize_only_field_names_and_messages() throws Exception {
    ErrorResource errorResource =
        new ErrorResource(
            Collections.singletonList(
                new FieldErrorResource("article-command", "title", "NotBlank", "can't be empty")));

    JsonNode actual = objectMapper.readTree(objectMapper.writeValueAsString(errorResource));

    assertThat(actual.path("errors").path("title").get(0).asText()).isEqualTo("can't be empty");
    assertThat(actual.toString()).doesNotContain("article-command", "NotBlank");
  }

  @Test
  public void should_serialize_empty_errors_object_when_there_are_no_field_errors()
      throws Exception {
    ErrorResource errorResource = new ErrorResource(Collections.emptyList());

    JsonNode actual = objectMapper.readTree(objectMapper.writeValueAsString(errorResource));

    assertThat(actual).isEqualTo(objectMapper.readTree("{\"errors\":{}}"));
  }
}
