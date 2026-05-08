package io.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.spring.JacksonCustomizations.DateTimeSerializer;
import java.io.IOException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class JacksonCustomizationsTest {

  @Test
  public void serialize_should_write_iso_8601_utc_for_known_datetime() throws IOException {
    DateTime value = new DateTime(2021, 6, 15, 12, 30, 45, 0, DateTimeZone.UTC);
    JsonGenerator gen = mock(JsonGenerator.class);
    SerializerProvider provider = mock(SerializerProvider.class);

    DateTimeSerializer serializer = new DateTimeSerializer();
    serializer.serialize(value, gen, provider);

    verify(gen, times(1)).writeString("2021-06-15T12:30:45.000Z");
  }

  @Test
  public void serialize_should_convert_non_utc_zone_to_utc() throws IOException {
    DateTime value = new DateTime(2021, 6, 15, 12, 0, 0, 0, DateTimeZone.forOffsetHours(2));
    JsonGenerator gen = mock(JsonGenerator.class);
    SerializerProvider provider = mock(SerializerProvider.class);

    DateTimeSerializer serializer = new DateTimeSerializer();
    serializer.serialize(value, gen, provider);

    verify(gen, times(1)).writeString("2021-06-15T10:00:00.000Z");
  }

  @Test
  public void serialize_should_write_null_when_value_is_null() throws IOException {
    JsonGenerator gen = mock(JsonGenerator.class);
    SerializerProvider provider = mock(SerializerProvider.class);

    DateTimeSerializer serializer = new DateTimeSerializer();
    serializer.serialize(null, gen, provider);

    verify(gen, times(1)).writeNull();
  }

  @Test
  public void real_world_modules_should_register_datetime_serializer() {
    JacksonCustomizations customizations = new JacksonCustomizations();
    com.fasterxml.jackson.databind.Module module = customizations.realWorldModules();
    assertEquals(JacksonCustomizations.RealWorldModules.class, module.getClass());
  }
}
