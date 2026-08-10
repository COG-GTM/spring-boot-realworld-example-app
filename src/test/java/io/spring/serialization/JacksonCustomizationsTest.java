package io.spring.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.JacksonCustomizations;
import java.io.StringWriter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

class JacksonCustomizationsTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper().registerModule(new JacksonCustomizations.RealWorldModules());

  @Test
  void should_expose_real_world_modules_bean() {
    Module module = new JacksonCustomizations().realWorldModules();

    assertThat(module).isInstanceOf(JacksonCustomizations.RealWorldModules.class);
  }

  @Test
  void should_serialize_date_time_as_iso_utc_string() throws Exception {
    DateTime dateTime = new DateTime(2020, 3, 1, 12, 30, 45, 123, DateTimeZone.UTC);

    String json = objectMapper.writeValueAsString(new Holder(dateTime));

    assertThat(json).isEqualTo("{\"createdAt\":\"2020-03-01T12:30:45.123Z\"}");
  }

  @Test
  void should_convert_non_utc_date_time_to_utc_when_serializing() throws Exception {
    DateTime dateTime = new DateTime(2020, 3, 1, 12, 0, 0, 0, DateTimeZone.forOffsetHours(8));

    String json = objectMapper.writeValueAsString(new Holder(dateTime));

    assertThat(json).isEqualTo("{\"createdAt\":\"2020-03-01T04:00:00.000Z\"}");
  }

  @Test
  void should_write_null_when_date_time_value_is_null() throws Exception {
    StringWriter writer = new StringWriter();
    JsonGenerator generator = objectMapper.getFactory().createGenerator(writer);
    generator.writeStartObject();
    generator.writeFieldName("createdAt");

    new NullSafeDateTimeSerializer().serialize(null, generator, null);

    generator.writeEndObject();
    generator.close();

    assertThat(writer.toString()).isEqualTo("{\"createdAt\":null}");
  }

  private static class NullSafeDateTimeSerializer
      extends JacksonCustomizations.DateTimeSerializer {}

  public static class Holder {
    private final DateTime createdAt;

    Holder(DateTime createdAt) {
      this.createdAt = createdAt;
    }

    public DateTime getCreatedAt() {
      return createdAt;
    }
  }
}
