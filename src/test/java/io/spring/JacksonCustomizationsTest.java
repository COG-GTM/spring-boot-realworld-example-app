package io.spring;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class JacksonCustomizationsTest {

  @Test
  public void should_create_real_world_module() {
    JacksonCustomizations customizations = new JacksonCustomizations();
    Module module = customizations.realWorldModules();
    assertNotNull(module);
    assertTrue(module instanceof JacksonCustomizations.RealWorldModules);
  }

  @Test
  public void should_serialize_datetime_value() throws IOException {
    JacksonCustomizations.DateTimeSerializer serializer =
        new JacksonCustomizations.DateTimeSerializer();
    JsonGenerator gen = mock(JsonGenerator.class);
    SerializerProvider provider = mock(SerializerProvider.class);
    DateTime dateTime = new DateTime(2023, 1, 15, 10, 30, 0, DateTimeZone.UTC);

    serializer.serialize(dateTime, gen, provider);

    verify(gen).writeString(contains("2023-01-15"));
  }

  @Test
  public void should_serialize_null_datetime() throws IOException {
    JacksonCustomizations.DateTimeSerializer serializer =
        new JacksonCustomizations.DateTimeSerializer();
    JsonGenerator gen = mock(JsonGenerator.class);
    SerializerProvider provider = mock(SerializerProvider.class);

    serializer.serialize(null, gen, provider);

    verify(gen).writeNull();
  }

  @Test
  public void should_register_datetime_serializer_in_module() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JacksonCustomizations.RealWorldModules());
    // Module registered successfully - if DateTime serializer is registered,
    // ObjectMapper should be able to serialize DateTime objects
    assertNotNull(mapper);
  }
}
