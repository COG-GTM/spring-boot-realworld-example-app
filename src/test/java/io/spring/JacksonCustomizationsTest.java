package io.spring;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
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
  public void should_expose_module_bean() {
    Module module = new JacksonCustomizations().realWorldModules();
    assertThat(module, is(notNullValue()));
  }

  @Test
  public void should_serialize_datetime_as_iso_utc() throws Exception {
    DateTime time = new DateTime(2020, 1, 2, 3, 4, 5, DateTimeZone.UTC);
    String json = objectMapper.writeValueAsString(time);
    String expected = ISODateTimeFormat.dateTime().withZoneUTC().print(time);
    assertThat(json, is("\"" + expected + "\""));
    assertThat(json, containsString("2020-01-02"));
  }

  @Test
  public void should_serialize_null_datetime_as_null() throws Exception {
    DateTime time = null;
    String json = objectMapper.writeValueAsString(time);
    assertThat(json, is("null"));
  }
}
