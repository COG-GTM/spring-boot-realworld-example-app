package io.spring.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class ErrorResourceSerializerTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void should_group_messages_by_field() throws Exception {
    ErrorResource errorResource =
        new ErrorResource(
            Arrays.asList(
                new FieldErrorResource("user", "email", "NotBlank", "can't be empty"),
                new FieldErrorResource("user", "email", "Email", "should be an email"),
                new FieldErrorResource("user", "username", "NotBlank", "can't be empty")));

    JsonNode node = mapper.readTree(mapper.writeValueAsString(errorResource));

    assertThat(node.get("errors").get("email")).hasSize(2);
    assertThat(node.get("errors").get("email").get(0).asText()).isEqualTo("can't be empty");
    assertThat(node.get("errors").get("email").get(1).asText()).isEqualTo("should be an email");
    assertThat(node.get("errors").get("username")).hasSize(1);
    assertThat(node.get("errors").get("username").get(0).asText()).isEqualTo("can't be empty");
  }

  @Test
  public void should_serialize_empty_errors_object() throws Exception {
    ErrorResource errorResource = new ErrorResource(Collections.emptyList());

    JsonNode node = mapper.readTree(mapper.writeValueAsString(errorResource));

    assertThat(node.get("errors").isObject()).isTrue();
    assertThat(node.get("errors")).isEmpty();
  }

  @Test
  public void should_expose_field_errors() {
    FieldErrorResource fieldError =
        new FieldErrorResource("user", "email", "NotBlank", "can't be empty");
    ErrorResource errorResource = new ErrorResource(Collections.singletonList(fieldError));

    assertThat(errorResource.getFieldErrors()).containsExactly(fieldError);
    assertThat(fieldError.getResource()).isEqualTo("user");
    assertThat(fieldError.getField()).isEqualTo("email");
    assertThat(fieldError.getCode()).isEqualTo("NotBlank");
    assertThat(fieldError.getMessage()).isEqualTo("can't be empty");
  }
}
