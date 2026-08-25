package io.spring;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.JacksonCustomizations.DateTimeSerializer;
import io.spring.JacksonCustomizations.RealWorldModules;
import java.io.IOException;
import java.io.StringWriter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class JacksonCustomizationsTest {

  @Test
  public void should_register_datetime_serializer_as_a_module_bean() throws Exception {
    assertThat(new JacksonCustomizations().realWorldModules()).isInstanceOf(RealWorldModules.class);

    ObjectMapper mapper = new ObjectMapper().registerModule(new RealWorldModules());
    DateTime dateTime = new DateTime(2020, 1, 2, 3, 4, 5, 0, DateTimeZone.UTC);

    assertThat(mapper.writeValueAsString(dateTime)).isEqualTo("\"2020-01-02T03:04:05.000Z\"");
  }

  @Test
  public void should_serialize_datetime_in_utc_iso_format() throws IOException {
    DateTime dateTime = new DateTime(2020, 1, 2, 3, 4, 5, 0, DateTimeZone.forOffsetHours(8));

    assertThat(serialize(dateTime)).isEqualTo("\"2020-01-01T19:04:05.000Z\"");
  }

  @Test
  public void should_serialize_null_datetime_as_json_null() throws IOException {
    assertThat(serialize(null)).isEqualTo("null");
  }

  private String serialize(DateTime value) throws IOException {
    StringWriter writer = new StringWriter();
    try (JsonGenerator generator = new JsonFactory().createGenerator(writer)) {
      new DateTimeSerializer().serialize(value, generator, null);
    }
    return writer.toString();
  }
}
