package io.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Assertions;
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
  public void should_serialize_instant_to_iso8601_string() throws Exception {
    Instant instant = Instant.parse("2024-01-15T12:34:56Z");
    String json = objectMapper.writeValueAsString(instant);
    Assertions.assertEquals("\"2024-01-15T12:34:56Z\"", json);
  }

  @Test
  public void should_serialize_null_instant_as_null() throws Exception {
    String json = objectMapper.writeValueAsString(new InstantHolder(null));
    Assertions.assertTrue(json.contains("\"value\":null"));
  }

  @Test
  public void should_serialize_instant_with_fractional_seconds() throws Exception {
    Instant instant = Instant.parse("2024-01-15T12:34:56.789Z");
    String json = objectMapper.writeValueAsString(instant);
    Assertions.assertEquals("\"2024-01-15T12:34:56.789Z\"", json);
  }

  static class InstantHolder {
    private final Instant value;

    InstantHolder(Instant value) {
      this.value = value;
    }

    public Instant getValue() {
      return value;
    }
  }
}
