package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.JacksonCustomizations;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

public class ErrorResourceSerializerTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JacksonCustomizations.RealWorldModules());
  }

  private List<FieldErrorResource> toFieldErrorResources(List<FieldError> fieldErrors) {
    return fieldErrors.stream()
        .map(
            fieldError ->
                new FieldErrorResource(
                    fieldError.getObjectName(),
                    fieldError.getField(),
                    fieldError.getCode(),
                    fieldError.getDefaultMessage()))
        .collect(Collectors.toList());
  }

  private List<String> messagesOf(JsonNode node) {
    return StreamSupport.stream(node.spliterator(), false)
        .map(JsonNode::asText)
        .collect(Collectors.toList());
  }

  @Test
  public void should_group_messages_by_field_name() throws Exception {
    BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "user");
    bindingResult.addError(new FieldError("user", "email", "can't be empty"));
    bindingResult.addError(new FieldError("user", "username", "can't be empty"));

    ErrorResource errorResource =
        new ErrorResource(toFieldErrorResources(bindingResult.getFieldErrors()));

    JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(errorResource));

    JsonNode errors = json.get("errors");
    assertEquals(2, errors.size());
    assertEquals(Arrays.asList("can't be empty"), messagesOf(errors.get("email")));
    assertEquals(Arrays.asList("can't be empty"), messagesOf(errors.get("username")));
  }

  @Test
  public void should_collect_multiple_messages_for_the_same_field() throws Exception {
    BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "user");
    bindingResult.addError(new FieldError("user", "email", "can't be empty"));
    bindingResult.addError(new FieldError("user", "email", "should be an email"));
    bindingResult.addError(new FieldError("user", "password", "can't be empty"));

    ErrorResource errorResource =
        new ErrorResource(toFieldErrorResources(bindingResult.getFieldErrors()));

    JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(errorResource));

    JsonNode errors = json.get("errors");
    assertEquals(2, errors.size());
    assertEquals(
        Arrays.asList("can't be empty", "should be an email"), messagesOf(errors.get("email")));
    assertEquals(Arrays.asList("can't be empty"), messagesOf(errors.get("password")));
  }

  @Test
  public void should_serialize_empty_errors_object_for_empty_error_list() throws Exception {
    ErrorResource errorResource = new ErrorResource(new ArrayList<>());

    String json = objectMapper.writeValueAsString(errorResource);

    assertEquals("{\"errors\":{}}", json);
    assertTrue(objectMapper.readTree(json).get("errors").isEmpty());
  }

  @Test
  public void should_keep_the_message_and_ignore_resource_and_code() throws Exception {
    ErrorResource errorResource =
        new ErrorResource(
            Arrays.asList(new FieldErrorResource("user", "email", "NotBlank", "can't be empty")));

    String json = objectMapper.writeValueAsString(errorResource);

    assertEquals("{\"errors\":{\"email\":[\"can't be empty\"]}}", json);
  }
}
