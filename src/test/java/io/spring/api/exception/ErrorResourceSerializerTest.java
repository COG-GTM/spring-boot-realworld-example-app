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
  public void should_group_messages_of_the_same_field_into_one_array() throws Exception {
    ErrorResource errorResource =
        new ErrorResource(
            Arrays.asList(
                new FieldErrorResource("user", "email", "NotBlank", "can't be empty"),
                new FieldErrorResource("user", "email", "Email", "should be an email"),
                new FieldErrorResource("user", "password", "Size", "too short")));

    JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(errorResource));

    JsonNode errors = node.get("errors");
    assertThat(errors).isNotNull();
    assertThat(errors.get("email").isArray()).isTrue();
    assertThat(errors.get("email").size()).isEqualTo(2);
    assertThat(errors.get("email").get(0).asText()).isEqualTo("can't be empty");
    assertThat(errors.get("email").get(1).asText()).isEqualTo("should be an email");
    assertThat(errors.get("password").size()).isEqualTo(1);
    assertThat(errors.get("password").get(0).asText()).isEqualTo("too short");
  }

  @Test
  public void should_serialize_single_field_error() throws Exception {
    ErrorResource errorResource =
        new ErrorResource(
            Collections.singletonList(
                new FieldErrorResource("article", "title", "NotBlank", "can't be empty")));

    String json = objectMapper.writeValueAsString(errorResource);

    assertThat(json).isEqualTo("{\"errors\":{\"title\":[\"can't be empty\"]}}");
  }

  @Test
  public void should_serialize_empty_errors_object_when_there_is_no_field_error() throws Exception {
    ErrorResource errorResource = new ErrorResource(Collections.emptyList());

    String json = objectMapper.writeValueAsString(errorResource);

    assertThat(json).isEqualTo("{\"errors\":{}}");
  }

  @Test
  public void should_keep_field_error_resource_properties() {
    FieldErrorResource fieldErrorResource =
        new FieldErrorResource("user", "email", "NotBlank", "can't be empty");

    ErrorResource errorResource = new ErrorResource(Collections.singletonList(fieldErrorResource));

    assertThat(errorResource.getFieldErrors()).containsExactly(fieldErrorResource);
    assertThat(fieldErrorResource.getResource()).isEqualTo("user");
    assertThat(fieldErrorResource.getField()).isEqualTo("email");
    assertThat(fieldErrorResource.getCode()).isEqualTo("NotBlank");
    assertThat(fieldErrorResource.getMessage()).isEqualTo("can't be empty");
  }
}
