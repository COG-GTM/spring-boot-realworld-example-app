package io.spring.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class ErrorResourceSerializerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void should_group_messages_by_field() throws Exception {
    ErrorResource errorResource =
        new ErrorResource(
            Arrays.asList(
                new FieldErrorResource("user", "email", "INVALID", "can't be empty"),
                new FieldErrorResource("user", "email", "DUPLICATED", "already exist"),
                new FieldErrorResource("user", "username", "INVALID", "can't be empty")));

    JsonNode errors =
        objectMapper.readTree(objectMapper.writeValueAsString(errorResource)).get("errors");

    assertEquals(2, errors.get("email").size());
    assertEquals("can't be empty", errors.get("email").get(0).asText());
    assertEquals("already exist", errors.get("email").get(1).asText());
    assertEquals(1, errors.get("username").size());
    assertEquals("can't be empty", errors.get("username").get(0).asText());
  }
}
