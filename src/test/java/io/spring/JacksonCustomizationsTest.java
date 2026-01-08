package io.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JacksonCustomizationsTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JacksonCustomizations.RealWorldModules());
  }

  @Test
  public void should_serialize_instant_in_iso_format_with_utc() throws JsonProcessingException {
    Instant instant = Instant.parse("2025-12-19T17:42:05.403Z");
    String json = objectMapper.writeValueAsString(instant);
    assertEquals("\"2025-12-19T17:42:05.403Z\"", json);
  }

  @Test
  public void should_serialize_instant_with_milliseconds() throws JsonProcessingException {
    Instant instant = Instant.ofEpochMilli(1734629525403L);
    String json = objectMapper.writeValueAsString(instant);
    assertTrue(json.contains("."));
    assertTrue(json.endsWith("Z\""));
  }

  @Test
  public void should_serialize_instant_with_zero_milliseconds() throws JsonProcessingException {
    Instant instant = Instant.ofEpochSecond(1734629525);
    String json = objectMapper.writeValueAsString(instant);
    assertEquals("\"2024-12-19T17:32:05.000Z\"", json);
  }

  @Test
  public void should_serialize_object_with_instant_field() throws JsonProcessingException {
    TestObject obj = new TestObject();
    obj.createdAt = Instant.parse("2025-01-01T00:00:00.000Z");
    String json = objectMapper.writeValueAsString(obj);
    assertTrue(json.contains("\"createdAt\":\"2025-01-01T00:00:00.000Z\""));
  }

  @Test
  public void should_serialize_null_instant_as_null() throws JsonProcessingException {
    TestObject obj = new TestObject();
    obj.createdAt = null;
    String json = objectMapper.writeValueAsString(obj);
    assertTrue(json.contains("\"createdAt\":null"));
  }

  static class TestObject {
    public Instant createdAt;
  }
}
