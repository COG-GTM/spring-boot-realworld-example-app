package io.spring;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class JacksonCustomizationsTest {

  @Test
  public void should_serialize_date_time_as_iso8601_utc() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JacksonCustomizations.RealWorldModules());

    DateTime dateTime = new DateTime(2020, 3, 4, 5, 6, 7, 890, DateTimeZone.UTC);

    assertThat(mapper.writeValueAsString(dateTime)).isEqualTo("\"2020-03-04T05:06:07.890Z\"");
  }

  @Test
  public void should_serialize_date_time_converted_to_utc() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JacksonCustomizations.RealWorldModules());

    DateTime dateTime = new DateTime(2020, 3, 4, 5, 0, 0, 0, DateTimeZone.forOffsetHours(2));

    assertThat(mapper.writeValueAsString(dateTime)).isEqualTo("\"2020-03-04T03:00:00.000Z\"");
  }

  @Test
  public void should_serialize_null_date_time_as_json_null() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JacksonCustomizations.RealWorldModules());

    assertThat(mapper.writeValueAsString(new DateTimeHolder(null))).isEqualTo("{\"time\":null}");
  }

  @Test
  public void should_provide_module_bean() {
    Module module = new JacksonCustomizations().realWorldModules();
    assertThat(module).isInstanceOf(JacksonCustomizations.RealWorldModules.class);
  }

  public static class DateTimeHolder {
    private final DateTime time;

    public DateTimeHolder(DateTime time) {
      this.time = time;
    }

    public DateTime getTime() {
      return time;
    }
  }
}
