package io.spring.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ErrorResourceSerializerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void should_serialize_single_field_error_as_errors_object() throws Exception {
    ErrorResource errorResource =
        new ErrorResource(
            Collections.singletonList(
                new FieldErrorResource("user", "email", "NotBlank", "can't be empty")));

    String json = objectMapper.writeValueAsString(errorResource);

    assertThat(json).isEqualTo("{\"errors\":{\"email\":[\"can't be empty\"]}}");
  }

  @Test
  void should_group_multiple_messages_under_the_same_field() throws Exception {
    ErrorResource errorResource =
        new ErrorResource(
            Arrays.asList(
                new FieldErrorResource("user", "email", "NotBlank", "can't be empty"),
                new FieldErrorResource("user", "email", "Email", "should be an email"),
                new FieldErrorResource("user", "username", "NotBlank", "can't be empty")));

    String json = objectMapper.writeValueAsString(errorResource);

    assertThat(objectMapper.readTree(json).get("errors").get("email"))
        .hasSize(2)
        .anySatisfy(node -> assertThat(node.asText()).isEqualTo("can't be empty"))
        .anySatisfy(node -> assertThat(node.asText()).isEqualTo("should be an email"));
    assertThat(objectMapper.readTree(json).get("errors").get("username")).hasSize(1);
  }

  @Test
  void should_serialize_empty_errors_object_when_there_is_no_field_error() throws Exception {
    String json = objectMapper.writeValueAsString(new ErrorResource(Collections.emptyList()));

    assertThat(json).isEqualTo("{\"errors\":{}}");
  }
}
