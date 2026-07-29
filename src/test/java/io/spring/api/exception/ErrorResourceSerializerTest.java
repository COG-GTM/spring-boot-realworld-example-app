package io.spring.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.JacksonCustomizations;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ErrorResourceSerializerTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JacksonCustomizations.RealWorldModules());
  }

  @Test
  public void should_serialize_a_single_field_error() throws Exception {
    ErrorResource errorResource =
        new ErrorResource(
            Collections.singletonList(
                new FieldErrorResource("registerParam", "email", "NotBlank", "can't be empty")));

    assertThat(objectMapper.writeValueAsString(errorResource))
        .isEqualTo("{\"errors\":{\"email\":[\"can't be empty\"]}}");
  }

  @Test
  public void should_group_multiple_messages_of_the_same_field() throws Exception {
    ErrorResource errorResource =
        new ErrorResource(
            Arrays.asList(
                new FieldErrorResource("registerParam", "email", "NotBlank", "can't be empty"),
                new FieldErrorResource("registerParam", "email", "Email", "should be an email")));

    assertThat(objectMapper.writeValueAsString(errorResource))
        .isEqualTo("{\"errors\":{\"email\":[\"can't be empty\",\"should be an email\"]}}");
  }

  @Test
  public void should_serialize_errors_of_several_fields() throws Exception {
    ErrorResource errorResource =
        new ErrorResource(
            Arrays.asList(
                new FieldErrorResource("registerParam", "email", "Email", "should be an email"),
                new FieldErrorResource("registerParam", "username", "NotBlank", "can't be empty"),
                new FieldErrorResource("registerParam", "username", "Duplicated", "is taken")));

    JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(errorResource));

    assertThat(json.fieldNames()).toIterable().containsExactly("errors");
    JsonNode errors = json.get("errors");
    assertThat(errors.fieldNames()).toIterable().containsExactlyInAnyOrder("email", "username");
    assertThat(errors.get("email").isArray()).isTrue();
    assertThat(errors.get("email").get(0).asText()).isEqualTo("should be an email");
    assertThat(errors.get("username").size()).isEqualTo(2);
    assertThat(errors.get("username").get(0).asText()).isEqualTo("can't be empty");
    assertThat(errors.get("username").get(1).asText()).isEqualTo("is taken");
  }

  @Test
  public void should_serialize_an_empty_error_list_to_an_empty_errors_object() throws Exception {
    assertThat(objectMapper.writeValueAsString(new ErrorResource(Collections.emptyList())))
        .isEqualTo("{\"errors\":{}}");
  }

  @Test
  public void should_keep_the_field_error_details_available_on_the_resource() {
    FieldErrorResource fieldError =
        new FieldErrorResource("registerParam", "email", "NotBlank", "can't be empty");
    ErrorResource errorResource = new ErrorResource(Collections.singletonList(fieldError));

    assertThat(errorResource.getFieldErrors()).containsExactly(fieldError);
    assertThat(fieldError.getResource()).isEqualTo("registerParam");
    assertThat(fieldError.getField()).isEqualTo("email");
    assertThat(fieldError.getCode()).isEqualTo("NotBlank");
    assertThat(fieldError.getMessage()).isEqualTo("can't be empty");
  }
}
